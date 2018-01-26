package com.systemallica.valenbisi;

import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.ClusterItem;


public class ClusterPoint implements ClusterItem {
    private final LatLng mPosition;
    private String mTitle;
    private String mSnippet;
    private BitmapDescriptor mIcon;
    private Float mAlpha;
    private Boolean mVisibility;

    public ClusterPoint(double lat, double lng, String title, String snippet,
                        BitmapDescriptor icon, Float alpha, Boolean visibility) {
        mPosition = new LatLng(lat, lng);
        mTitle = title;
        mSnippet = snippet;
        mIcon = icon;
        mAlpha = alpha;
        mVisibility = visibility;
    }

    @Override
    public LatLng getPosition() {
        return mPosition;
    }

    @Override
    public String getTitle() { return mTitle; }

    @Override
    public String getSnippet() { return mSnippet; }

    /**
     * Set the title of the marker
     * @param title string to be set as title
     */
    public void setTitle(String title) {
        mTitle = title;
    }

    /**
     * Set the description of the marker
     * @param snippet string to be set as snippet
     */
    public void setSnippet(String snippet) {
        mSnippet = snippet;
    }

    public BitmapDescriptor getIcon(){
        return mIcon;
    }

    public Float getAlpha(){
        return mAlpha;
    }

    public Boolean getVisibility(){
        return mVisibility;
    }

}
