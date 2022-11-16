module com.example.places {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.fasterxml.jackson.databind;
    requires java.net.http;
    requires com.google.gson;
    requires java.logging;


    opens nsu.nettech.places to javafx.fxml;
    exports nsu.nettech.places;
    opens nsu.nettech.places.data to com.google.gson;
}