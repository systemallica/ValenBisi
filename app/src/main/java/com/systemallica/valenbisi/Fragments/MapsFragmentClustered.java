package com.systemallica.valenbisi.fragments;

import android.Manifest;
import android.app.Fragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
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
import com.google.maps.android.clustering.Cluster;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.data.geojson.GeoJsonFeature;
import com.google.maps.android.data.geojson.GeoJsonLayer;
import com.google.maps.android.data.geojson.GeoJsonLineStringStyle;
import com.google.maps.android.data.geojson.GeoJsonPointStyle;
import com.systemallica.valenbisi.clustering.ClusterPoint;
import com.systemallica.valenbisi.clustering.IconRenderer;
import com.systemallica.valenbisi.R;
import com.systemallica.valenbisi.services.TrackGPSService;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Calendar;
import java.util.Formatter;
import java.util.GregorianCalendar;

import butterknife.BindView;
import butterknife.ButterKnife;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class MapsFragmentClustered extends Fragment implements OnMapReadyCallback {

    private static final String PREFS_NAME = "MyPrefsFile";
    private final static String mLogTag = "GeoJsonDemo";
    private int locationRequestCode = 1;
    private boolean stationsLayer = true;
    private boolean onFoot = true;
    private GeoJsonLayer lanes = null;
    private GeoJsonLayer parking = null;
    private View view;
    private GoogleMap mMap;
    private Context context;
    private ClusterManager<ClusterPoint> mClusterManager = null;
    private SharedPreferences settings;
    private SharedPreferences.Editor settingsEditor;

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

    public MapsFragmentClustered() {
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
        // Store context in member variable
        context = getActivity().getApplicationContext();

        MapView mapView = view.findViewById(R.id.map);
        mapView.onCreate(savedInstanceState);
        mapView.onResume();
        mapView.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap map) {
        // Store map in member variable
        mMap = map;
        onMapReadyHandler();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == locationRequestCode) {
            if (isLocationPermissionGranted()) {
                setLocationButtonEnabled(true);
            } else {
                setLocationButtonEnabled(false);
                Snackbar.make(view, R.string.no_location_permission, Snackbar.LENGTH_SHORT).show();
            }
        }
    }

    private void onMapReadyHandler() {
        if (isApplicationReady()) {

            initPreferences();

            initClusterManager();

            initMap();

            setInitialPosition();

            getStations();

            final SharedPreferences.Editor editor = settings.edit();
            if (settings.getBoolean("parkingLayer", false)) {
                editor.putBoolean("parkingLayer", false).apply();
                new GetParking().execute();
            }
        }
    }

    private boolean isApplicationReady() {
        return isAdded() && getActivity() != null;
    }

    private void initClusterManager() {
        // Load ClusterManager to the Map
        mClusterManager = new ClusterManager<>(context, mMap);
        // Set custom renderer
        mClusterManager.setRenderer(new IconRenderer(getActivity().getApplicationContext(), mMap, mClusterManager));
    }

    private void initPreferences() {
        settings = context.getSharedPreferences(PREFS_NAME, 0);
        settingsEditor = settings.edit();
        settingsEditor.putBoolean("firstTime", true).apply();
        settingsEditor.putBoolean("firstTimeParking", true).apply();
        settingsEditor.putBoolean("carrilLayer", false).apply();
    }

    private void initMap() {
        mMap.setOnInfoWindowClickListener(mClusterManager);
        mMap.setOnCameraIdleListener(mClusterManager);
        mMap.setOnMarkerClickListener(mClusterManager);

        mMap.setInfoWindowAdapter(mClusterManager.getMarkerManager());

        mMap.setMinZoomPreference(10);

        setMapSettings();

        setMapBasemap();

        initLocationButton();
    }

    private void setMapSettings() {
        UiSettings mapSettings;
        mapSettings = mMap.getUiSettings();
        mapSettings.setZoomControlsEnabled(true);
        mapSettings.setCompassEnabled(false);
    }

    private void setMapBasemap() {
        boolean satellite = settings.getBoolean("satellite", false);
        if (!satellite) {
            mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        } else {
            mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
        }
    }

    private void initLocationButton() {
        if (isLocationPermissionGranted()) {
            setLocationButtonEnabled(true);
        } else {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    locationRequestCode);
        }
    }

    private boolean isLocationPermissionGranted() {
        return !isSdkHigherThanLollipop() || getActivity().checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;

    }

    private boolean isSdkHigherThanLollipop() {
        return Build.VERSION.SDK_INT >= 23;
    }

    private void setLocationButtonEnabled(boolean mode) {
        try {
            mMap.setMyLocationEnabled(mode);
        } catch (SecurityException e) {
            Log.e("Security exception", e.getMessage());
        }
    }

    private boolean isValencia(LatLng location) {
        return location.latitude >= 39.420 && location.latitude <= 39.515 && location.longitude >= -0.572 && location.longitude <= -0.272;
    }

    private boolean isMapReady() {
        return mMap != null;
    }

    private void setInitialPosition() {

        LatLng currentLocation = getCurrentLocation();
        LatLng valencia = new LatLng(39.479, -0.372);

        boolean initialZoom = settings.getBoolean("initialZoom", true);

        if (isLocationPermissionGranted() && initialZoom && currentLocation != null) {
            if (isValencia(currentLocation)) {
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 16.0f));
            } else {
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(valencia, 13.0f));
            }
        } else {
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(valencia, 13.0f));
        }
    }

    private LatLng getCurrentLocation() {
        TrackGPSService gps = new TrackGPSService(context);

        if (gps.canGetLocation()) {
            double longitude = gps.getLongitude();
            double latitude = gps.getLatitude();
            gps.stopUsingGPS();
            return new LatLng(latitude, longitude);
        } else {
            gps.stopUsingGPS();
            return null;
        }
    }

    public void getStations() {

        // Load icon
        final Drawable myDrawableLaneOn = ContextCompat.getDrawable(context, R.drawable.ic_road_variant_black_24dp);

        boolean voronoiCell = settings.getBoolean("voronoiCell", false);
        boolean bikeLanes = settings.getBoolean("bikeLanes", false);

        // Clear map
        mMap.clear();
        mClusterManager.clearItems();

        setOfflineListeners();

        if (bikeLanes) {
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

        final OkHttpClient client = new OkHttpClient();
        String url = "https://api.jcdecaux.com/vls/v1/stations?contract=Valence&apiKey=adcac2d5b367dacef9846586d12df1bf7e8c7fcd";

        Request request = new Request.Builder()
                .url(url)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("error", "error with http call(no internet?)");
            }

            @Override
            public void onResponse(Call call, Response response) {
                try {
                    if (!response.isSuccessful())
                        throw new IOException("Unexpected code " + response);
                    ResponseBody responseBody = response.body();
                    String jsonStrTemp = "";

                    if (responseBody != null) {
                        // Show loading message
                        Snackbar.make(view, R.string.load_stations, Snackbar.LENGTH_LONG).show();
                        jsonStrTemp = responseBody.string();
                    }

                    final String jsonData = jsonStrTemp;
                    applyJSONData(jsonData);

                } catch (IOException e) {
                    Log.e("error", "error with http request");
                } finally {
                    if (response != null) {
                        response.close();
                    }
                }

            }
        });
    }

    public void applyJSONData(final String jsonData) {

        // Load default marker icons
        final BitmapDescriptor iconGreen = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN);
        final BitmapDescriptor iconOrange = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE);
        final BitmapDescriptor iconYellow = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW);
        final BitmapDescriptor iconRed = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED);
        final BitmapDescriptor iconViolet = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET);

        if (isApplicationReady()) {
            getActivity().runOnUiThread(new Runnable() {
                public void run() {
                    // Get user preferences
                    boolean showAvailable = settings.getBoolean("showAvailable", false);
                    boolean showFavorites = settings.getBoolean("showFavorites", false);
                    boolean lastUpdated = settings.getBoolean("lastUpdated", true);

                    try {
                        // If data is not empty
                        if (!jsonData.equals("")) {
                            if (stationsLayer) {
                                JSONArray jsonDataArray = new JSONArray(jsonData);

                                // Parse data from API
                                for (int i = 0; i < jsonDataArray.length(); i++) {
                                    String snippet;
                                    BitmapDescriptor icon;
                                    Float alpha;
                                    Boolean visibility = true;
                                    // Get current station position
                                    JSONObject station = jsonDataArray.getJSONObject(i);
                                    JSONObject latLong = station.getJSONObject("position");
                                    Double lat = latLong.getDouble("lat");
                                    Double lng = latLong.getDouble("lng");

                                    String address = station.getString("address");
                                    String status = station.getString("status");
                                    int spots = station.getInt("available_bike_stands");
                                    int bikes = station.getInt("available_bikes");
                                    String lastUpdate = station.getString("last_update");
                                    int bikeStands = station.getInt("bike_stands");

                                    boolean currentStationIsFav = settings.getBoolean(address, false);

                                    // If station is not favourite and "Display only favourites is enabled-> Do not add station
                                    if (showFavorites && !currentStationIsFav) {
                                        continue;
                                    }

                                    if (status.equals("OPEN")) {
                                        // Add number of available bikes/stands
                                        snippet = MapsFragmentClustered.this.getResources().getString(R.string.spots) + " " +
                                                spots + " - " +
                                                MapsFragmentClustered.this.getResources().getString(R.string.bikes) + " " +
                                                bikes;

                                        // Set markers colors depending on available bikes/stands
                                        if (onFoot) {
                                            if (bikes == 0) {
                                                icon = iconRed;
                                                if (showAvailable) {
                                                    visibility = false;
                                                }
                                            } else if (bikes < 5) {
                                                icon = iconOrange;
                                            } else if (bikes < 10) {
                                                icon = iconYellow;
                                            } else {
                                                icon = iconGreen;
                                            }
                                        } else {
                                            if (spots == 0) {
                                                icon = iconRed;
                                                if (showAvailable) {
                                                    visibility = false;
                                                }
                                            } else if (spots < 5) {
                                                icon = iconOrange;
                                            } else if (spots < 10) {
                                                icon = iconYellow;
                                            } else {
                                                icon = iconGreen;
                                            }
                                        }

                                        // Get API time
                                        long apiTime = Long.parseLong(lastUpdate);
                                        // Create calendar object
                                        Calendar date = new GregorianCalendar();
                                        // Get current time
                                        long time = date.getTimeInMillis();
                                        // Add last updated time if user has checked that option
                                        if (lastUpdated) {
                                            // Set API time
                                            date.setTimeInMillis(apiTime);
                                            // Format time as HH:mm:ss
                                            StringBuilder sbu = new StringBuilder();
                                            Formatter fmt = new Formatter(sbu);
                                            fmt.format("%tT", date.getTime());
                                            // Add to pointStyle
                                            snippet = snippet +
                                                    "\n" +
                                                    MapsFragmentClustered.this.getResources().getString(R.string.last_updated) + " " +
                                                    sbu;
                                        }
                                        // If data has not been updated for more than 1 hour
                                        if ((time - apiTime) > 3600000) {
                                            // Add warning that data may be unreliable
                                            snippet = snippet +
                                                    "\n\n" +
                                                    MapsFragmentClustered.this.getResources().getString(R.string.data_old) +
                                                    "\n" +
                                                    MapsFragmentClustered.this.getResources().getString(R.string.data_unreliable);
                                        }

                                    } else {
                                        snippet = MapsFragmentClustered.this.getResources().getString(R.string.closed);
                                        icon = iconViolet;
                                        if (showAvailable) {
                                            visibility = false;
                                        }
                                    }

                                    // Apply full opacity only to favourite stations
                                    if (currentStationIsFav) {
                                        alpha = (float) 1.0;
                                    } else {
                                        alpha = (float) 0.5;
                                    }

                                    // Add marker to map
                                    ClusterPoint clPoint = new ClusterPoint(lat
                                            , lng
                                            , address
                                            , snippet
                                            , icon
                                            , alpha
                                            , visibility
                                            , bikes
                                            , spots
                                            , bikeStands
                                            , onFoot);
                                    mClusterManager.addItem(clPoint);

                                }
                                mClusterManager.cluster();
                                setOnlineListeners();
                            }
                        } else {
                            // Show message if API response is empty
                            Snackbar.make(view, R.string.no_data, Snackbar.LENGTH_LONG).show();
                        }

                    } catch (JSONException e) {

                        Log.e(mLogTag, "JSONArray could not be created");

                    }
                }
            });
        }
    }

    private void setOfflineListeners() {
        // Load icons
        final Drawable myDrawableLaneOn = ContextCompat.getDrawable(context, R.drawable.ic_road_variant_black_24dp);
        final Drawable myDrawableLaneOff = ContextCompat.getDrawable(context, R.drawable.ic_road_variant_off_black_24dp);
        final Drawable myDrawableParkingOn = ContextCompat.getDrawable(context, R.drawable.ic_local_parking_black_24dp);

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

    private void setOnlineListeners() {
        // Load icons
        final Drawable myDrawableBike = ContextCompat.getDrawable(context, R.drawable.ic_directions_bike_black_24dp);
        final Drawable myDrawableWalk = ContextCompat.getDrawable(context, R.drawable.ic_directions_walk_black_24dp);
        final Drawable myDrawableStationsOn = ContextCompat.getDrawable(context, R.drawable.ic_place_black_24dp);
        final Drawable myDrawableStationsOff = ContextCompat.getDrawable(context, R.drawable.ic_map_marker_off_black_24dp);
        final Drawable myDrawableFavOff = ContextCompat.getDrawable(context, R.drawable.ic_star_outline_black_24dp);
        final Drawable myDrawableFavOn = ContextCompat.getDrawable(context, R.drawable.ic_star_black_24dp);

        // Toggle Stations
        btnStationsToggle.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                if (stationsLayer) {
                    mMap.clear();
                    mClusterManager.clearItems();
                } else {
                    getStations();
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
                if (onFoot) {
                    onFoot = false;
                    btnOnFootToggle.setCompoundDrawablesWithIntrinsicBounds(myDrawableBike, null, null, null);
                    getStations();
                } else {
                    onFoot = true;
                    btnOnFootToggle.setCompoundDrawablesWithIntrinsicBounds(myDrawableWalk, null, null, null);
                    getStations();
                }

            }
        });

        // Reload data
        btnRefresh.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                getStations();
            }
        });

        mClusterManager.setOnClusterClickListener(
                new ClusterManager.OnClusterClickListener<ClusterPoint>() {
                    @Override
                    public boolean onClusterClick(Cluster<ClusterPoint> cluster) {
                        float zoom = mMap.getCameraPosition().zoom;
                        LatLng position = cluster.getPosition();
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(position, zoom + (float) 1.0), 250, null);
                        return true;
                    }
                });

        mClusterManager.getMarkerCollection().setOnInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {

            // Use default InfoWindow frame
            @Override
            public View getInfoWindow(Marker marker) {
                return null;
            }

            // Defines the contents of the InfoWindow
            @Override
            public View getInfoContents(final Marker marker) {

                if (marker.getSnippet() != null) {
                    // Getting view from the layout file info_window_layout
                    final View popup = getActivity().getLayoutInflater().inflate(R.layout.marker_popup, null);

                    // Getting reference to the ImageView/title/snippet
                    TextView title = popup.findViewById(R.id.title);
                    TextView snippet = popup.findViewById(R.id.snippet);
                    ImageView btn_star = popup.findViewById(R.id.btn_star);

                    if (marker.getSnippet().contains("\n\n")) {
                        snippet.setTextColor(getResources().getColor(R.color.red));
                        snippet.setTypeface(null, Typeface.BOLD);
                        snippet.setText(marker.getSnippet());
                    } else {
                        snippet.setText(marker.getSnippet());
                    }
                    title.setText(marker.getTitle());

                    // Checking if current station is favorite
                    boolean currentStationIsFav = settings.getBoolean(marker.getTitle(), false);

                    // Setting correspondent icon
                    if (currentStationIsFav) {
                        btn_star.setImageDrawable(myDrawableFavOn);
                    } else {
                        btn_star.setImageDrawable(myDrawableFavOff);
                    }

                    mClusterManager.setOnClusterItemInfoWindowClickListener(
                            new ClusterManager.OnClusterItemInfoWindowClickListener<ClusterPoint>() {
                                @Override
                                public void onClusterItemInfoWindowClick(ClusterPoint item) {
                                    boolean showFavorites = settings.getBoolean("showFavorites", false);
                                    boolean currentStationIsFav = settings.getBoolean(item.getTitle(), false);
                                    if (currentStationIsFav) {
                                        item.setAlpha((float) 0.5);
                                        marker.setAlpha((float) 0.5);
                                        if (showFavorites) {
                                            item.setVisibility(false);
                                        }
                                        settingsEditor.putBoolean(item.getTitle(), false).apply();
                                    } else {
                                        item.setAlpha((float) 1.0);
                                        marker.setAlpha((float) 1.0);
                                        settingsEditor.putBoolean(item.getTitle(), true).apply();
                                    }
                                    marker.showInfoWindow();
                                    mClusterManager.cluster();
                                }
                            });

                    // Returning the view containing InfoWindow contents
                    return popup;
                } else {
                    return null;
                }
            }
        });
    }

    private class GetLanes extends AsyncTask<Void, Void, GeoJsonLayer> {

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
                    lanes = new GeoJsonLayer(mMap, R.raw.bike_lanes_0618, context);
                    for (GeoJsonFeature feature : lanes.getFeatures()) {
                        GeoJsonLineStringStyle stringStyle = new GeoJsonLineStringStyle();
                        stringStyle.setWidth(5);
                        if (settings.getBoolean("cicloCalles", true)) {
                            switch (feature.getProperty("estado")) {
                                // Normal bike lane
                                case "1":
                                    stringStyle.setColor(Color.BLACK);
                                    break;
                                // Ciclocalle
                                case "2":
                                    stringStyle.setColor(Color.BLUE);
                                    break;
                                // Weird fragments
                                case "3":
                                    stringStyle.setColor(Color.BLUE);
                                    break;
                                // Rio
                                case "4":
                                    stringStyle.setColor(Color.BLACK);
                                    break;
                                default:
                                    stringStyle.setColor(Color.RED);
                            }
                        }
                        feature.setLineStringStyle(stringStyle);
                    }
                    settingsEditor.putBoolean("firstTime", false).apply();
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
            if (lanes != null) {
                if (bikeLanes && !settings.getBoolean("carrilLayer", false)) {
                    lanes.addLayerToMap();
                    settingsEditor.putBoolean("carrilLayer", true).apply();
                } else {
                    if (!settings.getBoolean("carrilLayer", false)) {
                        lanes.addLayerToMap();
                        settingsEditor.putBoolean("carrilLayer", true).apply();
                    } else {
                        lanes.removeLayerFromMap();
                        settingsEditor.putBoolean("carrilLayer", false).apply();
                        settingsEditor.putBoolean("firstTime", true).apply();
                    }
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

            if (isApplicationReady()) {
                try {
                    // parking layer
                    if (settings.getBoolean("firstTimeParking", true)) {
                        parking = new GeoJsonLayer(mMap, R.raw.aparcabicis, context);
                        for (GeoJsonFeature feature : parking.getFeatures()) {
                            GeoJsonPointStyle pointStyle = new GeoJsonPointStyle();
                            pointStyle.setTitle(MapsFragmentClustered.this.getResources().getString(R.string.parking) + " " + feature.getProperty("id"));
                            pointStyle.setSnippet(MapsFragmentClustered.this.getResources().getString(R.string.plazas) + " " + feature.getProperty("plazas"));
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
            }

            return parking;
        }

        protected void onPostExecute(GeoJsonLayer parking) {

            if (parking != null) {
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

    }

    @Override
    public void onPause() {
        super.onPause();
        if (isApplicationReady() && isMapReady() && isLocationPermissionGranted()) {
            // Disable location to avoid battery drain
            setLocationButtonEnabled(false);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (isApplicationReady() && isMapReady() && isLocationPermissionGranted()) {
            // Re-enable location as the user returns to the app
            setLocationButtonEnabled(true);
        }
    }
}
