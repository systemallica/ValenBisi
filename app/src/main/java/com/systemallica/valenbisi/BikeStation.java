package com.systemallica.valenbisi;

import android.util.Log;

import com.google.android.gms.maps.model.BitmapDescriptor;

import org.json.JSONException;
import org.json.JSONObject;

public class BikeStation {
    public String snippet;
    public BitmapDescriptor icon;
    public Float alpha;
    public Boolean visibility = true;
    public Double lat;
    public Double lng;
    public String address;
    public String status;
    public int spots;
    public int bikes;
    public String lastUpdate;
    public int bikeStands;

    public BikeStation(JSONObject station) {
        try {
            JSONObject latLong = station.getJSONObject("position");
            this.lat = latLong.getDouble("lat");
            this.lng = latLong.getDouble("lng");
            this.address = station.getString("address");
            this.status = station.getString("status");
            this.spots = station.getInt("available_bike_stands");
            this.bikes = station.getInt("available_bikes");
            this.lastUpdate = station.getString("last_update");
            this.bikeStands = station.getInt("bike_stands");
        } catch (JSONException e) {
            Log.e("Valenbisi error", "JSONObject could not be created");
        }
    }
}
