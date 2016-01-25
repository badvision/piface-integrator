package org.badvision.pifaceintegrator.piface;

import com.pi4j.device.piface.PiFace;
import com.pi4j.device.piface.impl.PiFaceDevice;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.PinMode;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;
import com.pi4j.io.spi.SpiChannel;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Implements a local connection to a PiFace physically wired to this host.
 *
 * @author blurry
 */
public class LocalConnection implements PifaceConnection {

    // PWM pulses are at 100 microseconds
    public static final long PWM_UNIT = 50;
    PiFaceDevice device;
    private final Map<Integer, Integer> pwmValues;

    public LocalConnection() throws IOException {
        device = new PiFaceDevice(PiFace.DEFAULT_ADDRESS, SpiChannel.CS0);
        pwmValues = new ConcurrentHashMap<>();
    }

    @Override
    public boolean isConnected() {
        return device != null;
    }

    @Override
    public boolean getInputState(int inputPin) throws IOException {
        validateRange(inputPin);
        return device.getInputPin(inputPin).isLow();
    }

    @Override
    public void addListener(int inputPin, Consumer<Boolean> listener) throws IOException {
        validateRange(inputPin);
        device.getInputPin(inputPin).addListener((GpioPinListenerDigital) (GpioPinDigitalStateChangeEvent event)
                -> listener.accept(event.getState().isLow())
        );
    }

    @Override
    public void setOutputState(int pin, boolean state) throws IOException {
        validateRange(pin);
        disablePwm(pin);
        _setOutputState(pin, state);
    }

    private void _setOutputState(int pin, boolean state) {
        device.getOutputPin(pin).setState(state);
    }

    @Override
    public void setOutputPWM(int pin, int value) throws IOException {
        validateRange(pin);
        if (value <= 0) {
            setOutputState(pin, false);
        } else if (value >= 100) {
            setOutputState(pin, true);
        } else {
            setPwmValue(pin, value);
        }
    }

    @Override
    public int getOutputState(int pin) throws IOException {
        validateRange(pin);
        if (isPwmEnabledForPin(pin)) {
            return getPwmValue(pin);
        } else {
            return device.getOutputPin(pin).getState().isHigh() ? PWM_RANGE : 0;
        }
    }

    private boolean isPwmActive() {
        return !pwmValues.isEmpty() && executorService != null && !executorService.isShutdown();
    }

    private void setPwmValue(int pin, int value) {
        if (!isPwmActive()) {
            enablePWMMode();
        }
        pwmValues.put(pin, value);
    }

    private void disablePwm(int pin) {
        pwmValues.remove(pin);
        if (pwmValues.isEmpty()) {
            disablePWMMode();
        }
    }

    private boolean isPwmEnabledForPin(int pin) {
        return pwmValues.containsKey(pin);
    }

    private int getPwmValue(int pin) {
        return isPwmEnabledForPin(pin) ? pwmValues.get(pin) : -1;
    }

    ScheduledExecutorService executorService;

    private void enablePWMMode() {
        if (executorService == null || executorService.isShutdown()) {
            executorService = Executors.newSingleThreadScheduledExecutor();
            executorService.scheduleAtFixedRate(this::processPwm, 0, PWM_UNIT, TimeUnit.MICROSECONDS);
        }
    }

    AtomicInteger pwmCounter = new AtomicInteger(0);

    private void processPwm() {
        try {
            int counter = pwmCounter.updateAndGet(val -> {return val < PWM_RANGE ? val + 1 : 0;});
            pwmValues.entrySet().forEach(entry -> _setOutputState(entry.getKey(), entry.getValue() >= counter));
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private void disablePWMMode() {
        pwmValues.clear();
        executorService.shutdownNow();
        executorService = null;
    }
}
