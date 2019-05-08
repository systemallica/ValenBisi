package com.systemallica.valenbisi

import android.util.Log

import com.google.android.libraries.maps.model.BitmapDescriptor

import org.json.JSONException
import org.json.JSONObject

class BikeStation(station: JSONObject) {
    var snippet: String? = null
    var icon: BitmapDescriptor? = null
    var alpha: Float? = null
    var visibility: Boolean? = true
    var lat: Double? = null
    var lng: Double? = null
    var address: String = ""
    var status: String = ""
    var spots: Int = 0
    var bikes: Int = 0
    var lastUpdate: String = ""
    var bikeStands: Int = 0
    var number: String = ""
    var name: String = ""
    var isFavourite: Boolean = false

    init {
        try {
            val latLong = station.getJSONObject("position")
            this.lat = latLong.getDouble("lat")
            this.lng = latLong.getDouble("lng")
            this.address = station.getString("address")
            this.status = station.getString("status")
            this.spots = station.getInt("available_bike_stands")
            this.bikes = station.getInt("available_bikes")
            this.lastUpdate = station.getString("last_update")
            this.bikeStands = station.getInt("bike_stands")
            this.number = station.getString("number")
            this.name = station.getString("name")
        } catch (e: JSONException) {
            Log.e("Valenbisi error", "JSONObject could not be created")
        }

    }
}
