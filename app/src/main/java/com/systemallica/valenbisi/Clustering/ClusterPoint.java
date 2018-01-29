package com.systemallica.valenbisi.Clustering;

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
    private int mBikes;
    private int mSpots;
    private int mBikeStands;
    private boolean mOnFoot;

    public ClusterPoint(double lat, double lng, String title, String snippet,
                        BitmapDescriptor icon, Float alpha, Boolean visibility,
                        int bikes, int spots, int bikeStands, boolean onFoot) {
        mPosition   = new LatLng(lat, lng);
        mTitle      = title;
        mSnippet    = snippet;
        mIcon       = icon;
        mAlpha      = alpha;
        mVisibility = visibility;
        mBikes      = bikes;
        mSpots      = spots;
        mBikeStands = bikeStands;
        mOnFoot     = onFoot;
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

    public BitmapDescriptor getIcon(){
        return mIcon;
    }

    public Float getAlpha(){
        return mAlpha;
    }

    public void setAlpha(Float alpha){
        mAlpha = alpha;
    }

    public void setVisibility(Boolean visibility){
        mVisibility = visibility;
    }

    Boolean getVisibility(){
        return mVisibility;
    }

    int getBikes(){
        return mBikes;
    }

    int getSpots(){
        return mSpots;
    }

    int getBikeStands(){
        return mBikeStands;
    }

    boolean getMode(){ return mOnFoot; }

}
