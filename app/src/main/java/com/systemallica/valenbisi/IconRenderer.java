package com.systemallica.valenbisi;

import android.content.Context;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.view.DefaultClusterRenderer;


public class IconRenderer extends DefaultClusterRenderer<ClusterPoint> {

    public IconRenderer(Context context, GoogleMap map,
                        ClusterManager<ClusterPoint> clusterManager) {
        super(context, map, clusterManager);
    }

    @Override
    protected void onBeforeClusterItemRendered(ClusterPoint item, MarkerOptions markerOptions) {
        markerOptions.icon(item.getIcon());
        markerOptions.snippet(item.getSnippet());
        markerOptions.title(item.getTitle());
        markerOptions.alpha(item.getAlpha());
        markerOptions.visible(item.getVisibility());
        super.onBeforeClusterItemRendered(item, markerOptions);
    }
}