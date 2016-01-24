package org.badvision.pifaceintegrator.piface;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This implement a mock service to simulate the presence of a PiFace connection,
 * useful for UI and network layer testing as well as application demo without
 * the target platform being present.
 * 
 * @author blurry
 */
public class MockConnection implements PifaceConnection {
    Set<Consumer<Boolean>> inputListeners = new HashSet<>();
    @Override
    public boolean isConnected() {
        return true;
    }
    
    @Override
    public boolean getInputState(int pin) throws IOException {
        return Math.random() < 0.25;
    }

    @Override
    public void addListener(int pin, Consumer<Boolean> listener) throws IOException {
        validateRange(pin);
        inputListeners.add(listener);
        initRandomCallbacks();
    }

    Map<Integer, Integer> outputPinStates = new HashMap<>();
    @Override
    public void setOutputState(int pin, boolean state) throws IOException {
        validateRange(pin);
        outputPinStates.put(pin, state ? PWM_RANGE : 0);
    }

    @Override
    public void setOutputPWM(int pin, int value) throws IOException {
        validateRange(pin);
        outputPinStates.put(pin, value);
    }

    @Override
    public int getOutputState(int pin) throws IOException {
        validateRange(pin);
        return outputPinStates.containsKey(pin) ? outputPinStates.get(pin) : 0;
    }    

    ScheduledExecutorService executorService;
    private void initRandomCallbacks() {
        if (executorService == null || executorService.isShutdown()) {
            executorService = Executors.newSingleThreadScheduledExecutor();
            executorService.scheduleAtFixedRate(this::randomlyTriggerListeners, 1, 1, TimeUnit.SECONDS);
        }
    }
    
    private void randomlyTriggerListeners() {
        try {
            inputListeners.stream().filter(l->Math.random() < 0.25).forEach(this::triggerListener);
        } catch (Throwable ex) {
            Logger.getLogger(MockConnection.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private void triggerListener(Consumer<Boolean> listener) {
        listener.accept(Math.random() < 0.25);
    }
}
