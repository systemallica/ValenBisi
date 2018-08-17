package com.systemallica.valenbisi.clustering

import android.content.Context

import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.MarkerOptions
import com.google.maps.android.clustering.ClusterManager
import com.google.maps.android.clustering.view.DefaultClusterRenderer


class IconRenderer(
    context: Context, map: GoogleMap,
    clusterManager: ClusterManager<ClusterPoint>
) : DefaultClusterRenderer<ClusterPoint>(context, map, clusterManager) {

    override fun onBeforeClusterItemRendered(item: ClusterPoint?, markerOptions: MarkerOptions?) {
        markerOptions!!.icon(item!!.icon)
        markerOptions.snippet(item.snippet)
        markerOptions.title(item.title)
        markerOptions.alpha(item.alpha!!)
        markerOptions.visible(item.visibility!!)
        super.onBeforeClusterItemRendered(item, markerOptions)
    }
}