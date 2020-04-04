package com.systemallica.valenbisi

import com.google.android.gms.maps.model.BitmapDescriptor
import org.json.JSONObject

class BikeStation(station: JSONObject) {
    var snippet: String? = null
    var icon: BitmapDescriptor? = null
    var alpha: Float? = null
    var visibility: Boolean = true
    var isFavourite: Boolean = false

    private val latLong = station.getJSONObject("position")
    val lat: Double = latLong.getDouble("lat")
    val lng: Double = latLong.getDouble("lng")
    val address: String = station.getString("address")
    val status: String = station.getString("status")
    val spots: Int = station.getInt("available_bike_stands")
    val bikes: Int = station.getInt("available_bikes")
    val lastUpdate: String = station.getString("last_update")
    val number: String = station.getString("number")
    val name: String = station.getString("name")
}
