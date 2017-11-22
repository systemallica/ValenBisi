package com.systemallica.valenbisi.Fragments;

import android.Manifest;
import android.app.Fragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import com.google.maps.android.geojson.GeoJsonPoint;
import com.google.maps.android.geojson.GeoJsonPointStyle;
import com.systemallica.valenbisi.PrivateInfo;
import com.systemallica.valenbisi.R;
import com.systemallica.valenbisi.TrackGPS;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Calendar;
import java.util.Formatter;
import java.util.GregorianCalendar;
import java.util.HashMap;

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
    private String url = PrivateInfo.url;
    int locationRequestCode = 1;
    boolean stationsLayer = true;
    boolean onFoot = true;
    private GeoJsonLayer lanes = null;
    private GeoJsonLayer parking = null;
    private View view;
    double longitude;
    double latitude;
    private GoogleMap mMap;
    private Context context;

    @BindView(R.id.btnLanesToggle)
    Button btnLanesToggle;
    @BindView(R.id.btnParkingToggle)
    Button btnParkingToggle;
    @BindView(R.id.btnStationsToggle)
    Button btnStationsToggle;
    @BindView(R.id.btnOnFootToggle)
    Button btnOnFootToggle;
    @BindView(R.id.btnRefresh)
    Button btnRefresh;

    public MainFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        view = inflater.inflate(R.layout.fragment_main, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {

        // Change toolbar title
        getActivity().setTitle(R.string.nav_map);
        // Store context in variable
        context = getActivity().getApplicationContext();

        MapView mapView;
        mapView = view.findViewById(R.id.map);
        mapView.onCreate(savedInstanceState);
        mapView.onResume();
        mapView.getMapAsync(this);

    }


    @Override
    public void onMapReady(GoogleMap googleMap) {

        // Load icons
        final Drawable myDrawableLaneOn = ContextCompat.getDrawable(context, R.drawable.ic_road_variant_black_24dp);
        final Drawable myDrawableLaneOff = ContextCompat.getDrawable(context, R.drawable.ic_road_variant_off_black_24dp);
        final Drawable myDrawableParkingOn = ContextCompat.getDrawable(context, R.drawable.ic_local_parking_black_24dp);
        // Map settings
        UiSettings mapSettings;
        // GPS object
        TrackGPS gps;
        // User preferences
        final SharedPreferences settings = context.getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();

        mMap = googleMap;
        boolean satellite = settings.getBoolean("satellite", false);
        editor.putBoolean("firstTime", true).apply();
        editor.putBoolean("firstTimeParking", true).apply();
        editor.putBoolean("carrilLayer", false).apply();

        //Check for sdk >= 23
        if (Build.VERSION.SDK_INT >= 23) {
            //Check location permission
            if (getActivity().checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {

                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        locationRequestCode);

            } else {
                mMap.setMyLocationEnabled(true);
            }
        } else {
            mMap.setMyLocationEnabled(true);
        }

        // Set map zoom controls
        mapSettings = mMap.getUiSettings();
        mapSettings.setZoomControlsEnabled(true);
        mapSettings.setCompassEnabled(false);

        // Set type of map and min zoom
        if (!satellite) {
            mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        } else {
            mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
        }
        mMap.setMinZoomPreference(12);

        gps = new TrackGPS(context);

        if (gps.canGetLocation()) {

            longitude = gps.getLongitude();
            latitude = gps.getLatitude();

        }
        // 39.420//39.515
        // -0.272//-0.572
        LatLng currentLocation = new LatLng(latitude, longitude);
        LatLng valencia = new LatLng(39.479, -0.372);

        boolean initialZoom = settings.getBoolean("initialZoom", true);

        // Move the camera
        if (Build.VERSION.SDK_INT >= 23) {
            // Check location permission
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

        btnLanesToggle.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (!settings.getBoolean("carrilLayer", false)) {
                    btnLanesToggle.setCompoundDrawablesWithIntrinsicBounds(myDrawableLaneOn, null, null, null);
                    new GetLanes().execute();
                } else {
                    btnLanesToggle.setCompoundDrawablesWithIntrinsicBounds(myDrawableLaneOff, null, null, null);
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
        if (requestCode == locationRequestCode) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                mMap.setMyLocationEnabled(true);
            } else {
                mMap.setMyLocationEnabled(false);
                Snackbar.make(view, R.string.no_location_permission, Snackbar.LENGTH_SHORT).show();

            }
        }
    }

    public void getStations() throws IOException{

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

                    final String jsonData = jsonStrTemp;
                    applyJSONData(jsonData);

                }finally {
                    if (response != null) {
                        response.close();
                    }
                }

            }
        });
    }

    public void applyJSONData(final String jsonData){

        final SharedPreferences settings = context.getSharedPreferences(PREFS_NAME, 0);
        final SharedPreferences.Editor editor = settings.edit();

        // Load default marker icons
        final BitmapDescriptor iconGreen = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN);
        final BitmapDescriptor iconOrange = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE);
        final BitmapDescriptor iconYellow = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW);
        final BitmapDescriptor iconRed = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED);
        final BitmapDescriptor iconViolet = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET);

        // Load icons
        final Drawable myDrawableBike = ContextCompat.getDrawable(context, R.drawable.ic_directions_bike_black_24dp);
        final Drawable myDrawableWalk = ContextCompat.getDrawable(context, R.drawable.ic_directions_walk_black_24dp);
        final Drawable myDrawableStationsOn = ContextCompat.getDrawable(context, R.drawable.ic_place_black_24dp);
        final Drawable myDrawableStationsOff = ContextCompat.getDrawable(context, R.drawable.ic_map_marker_off_black_24dp);
        final Drawable myDrawableFavOn = ContextCompat.getDrawable(context, R.drawable.ic_star_black_24dp);
        final Drawable myDrawableFavOff = ContextCompat.getDrawable(context, R.drawable.ic_star_outline_black_24dp);
        final Drawable myDrawableLaneOn = ContextCompat.getDrawable(context, R.drawable.ic_road_variant_black_24dp);

        if(getActivity() != null) {
            getActivity().runOnUiThread(new Runnable() {
                public void run() {
                    // Get user preferences
                    boolean showAvailable = settings.getBoolean("showAvailable", false);
                    boolean showFavorites = settings.getBoolean("showFavorites", false);
                    boolean voronoiCell = settings.getBoolean("voronoiCell", false);
                    boolean bikeLanes = settings.getBoolean("bikeLanes", false);
                    boolean lastUpdated = settings.getBoolean("lastUpdated", true);
                    // Create empty GeoJsonLayer
                    JSONObject dummy = new JSONObject();
                    final GeoJsonLayer layer = new GeoJsonLayer(mMap, dummy);

                    try {
                        // If data is not empty
                        if (!jsonData.equals("")) {
                            // Show loading message
                            Snackbar.make(view, R.string.load_stations, Snackbar.LENGTH_SHORT).show();

                            JSONArray jsonDataArray = new JSONArray(jsonData);

                            // Parse data from API
                            for(int i = 0; i < jsonDataArray.length(); i++){
                                // Get current station position
                                JSONObject station = jsonDataArray.getJSONObject(i);
                                JSONObject latLong = station.getJSONObject("position");
                                Double lat = latLong.getDouble("lat");
                                Double lng = latLong.getDouble("lng");
                                // Create point in map
                                GeoJsonPoint point = new GeoJsonPoint(new LatLng(lat, lng));
                                // Add properties
                                HashMap<String, String> properties = new HashMap<>();
                                properties.put("name", station.getString("name"));
                                properties.put("number", station.getString("number"));
                                properties.put("address", station.getString("address"));
                                properties.put("status", station.getString("status"));
                                properties.put("available_bike_stands", station.getString("available_bike_stands"));
                                properties.put("available_bikes", station.getString("available_bikes"));
                                properties.put("last_updated", station.getString("last_update"));
                                properties.put("status", station.getString("status"));
                                // Transform in GeoJsonFeature
                                GeoJsonFeature pointFeature = new GeoJsonFeature(point, "Origin", properties, null);
                                // Add feature to GeoJsonLayer
                                layer.addFeature(pointFeature);
                            }

                            // Add parsed data to each point in map
                            for (GeoJsonFeature feature : layer.getFeatures()) {  // loop through features

                                // Find if current station is marked as favourite
                                boolean currentStationIsFav = settings.getBoolean(feature.getProperty("address"), false);

                                // Add title
                                GeoJsonPointStyle pointStyle = new GeoJsonPointStyle();
                                pointStyle.setTitle(feature.getProperty("address"));
                                // If the station is open
                                if(feature.getProperty("status").equals("OPEN")) {
                                    // Add number of available bikes/stands
                                    pointStyle.setSnippet(MainFragment.this.getResources().getString(R.string.spots) + " " +
                                            feature.getProperty("available_bike_stands") + " - " +
                                            MainFragment.this.getResources().getString(R.string.bikes) + " " +
                                            feature.getProperty("available_bikes"));

                                    int available_bikes = Integer.parseInt(feature.getProperty("available_bikes"));
                                    int available_bike_stands = Integer.parseInt(feature.getProperty("available_bike_stands"));

                                    // Set markers colors depending on available bikes/stands
                                    if (onFoot) {
                                        if (available_bikes == 0) {
                                            pointStyle.setIcon(iconRed);
                                            if (showAvailable) {
                                                pointStyle.setVisible(false);
                                            }
                                        } else if (available_bikes < 5) {
                                            pointStyle.setIcon(iconOrange);
                                        } else if (available_bikes < 10) {
                                            pointStyle.setIcon(iconYellow);
                                        } else {
                                            pointStyle.setIcon(iconGreen);
                                        }
                                    } else {
                                        if (available_bike_stands == 0) {
                                            pointStyle.setIcon(iconRed);
                                            if (showAvailable) {
                                                pointStyle.setVisible(false);
                                            }
                                        } else if (available_bike_stands < 5) {
                                            pointStyle.setIcon(iconOrange);
                                        } else if (available_bike_stands < 10) {
                                            pointStyle.setIcon(iconYellow);
                                        } else {
                                            pointStyle.setIcon(iconGreen);
                                        }
                                    }

                                    // Get API time
                                    long apiTime = Long.parseLong(feature.getProperty("last_updated"));
                                    // Create calendar object
                                    Calendar date = new GregorianCalendar();
                                    // Get current time
                                    long time = date.getTimeInMillis();
                                    // Add last updated time if user has checked that option
                                    if(lastUpdated){
                                        // Set API time
                                        date.setTimeInMillis(apiTime);
                                        // Format time as HH:mm:ss
                                        StringBuilder sbu = new StringBuilder();
                                        Formatter fmt = new Formatter(sbu);
                                        fmt.format("%tT", date.getTime());
                                        // Add to pointStyle
                                        pointStyle.setSnippet(pointStyle.getSnippet() +
                                                "\n"+
                                                MainFragment.this.getResources().getString(R.string.last_updated) + " " +
                                                sbu);
                                    }
                                    // If data has not been updated for more than 1 hour
                                    if((time-apiTime) > 3600000){
                                        // Add warning that data may be unreliable
                                        pointStyle.setSnippet(pointStyle.getSnippet() +
                                                "\n\n" +
                                                MainFragment.this.getResources().getString(R.string.data_old) +
                                                "\n" +
                                                MainFragment.this.getResources().getString(R.string.data_unreliable));
                                    }

                                }else{
                                    // Add "CLOSED" snippet and icon
                                    pointStyle.setSnippet(MainFragment.this.getResources().getString(R.string.closed));
                                    pointStyle.setIcon(iconViolet);
                                }

                                // Set transparency
                                pointStyle.setAlpha((float) 0.5);

                                // Apply full opacity to favourite stations
                                if (currentStationIsFav) {
                                    pointStyle.setAlpha(1);
                                }

                                // If displaying only favorites are selected, hide the rest
                                if (showFavorites) {
                                    if (!currentStationIsFav) {
                                        pointStyle.setVisible(false);
                                    }
                                }
                                // Set the style to the point in the map
                                feature.setPointStyle(pointStyle);
                            }

                            if (stationsLayer) {
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
                                    TextView title = v.findViewById(R.id.title);
                                    TextView snippet = v.findViewById(R.id.snippet);
                                    ImageView btn_star = v.findViewById(R.id.btn_star);

                                    title.setText(marker.getTitle());
                                    snippet.setText(marker.getSnippet());

                                    // Checking if current station is favorite
                                    boolean currentStationIsFav = settings.getBoolean(marker.getTitle(), false);

                                    // Setting correspondent icon
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
                                        }
                                    });

                                    // Returning the view containing InfoWindow contents
                                    return v;
                                }
                            });
                            // Toggle Stations
                            btnStationsToggle.setOnClickListener(new View.OnClickListener() {
                                public void onClick(View v) {
                                    boolean showFavorites = settings.getBoolean("showFavorites", false);

                                    for (GeoJsonFeature feature : layer.getFeatures()) {
                                        GeoJsonPointStyle pointStyle = feature.getPointStyle();
                                        boolean currentStationIsFav = settings.getBoolean(feature.getProperty("Address"), false);
                                        if (stationsLayer) {
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

                                    if (stationsLayer) {
                                        btnStationsToggle.setCompoundDrawablesWithIntrinsicBounds(myDrawableStationsOff, null, null, null);
                                        stationsLayer = false;
                                    } else {
                                        btnStationsToggle.setCompoundDrawablesWithIntrinsicBounds(myDrawableStationsOn, null, null, null);
                                        stationsLayer = true;
                                    }
                                }
                            });

                            // Toggle onFoot/onBike
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

                            // Reload data
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
                        // Show message if API response is empty
                        }else{
                            Snackbar.make(view, R.string.no_data, Snackbar.LENGTH_LONG).show();
                        }

                        if(bikeLanes){
                            btnLanesToggle.setCompoundDrawablesWithIntrinsicBounds(myDrawableLaneOn, null, null, null);
                            new GetLanes().execute();
                        }

                        if (voronoiCell) {
                            try {
                                final GeoJsonLayer voronoi = new GeoJsonLayer(mMap, R.raw.voronoi, context);
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

                    } catch (JSONException e) {

                        Log.e(mLogTag, "JSONArray could not be created");

                    }
                }
            });
        }
    }

    private class GetLanes extends AsyncTask<Void, Void, GeoJsonLayer> {


        final SharedPreferences settings = context.getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();

        protected void onPreExecute() {
            if (!settings.getBoolean("carrilLayer", false)) {
                Snackbar.make(view, R.string.load_lanes, Snackbar.LENGTH_LONG).show();

            }
        }

        @Override
        protected GeoJsonLayer doInBackground(Void... params) {

            try {
                // lanes layer
                if (settings.getBoolean("firstTime", true)) {
                    lanes = new GeoJsonLayer(mMap, R.raw.bike_lanes_0917, context);
                    for (GeoJsonFeature feature : lanes.getFeatures()) {
                        GeoJsonLineStringStyle stringStyle = lanes.getDefaultLineStringStyle();
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

            return lanes;
        }

        protected void onPostExecute(GeoJsonLayer lanes) {
            boolean bikeLanes = settings.getBoolean("bikeLanes", false);

            if(bikeLanes && !settings.getBoolean("carrilLayer", false)) {
                lanes.addLayerToMap();
                editor.putBoolean("carrilLayer", true).apply();
            }else{
                if (!settings.getBoolean("carrilLayer", false)) {
                    lanes.addLayerToMap();
                    editor.putBoolean("carrilLayer", true).apply();
                } else {
                    lanes.removeLayerFromMap();
                    editor.putBoolean("carrilLayer", false).apply();
                    editor.putBoolean("firstTime", true).apply();
                }
            }
        }
    }

    private class GetParking extends AsyncTask<Void, Void, GeoJsonLayer> {
        final SharedPreferences settings = context.getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();

        protected void onPreExecute() {
            if (!settings.getBoolean("parkingLayer", false)) {
                Snackbar.make(view, R.string.load_parking, Snackbar.LENGTH_SHORT).show();

            }
        }

        @Override
        protected GeoJsonLayer doInBackground(Void... params) {
            boolean showFavorites = settings.getBoolean("showFavorites", false);

            Bitmap bm = BitmapFactory.decodeResource(getResources(), R.drawable.ic_map_marker_radius_black_48dp);
            bm = Bitmap.createScaledBitmap(bm, 50, 50, false);

            BitmapDescriptor icon_parking = BitmapDescriptorFactory.fromBitmap(bm);

            try {
                // parking layer
                if (settings.getBoolean("firstTimeParking", true)) {
                    parking = new GeoJsonLayer(mMap, R.raw.aparcabicis, context);
                    for (GeoJsonFeature feature : parking.getFeatures()) {
                        GeoJsonPointStyle pointStyle = new GeoJsonPointStyle();
                        pointStyle.setTitle(MainFragment.this.getResources().getString(R.string.parking) + " " + feature.getProperty("id"));
                        pointStyle.setSnippet(MainFragment.this.getResources().getString(R.string.plazas) + " " + feature.getProperty("plazas"));
                        pointStyle.setAlpha((float) 0.5);
                        pointStyle.setIcon(icon_parking);

                        boolean currentStationIsFav = settings.getBoolean(pointStyle.getTitle(), false);

                        // Apply full opacity to fav stations
                        if (currentStationIsFav) {
                            pointStyle.setAlpha(1);
                        }

                        // If favorites are selected, hide the rest
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
                // Check location permission
                if (getActivity().checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    // Re-enable location as the user returns to the app
                    mMap.setMyLocationEnabled(false);
                }
            } else {
                // Disable location to avoid battery drain
                mMap.setMyLocationEnabled(false);
            }
        }

    }

    @Override
    public void onResume(){
        super.onResume();
        if(mMap != null && getActivity() != null) {
            if (Build.VERSION.SDK_INT >= 23) {
                // Check location permission
                if (getActivity().checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    mMap.setMyLocationEnabled(true);
                }
            } else {
                mMap.setMyLocationEnabled(true);
            }
        }
    }
}
