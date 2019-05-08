package com.systemallica.valenbisi.clustering

import com.google.android.libraries.maps.model.BitmapDescriptor
import com.google.android.libraries.maps.model.LatLng
import com.google.maps.android.clustering.ClusterItem
import com.systemallica.valenbisi.BikeStation


class ClusterPoint(station: BikeStation) : ClusterItem {
    private var position: LatLng
    private var title: String? = null
    private val snippet: String?
    val icon: BitmapDescriptor?
    var alpha: Float? = null
    var visibility: Boolean? = null

    init {
        this.position = LatLng(station.lat!!, station.lng!!)
        this.title = station.address
        this.snippet = station.snippet
        this.icon = station.icon
        this.alpha = station.alpha
        this.visibility = station.visibility
    }

    override fun getPosition(): LatLng {
        return this.position
    }

    override fun getTitle(): String? {
        return this.title
    }

    override fun getSnippet(): String? {
        return this.snippet
    }
}
