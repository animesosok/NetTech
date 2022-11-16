package nsu.nettech.places.data;

import com.google.gson.annotations.SerializedName;

public class Place {
    @SerializedName("xid")
    String id;
    @SerializedName("name")
    String name;

    @Override
    public String toString() {
        if (name.length() <=0){
            return "NameNotFound";
        }
        return name;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
