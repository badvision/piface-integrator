package org.badvision.pifaceintegrator;

import java.io.IOException;
import java.util.function.Consumer;
import org.badvision.pifaceintegrator.piface.PifaceConnection;

/**
 *
 * @author blurry
 */
public class PifaceController implements PifaceConnection {

    public PifaceController(PifaceConnection piface) {
        this.piface = piface;
    }

    public void connectToUI(UserInterface mainGui) throws IOException {
        controller = mainGui;
        init();
    }
    UserInterface controller;
    PifaceConnection piface;

    private void init() throws IOException {
        isConnected();
        for (int i=0; i < 8; i++) {
            final int pin = i;
            piface.addListener(i, state->pinChanged(pin, state));
        }
    }    
    
    private void pinChanged(int pin, boolean isOn) {
        if (isOn) {
            controller.showPifaceBroadcast("IN"+pin);
        }
    }

    @Override
    public boolean getInputState(int pin) throws IOException {
        return piface.getInputState(pin);
    }

    @Override
    public void addListener(int pin, Consumer<Boolean> listener) throws IOException {
        piface.addListener(pin, listener);
    }

    @Override
    public void setOutputState(int pin, boolean state) throws IOException {
        controller.setPifaceValue(pin, state ? 100 : 0);
        piface.setOutputState(pin, state);
    }

    @Override
    public void setOutputPWM(int pin, int value) throws IOException {
        controller.setPifaceValue(pin, value);
        piface.setOutputPWM(pin, value);
    }

    @Override
    public int getOutputState(int pin) throws IOException {
        return piface.getOutputState(pin);
    }

    @Override
    public boolean isConnected() {
        controller.setPifaceStatus(piface.isConnected() ? UserInterface.SystemStatus.active : UserInterface.SystemStatus.error);
        return piface.isConnected();
    }
}
