package nsu.nettech.places.data;

import com.google.gson.annotations.SerializedName;

public class Temperature {
    @SerializedName("temp")
    private double temp;
    @SerializedName("feels_like")
    private double feelsLike ;
    Temperature(){
        super();
    }

    public double getFeelsLike() {
        return feelsLike;
    }

    public double getTemp() {
        return temp;
    }
}
