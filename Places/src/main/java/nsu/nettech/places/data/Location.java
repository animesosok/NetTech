package nsu.nettech.places.data;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class Location implements Serializable {
    @SerializedName("point")
    private Coordinates point;
    @SerializedName("name")
    private String name;
    @SerializedName("country")
    private String country;
    @SerializedName("osm_value")
    private String osm_value;

    public Location(){
        super();
    }

    @Override
    public String toString() {
        return  name + ", " +
                country + " " +
                point;
    }
    public double getPointLng(){
        return point.getLng();
    }
    public double getPointLat(){
        return point.getLat();
    }

    public String getName() {
        return name;
    }
}
