package nsu.nettech.places.data;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class Coordinates implements Serializable {
    @SerializedName("lat")
    private double lat;
    @SerializedName("lng")
    private double lng;

    public Coordinates(){
        super();
    }
    @Override
    public String toString() {
        return "Coordinates: "+
                lat + ", " +
                lng;
    }

    public double getLat() {
        return lat;
    }
    public double getLng() {
        return lng;
    }
}
