package org.badvision.pifaceintegrator.piface;

import java.io.IOException;
import java.util.function.Consumer;

/**
 *
 * @author blurry
 */
public interface PifaceConnection {
    public static int PWM_RANGE = 100;
    
    public boolean getInputState(int pin) throws IOException;
    public void addListener(int pin, Consumer<Boolean> listener) throws IOException;
    public void setOutputState(int pin, boolean state) throws IOException;
    public void setOutputPWM(int pin, int value) throws IOException;
    public int getOutputState(int pin) throws IOException;
    
    default void validateRange(int pin) throws IOException {
        if (pin < 0 || pin > 7) {
            throw new IOException("Pin must be 0-7");
        }
    }

    public boolean isConnected();
}
