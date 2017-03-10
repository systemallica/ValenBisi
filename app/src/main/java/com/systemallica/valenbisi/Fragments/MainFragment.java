package com.systemallica.valenbisi.Fragments;

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
import com.systemallica.valenbisi.R;
import com.systemallica.valenbisi.TrackGPS;
import com.systemallica.valenbisi.WebRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class MainFragment extends Fragment implements OnMapReadyCallback {


    public static final String PREFS_NAME = "MyPrefsFile";
    private final static String mLogTag = "GeoJsonDemo";
    private final static String url = "https://api.jcdecaux.com/vls/v1/stations?contract=Valence&apiKey=adcac2d5b367dacef9846586d12df1bf7e8c7fcd"; // api request of all valencia stations' data
    UiSettings mapSettings;
    int MY_LOCATION_REQUEST_CODE = 1;
    boolean estacionesLayer = true;
    boolean onFoot = true;
    GeoJsonLayer carril = null;
    GeoJsonLayer parking = null;
    View view;
    TrackGPS gps;
    double longitude;
    double latitude;
    private GoogleMap mMap;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        view = inflater.inflate(R.layout.fragment_main, container, false);
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

        final SharedPreferences settings = getActivity().getApplicationContext().getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();

        mMap = googleMap;
        boolean satellite = settings.getBoolean("satellite", false);
        editor.putBoolean("firstTime", true).apply();
        editor.putBoolean("firstTimeParking", true).apply();

        //Check for sdk >= 23
        if (Build.VERSION.SDK_INT >= 23) {
            //Check location permission
            if (getActivity().checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {

                requestPermissions(new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_LOCATION_REQUEST_CODE);

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
            if (getActivity().checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED && initialZoom && gps.canGetLocation()) {
                if (currentLocation.latitude >= 39.515 || currentLocation.latitude <= 39.420 || currentLocation.longitude >= -0.272 || currentLocation.longitude <= -0.572) {
                    mMap.moveCamera(CameraUpdateFactory.newLatLng(valencia));
                } else {
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 16.0f));
                    gps.stopUsingGPS();
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
                    gps.stopUsingGPS();
                }
            } else {
                mMap.moveCamera(CameraUpdateFactory.newLatLng(valencia));
            }
        }

        new GetStations().execute();
        if (settings.getBoolean("parkingLayer", false)) {
            editor.putBoolean("parkingLayer", false).apply();
            new GetParking().execute();
        }

        final Button btn_carril = (Button) view.findViewById(R.id.btnCarrilToggle);
        final Drawable myDrawableLaneOn = ContextCompat.getDrawable(getActivity().getApplicationContext(), R.drawable.ic_road_variant_black_24dp);
        final Drawable myDrawableLaneOff = ContextCompat.getDrawable(getActivity().getApplicationContext(), R.drawable.ic_road_variant_off_black_24dp);

        btn_carril.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (!settings.getBoolean("carrilLayer", false)) {
                    btn_carril.setCompoundDrawablesWithIntrinsicBounds(myDrawableLaneOn, null, null, null);
                    new GetLanes().execute();
                } else {
                    btn_carril.setCompoundDrawablesWithIntrinsicBounds(myDrawableLaneOff, null, null, null);
                    new GetLanes().execute();
                }
            }
        });

        final Button btn_parking = (Button) view.findViewById(R.id.btnParkingToggle);
        final Drawable myDrawableParkingOn = ContextCompat.getDrawable(getActivity().getApplicationContext(), R.drawable.ic_local_parking_black_24dp);
        //final Drawable myDrawableParkingOff = ContextCompat.getDrawable(getActivity().getApplicationContext(), R.drawable.ic_road_variant_off_black_24dp);

        btn_parking.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (!settings.getBoolean("parkingLayer", false)) {
                    btn_parking.setCompoundDrawablesWithIntrinsicBounds(myDrawableParkingOn, null, null, null);
                    new GetParking().execute();
                } else {
                    btn_parking.setCompoundDrawablesWithIntrinsicBounds(myDrawableParkingOn, null, null, null);
                    new GetParking().execute();
                }
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == MY_LOCATION_REQUEST_CODE) {
            if (ContextCompat.checkSelfPermission(getActivity().getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                mMap.setMyLocationEnabled(true);
            } else {

                Snackbar.make(view, R.string.no_location_permission, Snackbar.LENGTH_SHORT).show();

            }
        }
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

    private class GetStations extends AsyncTask<Void, Void, GeoJsonLayer> {

        final SharedPreferences settings = getActivity().getApplicationContext().getSharedPreferences(PREFS_NAME, 0);
        final Button btn_estaciones = (Button) view.findViewById(R.id.btnEstacionesToggle);
        final Button btnOnFootToggle = (Button) view.findViewById(R.id.btnOnFootToggle);
        final Button btnRefresh = (Button) view.findViewById(R.id.btnRefresh);
        final Drawable myDrawableBike = ContextCompat.getDrawable(getActivity().getApplicationContext(), R.drawable.ic_directions_bike_black_24dp);
        final Drawable myDrawableWalk = ContextCompat.getDrawable(getActivity().getApplicationContext(), R.drawable.ic_directions_walk_black_24dp);
        final Drawable myDrawableStationsOn = ContextCompat.getDrawable(getActivity().getApplicationContext(), R.drawable.ic_place_black_24dp);

        //BitmapDescriptor icon_reed = (BitmapDescriptorFactory.fromResource(R.drawable.splash_inverted));
        final Drawable myDrawableStationsOff = ContextCompat.getDrawable(getActivity().getApplicationContext(), R.drawable.ic_map_marker_off_black_24dp);
        SharedPreferences.Editor editor = settings.edit();
        //Load default marker icons
        BitmapDescriptor icon_green = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN);
        BitmapDescriptor icon_orange = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE);
        BitmapDescriptor icon_yellow = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW);
        BitmapDescriptor icon_red = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED);
        BitmapDescriptor icon_blue = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE);

        @Override
        protected GeoJsonLayer doInBackground(Void... params) {

            Snackbar.make(view, R.string.load_stations, Snackbar.LENGTH_LONG).show();

            // Creating service handler class instance
            WebRequest webreq = new WebRequest();

            // Making a request to url and getting response
            String jsonStr = webreq.makeWebServiceCall(url, WebRequest.GET);

            boolean showAvailable = settings.getBoolean("showAvailable", false);

            try {
                if (!jsonStr.equals("")) {
                    JSONArray array = new JSONArray(jsonStr);
                    boolean alone;

                    final GeoJsonLayer layer = new GeoJsonLayer(mMap, R.raw.valencia, getActivity().getApplicationContext());

                    for (GeoJsonFeature feature : layer.getFeatures()) {  //loop through features
                        alone = true;
                        for (int counter = 0; counter < array.length(); counter++) {
                            JSONObject object = array.getJSONObject(counter);
                            //Add each number and address to its correspondent marker

                            if (object.getString("number").equals(feature.getProperty("Number"))) {
                                alone = false;
                                GeoJsonPointStyle pointStyle = new GeoJsonPointStyle();
                                pointStyle.setTitle(feature.getProperty("Address"));
                                pointStyle.setSnippet(getString(R.string.spots) + " " + object.getInt("available_bike_stands") + " - " + getString(R.string.bikes) + " " + object.getInt("available_bikes"));
                                pointStyle.setAlpha((float) 0.5);

                                //set markers colors depending on available bikes/stands
                                if (onFoot) {
                                    if (object.getInt("available_bikes") == 0) {
                                        pointStyle.setIcon(icon_red);
                                        if (showAvailable) {
                                            pointStyle.setVisible(false);
                                        }
                                    } else if (object.getInt("available_bikes") < 5) {
                                        pointStyle.setIcon(icon_orange);
                                    } else if (object.getInt("available_bikes") < 10) {
                                        pointStyle.setIcon(icon_yellow);
                                    } else {
                                        pointStyle.setIcon(icon_green);
                                    }
                                } else {
                                    if (object.getInt("available_bike_stands") == 0) {
                                        pointStyle.setIcon(icon_red);
                                        if (showAvailable) {
                                            pointStyle.setVisible(false);
                                        }
                                    } else if (object.getInt("available_bike_stands") < 5) {
                                        pointStyle.setIcon(icon_orange);
                                    } else if (object.getInt("available_bike_stands") < 10) {
                                        pointStyle.setIcon(icon_yellow);
                                    } else {
                                        pointStyle.setIcon(icon_green);
                                    }
                                }

                                boolean showFavorites = settings.getBoolean("showFavorites", false);
                                boolean currentStationIsFav = settings.getBoolean(feature.getProperty("Address"), false);

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
                        if (alone) {
                            GeoJsonPointStyle pointStyle = new GeoJsonPointStyle();
                            pointStyle.setTitle("No data available :(");
                            pointStyle.setSnippet("No data available :(");
                            pointStyle.setIcon(icon_blue);
                            pointStyle.setAlpha((float) 0.5);
                            if (showAvailable) {
                                pointStyle.setVisible(false);
                            }
                            feature.setPointStyle(pointStyle);
                        }
                    }
                    return layer;
                }
            } catch (JSONException e) {

                Log.e(mLogTag, "JSONArray could not be created");


            } catch (IOException e) {

                Log.e(mLogTag, "GeoJSON file could not be read");

            }

            return null;
        }

        protected void onPostExecute(final GeoJsonLayer layer) {
            boolean voronoiCell = settings.getBoolean("voronoiCell", false);
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
            if (layer != null) {
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

                        final Drawable myDrawableFavOn = ContextCompat.getDrawable(getActivity().getApplicationContext(), R.drawable.ic_star_black_24dp);
                        final Drawable myDrawableFavOff = ContextCompat.getDrawable(getActivity().getApplicationContext(), R.drawable.ic_star_outline_black_24dp);

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
                btn_estaciones.setOnClickListener(new View.OnClickListener() {
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
                            btn_estaciones.setCompoundDrawablesWithIntrinsicBounds(myDrawableStationsOff, null, null, null);
                            estacionesLayer = false;
                        } else {
                            btn_estaciones.setCompoundDrawablesWithIntrinsicBounds(myDrawableStationsOn, null, null, null);
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
                            new GetStations().execute();

                        } else {
                            onFoot = true;
                            btnOnFootToggle.setCompoundDrawablesWithIntrinsicBounds(myDrawableWalk, null, null, null);
                            new GetStations().execute();
                        }

                    }
                });

                //Reload data
                btnRefresh.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        layer.removeLayerFromMap();
                        new GetStations().execute();
                    }
                });


            } else {
                if (isAdded()) {
                    Snackbar.make(view, "No se han podido cargar los datos, prueba más tarde", Snackbar.LENGTH_SHORT).show();
                }
            }
        }

    }

}
