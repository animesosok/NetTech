package nsu.nettech.places.data;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class WeatherDescription {
    @SerializedName("main")
    private Temperature temp;
    @SerializedName("weather")
    private List<Weather> whether;

    public String getWeatherDescription(){
        return whether.get(0).getDesc();
    }
    public double getTemperatureReal(){
       return temp.getTemp();
    }
    public double getTemperatureFeelsLike(){
        return temp.getFeelsLike();
    }
}
