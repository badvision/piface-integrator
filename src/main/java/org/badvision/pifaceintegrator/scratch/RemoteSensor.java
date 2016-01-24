package org.badvision.pifaceintegrator.scratch;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.concurrent.locks.LockSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a remote sensor of the Scratch 1.4 programming environment which
 * can connect to Scratch on a specified host and port an receive broadcast and
 * sensor update messages from in an independent thread. If no host and/or port
 * is specified instances of this class will try to connect to localhost:42001.
 *
 * @see also http://wiki.scratch.mit.edu/wiki/Remote_Sensors_Protocol
 * @see original source: https://github.com/joe-akeem/scratch-java-rsp
 *
 * @author joe
 *
 */
public abstract class RemoteSensor implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(RemoteSensor.class);

    /**
     * Possible states of the internal state machine
     */
    private enum ParserState {
        IDENTIFY_MESSAGE_TYPE, BROADCAST_MESSAGE, READ_VARIABLE_NAME, READ_VARIABLE_VALUE, DONE
    };

    /**
     * The host Scratch is listening on for remote sensor connections.
     */
    private String scratchHost = Constants.SCRATCH_DEFAULT_HOST;

    /**
     * The port Scratch is listening on for remote sensor connections.
     */
    private int scratchPort = Constants.SCRATCH_DEFAULT_PORT;

    /**
     * Creates a RemoteSensor that connects to Scratch using the default host
     * and port.
     */
    public RemoteSensor() {
    }

    /**
     * Creates a RemoteSensor that connects to Scratch using the specified host
     * and port.
     *
     * @param scratchHost - the host Scratch is running on
     * @param scratchPort - the port Scratch is running on
     */
    public RemoteSensor(String scratchHost, int scratchPort) {
        this.scratchHost = scratchHost;
        this.scratchPort = scratchPort;
    }

    Thread connectionManager;

    /**
     * Connects to the remote Scratch instance specified by this instances host
     * and port and starts receiving messages in a new thread. This method will
     * return immediately with the new thread it created.
     */
    public Thread connect() {
        connectionManager = new Thread(this);
        halted = false;
        connectionManager.start();
        return connectionManager;
    }

    protected void halt() throws InterruptedException {
        halted = true;
        if (connectionManager != null && connectionManager.isAlive()) {
            connectionManager.interrupt();
            connectionManager.join();
        }
        connectionManager = null;
    }
    private boolean halted = false;

    /**
     * Connects to the remote Scratch instance specified by this instances host
     * and port and starts receiving messages in loop. When a message is
     * received this instances broadcast and sensor update methods are invoked.
     *
     * The scratch messages are parsed according to the protocol specified here:
     * <a href="http://wiki.scratch.mit.edu/wiki/Remote_Sensors_Protocol">Scratch
     * Remote Sensor Protocol</a>
     */
    @Override
    public void run() {
        try (Socket socket = new Socket(scratchHost, scratchPort);
                InputStream inputStream = socket.getInputStream()) {
            LOG.info("Connected to Scratch. Waiting for incoming messages...");
            byte[] sizeBuf = new byte[4];
            connectionStateChange(true, null);
            while (!halted) {
                socket.setSoTimeout(10000);
                int readCount = inputStream.read(sizeBuf, 0, 4);
                if (halted) {
                    break;
                }
                if (readCount != 4) {
                    throw new IOException("Expected 4 bytes for message size but got " + readCount + " instead.");
                }
                ByteBuffer bb = ByteBuffer.wrap(sizeBuf);
                int messageSize = bb.getInt();
                byte[] messageBuf = new byte[messageSize];
                readCount = inputStream.read(messageBuf, 0, messageSize);
                if (readCount != messageSize) {
                    throw new IOException("Expectes message of size " + messageSize + " bytes but got " + readCount + " instead.");
                }
                String message = new String(messageBuf, "UTF-8");
                parseMessage(message);
            }
            connectionStateChange(false, null);
        } catch (IOException e) {
            connectionStateChange(false, e);
            LOG.error("Error while communicating to Scratch", e);
        }
    }

    /**
     * Parses the transmitted message that follows the 4 byte message length
     * indicator and calls the corresponding broadcast and sensor update
     * methods.
     *
     * @param message
     */
    private void parseMessage(String message) {
        ParserState state = ParserState.IDENTIFY_MESSAGE_TYPE;
        String remainder = message;
        String variableName = null;
        String variableValue;
        while (true) {
            switch (state) {
                case IDENTIFY_MESSAGE_TYPE:
                    String messageType = remainder.substring(0, message.indexOf(' '));
                    remainder = remainder.substring(messageType.length()).trim();
                    switch (messageType) {
                        case Constants.BROADCAST_MESSAGE_TYPE:
                            state = ParserState.BROADCAST_MESSAGE;
                            break;
                        case Constants.SENSOR_UPDATE_MESSAGE_TYPE:
                            state = ParserState.READ_VARIABLE_NAME;
                            break;
                        default:
                            otherMessage(message);
                            state = ParserState.DONE;
                            break;
                    }
                    break;
                case BROADCAST_MESSAGE:
                    String broadcastMsg;
                    if (remainder.startsWith("\"")) {
                        broadcastMsg = remainder.substring(1, remainder.indexOf('"', 1));
                    } else {
                        // according to the protocol this
                        // should not contain any spaces any more...
                        broadcastMsg = remainder;
                    }
                    broadcast(broadcastMsg);
                    state = ParserState.DONE;
                    break;
                case READ_VARIABLE_NAME:
                    variableName = remainder.substring(1, remainder.indexOf('"', 1));
                    remainder = remainder.substring(remainder.indexOf('"', 1) + 1).trim();
                    state = ParserState.READ_VARIABLE_VALUE;
                    break;
                case READ_VARIABLE_VALUE:
                    if (remainder.startsWith("\"")) {
                        variableValue = remainder.substring(1, remainder.indexOf('"', 1));
                        remainder = remainder.substring(remainder.indexOf('"', 1) + 1).trim();
                    } else {
                        int next = remainder.indexOf(' ');
                        if (next != -1) {
                            variableValue = remainder.substring(0, next);
                            remainder = remainder.substring(next).trim();
                        } else {
                            variableValue = remainder.substring(0);
                            remainder = "";
                        }

                    }
                    switchSensorUpdateMessage(variableName, variableValue);
                    if (remainder.length() > 0) {
                        state = ParserState.READ_VARIABLE_NAME;
                    } else {
                        state = ParserState.DONE;
                    }
                    break;
                case DONE:
                    return;
                default:
                    throw new IllegalStateException("Unhandled state " + state);
            }
        }
    }

    private synchronized void switchSensorUpdateMessage(String name, String value) {
        Double doubleValue = null;
        Boolean booleanValue = null;
        String stringValue = null;
        try {
            doubleValue = Double.parseDouble(value);
        } catch (NumberFormatException e) {
            if ("true".equals(value.toLowerCase()) || "false".equals(value.toLowerCase())) {
                booleanValue = Boolean.parseBoolean(value);
            } else {
                stringValue = value;
            }
        }

        if (doubleValue != null) {
            sensorUpdate(name, doubleValue);
        } else if (booleanValue != null) {
            sensorUpdate(name, booleanValue);
        } else {
            sensorUpdate(name, stringValue);
        }
    }

    /**
     * This method is invoked if this RemoteSensor received a broadcast message
     * from Scratch.
     *
     * @param message - the broadcast message that was transmitted by Scratch
     */
    protected abstract void broadcast(String message);

    /**
     * This method is invoked if this RemoteSensor received a sensor update
     * message of type String from Scratch.
     *
     * @param name - the name of the sensor to update
     * @param value - the value to set the sensor to
     */
    protected abstract void sensorUpdate(String name, String value);

    /**
     * This method is invoked if this RemoteSensor received a sensor update
     * message of type Double from Scratch.
     *
     * @param name - the name of the sensor to update
     * @param value - the value to set the sensor to
     */
    protected abstract void sensorUpdate(String name, double value);

    /**
     * This method is invoked if this RemoteSensor received a sensor update
     * message of type Boolean from Scratch.
     *
     * @param name - the name of the sensor to update
     * @param value - the value to set the sensor to
     */
    protected abstract void sensorUpdate(String name, boolean value);

    /**
     * This method is invoked if this RemoteSensor received an unknown message
     * from Scratch.
     *
     * @param message - the complete message as received from Scratch
     */
    protected abstract void otherMessage(String message);

    protected abstract void connectionStateChange(boolean isConnected, Exception error);

    public String getScratchHost() {
        return scratchHost;
    }

    public void setScratchHost(String scratchHost) {
        this.scratchHost = scratchHost;
    }

    public int getScratchPort() {
        return scratchPort;
    }

    public void setScratchPort(int scratchPort) {
        this.scratchPort = scratchPort;
    }

}
