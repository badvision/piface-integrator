package org.badvision.pifaceintegrator;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import org.badvision.pifaceintegrator.ui.IndicatorController;
import org.badvision.pifaceintegrator.ui.ScaleController;

public class GuiApp extends Application {

    static UserInterface ui;

    public static IndicatorController createStatusIndicator(String name, String value) throws IOException {
        FXMLLoader loader = new FXMLLoader(GuiApp.class.getResource("/fxml/LCARS-statusIndicator.fxml"));
        loader.load();
        IndicatorController controller = loader.getController();
        controller.nameProperty().set(name);
        controller.valueProperty().set(value);
        return controller;
    }

    public static ScaleController createScaleValue(String name, int value) throws IOException {
        FXMLLoader loader = new FXMLLoader(GuiApp.class.getResource("/fxml/LCARS-scale.fxml"));
        loader.load();
        ScaleController controller = loader.getController();
        controller.nameProperty().set(name);
        controller.valueProperty().set(value);
        return controller;
    }

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/LCARS-main.fxml"));

        Parent root = loader.load();
        ui = loader.getController();

        Scene scene = new Scene(root);
        scene.getStylesheets().add("/styles/Styles.css");

        stage.setTitle("PiFace Mesh Integrator");
        stage.setScene(scene);
        stage.show();
        
        stage.setOnCloseRequest((final WindowEvent t) -> {
            t.consume();
            ui.announceShutdown();
            new Thread(()->{
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException ex) {
                    Logger.getLogger(GuiApp.class.getName()).log(Level.SEVERE, null, ex);
                }                
                Platform.exit();
                System.exit(0);
            }).start();
        });
    }

    /**
     * The main() method is ignored in correctly deployed JavaFX application.
     * main() serves only as fallback in case the application can not be
     * launched through deployment artifacts, e.g., in IDEs with limited FX
     * support. NetBeans ignores main().
     *
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }

}
