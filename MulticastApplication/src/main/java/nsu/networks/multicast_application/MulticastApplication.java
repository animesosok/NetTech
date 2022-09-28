package nsu.networks.multicast_application;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class MulticastApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(MulticastApplication.class.getResource("multicast-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 240, 180);
        stage.setTitle("MulticastApp");
        stage.setOnCloseRequest(e -> {
            MulticastController.closeApp();
        });
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}