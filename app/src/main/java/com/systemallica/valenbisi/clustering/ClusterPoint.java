package com.systemallica.valenbisi.clustering;

import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.ClusterItem;
import com.systemallica.valenbisi.BikeStation;


public class ClusterPoint implements ClusterItem {
    private final LatLng position;
    private String title;
    private String snippet;
    private BitmapDescriptor icon;
    private Float alpha;
    private Boolean visibility;
    private int bikes;
    private int spots;
    private int bikeStands;
    private boolean onFoot;

    public ClusterPoint(BikeStation station, Boolean onFoot) {
        this.position   = new LatLng(station.lat, station.lng);
        this.title      = station.address;
        this.snippet    = station.snippet;
        this.icon       = station.icon;
        this.alpha      = station.alpha;
        this.visibility = station.visibility;
        this.bikes      = station.bikes;
        this.spots      = station.spots;
        this.bikeStands = station.bikeStands;
        this.onFoot     = onFoot;
    }

    @Override
    public LatLng getPosition() {
        return this.position;
    }

    @Override
    public String getTitle() { return this.title; }

    @Override
    public String getSnippet() { return this.snippet; }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setAlpha(Float alpha){
        this.alpha = alpha;
    }

    public Float getAlpha(){
        return this.alpha;
    }

    public void setVisibility(Boolean visibility){
        this.visibility = visibility;
    }

    Boolean getVisibility(){
        return this.visibility;
    }

    public BitmapDescriptor getIcon(){
        return this.icon;
    }
}
