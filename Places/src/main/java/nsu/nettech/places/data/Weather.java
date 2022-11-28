package nsu.nettech.places.data;

import com.google.gson.annotations.SerializedName;

public class Weather {
    @SerializedName("description")
    private String desc;

    public String getDesc() {
        return desc;
    }
}
