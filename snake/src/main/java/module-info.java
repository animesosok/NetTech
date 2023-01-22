module nsu.nets.snake {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.google.protobuf;


    opens nsu.nets.snake to javafx.fxml;
    exports nsu.nets.snake;
    exports nsu.nets.snake.net;
    opens nsu.nets.snake.net to javafx.fxml;
    exports nsu.nets.snake.controllers;
    opens nsu.nets.snake.controllers to javafx.fxml;
}