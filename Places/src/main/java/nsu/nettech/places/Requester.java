package nsu.nettech.places;

import com.google.gson.Gson;
import nsu.nettech.places.data.*;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

public class Requester {
    static private String LOCATION_KEY = "key";
    static private String WEATHER_KEY = "key";
    static private String PLACE_KEY = "key";
    static private final HttpClient client = HttpClient.newHttpClient();
    static private final Gson gson = new Gson();
    static private final Logger logger = MainApp.LOGGER;
    static {
        Properties key_property = new Properties();
        try {
            key_property.load(new FileInputStream("keys.properties"));
        } catch (IOException e) {
            String msg = "Keys сan not be loaded. Check file keys.properties";
            logger.info(msg);
        }
        LOCATION_KEY = key_property.getProperty("GRAPHHOPER_KEY");
        WEATHER_KEY = key_property.getProperty("OPENWEATHERMAP_KEY");
        PLACE_KEY =key_property.getProperty("OPENTRIPMAP_KEY");
    }

    static public  CompletableFuture<LocationList> getLocationList(String locationName) {
        String locName = locationName.replace(" ", "+");
        String requestURIString = String.format(
                "https://graphhopper.com/api/1/geocode?q=%s&key=%s",
                locName,
                LOCATION_KEY
                );
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(requestURIString))
                .GET()
                .build();
        String msg = "[REQUEST] Location: " + locationName;
        logger.info(msg);
        return client
                .sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenApply(Requester::parseLocationList);
    }
    static public  CompletableFuture<WeatherDescription> getWeather(Location location){
        if(location == null){
            return null;
        }
        String requestURIString= String.format(
                "http://api.openweathermap.org/data/2.5/weather?lat=%s&lon=%s&appid=%s",
                location.getPointLat(),
                location.getPointLng(),
                Requester.WEATHER_KEY
        );
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(requestURIString))
                .GET()
                .build();
        String msg = "[REQUEST] Weather in location: " + location.getName();
        logger.info(msg);
        return client
                .sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenApply(Requester::parseWeather);
    }
    static public CompletableFuture<Place[] > getPlaceList(Location location){
        if(location == null){
            return null;
        }
        String requestURIString= String.format(
                "https://api.opentripmap.com/0.1/en/places/radius?radius=3000&lon=%s&lat=%s&format=json&apikey=%s",
                location.getPointLng(),
                location.getPointLat(),
                Requester.PLACE_KEY
        );
        String msg = "[REQUEST] Interesting places in location: " + location.getName();
        logger.info(msg);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(requestURIString))
                .GET()
                .build();
        return client
                .sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenApply(Requester::parsePlaceList);

    }
    static public CompletableFuture<PlaceDescription> getPlaceDescription(Place place){
        if (place == null){
            return null;
        }
        String requestURIString= String.format(
                "https://api.opentripmap.com/0.1/en/places/xid/%s?apikey=%s",
                place.getId(),
                Requester.PLACE_KEY
        );
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(requestURIString))
                .GET()
                .build();
        String msg = "[REQUEST] Description place: " + place.getName();
        logger.info(msg);
        return client
                .sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenApply(Requester::parsePlaceDesc);
    }
    static private LocationList parseLocationList(String json){
        return gson.fromJson(json, LocationList.class);
    }
    static private WeatherDescription parseWeather(String json){
        return gson.fromJson(json, WeatherDescription.class);
    }
    static private Place[] parsePlaceList(String json){
        return  gson.fromJson(json, Place[].class);
    }
    static private PlaceDescription parsePlaceDesc(String json){
        String newJson =  json.replace("\"lon\"", "\"lng\"");
        return gson.fromJson(newJson, PlaceDescription.class);
    }

}
