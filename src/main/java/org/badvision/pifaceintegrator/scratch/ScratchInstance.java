package org.badvision.pifaceintegrator.scratch;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Represents a Scratch 1.4 instance to which broadcast messages and sensor
 * updates can be sent.
 *
 * @author joe
 * @see original source: https://github.com/joe-akeem/scratch-java-rsp
 *
 */
public class ScratchInstance {

    private static final Logger LOG = Logger.getLogger(ScratchInstance.class.getName());

    /**
     * The host Scratch is listening on for remote sensor connections.
     */
    private String scratchHost = Constants.SCRATCH_DEFAULT_HOST;

    /**
     * The port Scratch is listening on for remote sensor connections.
     */
    private int scratchPort = Constants.SCRATCH_DEFAULT_PORT;

    /**
     * The socket that is used to connect to Scratch. This will be initialized
     * by the connect() method.
     */
    private Socket socket;

    /**
     * The output stream used to send messages to Scratch. This will be
     * initialized by the connect() method.
     */
    private OutputStream outputStream;

    /**
     * Creates a Scratch14Instance that connects to Scratch using the default
     * host and port.
     */
    public ScratchInstance() {
    }

    /**
     * Creates a Scratch14Instance that connects to Scratch using the specified
     * host and port.
     *
     * @param scratchHost - the host Scratch is running on
     * @param scratchPort - the port Scratch is running on
     */
    public ScratchInstance(String scratchHost, int scratchPort) {
        this.scratchHost = scratchHost;
        this.scratchPort = scratchPort;
    }

    /**
     * Connects to the remote Scratch instance specified by this instances host
     * and port. Once this instance is connected to Scratch it can start sending
     * broadcast messages and sensor updates to Scratch.
     *
     * @throws IOException - if connecting to Scratch fails
     * @throws UnknownHostException - if the specified host is not known
     */
    public void connect() throws UnknownHostException, IOException {
        socket = new Socket(scratchHost, scratchPort);
        outputStream = socket.getOutputStream();
    }

    /**
     * Disconnects from the remote Scratch instance.
     *
     * @throws IOException - if disconnecting fails.
     */
    public void disconnect() throws IOException {
        if (outputStream != null) {
            outputStream.close();
        }
        if (socket != null) {
            socket.close();
        }
    }

    /**
     * Sends a broadcast message to the remote Scratch instance.
     *
     * @param message - the message to be sent to Scratch
     * @throws IOException - if an I/O error occurs.
     */
    public void broadcast(String message) throws IOException {
        String broadcastMessage = Constants.BROADCAST_MESSAGE_TYPE + " \"" + message + "\"";
        byte[] messageSize = ByteBuffer.allocate(4).putInt(broadcastMessage.length()).array();
        outputStream.write(messageSize);
        outputStream.write(broadcastMessage.getBytes(Charset.forName("UTF-8")));
        LOG.log(Level.INFO, "Sent broadcast message '{0}' to Scratch.", message);
    }

    /**
     * Sends a sensor update message to the remote Scratch instance.
     *
     * @param name - the remote variable to update
     * @param value - the value to set the variable to
     * @throws IOException - if an I/O error occurs.
     */
    public void sensorUpdate(String name, String value) throws IOException {
        String sensorUpdateMessage = Constants.SENSOR_UPDATE_MESSAGE_TYPE + " \"" + name + "\" \"" + value + "\"";
        sendSensorUpdateMessage(sensorUpdateMessage);
    }

    /**
     * Sends a sensor update message to the remote Scratch instance.
     *
     * @param name - the remote variable to update
     * @param value - the value to set the variable to
     * @throws IOException - if an I/O error occurs.
     */
    public void sensorUpdate(String name, double value) throws IOException {
        String sensorUpdateMessage = Constants.SENSOR_UPDATE_MESSAGE_TYPE + " \"" + name + "\" " + value;
        sendSensorUpdateMessage(sensorUpdateMessage);
    }

    /**
     * Sends a sensor update message to the remote Scratch instance.
     *
     * @param name - the remote variable to update
     * @param value - the value to set the variable to
     * @throws IOException - if an I/O error occurs.
     */
    public void sensorUpdate(String name, boolean value) throws IOException {
        String sensorUpdateMessage = Constants.SENSOR_UPDATE_MESSAGE_TYPE + " \"" + name + "\" " + value;
        sendSensorUpdateMessage(sensorUpdateMessage);
    }

    private void sendSensorUpdateMessage(String sensorUpdateMessage)
            throws IOException {
        byte[] messageSize = ByteBuffer.allocate(4).putInt(sensorUpdateMessage.length()).array();
        outputStream.write(messageSize);
        outputStream.write(sensorUpdateMessage.getBytes(Charset.forName("UTF-8")));
        LOG.log(Level.INFO, "Sent sensor update '{0}' to Scratch.", sensorUpdateMessage);
    }

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
