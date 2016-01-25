package org.badvision.pifaceintegrator;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

/**
 *
 * @author blurry
 */
public class TextApp implements UserInterface {

    static final Logger log = Logger.getLogger(TextApp.class.getName());
    BooleanProperty meshConnectedProperty = new SimpleBooleanProperty(false);
    BooleanProperty pifaceConnectedProperty = new SimpleBooleanProperty(false);

    public TextApp() {
        meshConnectedProperty.addListener((prop, oldVal, newVal) -> {
            log.log(Level.INFO, "Mesh connected: {0}", newVal.toString());
        });
        pifaceConnectedProperty.addListener((prop, oldVal, newVal) -> {
            log.log(Level.INFO, "PiFace connected: {0}", newVal.toString());
        });
    }

    @Override
    public void setMeshStatus(SystemStatus status) {
        log.log(Level.INFO, "Mesh network status: {0}", status.name());
    }

    @Override
    public BooleanProperty meshConnectedProperty() {
        return meshConnectedProperty;
    }

    @Override
    public void showMeshBroadcast(String message) {
        log.log(Level.INFO, "Mesh broadcast: {0}", message);
    }

    @Override
    public void clearMeshVariables() {

    }

    @Override
    public void updateMeshVariable(String name, String val) {
        log.log(Level.INFO, "Mesh variable {0}={1}", new Object[]{name, val});
    }

    @Override
    public void setPifaceStatus(SystemStatus status) {
        log.log(Level.INFO, "PiFace status: {0}", status.name());
    }

    @Override
    public void showPifaceBroadcast(String message) {
        log.log(Level.INFO, "PiFace broadcast: {0}", message);
    }

    @Override
    public void announceShutdown() {
        log.warning("Shutdown requested.");
        shutdownListeners.forEach(Runnable::run);
    }

    List<Runnable> shutdownListeners = new ArrayList<>();

    @Override
    public void addShutdownHook(Runnable listener) {
        shutdownListeners.add(listener);
    }

    @Override
    public void setPifaceValue(int pin, int value) {
        log.log(Level.INFO, "PiFace output: {0}={1}", new Object[]{pin, value});
    }

    @Override
    public void mainLoop() {
        while (true) {
            Thread.yield();
        }
    }
}
