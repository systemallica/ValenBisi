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
import com.google.maps.android.data.geojson.GeoJsonPoint;
import com.google.maps.android.data.geojson.GeoJsonPointStyle;
import com.systemallica.valenbisi.BikeStation;
import com.systemallica.valenbisi.R;
import com.systemallica.valenbisi.clustering.ClusterPoint;
import com.systemallica.valenbisi.clustering.IconRenderer;
import com.systemallica.valenbisi.services.TrackGPSService;

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

public class MapsFragment extends Fragment implements OnMapReadyCallback {

    private static final String PREFS_NAME = "MyPrefsFile";
    private SharedPreferences settings;
    private SharedPreferences.Editor settingsEditor;
    private final static String mLogTag = "Valenbisi error";
    private GeoJsonLayer stations = null;
    private GeoJsonLayer lanes = null;
    private GeoJsonLayer parking = null;
    private View view;
    private GoogleMap mMap;
    private Context context;
    private ClusterManager<ClusterPoint> mClusterManager = null;

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

    public MapsFragment() {
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
        if (requestCode == 1) {
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

            setButtons();

            manageOptionalLayers();

            getStations();
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
                    1);
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
            Log.e(mLogTag, e.getMessage());
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

    private void manageOptionalLayers() {
        boolean isDrawVoronoiCellsChecked = settings.getBoolean("voronoiCell", false);
        if (isDrawVoronoiCellsChecked) {
            drawBoronoiCells();
        }

        boolean isCarrilLayerAdded = settings.getBoolean("isCarrilLayerAdded", false);
        if (isCarrilLayerAdded) {
            new GetLanes().execute();
        }

        boolean isDrawParkingSpotsChecked = settings.getBoolean("isParkingLayerAdded", false);
        if (isDrawParkingSpotsChecked) {
            new GetParking().execute();
        }
    }

    private void drawBoronoiCells() {
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

    private void getStations() {
        // Clear map
        mMap.clear();
        mClusterManager.clearItems();

        final OkHttpClient client = new OkHttpClient();
        String url = "https://api.jcdecaux.com/vls/v1/stations?contract=Valence&apiKey=adcac2d5b367dacef9846586d12df1bf7e8c7fcd";

        Request request = new Request.Builder()
                .url(url)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(mLogTag, "error with http call(no internet?)");
            }

            @Override
            public void onResponse(Call call, Response response) {
                try {
                    handleApiResponse(response);
                } catch (IOException e) {
                    Log.e(mLogTag, "error with http request");
                } finally {
                    if (response != null) {
                        response.close();
                    }
                }

            }
        });
    }

    private void handleApiResponse(Response response) throws IOException {
        if (!response.isSuccessful())
            throw new IOException("Unexpected code " + response);
        ResponseBody responseBody = response.body();

        if (responseBody != null) {
            // Show loading message
            Snackbar.make(view, R.string.load_stations, Snackbar.LENGTH_LONG).show();
            addDataToMap(responseBody.string());
        } else {
            Log.e(mLogTag, "Empty server response");
            getActivity().runOnUiThread(new Runnable() {
                public void run() {
                    // Show message if API response is empty
                    Snackbar.make(view, R.string.no_data, Snackbar.LENGTH_LONG).show();
                }
            });
        }
    }

    private void addDataToMap(String jsonData) {

        boolean showStationsLayer = settings.getBoolean("showStationsLayer", true);
        boolean isClusteringActivated = settings.getBoolean("isClusteringActivated", true);

        if (isApplicationReady() && showStationsLayer) {
            try {
                JSONArray jsonDataArray = new JSONArray(jsonData);

                if (isClusteringActivated) {
                    addPointsToCluster(jsonDataArray);
                    getActivity().runOnUiThread(new Runnable() {
                        public void run() {
                            mClusterManager.cluster();
                        }
                    });
                } else {
                    addPointsToLayer(jsonDataArray);
                    getActivity().runOnUiThread(new Runnable() {
                        public void run() {
                            stations.addLayerToMap();
                        }
                    });
                }

            } catch (JSONException e) {
                Log.e(mLogTag, "JSONArray could not be created");
            }
        }
    }

    private void addPointsToCluster(JSONArray jsonDataArray) throws JSONException {
        boolean showOnlyFavoriteStations = settings.getBoolean("showFavorites", false);

        // Parse data from API
        for (int i = 0; i < jsonDataArray.length(); i++) {
            // Get current station
            JSONObject jsonStation = jsonDataArray.getJSONObject(i);
            BikeStation station = new BikeStation(jsonStation);
            station = generateCompleteStationData(station);

            // If station is not favourite and "Display only favourites" is enabled-> Do not add station
            if (showOnlyFavoriteStations && !station.isFavourite) {
                continue;
            }

            ClusterPoint clusterPoint = new ClusterPoint(station);
            mClusterManager.addItem(clusterPoint);
        }
    }


    private void addPointsToLayer(JSONArray jsonDataArray) throws JSONException {
        boolean showOnlyFavoriteStations = settings.getBoolean("showFavorites", false);

        JSONObject dummy = new JSONObject();
        stations = new GeoJsonLayer(mMap, dummy);

        // Parse data from API
        for (int i = 0; i < jsonDataArray.length(); i++) {
            // Get current station
            JSONObject jsonStation = jsonDataArray.getJSONObject(i);
            BikeStation station = new BikeStation(jsonStation);
            station = generateCompleteStationData(station);

            // If station is not favourite and "Display only favourites" is enabled-> Do not add station
            if (showOnlyFavoriteStations && !station.isFavourite) {
                continue;
            }

            // Create Point
            GeoJsonPoint point = new GeoJsonPoint(new LatLng(station.lat, station.lng));
            // Add properties
            HashMap<String, String> properties = getStationProperties(station);
            // Create feature
            GeoJsonFeature pointFeature = new GeoJsonFeature(point, "Origin", properties, null);

            GeoJsonPointStyle pointStyle = generatePointStyle(station);
            pointFeature.setPointStyle(pointStyle);

            stations.addFeature(pointFeature);
        }
    }

    private BikeStation generateCompleteStationData(BikeStation station) {
        boolean showOnlyAvailableStations = settings.getBoolean("showAvailable", false);
        boolean isOnFoot = settings.getBoolean("isOnFoot", true);

        station.isFavourite = settings.getBoolean(station.address, false);

        if (station.status.equals("OPEN")) {
            // Set markers colors depending on station availability
            station.icon = getMarkerIcon(isOnFoot, station.bikes, station.spots);

            // Get markers visibility depending on station availability
            if (showOnlyAvailableStations) {
                station.visibility = getMarkerVisibility(isOnFoot, station.bikes, station.spots);
            }

            station.snippet = getMarkerSnippet(station.bikes, station.spots, station.lastUpdate);

        } else {
            station.icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET);
            station.snippet = MapsFragment.this.getResources().getString(R.string.closed);
            if (showOnlyAvailableStations) {
                station.visibility = false;
            }
        }

        station.alpha = getMarkerAlpha(station.isFavourite);

        return station;
    }

    private HashMap<String, String> getStationProperties(BikeStation station) {
        HashMap<String, String> properties = new HashMap<>();

        properties.put("name", station.name);
        properties.put("number", station.number);
        properties.put("address", station.address);
        properties.put("status", station.status);
        properties.put("available_bike_stands", Integer.toString(station.spots));
        properties.put("available_bikes", Integer.toString(station.bikes));
        properties.put("last_updated", station.lastUpdate);

        return properties;
    }

    private GeoJsonPointStyle generatePointStyle(BikeStation station) {
        GeoJsonPointStyle pointStyle = new GeoJsonPointStyle();

        pointStyle.setTitle(station.address);
        pointStyle.setSnippet(station.snippet);
        pointStyle.setIcon(station.icon);
        pointStyle.setAlpha(station.alpha);
        pointStyle.setVisible(station.visibility);

        return pointStyle;
    }

    public BitmapDescriptor getMarkerIcon(boolean isOnFoot, int bikes, int spots) {
        // Load default marker icons
        final BitmapDescriptor iconGreen = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN);
        final BitmapDescriptor iconOrange = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE);
        final BitmapDescriptor iconYellow = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW);
        final BitmapDescriptor iconRed = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED);
        BitmapDescriptor icon;

        if (isOnFoot) {
            if (bikes == 0) {
                icon = iconRed;
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
            } else if (spots < 5) {
                icon = iconOrange;
            } else if (spots < 10) {
                icon = iconYellow;
            } else {
                icon = iconGreen;
            }
        }

        return icon;
    }

    private boolean getMarkerVisibility(boolean isOnFoot, int bikes, int spots) {
        Boolean visibility = true;

        if (isOnFoot && bikes == 0) {
            visibility = false;
        } else if (spots == 0) {
            visibility = false;
        }

        return visibility;
    }

    private String getMarkerSnippet(int bikes, int spots, String lastUpdate) {
        String snippet;
        boolean showLastUpdatedInfo = settings.getBoolean("lastUpdated", true);

        // Add number of available bikes/stands
        snippet = MapsFragment.this.getResources().getString(R.string.spots) + " " +
                spots + " - " +
                MapsFragment.this.getResources().getString(R.string.bikes) + " " +
                bikes;

        // Add last updated time if user has checked that option
        if (showLastUpdatedInfo) {
            snippet = snippet + getLastUpdatedInfo(lastUpdate);
        }

        // If data has not been updated for more than 1 hour
        if (millisecondsFrom(lastUpdate) > 3600000) {
            snippet = snippet + getWarningMessage();
        }

        return snippet;
    }

    private long millisecondsFrom(String event) {
        long eventTime = Long.parseLong(event);
        Calendar date = new GregorianCalendar();
        long currentTime = date.getTimeInMillis();

        return currentTime - eventTime;
    }

    private String getWarningMessage() {
        String snippet;

        // Add warning that data may be unreliable
        snippet = "\n\n" +
                MapsFragment.this.getResources().getString(R.string.data_old) +
                "\n" +
                MapsFragment.this.getResources().getString(R.string.data_unreliable);

        return snippet;
    }

    private String getLastUpdatedInfo(String lastUpdate) {
        String snippet;
        long apiTime = Long.parseLong(lastUpdate);
        Calendar date = new GregorianCalendar();

        // Set API time
        date.setTimeInMillis(apiTime);
        // Format time as HH:mm:ss
        StringBuilder sbu = new StringBuilder();
        Formatter fmt = new Formatter(sbu);
        fmt.format("%tT", date.getTime());
        // Add to pointStyle
        snippet = "\n" +
                MapsFragment.this.getResources().getString(R.string.last_updated) + " " +
                sbu;

        return snippet;
    }

    public float getMarkerAlpha(boolean currentStationIsFav) {
        float alpha;
        // Apply full opacity only to favourite stations
        if (currentStationIsFav) {
            alpha = (float) 1.0;
        } else {
            alpha = (float) 0.5;
        }

        return alpha;
    }

    private void setButtons() {
        setInitialButtonState();
        setOfflineListeners();
        setOnlineListeners();
    }

    private void setInitialButtonState() {
        final Drawable myDrawableStationsOn = ContextCompat.getDrawable(context, R.drawable.ic_place_black_24dp);
        final Drawable myDrawableStationsOff = ContextCompat.getDrawable(context, R.drawable.ic_map_marker_off_black_24dp);
        final Drawable myDrawableBike = ContextCompat.getDrawable(context, R.drawable.ic_directions_bike_black_24dp);
        final Drawable myDrawableWalk = ContextCompat.getDrawable(context, R.drawable.ic_directions_walk_black_24dp);
        final Drawable myDrawableLaneOn = ContextCompat.getDrawable(context, R.drawable.ic_road_variant_black_24dp);
        final Drawable myDrawableLaneOff = ContextCompat.getDrawable(context, R.drawable.ic_road_variant_off_black_24dp);

        boolean showStationsLayer = settings.getBoolean("showStationsLayer", true);
        if (showStationsLayer) {
            btnStationsToggle.setCompoundDrawablesWithIntrinsicBounds(myDrawableStationsOn, null, null, null);
        } else {
            btnStationsToggle.setCompoundDrawablesWithIntrinsicBounds(myDrawableStationsOff, null, null, null);
        }

        boolean isCarrilLayerAdded = settings.getBoolean("isCarrilLayerAdded", true);
        if (isCarrilLayerAdded) {
            btnLanesToggle.setCompoundDrawablesWithIntrinsicBounds(myDrawableLaneOn, null, null, null);
        } else {
            btnLanesToggle.setCompoundDrawablesWithIntrinsicBounds(myDrawableLaneOff, null, null, null);
        }

        boolean isOnFoot = settings.getBoolean("isOnFoot", false);
        if (isOnFoot) {
            btnOnFootToggle.setCompoundDrawablesWithIntrinsicBounds(myDrawableWalk, null, null, null);
        } else {
            btnOnFootToggle.setCompoundDrawablesWithIntrinsicBounds(myDrawableBike, null, null, null);
        }
    }

    private void setOfflineListeners() {
        final Drawable myDrawableLaneOn = ContextCompat.getDrawable(context, R.drawable.ic_road_variant_black_24dp);
        final Drawable myDrawableLaneOff = ContextCompat.getDrawable(context, R.drawable.ic_road_variant_off_black_24dp);
        final Drawable myDrawableParkingOn = ContextCompat.getDrawable(context, R.drawable.ic_local_parking_black_24dp);

        btnLanesToggle.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (!settings.getBoolean("isCarrilLayerAdded", false)) {
                    btnLanesToggle.setCompoundDrawablesWithIntrinsicBounds(myDrawableLaneOn, null, null, null);
                    new GetLanes().execute();
                } else {
                    btnLanesToggle.setCompoundDrawablesWithIntrinsicBounds(myDrawableLaneOff, null, null, null);
                    settingsEditor.putBoolean("isCarrilLayerAdded", false).apply();
                    if (lanes != null) {
                        lanes.removeLayerFromMap();
                    }
                }
            }
        });

        btnParkingToggle.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (!settings.getBoolean("isParkingLayerAdded", false)) {
                    btnParkingToggle.setCompoundDrawablesWithIntrinsicBounds(myDrawableParkingOn, null, null, null);
                    new GetParking().execute();
                } else {
                    btnParkingToggle.setCompoundDrawablesWithIntrinsicBounds(myDrawableParkingOn, null, null, null);
                    settingsEditor.putBoolean("isParkingLayerAdded", false).apply();
                    if (parking != null) {
                        parking.removeLayerFromMap();
                    }
                }
            }
        });
    }

    private void setOnlineListeners() {
        final Drawable myDrawableBike = ContextCompat.getDrawable(context, R.drawable.ic_directions_bike_black_24dp);
        final Drawable myDrawableWalk = ContextCompat.getDrawable(context, R.drawable.ic_directions_walk_black_24dp);
        final Drawable myDrawableStationsOn = ContextCompat.getDrawable(context, R.drawable.ic_place_black_24dp);
        final Drawable myDrawableStationsOff = ContextCompat.getDrawable(context, R.drawable.ic_map_marker_off_black_24dp);
        boolean isClusteringActivated = settings.getBoolean("isClusteringActivated", true);

        // Toggle Stations
        btnStationsToggle.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                boolean showStationsLayer = settings.getBoolean("showStationsLayer", true);
                if (showStationsLayer) {
                    mMap.clear();
                    mClusterManager.clearItems();
                    btnStationsToggle.setCompoundDrawablesWithIntrinsicBounds(myDrawableStationsOff, null, null, null);
                    settingsEditor.putBoolean("showStationsLayer", false).apply();
                } else {
                    getStations();
                    btnStationsToggle.setCompoundDrawablesWithIntrinsicBounds(myDrawableStationsOn, null, null, null);
                    settingsEditor.putBoolean("showStationsLayer", true).apply();
                }
            }
        });

        // Toggle onFoot/onBike
        btnOnFootToggle.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                boolean isOnFoot = settings.getBoolean("isOnFoot", false);
                if (isOnFoot) {
                    settingsEditor.putBoolean("isOnFoot", false).apply();
                    btnOnFootToggle.setCompoundDrawablesWithIntrinsicBounds(myDrawableBike, null, null, null);
                    getStations();
                } else {
                    settingsEditor.putBoolean("isOnFoot", true).apply();
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

        if (isClusteringActivated) {
            setClusteredInfoWindow();
        } else {
            setNormalInfoWindow();
        }
    }

    private void setNormalInfoWindow() {
        mMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {
            // Use default InfoWindow frame
            @Override
            public View getInfoWindow(Marker marker) {
                return null;
            }

            // Defines the contents of the InfoWindow
            @Override
            public View getInfoContents(Marker marker) {
                View popup = getInfoWindowCommonInfo(marker);

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
                            settingsEditor.putBoolean(marker.getTitle(), false).apply();
                        } else {
                            marker.setAlpha(1);
                            settingsEditor.putBoolean(marker.getTitle(), true).apply();
                        }
                        marker.showInfoWindow();
                    }
                });
                return popup;
            }
        });
    }

    private void setClusteredInfoWindow() {
        mClusterManager.getMarkerCollection().setOnInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {
            // Use default InfoWindow frame
            @Override
            public View getInfoWindow(Marker marker) {
                return null;
            }

            // Defines the contents of the InfoWindow
            @Override
            public View getInfoContents(final Marker marker) {
                // Getting view from the layout file info_window_layout
                View popup = getInfoWindowCommonInfo(marker);

                mClusterManager.setOnClusterItemInfoWindowClickListener(
                        new ClusterManager.OnClusterItemInfoWindowClickListener<ClusterPoint>() {
                            @Override
                            public void onClusterItemInfoWindowClick(ClusterPoint item) {
                                boolean currentStationIsFav = settings.getBoolean(item.getTitle(), false);
                                boolean showFavorites = settings.getBoolean("showFavorites", false);

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
                return popup;
            }
        });
    }

    private View getInfoWindowCommonInfo(Marker marker) {
        final Drawable myDrawableFavOff = ContextCompat.getDrawable(context, R.drawable.ic_star_outline_black_24dp);
        final Drawable myDrawableFavOn = ContextCompat.getDrawable(context, R.drawable.ic_star_black_24dp);

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
        return popup;
    }

    private class GetLanes extends AsyncTask<Void, Void, GeoJsonLayer> {

        protected void onPreExecute() {
            Snackbar.make(view, R.string.load_lanes, Snackbar.LENGTH_LONG).show();
        }

        @Override
        protected GeoJsonLayer doInBackground(Void... params) {

            try {
                lanes = new GeoJsonLayer(mMap, R.raw.bike_lanes_0618, context);
                for (GeoJsonFeature feature : lanes.getFeatures()) {
                    GeoJsonLineStringStyle stringStyle = new GeoJsonLineStringStyle();
                    stringStyle.setWidth(5);
                    if (settings.getBoolean("cicloCalles", true)) {
                        stringStyle.setColor(getLaneColor(feature));
                    }
                    feature.setLineStringStyle(stringStyle);
                }
            } catch (IOException e) {
                Log.e(mLogTag, "GeoJSON file could not be read");
            } catch (JSONException e) {
                Log.e(mLogTag, "GeoJSON file could not be converted to a JSONObject");
            }
            return lanes;
        }

        private int getLaneColor(GeoJsonFeature feature) {
            switch (feature.getProperty("estado")) {
                // Normal bike lane
                case "1":
                    return Color.BLACK;
                // Ciclocalle
                case "2":
                    return Color.BLUE;
                // Weird fragments
                case "3":
                    return Color.BLUE;
                // Rio
                case "4":
                    return Color.BLACK;
                default:
                    return Color.RED;
            }
        }

        protected void onPostExecute(GeoJsonLayer lanes) {
            if (lanes != null) {
                lanes.addLayerToMap();
                settingsEditor.putBoolean("isCarrilLayerAdded", true).apply();
            }
        }
    }

    private class GetParking extends AsyncTask<Void, Void, GeoJsonLayer> {
        protected void onPreExecute() {
            Snackbar.make(view, R.string.load_parking, Snackbar.LENGTH_SHORT).show();
        }

        @Override
        protected GeoJsonLayer doInBackground(Void... params) {
            boolean showFavorites = settings.getBoolean("showFavorites", false);
            Bitmap bm = BitmapFactory.decodeResource(getResources(), R.drawable.ic_map_marker_radius_black_48dp);
            bm = Bitmap.createScaledBitmap(bm, 50, 50, false);
            BitmapDescriptor icon_parking = BitmapDescriptorFactory.fromBitmap(bm);

            if (isApplicationReady()) {
                try {
                    parking = new GeoJsonLayer(mMap, R.raw.aparcabicis, context);
                    for (GeoJsonFeature feature : parking.getFeatures()) {
                        GeoJsonPointStyle pointStyle = new GeoJsonPointStyle();
                        pointStyle.setTitle(MapsFragment.this.getResources().getString(R.string.parking) + " " + feature.getProperty("id"));
                        pointStyle.setSnippet(MapsFragment.this.getResources().getString(R.string.plazas) + " " + feature.getProperty("plazas"));
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
                parking.addLayerToMap();
                settingsEditor.putBoolean("isParkingLayerAdded", true).apply();
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
