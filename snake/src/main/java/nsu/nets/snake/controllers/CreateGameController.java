package nsu.nets.snake.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import nsu.nets.snake.MainApplication;
import nsu.nets.snake.models.GameSettings;
import nsu.nets.snake.net.MessageController;


public class CreateGameController {
    MessageController msgController;
    Thread th;
    String playerName;
    @FXML
    Button createButton;
    @FXML
    TextField sizeXField;
    @FXML
    TextField sizeYField;
    @FXML
    TextField foodField;
    @FXML
    TextField delayField;
    @FXML
    TextField gameNameField;

    public CreateGameController(MessageController controller, String playerName) {
        msgController = controller;
        this.playerName = playerName;
    }

    public void init(){
        createButton.setOnAction(event -> createGame());
    }

    public void createGame(){
        // Check args
        GameSettings settings = new GameSettings();
        settings.setMaster(true);
        settings.setPlayerName(playerName);
        settings.setSizeX(Integer.parseInt(sizeXField.getText()));
        settings.setSizeY(Integer.parseInt(sizeYField.getText()));
        settings.setFoodStatic(Integer.parseInt(foodField.getText()));
        settings.setStateDelay(Integer.parseInt(delayField.getText()));
        settings.setName(gameNameField.getText());
        MainApplication.showGame(settings);
    }


}
