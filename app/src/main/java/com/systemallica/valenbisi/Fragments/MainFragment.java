package com.systemallica.valenbisi.Fragments;

import android.Manifest;
import android.app.Fragment;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.maps.android.geojson.GeoJsonFeature;
import com.google.maps.android.geojson.GeoJsonLayer;
import com.google.maps.android.geojson.GeoJsonLineStringStyle;
import com.google.maps.android.geojson.GeoJsonPointStyle;
import com.systemallica.valenbisi.PrivateInfo;
import com.systemallica.valenbisi.R;
import com.systemallica.valenbisi.TrackGPS;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import butterknife.BindView;
import butterknife.ButterKnife;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class MainFragment extends Fragment implements OnMapReadyCallback {

    public static final String PREFS_NAME = "MyPrefsFile";
    private final static String mLogTag = "GeoJsonDemo";
    String url = PrivateInfo.url;
    UiSettings mapSettings;
    int location_request_code = 1;
    boolean estacionesLayer = true;
    boolean onFoot = true;
    GeoJsonLayer carril = null;
    GeoJsonLayer parking = null;
    View view;
    TrackGPS gps;
    double longitude;
    double latitude;
    private GoogleMap mMap;

    @BindView(R.id.btnCarrilToggle)
    Button btnCarrilToggle;
    @BindView(R.id.btnParkingToggle)
    Button btnParkingToggle;
    @BindView(R.id.btnEstacionesToggle)
    Button btnEstacionesToggle;
    @BindView(R.id.btnOnFootToggle)
    Button btnOnFootToggle;
    @BindView(R.id.btnRefresh)
    Button btnRefresh;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        view = inflater.inflate(R.layout.fragment_main, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {

        //Change toolbar title
        getActivity().setTitle(R.string.nav_map);

        MapView mapView;
        mapView = (MapView) view.findViewById(R.id.map);
        mapView.onCreate(savedInstanceState);
        mapView.onResume();
        mapView.getMapAsync(this);

    }


    @Override
    public void onMapReady(GoogleMap googleMap) {

        //Load icons
        final Drawable myDrawableLaneOn = ContextCompat.getDrawable(getActivity().getApplicationContext(), R.drawable.ic_road_variant_black_24dp);
        final Drawable myDrawableLaneOff = ContextCompat.getDrawable(getActivity().getApplicationContext(), R.drawable.ic_road_variant_off_black_24dp);
        final Drawable myDrawableParkingOn = ContextCompat.getDrawable(getActivity().getApplicationContext(), R.drawable.ic_local_parking_black_24dp);
        //final Drawable myDrawableParkingOff = ContextCompat.getDrawable(getActivity().getApplicationContext(), R.drawable.ic_road_variant_off_black_24dp);

        final SharedPreferences settings = getActivity().getApplicationContext().getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();

        mMap = googleMap;
        boolean satellite = settings.getBoolean("satellite", false);
        editor.putBoolean("firstTime", true).apply();
        editor.putBoolean("firstTimeParking", true).apply();

        //Check for sdk >= 23
        if (Build.VERSION.SDK_INT >= 23) {
            //Check location permission
            if (getActivity().checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {

                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        location_request_code);

            } else {
                mMap.setMyLocationEnabled(true);
            }
        } else {
            mMap.setMyLocationEnabled(true);
        }

        //Set map zoom controls
        mapSettings = mMap.getUiSettings();
        mapSettings.setZoomControlsEnabled(true);
        mapSettings.setCompassEnabled(false);

        //Set type of map and min zoom
        if (!satellite) {
            mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        } else {
            mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
        }
        mMap.setMinZoomPreference(12);

        gps = new TrackGPS(getActivity().getApplicationContext());

        if (gps.canGetLocation()) {

            longitude = gps.getLongitude();
            latitude = gps.getLatitude();

        }
        //39.420//39.515
        //-0.272//-0.572
        LatLng currentLocation = new LatLng(latitude, longitude);
        LatLng valencia = new LatLng(39.479, -0.372);

        boolean initialZoom = settings.getBoolean("initialZoom", true);

        // Move the camera
        if (Build.VERSION.SDK_INT >= 23) {
            //Check location permission
            if (getActivity().checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED && initialZoom && gps.canGetLocation()) {
                if (currentLocation.latitude >= 39.515 || currentLocation.latitude <= 39.420 || currentLocation.longitude >= -0.272 || currentLocation.longitude <= -0.572) {
                    mMap.moveCamera(CameraUpdateFactory.newLatLng(valencia));
                } else {
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 16.0f));
                }
            } else {
                mMap.moveCamera(CameraUpdateFactory.newLatLng(valencia));
            }

        } else {
            if (initialZoom && gps.canGetLocation()) {
                if (currentLocation.latitude >= 39.515 || currentLocation.latitude <= 39.420 || currentLocation.longitude >= -0.272 || currentLocation.longitude <= -0.572) {
                    mMap.moveCamera(CameraUpdateFactory.newLatLng(valencia));
                } else {
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 16.0f));
                }
            } else {
                mMap.moveCamera(CameraUpdateFactory.newLatLng(valencia));
            }
        }

        gps.stopUsingGPS();

        try {
            getStations();
        }catch(IOException e){
            e.printStackTrace();
        }

        if (settings.getBoolean("parkingLayer", false)) {
            editor.putBoolean("parkingLayer", false).apply();
            new GetParking().execute();
        }

        btnCarrilToggle.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (!settings.getBoolean("carrilLayer", false)) {
                    btnCarrilToggle.setCompoundDrawablesWithIntrinsicBounds(myDrawableLaneOn, null, null, null);
                    new GetLanes().execute();
                } else {
                    btnCarrilToggle.setCompoundDrawablesWithIntrinsicBounds(myDrawableLaneOff, null, null, null);
                    new GetLanes().execute();
                }
            }
        });

        btnParkingToggle.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (!settings.getBoolean("parkingLayer", false)) {
                    btnParkingToggle.setCompoundDrawablesWithIntrinsicBounds(myDrawableParkingOn, null, null, null);
                    new GetParking().execute();
                } else {
                    btnParkingToggle.setCompoundDrawablesWithIntrinsicBounds(myDrawableParkingOn, null, null, null);
                    new GetParking().execute();
                }
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == location_request_code) {
            if (ContextCompat.checkSelfPermission(getActivity().getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                mMap.setMyLocationEnabled(true);
            } else {
                mMap.setMyLocationEnabled(false);
                Snackbar.make(view, R.string.no_location_permission, Snackbar.LENGTH_SHORT).show();

            }
        }
    }


    public void getStations() throws IOException{
        final SharedPreferences settings = getActivity().getApplicationContext().getSharedPreferences(PREFS_NAME, 0);
        final SharedPreferences.Editor editor = settings.edit();

        //Load default marker icons
        final BitmapDescriptor icongreen = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN);
        final BitmapDescriptor iconorange = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE);
        final BitmapDescriptor iconyellow = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW);
        final BitmapDescriptor iconred = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED);
        final BitmapDescriptor iconblue = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE);

        //Load icons
        final Drawable myDrawableBike = ContextCompat.getDrawable(getActivity().getApplicationContext(), R.drawable.ic_directions_bike_black_24dp);
        final Drawable myDrawableWalk = ContextCompat.getDrawable(getActivity().getApplicationContext(), R.drawable.ic_directions_walk_black_24dp);
        final Drawable myDrawableStationsOn = ContextCompat.getDrawable(getActivity().getApplicationContext(), R.drawable.ic_place_black_24dp);
        final Drawable myDrawableStationsOff = ContextCompat.getDrawable(getActivity().getApplicationContext(), R.drawable.ic_map_marker_off_black_24dp);
        final Drawable myDrawableFavOn = ContextCompat.getDrawable(getActivity().getApplicationContext(), R.drawable.ic_star_black_24dp);
        final Drawable myDrawableFavOff = ContextCompat.getDrawable(getActivity().getApplicationContext(), R.drawable.ic_star_outline_black_24dp);

        Snackbar.make(view, R.string.load_stations, Snackbar.LENGTH_LONG).show();

        final OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url(url)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {

            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    if (!response.isSuccessful())throw new IOException("Unexpected code " + response);
                    ResponseBody responseBody = response.body();
                    String jsonStrTemp = "";

                    if(responseBody!=null) {
                        jsonStrTemp = responseBody.string();
                    }

                    final String jsonStr = jsonStrTemp;
                    //Log.e("jsonStr1", jsonStr);
                    if(getActivity() != null) {
                        getActivity().runOnUiThread(new Runnable() {
                            public void run() {
                                boolean showAvailable = settings.getBoolean("showAvailable", false);
                                boolean showFavorites = settings.getBoolean("showFavorites", false);
                                boolean voronoiCell = settings.getBoolean("voronoiCell", false);

                                try {
                                    if (!jsonStr.equals("")) {
                                        JSONArray array = new JSONArray(jsonStr);

                                        final GeoJsonLayer layer = new GeoJsonLayer(mMap, R.raw.valencia, getActivity().getApplicationContext());

                                        for (GeoJsonFeature feature : layer.getFeatures()) {  //loop through features
                                            boolean noStationMatch = true;
                                            boolean currentStationIsFav = settings.getBoolean(feature.getProperty("Address"), false);
                                            for (int counter = 0; counter < array.length(); counter++) {
                                                JSONObject object = array.getJSONObject(counter);
                                                //Add each number and address to its correspondent marker

                                                if (object.getString("number").equals(feature.getProperty("Number"))) {
                                                    noStationMatch = false;
                                                    GeoJsonPointStyle pointStyle = new GeoJsonPointStyle();
                                                    pointStyle.setTitle(feature.getProperty("Address"));
                                                    pointStyle.setSnippet(getString(R.string.spots) + " " + object.getInt("available_bike_stands") + " - " + getString(R.string.bikes) + " " + object.getInt("available_bikes"));
                                                    pointStyle.setAlpha((float) 0.5);

                                                    //set markers colors depending on available bikes/stands
                                                    if (onFoot) {
                                                        if (object.getInt("available_bikes") == 0) {
                                                            pointStyle.setIcon(iconred);
                                                            if (showAvailable) {
                                                                pointStyle.setVisible(false);
                                                            }
                                                        } else if (object.getInt("available_bikes") < 5) {
                                                            pointStyle.setIcon(iconorange);
                                                        } else if (object.getInt("available_bikes") < 10) {
                                                            pointStyle.setIcon(iconyellow);
                                                        } else {
                                                            pointStyle.setIcon(icongreen);
                                                        }
                                                    } else {
                                                        if (object.getInt("available_bike_stands") == 0) {
                                                            pointStyle.setIcon(iconred);
                                                            if (showAvailable) {
                                                                pointStyle.setVisible(false);
                                                            }
                                                        } else if (object.getInt("available_bike_stands") < 5) {
                                                            pointStyle.setIcon(iconorange);
                                                        } else if (object.getInt("available_bike_stands") < 10) {
                                                            pointStyle.setIcon(iconyellow);
                                                        } else {
                                                            pointStyle.setIcon(icongreen);
                                                        }
                                                    }

                                                    //Apply full opacity to fav stations
                                                    if (currentStationIsFav) {
                                                        pointStyle.setAlpha(1);
                                                    }

                                                    //If favorites r selected, hide the rest
                                                    if (showFavorites) {
                                                        if (!currentStationIsFav) {
                                                            pointStyle.setVisible(false);
                                                        }
                                                    }
                                                    feature.setPointStyle(pointStyle);
                                                }
                                            }
                                            if (noStationMatch) {
                                                GeoJsonPointStyle pointStyle = new GeoJsonPointStyle();
                                                pointStyle.setTitle("No data available :(");
                                                pointStyle.setSnippet("No data available :(");
                                                pointStyle.setIcon(iconblue);
                                                pointStyle.setAlpha((float) 0.5);
                                                if (showAvailable) {
                                                    pointStyle.setVisible(false);
                                                }
                                                feature.setPointStyle(pointStyle);
                                            }
                                        }

                                        if (voronoiCell) {
                                            try {
                                                final GeoJsonLayer voronoi = new GeoJsonLayer(mMap, R.raw.voronoi, getActivity().getApplicationContext());
                                                for (GeoJsonFeature feature : voronoi.getFeatures()) {
                                                    GeoJsonLineStringStyle stringStyle = voronoi.getDefaultLineStringStyle();
                                                    stringStyle.setColor(-16776961);
                                                    stringStyle.setWidth(2);
                                                    feature.setLineStringStyle(stringStyle);
                                                }
                                                voronoi.addLayerToMap();
                                            } catch (JSONException e) {
                                                Log.e(mLogTag, "JSONArray could not be created");
                                            } catch (IOException e) {
                                                Log.e(mLogTag, "GeoJSON file could not be read");
                                            }
                                        }

                                        if (estacionesLayer) {
                                            layer.addLayerToMap();
                                        }

                                        mMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {

                                            // Use default InfoWindow frame
                                            @Override
                                            public View getInfoWindow(Marker marker) {
                                                return null;
                                            }

                                            // Defines the contents of the InfoWindow
                                            @Override
                                            public View getInfoContents(Marker marker) {

                                                // Getting view from the layout file info_window_layout
                                                final View v = getActivity().getLayoutInflater().inflate(R.layout.windowlayout, null);

                                                // Getting reference to the ImageView/title/snippet
                                                TextView title = (TextView) v.findViewById(R.id.title);
                                                TextView snippet = (TextView) v.findViewById(R.id.snippet);
                                                ImageView btn_star = (ImageView) v.findViewById(R.id.btn_star);

                                                title.setText(marker.getTitle());
                                                snippet.setText(marker.getSnippet());

                                                //Checking if current station is favorite
                                                boolean currentStationIsFav = settings.getBoolean(marker.getTitle(), false);

                                                //Setting correspondent icon
                                                if (currentStationIsFav) {
                                                    btn_star.setImageDrawable(myDrawableFavOn);
                                                } else {
                                                    btn_star.setImageDrawable(myDrawableFavOff);
                                                }

                                                mMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
                                                    @Override
                                                    public void onInfoWindowClick(Marker marker) {

                                                        boolean currentStationIsFav = settings.getBoolean(marker.getTitle(), false);
                                                        boolean showFavorites = settings.getBoolean("showFavorites", false);

                                                        if (currentStationIsFav) {
                                                            marker.setAlpha((float) 0.5);
                                                            if (showFavorites) {
                                                                marker.setVisible(false);
                                                            }
                                                            marker.showInfoWindow();
                                                            editor.putBoolean(marker.getTitle(), false);
                                                            editor.apply();
                                                        } else {
                                                            marker.setAlpha(1);
                                                            marker.showInfoWindow();
                                                            editor.putBoolean(marker.getTitle(), true);
                                                            editor.apply();
                                                        }
                                                        marker.showInfoWindow();
                                                        //Log.e("map marker", "marker is  " + marker.getTitle());

                                                    }
                                                });

                                                // Returning the view containing InfoWindow contents
                                                return v;
                                            }
                                        });

                                        //Toggle Stations
                                        btnEstacionesToggle.setOnClickListener(new View.OnClickListener() {
                                            public void onClick(View v) {
                                                boolean showFavorites = settings.getBoolean("showFavorites", false);

                                                for (GeoJsonFeature feature : layer.getFeatures()) {
                                                    GeoJsonPointStyle pointStyle = feature.getPointStyle();
                                                    boolean currentStationIsFav = settings.getBoolean(feature.getProperty("Address"), false);
                                                    if (estacionesLayer) {
                                                        pointStyle.setVisible(false);
                                                    } else {
                                                        if (showFavorites && currentStationIsFav) {
                                                            pointStyle.setVisible(true);
                                                        } else if (!showFavorites) {
                                                            pointStyle.setVisible(true);
                                                        }
                                                    }
                                                    feature.setPointStyle(pointStyle);
                                                }

                                                if (estacionesLayer) {
                                                    btnEstacionesToggle.setCompoundDrawablesWithIntrinsicBounds(myDrawableStationsOff, null, null, null);
                                                    estacionesLayer = false;
                                                } else {
                                                    btnEstacionesToggle.setCompoundDrawablesWithIntrinsicBounds(myDrawableStationsOn, null, null, null);
                                                    estacionesLayer = true;
                                                }
                                            }
                                        });

                                        //Toggle onFoot/onBike
                                        btnOnFootToggle.setOnClickListener(new View.OnClickListener() {
                                            public void onClick(View v) {
                                                layer.removeLayerFromMap();
                                                if (onFoot) {
                                                    onFoot = false;
                                                    btnOnFootToggle.setCompoundDrawablesWithIntrinsicBounds(myDrawableBike, null, null, null);
                                                    try {
                                                        getStations();
                                                    } catch (IOException e) {
                                                        e.printStackTrace();
                                                    }

                                                } else {
                                                    onFoot = true;
                                                    btnOnFootToggle.setCompoundDrawablesWithIntrinsicBounds(myDrawableWalk, null, null, null);
                                                    try {
                                                        getStations();
                                                    } catch (IOException e) {
                                                        e.printStackTrace();
                                                    }
                                                }
                                            }
                                        });

                                        //Reload data
                                        btnRefresh.setOnClickListener(new View.OnClickListener() {
                                            public void onClick(View v) {
                                                layer.removeLayerFromMap();
                                                try {
                                                    getStations();
                                                } catch (IOException e) {
                                                    e.printStackTrace();
                                                }
                                            }
                                        });

                                    }

                                } catch (JSONException e) {

                                    Log.e(mLogTag, "JSONArray could not be created");

                                } catch (IOException e) {

                                    Log.e(mLogTag, "GeoJSON file could not be read");
                                }
                            }
                        });
                    }

                }finally {
                    if (response != null) {
                        response.close();
                    }
                }

            }
        });



    }

    private class GetLanes extends AsyncTask<Void, Void, GeoJsonLayer> {
        final SharedPreferences settings = getActivity().getApplicationContext().getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();

        protected void onPreExecute() {
            if (!settings.getBoolean("carrilLayer", false)) {
                Snackbar.make(view, R.string.load_lanes, Snackbar.LENGTH_LONG).show();

            }
        }

        @Override
        protected GeoJsonLayer doInBackground(Void... params) {

            try {
                //lanes layer
                if (settings.getBoolean("firstTime", true)) {
                    carril = new GeoJsonLayer(mMap, R.raw.oficialcarril, getActivity().getApplicationContext());
                    for (GeoJsonFeature feature : carril.getFeatures()) {
                        GeoJsonLineStringStyle stringStyle = carril.getDefaultLineStringStyle();
                        stringStyle.setWidth(5);
                        feature.setLineStringStyle(stringStyle);
                    }
                    editor.putBoolean("firstTime", false).apply();
                }

            } catch (IOException e) {

                Log.e(mLogTag, "GeoJSON file could not be read");

            } catch (JSONException e) {

                Log.e(mLogTag, "GeoJSON file could not be converted to a JSONObject");
            }

            return carril;
        }

        protected void onPostExecute(GeoJsonLayer carril) {

            if (!settings.getBoolean("carrilLayer", false)) {
                carril.addLayerToMap();
                editor.putBoolean("carrilLayer", true).apply();
            } else {
                carril.removeLayerFromMap();
                editor.putBoolean("carrilLayer", false).apply();
                editor.putBoolean("firstTime", true).apply();

            }

        }
    }

    private class GetParking extends AsyncTask<Void, Void, GeoJsonLayer> {
        final SharedPreferences settings = getActivity().getApplicationContext().getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();

        protected void onPreExecute() {
            if (!settings.getBoolean("parkingLayer", false)) {
                Snackbar.make(view, R.string.load_parking, Snackbar.LENGTH_SHORT).show();

            }
        }

        @Override
        protected GeoJsonLayer doInBackground(Void... params) {
            boolean showFavorites = settings.getBoolean("showFavorites", false);

            BitmapDescriptor icon_blue = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE);

            try {
                //parking layer
                if (settings.getBoolean("firstTimeParking", true)) {
                    parking = new GeoJsonLayer(mMap, R.raw.aparcabicis, getActivity().getApplicationContext());
                    for (GeoJsonFeature feature : parking.getFeatures()) {
                        GeoJsonPointStyle pointStyle = new GeoJsonPointStyle();
                        pointStyle.setTitle(getString(R.string.parking) + " " + feature.getProperty("id"));
                        pointStyle.setSnippet(getString(R.string.plazas) + " " + feature.getProperty("plazas"));
                        pointStyle.setAlpha((float) 0.5);
                        pointStyle.setIcon(icon_blue);

                        boolean currentStationIsFav = settings.getBoolean(pointStyle.getTitle(), false);

                        //Apply full opacity to fav stations
                        if (currentStationIsFav) {
                            pointStyle.setAlpha(1);
                        }

                        //If favorites are selected, hide the rest
                        if (showFavorites) {
                            if (!currentStationIsFav) {
                                pointStyle.setVisible(false);
                            }
                        }
                        feature.setPointStyle(pointStyle);

                    }
                    editor.putBoolean("firstTimeParking", false).apply();
                }

            } catch (IOException e) {

                Log.e(mLogTag, "GeoJSON file could not be read");

            } catch (JSONException e) {

                Log.e(mLogTag, "GeoJSON file could not be converted to a JSONObject");
            }

            return parking;
        }

        protected void onPostExecute(GeoJsonLayer parking) {

            if (!settings.getBoolean("parkingLayer", false)) {
                parking.addLayerToMap();
                editor.putBoolean("parkingLayer", true).apply();
            } else {
                parking.removeLayerFromMap();
                editor.putBoolean("parkingLayer", false).apply();
                editor.putBoolean("firstTimeParking", true).apply();
            }
        }
    }

    @Override
    public void onPause(){
        super.onPause();
        if(mMap != null && getActivity() != null) {
            if (Build.VERSION.SDK_INT >= 23) {
                //Check location permission
                if (getActivity().checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    mMap.setMyLocationEnabled(false);
                }
            } else {
                mMap.setMyLocationEnabled(false);
            }
        }

    }
}
