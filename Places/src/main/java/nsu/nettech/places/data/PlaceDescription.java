package nsu.nettech.places.data;

import com.google.gson.annotations.SerializedName;

public class PlaceDescription {
    @SerializedName("point")
    Coordinates point;
    @SerializedName("kinds")
    String kinds;

    public double getPointLng(){
        return point.getLng();
    }
    public double getPointLat(){
        return point.getLat();
    }

    public String getKinds() {
        return kinds;
    }
}
