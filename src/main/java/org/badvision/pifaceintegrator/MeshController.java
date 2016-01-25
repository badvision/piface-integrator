/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.badvision.pifaceintegrator;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import org.badvision.pifaceintegrator.UserInterface.SystemStatus;
import org.badvision.pifaceintegrator.piface.PifaceConnection;
import org.badvision.pifaceintegrator.scratch.RemoteSensor;
import org.badvision.pifaceintegrator.scratch.ScratchInstance;

/**
 *
 * @author blurry
 */
public class MeshController extends RemoteSensor {

    private UserInterface ui;
    private final BooleanProperty enabledProperty = new SimpleBooleanProperty(true);
    private final Map<String, Integer> allIntVariables = new HashMap<>();
    private final Map<String, String> allStringVariables = new HashMap<>();
    private final ScratchInstance scratch;

    private MeshController() {
        // Must initalize with host/port
        scratch = null;
    }

    public MeshController(String host, int port) throws IOException {
        super(host, port);
        scratch = new ScratchInstance(host, port);
    }

    public void disable() {
        enabledProperty.set(false);
        try {
            halt();
        } catch (InterruptedException ex) {
            Logger.getLogger(MeshController.class.getName()).log(Level.SEVERE, null, ex);
        }
        ui.setMeshStatus(SystemStatus.inactive);
    }

    void connectToUI(UserInterface mainGui) {
        ui = mainGui;
        if (enabledProperty.get()) {
            reconnect();
        }
        ui.setMeshStatus(SystemStatus.inactive);
        ui.addShutdownHook(this::disable);
        ui.addShutdownHook(() -> {
            try {
                scratch.disconnect();
            } catch (IOException ex) {
                Logger.getLogger(MeshController.class.getName()).log(Level.SEVERE, null, ex);
            }
        });
    }

    private void reconnect() {
        if (enabledProperty.get()) {
            try {
                try {
                    scratch.disconnect();
                } catch (Throwable t) {
                    // Ignore disconnection errors
                }
                halt();
                ui.setMeshStatus(SystemStatus.error);
                allIntVariables.clear();
                allStringVariables.clear();
                ui.clearMeshVariables();
                connect();
                scratch.connect();
            } catch (InterruptedException | IOException ex) {
                Logger.getLogger(MeshController.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    @Override
    protected void broadcast(String message) {
        ui.showMeshBroadcast(message);
    }

    @Override
    protected void sensorUpdate(String name, String value) {
        updateSensorStringValue(name, value);
    }

    @Override
    protected void sensorUpdate(String name, double value) {
        updateSensorIntValue(name, (int) value);
    }

    @Override
    protected void sensorUpdate(String name, boolean value) {
        updateSensorIntValue(name, value ? 1 : 0);
    }

    @Override
    protected void otherMessage(String message) {
        broadcast("(?) " + message);
    }

    Exception lastConnectionError;
    ScheduledExecutorService restartService = Executors.newSingleThreadScheduledExecutor();

    @Override
    protected void connectionStateChange(boolean isConnected, Exception error) {
        lastConnectionError = error;
        ui.meshConnectedProperty().set(isConnected);
        if (!isConnected && enabledProperty.get()) {
            restartService.schedule(this::reconnect, 1, TimeUnit.SECONDS);
        }
    }

    private void updateSensorStringValue(String name, String value) {
        allStringVariables.put(name, value);
        ui.updateMeshVariable(name, value);
    }

    private void updateSensorIntValue(String name, int value) {
        allIntVariables.put(name, value);
        ui.updateMeshVariable(name, String.valueOf(value));
        if (name.matches("out[0-7]")) {
            int pin = name.charAt(3) - '0';
            if (piface != null) {
                try {
                    piface.setOutputState(pin, value > 0);
                } catch (IOException ex) {
                    Logger.getLogger(MeshController.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        } else if (name.matches("pwm[0-7]")) {
            int pin = name.charAt(3) - '0';
            if (piface != null) {
                try {
                    piface.setOutputPWM(pin, Math.min(Math.max(value, 0), 100));
                } catch (IOException ex) {
                    Logger.getLogger(MeshController.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

        }
    }

    PifaceConnection piface;

    void connectToPiface(PifaceConnection piface) throws IOException {
        this.piface = piface;
        for (int i = 0; i < 8; i++) {
            final int pin = i;
            piface.addListener(i, state -> {
                try {
                    scratch.sensorUpdate("in" + pin, state);
                } catch (IOException ex) {
                    Logger.getLogger(MeshController.class.getName()).log(Level.SEVERE, null, ex);
                }
            });
        }
    }
}
