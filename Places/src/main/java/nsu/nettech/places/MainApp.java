package nsu.nettech.places;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.logging.Logger;

public class MainApp extends Application {
    public static final Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(MainApp.class.getResource("app-view.fxml"));
        AppController controller= new AppController();
        fxmlLoader.setController(controller);

        Scene scene = new Scene(fxmlLoader.load(), 720, 480);
        stage.setTitle("Places");
        stage.setScene(scene);
        stage.show();
        controller.init();

    }

    public static void main(String[] args) {
        launch();
    }
}