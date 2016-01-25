package org.badvision.pifaceintegrator.piface;

import com.google.gson.Gson;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.http.HttpEntity;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.config.SocketConfig;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.bootstrap.HttpServer;
import org.apache.http.impl.bootstrap.ServerBootstrap;
import org.apache.http.protocol.HttpContext;

/**
 * This provides a REST layer for a PiFace
 *
 * @author blurry
 */
public class RestServer {

    public static class RestResponse {

        int pin;
        int value;
        boolean state;

        public void setPin(int pin) {
            this.pin = pin;
        }

        public int getPin() {
            return pin;
        }

        public void setValue(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public void setState(boolean state) {
            this.state = state;
        }

        public boolean getState() {
            return state;
        }
    }
    public static String GET_INPUT = "/input";
    public static String GET_INPUTS = "/inputs";
    public static String SET_OUTPUT = "/output";
    public static String SET_OUTPUT_PWM = "/outputPwm";

    public static String PARAM_PIN = "pin";
    public static String PARAM_STATE = "state";
    public static String PARAM_VALUE = "value";
    public static int DEFAULT_PORT = 1701;

    HttpServer server;
    PifaceConnection device;
    Gson gson;

    public RestServer(int port, PifaceConnection piface) throws IOException {
        device = piface;
        gson = new Gson();
        ServerBootstrap bootstrap = ServerBootstrap.bootstrap();
        bootstrap.setListenerPort(port);
        bootstrap.setSocketConfig(SocketConfig.DEFAULT);
        bootstrap.registerHandler(GET_INPUT, this::handleGetInputRequest);
        bootstrap.registerHandler(GET_INPUTS, this::handleGetAllInputsRequest);
        bootstrap.registerHandler(SET_OUTPUT, this::handleSetOutputRequest);
        bootstrap.registerHandler(SET_OUTPUT_PWM, this::handleSetOutputPwmRequest);
        server = bootstrap.create();
        server.start();
    }

    void handleGetInputRequest(HttpRequest request, HttpResponse response, HttpContext context) throws UnsupportedEncodingException {
        handleRequest(request, response, (params, output) -> {
            int pinNumber = getRequiredInt(params, PARAM_PIN);
            output.put(PARAM_PIN, pinNumber);
            try {
                output.put(PARAM_STATE, device.getInputState(pinNumber));
            } catch (IOException ex) {
                throw new RuntimeException(ex.getMessage(), ex);
            }
        });
    }

    void handleGetAllInputsRequest(HttpRequest request, HttpResponse response, HttpContext context) throws UnsupportedEncodingException {
        handleListRequest(request, response, (params, output) -> {
            try {
                for (int i = 0; i < 8; i++) {
                    RestResponse pin = new RestResponse();
                    pin.setPin(i);
                    pin.setState(device.getInputState(i));
                    output.add(pin);
                }
            } catch (IOException ex) {
                throw new RuntimeException(ex.getMessage(), ex);
            }
        });
    }

    void handleSetOutputRequest(HttpRequest request, HttpResponse response, HttpContext context) throws UnsupportedEncodingException {
        handleRequest(request, response, (params, output) -> {
            int pinNumber = getRequiredInt(params, PARAM_PIN);
            boolean state = getRequiredBoolean(params, PARAM_STATE);
            try {
                device.setOutputState(pinNumber, state);
            } catch (IOException ex) {
                throw new RuntimeException(ex.getMessage(), ex);
            }
            output.put(PARAM_PIN, pinNumber);
            output.put(PARAM_STATE, state);
        });
    }

    void handleSetOutputPwmRequest(HttpRequest request, HttpResponse response, HttpContext context) throws UnsupportedEncodingException {
        handleRequest(request, response, (params, output) -> {
            int pinNumber = getRequiredInt(params, PARAM_PIN);
            int val = getRequiredInt(params, PARAM_VALUE);
            try {
                device.setOutputPWM(pinNumber, val);
            } catch (IOException ex) {
                throw new RuntimeException(ex.getMessage(), ex);
            }
            output.put(PARAM_PIN, pinNumber);
            output.put(PARAM_VALUE, val);
        });
    }

    private Optional<String> getParamValue(List<NameValuePair> params, String name) {
        Optional<NameValuePair> found = params.stream().filter(nvp -> nvp.getName().equals(name)).findFirst();
        if (found.isPresent()) {
            return Optional.of(found.get().getValue());
        } else {
            return Optional.empty();
        }
    }

    private void handleRequest(HttpRequest request, HttpResponse response, BiConsumer<List<NameValuePair>, Map<String, Object>> handler) throws UnsupportedEncodingException {
        try {
            Map output = new HashMap();
            handler.accept(getQueryParameters(request), output);
            outputResponse(response, output);
        } catch (URISyntaxException | UnsupportedEncodingException ex) {
            Logger.getLogger(RestServer.class.getName()).log(Level.SEVERE, null, ex);
            outputError(response, ex);
        }
    }
    
    private void handleListRequest(HttpRequest request, HttpResponse response, BiConsumer<List<NameValuePair>, List<Object>> handler) throws UnsupportedEncodingException {
        try {
            List output = new ArrayList();
            handler.accept(getQueryParameters(request), output);
            outputResponse(response, output);
        } catch (URISyntaxException | UnsupportedEncodingException ex) {
            Logger.getLogger(RestServer.class.getName()).log(Level.SEVERE, null, ex);
            outputError(response, ex);
        }
    }
    

    public void shutdown() {
        if (server != null) {
            server.shutdown(1, TimeUnit.SECONDS);
        }
    }

    private List<NameValuePair> getQueryParameters(HttpRequest request) throws URISyntaxException {
        URIBuilder newBuilder = new URIBuilder(request.getRequestLine().getUri());
        return newBuilder.getQueryParams();
    }

    private void outputResponse(HttpResponse response, Object data) throws UnsupportedEncodingException {
        Map outputResponse = new HashMap();
        outputResponse.put("response", data);
        HttpEntity responseObject = new StringEntity(gson.toJson(outputResponse));
        response.setEntity(responseObject);
    }
    
    private void outputError(HttpResponse response, Exception ex) throws UnsupportedEncodingException {
        Map errorDetails = new HashMap();
        errorDetails.put("message", ex.getMessage());
        StringWriter stackWriter = new StringWriter();
        ex.printStackTrace(new PrintWriter(stackWriter));
        errorDetails.put("stacktrace", stackWriter.toString());
        if (ex.getCause() != null) {
            errorDetails.put("cause", ex.getCause().getMessage());
        }
        response.setStatusCode(500);
        outputResponse(response, errorDetails);
    }

    private boolean isEmptyOrNull(String str) {
        return str == null || str.trim().isEmpty();
    }

    private int getRequiredInt(List<NameValuePair> params, String name) {
        String str = getParamValue(params, name).orElse(null);
        if (isEmptyOrNull(str)) {
            throw new IllegalArgumentException("Numeric parameter " + name + " is missing or invalid");
        }
        return Integer.parseInt(str);
    }

    private boolean getRequiredBoolean(List<NameValuePair> params, String name) {
        String str = getParamValue(params, name).orElse(null);
        if (isEmptyOrNull(str)) {
            throw new IllegalArgumentException("Boolean parameter " + name + " is missing or invalid");
        }
        return Boolean.parseBoolean(str);
    }
}
