package org.badvision.pifaceintegrator.ui;

import java.net.URL;
import java.util.ResourceBundle;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

public class IndicatorController {

    @FXML // ResourceBundle that was given to the FXMLLoader
    private ResourceBundle resources;

    @FXML // URL location of the FXML file that was given to the FXMLLoader
    private URL location;

    @FXML // fx:id="hbox"
    private HBox hbox; // Value injected by FXMLLoader

    @FXML // fx:id="variableName"
    private Label variableName; // Value injected by FXMLLoader

    @FXML // fx:id="variableValue"
    private Label variableValue; // Value injected by FXMLLoader

    @FXML // This method is called by the FXMLLoader when initialization is complete
    void initialize() {
        assert hbox != null : "fx:id=\"hbox\" was not injected: check your FXML file 'LCARS-statusIndicator.fxml'.";
        assert variableName != null : "fx:id=\"variableName\" was not injected: check your FXML file 'LCARS-statusIndicator.fxml'.";
        assert variableValue != null : "fx:id=\"variableValue\" was not injected: check your FXML file 'LCARS-statusIndicator.fxml'.";
        setColor((int) ((Math.random()*4.0)+1.0));
    }
    
    public void setColor(int color) {
        hbox.getStyleClass().clear();
        hbox.getStyleClass().add("status-indicator"+Math.max(1, Math.min(color, 3)));
    }
    
    public StringProperty nameProperty() {
        return variableName.textProperty();
    }
    
    public StringProperty valueProperty() {
        return variableValue.textProperty();
    }

    public Node getRoot() {
        return hbox;
    }
}