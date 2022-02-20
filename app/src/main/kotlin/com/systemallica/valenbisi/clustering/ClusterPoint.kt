package com.systemallica.valenbisi.clustering

import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.clustering.ClusterItem
import com.systemallica.valenbisi.BikeStation

class ClusterPoint(station: BikeStation) : ClusterItem {

    private var position: LatLng = LatLng(station.lat, station.lng)
    private var title: String = station.address
    private val snippet: String = station.snippet!!
    val icon: BitmapDescriptor = station.icon!!
    var alpha: Float = station.alpha!!
    var visibility: Boolean = station.visibility

    override fun getPosition(): LatLng {
        return this.position
    }

    override fun getTitle(): String {
        return this.title
    }

    override fun getSnippet(): String {
        return this.snippet
    }
}
