package nsu.nettech.places;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import nsu.nettech.places.data.*;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

public class AppController {

    public TextField searchField;
    public Button searchButton;
    public ListView<Location> locationsList;
    public Label weatherField;
    public ListView<Place> intrPlacesList;
    public Label placeDescription;
    public static Logger logger = MainApp.LOGGER;

    void init(){
        searchButton.setOnAction(event -> searchButtonClick());
        locationsList.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<Location>() {
            @Override
            public void changed(ObservableValue<? extends Location> observable, Location oldValue, Location newValue) {
                CompletableFuture<WeatherDescription> whetherResult = Requester.getWeather(newValue);
                if(whetherResult != null) {
                    whetherResult.thenAccept(weather -> printWeather(weather));
                }
                CompletableFuture<Place[]> placeResult = Requester.getPlaceList(newValue);
                if (placeResult != null) {
                    placeResult.thenAccept(places -> printPlaceList(places));
                }

            }
        });
        intrPlacesList.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<Place>() {
            @Override
            public void changed(ObservableValue<? extends Place> observable, Place oldValue, Place newValue) {
                CompletableFuture<PlaceDescription> placeResult = Requester.getPlaceDescription(newValue);
                if (placeResult != null) {
                    placeResult.thenAccept(desc -> printPlaceDesc(desc));
                }
            }
        });
    }
    public void searchButtonClick(){
        String locationName = searchField.getText();
        if (locationName.length() <= 0){
            String msg = "Location name is empty";
            logger.info(msg);
            return;
        }
        CompletableFuture<LocationList> locationsResult = Requester.getLocationList(locationName);
        if (locationsResult != null) {
            locationsResult.thenAccept(list -> printLocationList(list));
        }


    }
    void printLocationList(LocationList list1){
        List<Location> list = list1.getLocationList();
        ObservableList<Location> observableList = FXCollections.observableArrayList(list);
        Platform.runLater(() -> locationsList.setItems(observableList));
    }
    void printWeather(WeatherDescription weatherDesc){
        String text = String.format(" Temperature: %.2f C\n Feels like: %.2f C\n %s",
                (weatherDesc.getTemperatureReal() - 273.15),
                (weatherDesc.getTemperatureReal() - 273.15),
                weatherDesc.getWeatherDescription());

        Platform.runLater(() -> weatherField.setText(text));
    }
    void printPlaceList(Place[] list1){
        List<Place> list = Arrays.asList(list1);
        ObservableList<Place> observableList = FXCollections.observableArrayList(list);
        Platform.runLater(() -> intrPlacesList.setItems(observableList));
    }
    void printPlaceDesc(PlaceDescription desc){
        String text = desc.getKinds() + "\ncoordinates: " + desc.getPointLat() + " " + desc.getPointLng();
        Platform.runLater(() -> placeDescription.setText(text));
    }


}