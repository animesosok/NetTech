package nsu.nettech.places.data;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

public class LocationList{
    @SerializedName("hits")
    private List<Location> hits;

    public LocationList(){
        this.hits = new ArrayList<>();
    }

    public List<Location> getLocationList() {
        return hits;
    }
}
