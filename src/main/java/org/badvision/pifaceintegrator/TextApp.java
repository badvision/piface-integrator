package org.badvision.pifaceintegrator;

import com.sun.istack.internal.logging.Logger;
import java.util.ArrayList;
import java.util.List;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

/**
 *
 * @author blurry
 */
public class TextApp implements UserInterface {

    static Logger log = Logger.getLogger(TextApp.class);
    BooleanProperty meshConnectedProperty = new SimpleBooleanProperty(false);
    BooleanProperty pifaceConnectedProperty = new SimpleBooleanProperty(false);

    public TextApp() {
        meshConnectedProperty.addListener((prop, oldVal, newVal) -> {
            log.info("Mesh connected: " + newVal.toString());
        });
        pifaceConnectedProperty.addListener((prop, oldVal, newVal) -> {
            log.info("PiFace connected: " + newVal.toString());
        });
    }

    @Override
    public void setMeshStatus(SystemStatus status) {
        log.info("Mesh network status: " + status.name());
    }

    @Override
    public BooleanProperty meshConnectedProperty() {
        return meshConnectedProperty;
    }

    @Override
    public void showMeshBroadcast(String message) {
        log.info("Mesh broadcast: " + message);
    }

    @Override
    public void clearMeshVariables() {

    }

    @Override
    public void updateMeshVariable(String name, String val) {
        log.info("Mesh variable " + name + "=" + val);
    }

    @Override
    public void setPifaceStatus(SystemStatus status) {
        log.info("PiFace status: " + status.name());
    }

    @Override
    public void showPifaceBroadcast(String message) {
        log.info("PiFace broadcast: " + message);
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
        log.info("PiFace output: " + pin + "=" + value);
    }

    @Override
    public void mainLoop() {
        while (true) {
            Thread.yield();
        }
    }
}
