package org.badvision.pifaceintegrator.ui;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.TreeMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.effect.BlendMode;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import org.badvision.pifaceintegrator.GuiApp;
import org.badvision.pifaceintegrator.UserInterface;

public class MainController implements UserInterface {

    @FXML // ResourceBundle that was given to the FXMLLoader
    private ResourceBundle resources;

    @FXML // URL location of the FXML file that was given to the FXMLLoader
    private URL location;

    @FXML // fx:id="root"
    private StackPane root; // Value injected by FXMLLoader

    @FXML // fx:id="meshLabel"
    private Label meshLabel; // Value injected by FXMLLoader

    @FXML // fx:id="meshActivity"
    private FlowPane meshActivity; // Value injected by FXMLLoader

    @FXML // fx:id="meshNotifications"
    private FlowPane meshNotifications; // Value injected by FXMLLoader

    @FXML // fx:id="pifaceLabel"
    private Label pifaceLabel; // Value injected by FXMLLoader

    @FXML // fx:id="pifaceActivity"
    private FlowPane pifaceActivity; // Value injected by FXMLLoader

    @FXML // fx:id="pifaceNotifications"
    private FlowPane pifaceNotifications; // Value injected by FXMLLoader

    @FXML // This method is called by the FXMLLoader when initialization is complete
    void initialize() throws IOException {
        assert root != null : "fx:id=\"root\" was not injected: check your FXML file 'LCARS-main.fxml'.";
        assert meshLabel != null : "fx:id=\"meshLabel\" was not injected: check your FXML file 'LCARS-main.fxml'.";
        assert meshActivity != null : "fx:id=\"meshActivity\" was not injected: check your FXML file 'LCARS-main.fxml'.";
        assert meshNotifications != null : "fx:id=\"meshNotifications\" was not injected: check your FXML file 'LCARS-main.fxml'.";
        assert pifaceLabel != null : "fx:id=\"pifaceLabel\" was not injected: check your FXML file 'LCARS-main.fxml'.";
        assert pifaceActivity != null : "fx:id=\"pifaceActivity\" was not injected: check your FXML file 'LCARS-main.fxml'.";
        assert pifaceNotifications != null : "fx:id=\"pifaceNotifications\" was not injected: check your FXML file 'LCARS-main.fxml'.";

        meshConnectedProperty.addListener((prop, oldVal, newVal)
                -> setMeshStatus(newVal
                        ? SystemStatus.active
                        : SystemStatus.scanning)
        );
        pifaceOutputs = new ScaleController[8];
        for (int i = 0; i < 8; i++) {
            pifaceOutputs[i] = GuiApp.createScaleValue("OUT" + i, 0);
            pifaceActivity.getChildren().add(pifaceOutputs[i].getRoot());
        }
    }

    private ScaleController[] pifaceOutputs;

    private final BooleanProperty meshConnectedProperty = new SimpleBooleanProperty(false);

    @Override
    public void setMeshStatus(SystemStatus status) {
        Platform.runLater(() -> {
            meshLabel.getStyleClass().clear();
            meshLabel.getStyleClass().add("system-" + status.name());
        });
    }

    @Override
    public void setPifaceStatus(SystemStatus status) {
        Platform.runLater(() -> {
            pifaceLabel.getStyleClass().clear();
            pifaceLabel.getStyleClass().add("system-" + status.name());
        });
    }

    ScheduledExecutorService animationService = Executors.newSingleThreadScheduledExecutor();

    @Override
    public void showMeshBroadcast(String message) {
        showBroadcast(message, meshNotifications);
    }

    @Override
    public void showPifaceBroadcast(String message) {
        showBroadcast(message, pifaceNotifications);
    }

    private void showBroadcast(String message, Pane location) {
        Label label = new Label(message);
        label.getStyleClass().add("message-indicator");
        label.setMinWidth(70);
        Platform.runLater(() -> {
            location.getChildren().add(label);
        });
        animationService.schedule(() -> {
            Platform.runLater(() -> {
                location.getChildren().remove(label);
            });
        }, 2, TimeUnit.SECONDS);

    }

    private final Map<String, IndicatorController> meshVariables = new TreeMap<>(
            (s1, s2) -> s1.toLowerCase().compareTo(s2.toLowerCase())
    );

    @Override
    public void updateMeshVariable(String name, String val) {
        if (!meshVariables.containsKey(name)) {
            try {
                meshVariables.put(name, GuiApp.createStatusIndicator(name, val));
                updateMeshVariables();
            } catch (IOException ex) {
                Logger.getLogger(UserInterface.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            Platform.runLater(() -> meshVariables.get(name).valueProperty().set(val));
        }
    }

    @Override
    public void clearMeshVariables() {
        meshVariables.clear();
    }

    private void updateMeshVariables() {
        ObservableList<Node> children = meshActivity.getChildren();
        Platform.runLater(() -> {
            children.setAll(meshVariables.values().stream().map(c -> c.getRoot()).collect(Collectors.toList()));
        });
    }

    @Override
    public BooleanProperty meshConnectedProperty() {
        return meshConnectedProperty;
    }

    @Override
    public void announceShutdown() {
        Label redAlert = new Label("SELF DESTRUCT INITIATED");
        redAlert.getStyleClass().add("system-error-message");
        AnchorPane redOverlay = new AnchorPane();
        redOverlay.setMinSize(root.getWidth(), root.getHeight());
        redOverlay.setStyle("-fx-background-color:red");
        redOverlay.setBlendMode(BlendMode.MULTIPLY);
        root.getChildren().add(redOverlay);
        root.getChildren().add(redAlert);
        shutdownListeners.stream().map(Thread::new).forEach(Thread::start);
    }
    
    List<Runnable> shutdownListeners = new ArrayList<>();
    @Override
    public void addShutdownHook(Runnable listener) {
        shutdownListeners.add(listener);
    }

    @Override
    public void setPifaceValue(int pin, int value) {
        pifaceOutputs[pin].valueProperty.set(value);
    }

    @Override
    public void mainLoop() {
        // Do nothing -- JavaFX already owns the main loop
    }
}
