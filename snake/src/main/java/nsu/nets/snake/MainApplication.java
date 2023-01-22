package nsu.nets.snake;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import nsu.nets.snake.controllers.CreateGameController;
import nsu.nets.snake.controllers.GameController;
import nsu.nets.snake.controllers.MenuController;
import nsu.nets.snake.models.GameSettings;
import nsu.nets.snake.net.MessageController;

import java.io.IOException;
import java.net.UnknownHostException;

public class MainApplication extends Application {

    private static Stage appStage;
    private static MessageController msgController = new MessageController();
    MenuController menuController;

    /*
        Создать поля для сцен чтобы не создавать несколько раз
        переписать функции show
    */
    @Override
    public void start(Stage stage) throws IOException {
        appStage = stage;
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("menu-view.fxml"));
        menuController = new MenuController(msgController);
        fxmlLoader.setController(menuController);
        Scene scene = new Scene(fxmlLoader.load(), 600, 400);
        stage.setScene(scene);
        stage.show();
        menuController.init();

    }

    public static void showCreationGameScene(String playerName){
        FXMLLoader fxmlLoader = new FXMLLoader(MainApplication.class.getResource("creation_menu-view.fxml"));
        CreateGameController controller = new CreateGameController(msgController, playerName);
        fxmlLoader.setController(controller);
        Scene scene = null;
        try {
            scene = new Scene(fxmlLoader.load(), 600, 400);
        } catch (IOException e) {
            e.printStackTrace();
        }
        appStage.setScene(scene);
        appStage.show();
        controller.init();
    }
    public static void showGame(GameSettings settings){
        FXMLLoader fxmlLoader = new FXMLLoader(MainApplication.class.getResource("game-view.fxml"));
        GameController controller;
        if (settings.isMaster()) {
            controller = new GameController(settings, msgController);
        }
        else {
            controller = new GameController(settings, msgController, settings.getHost(), settings.getPort());
        }

        fxmlLoader.setController(controller);
        Scene scene = null;
        try {
            scene = new Scene(fxmlLoader.load(), 600, 400);
        } catch (IOException e) {
            e.printStackTrace();
        }
        appStage.setScene(scene);
        appStage.show();
        try {
            controller.init();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }
    public static void main(String[] args) {
        launch();
    }
}