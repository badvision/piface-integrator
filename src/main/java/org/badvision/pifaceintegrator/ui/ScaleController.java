package org.badvision.pifaceintegrator.ui;

import java.net.URL;
import java.util.ResourceBundle;
import javafx.application.Platform;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;

public class ScaleController {

    IntegerProperty valueProperty = new SimpleIntegerProperty(0);

    @FXML // ResourceBundle that was given to the FXMLLoader
    private ResourceBundle resources;

    @FXML // URL location of the FXML file that was given to the FXMLLoader
    private URL location;

    @FXML // fx:id="valueSlider"
    private AnchorPane valueSlider; // Value injected by FXMLLoader

    @FXML // fx:id="gradient"
    private Pane gradient; // Value injected by FXMLLoader

    @FXML // fx:id="nameLabel"
    private Label nameLabel; // Value injected by FXMLLoader

    @FXML // This method is called by the FXMLLoader when initialization is complete
    void initialize() {
        assert valueSlider != null : "fx:id=\"valueSlider\" was not injected: check your FXML file 'LCARS-scale.fxml'.";
        assert gradient != null : "fx:id=\"gradient\" was not injected: check your FXML file 'LCARS-scale.fxml'.";
        assert nameLabel != null : "fx:id=\"nameLabel\" was not injected: check your FXML file 'LCARS-scale.fxml'.";
        valueProperty.addListener((prop, oldVal, newVal) -> {
            setValue(newVal.intValue());
        });
        setColor((int) ((Math.random()*4.0)+1.0));
    }

    public StringProperty nameProperty() {
        return nameLabel.textProperty();
    }

    public IntegerProperty valueProperty() {
        return valueProperty;
    }

    public Node getRoot() {
        return valueSlider;
    }

    public void setColor(int color) {
        Platform.runLater(() -> {
            gradient.getStyleClass().clear();
            gradient.getStyleClass().add("gradient" + Math.max(1, Math.min(color, 3)));
        });
    }

    private void setValue(int newVal) {
        double parentHeight = 119.0;
        double newHeight = newVal * parentHeight / 100.0;
        Platform.runLater(() -> {
            gradient.setMinHeight(newHeight);
        });
    }
}
