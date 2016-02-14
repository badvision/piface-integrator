package org.badvision.pifaceintegrator.piface;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.badvision.pifaceintegrator.piface.RestServer.RestResponse;

/**
 * Implements a connection (via network) to a host connected to a PiFace module.
 *
 * @author blurry
 */
public class RestClient implements PifaceConnection {

    PoolingHttpClientConnectionManager connectionManager;
    public static final int MAX_CONNECTIONS = 9;
    public static final int POLLING_FREQUENCY = 500;
    Gson gson = new Gson();
    String host;
    int port;

    public RestClient() {
        init("localhost", RestServer.DEFAULT_PORT);
    }

    public RestClient(String host, int port) {
        init(host, port);
    }

    private void init(String host, int port) {
        connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setDefaultMaxPerRoute(MAX_CONNECTIONS);
        connectionManager.setMaxTotal(MAX_CONNECTIONS);
        this.host = host;
        this.port = port;
    }

    @Override
    public boolean isConnected() {
        try {
            URI uri = generateUri(RestServer.GET_INPUTS);
            Collection<RestResponse> restResponse = getRestResponseList(uri);
            return restResponse != null;
        } catch (Throwable t) {
            return false;
        }
    }

    Map<Integer, List<Consumer<Boolean>>> listeners = new ConcurrentHashMap<>();

    @Override
    public void addListener(int pin, Consumer<Boolean> listener) throws IOException {
        setupPollingLoop();
        getListenersForPin(pin).add(listener);
    }

    final ConcurrentHashMap<Integer, Integer> pwmOutputChanges = new ConcurrentHashMap<>();

    @Override
    public void setOutputState(int pin, boolean state) throws IOException {
        setOutputPWM(pin, state ? 100 : 0);
    }

    @Override
    public void setOutputPWM(int pin, int value) throws IOException {
        synchronized (pwmOutputChanges) {
            pwmOutputChanges.put(pin, value);
        }

        new Thread(() -> {
            try {
                Thread.sleep(25);
            } catch (InterruptedException ex) {
                Logger.getLogger(RestClient.class.getName()).log(Level.SEVERE, null, ex);
            }
            List<String> params = new ArrayList<>();
            synchronized (pwmOutputChanges) {
                pwmOutputChanges.forEach((p, v) -> {
                    params.add(RestServer.PARAM_PIN + p);
                    params.add(String.valueOf(v));
                });
                pwmOutputChanges.clear();
            }
            if (params.isEmpty()) {
                return;
            }
            try {
                URI uri = generateUri(RestServer.SET_OUTPUT_PWM, (String[]) params.toArray(new String[0]));
                RestResponse restResponse = getRestResponse(uri);
            } catch (URISyntaxException | IOException ex) {
                Logger.getLogger(RestClient.class.getName()).log(Level.SEVERE, null, ex);
            }
        }).start();
    }

    @Override
    public int getOutputState(int pin) throws IOException {
        try {
            URI uri = generateUri(RestServer.SET_OUTPUT,
                    RestServer.PARAM_PIN, String.valueOf(pin));
            RestResponse restResponse = getRestResponse(uri);
            if (restResponse == null) {
                throw new IOException("Bad response");
            }
            return restResponse.getValue();
        } catch (URISyntaxException ex) {
            Logger.getLogger(RestClient.class.getName()).log(Level.SEVERE, null, ex);
            throw new IOException("Error executing request", ex);
        }
    }

    @Override
    public boolean getInputState(int pin) throws IOException {
        try {
            URI uri = generateUri(RestServer.GET_INPUT,
                    RestServer.PARAM_PIN, String.valueOf(pin));
            RestResponse restResponse = getRestResponse(uri);
            if (restResponse == null) {
                throw new IOException("Bad response");
            }
            return restResponse.getState();
        } catch (URISyntaxException ex) {
            Logger.getLogger(RestClient.class.getName()).log(Level.SEVERE, null, ex);
            throw new IOException("Error executing request", ex);
        }
    }

    private URI generateUri(String path, String... params) throws URISyntaxException {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < params.length; i++) {
            if (i % 2 == 0) {
                if (i > 0) {
                    builder.append("&");
                }
            } else {
                builder.append("=");
            }
            builder.append(params[i]);
        }
        String url = "http://" + host + ":" + port + path + "?" + builder.toString();
        System.out.println(url);
        return new URI(url);
    }

    private CloseableHttpClient getClient() {
        return HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setConnectionManagerShared(true)
                .build();
    }

    private RestResponse getRestResponse(URI uri) throws IOException {
        int retries = 3;
        RestResponse restResponse = null;
        while (restResponse == null && retries > 0) {
            HttpGet request = new HttpGet(uri);
            try (CloseableHttpResponse response = getClient().execute(request)) {
                InputStreamReader reader = new InputStreamReader(response.getEntity().getContent());
                restResponse = gson.fromJson(reader, RestResponse.class);
            } catch (Throwable t) {
                retries--;
                if (retries > 0) {
                    Logger.getLogger(getClass().getName()).log(Level.WARNING, "Error trying to communicate with server: {0}", t.getMessage());
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException ex) {
                        throw new IOException("Interrupted during retry sequence", t);
                    }
                } else {
                    throw new IOException("Unable to parse result", t);
                }
            }
        }
        return restResponse;
    }

    private Collection<RestResponse> getRestResponseList(URI uri) throws IOException {
        int retries = 3;
        Collection<RestResponse> restResponse = null;
        while (restResponse == null && retries > 0) {
            HttpGet request = new HttpGet(uri);
            try (CloseableHttpResponse response = getClient().execute(request)) {
                InputStreamReader reader = new InputStreamReader(response.getEntity().getContent());
                JsonObject result = gson.fromJson(reader, JsonObject.class);
                JsonArray list = result.get("response").getAsJsonArray();

                Type collectionType = new TypeToken<Collection<RestResponse>>() {
                }.getType();
                restResponse = gson.fromJson(list, collectionType);
            } catch (Throwable t) {
                retries--;
                if (retries > 0) {
                    Logger.getLogger(getClass().getName()).log(Level.WARNING, "Error trying to communicate with server: {0}", t.getMessage());
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException ex) {
                        throw new IOException("Interrupted during retry sequence", t);
                    }
                } else {
                    throw new IOException("Unable to parse result", t);
                }
            }
        }
        return restResponse;
    }

    ScheduledExecutorService scheduler;

    private void setupPollingLoop() {
        if (scheduler != null && !scheduler.isShutdown()) {
            return;
        }
        System.out.println("Creating new scheduler");
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(this::pollInputs, 0, POLLING_FREQUENCY, TimeUnit.MILLISECONDS);
    }

    private List<Consumer<Boolean>> getListenersForPin(int pin) {
        if (listeners.get(pin) == null) {
            listeners.put(pin, new ArrayList<>());
        }
        return listeners.get(pin);
    }

    boolean[] inputState = new boolean[8];

    private void pollInputs() {
        try {
            URI uri = generateUri(RestServer.GET_INPUTS);
            Collection<RestResponse> restResponse = getRestResponseList(uri);
            if (restResponse != null) {
                restResponse.forEach(this::evaluateInputStateChange);
            }

        } catch (URISyntaxException | IOException ex) {
            Logger.getLogger(RestClient.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    private void evaluateInputStateChange(RestResponse pinStatus) {
        if (pinStatus.getState() != inputState[pinStatus.getPin()]) {
            getListenersForPin(pinStatus.getPin()).forEach(listener -> {
                listener.accept(pinStatus.getState());
            });
        }
        inputState[pinStatus.getPin()] = pinStatus.getState();
    }
}
