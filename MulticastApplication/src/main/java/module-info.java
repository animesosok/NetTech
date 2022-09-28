module nsu.networks.multicastapplication {
    requires javafx.controls;
    requires javafx.fxml;


    opens nsu.networks.multicast_application to javafx.fxml;
    exports nsu.networks.multicast_application;
}