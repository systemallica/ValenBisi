package com.systemallica.valenbisi.Fragments;

import android.app.Fragment;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.geojson.GeoJsonFeature;
import com.google.maps.android.geojson.GeoJsonLayer;
import com.google.maps.android.geojson.GeoJsonPointStyle;
import com.systemallica.valenbisi.R;
import com.systemallica.valenbisi.WebRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;




public class MainFragment extends Fragment implements OnMapReadyCallback {

    private MapView mapView;
    private GoogleMap mMap;
    UiSettings mapSettings;
    int MY_LOCATION_REQUEST_CODE = 1;
    int carrilLayer = 0;
    boolean estacionesLayer = true;
    int onFoot=1;
    int counter = 0;
    View view;


    private final static String mLogTag = "GeoJsonDemo";
    private final static String url = "https://api.jcdecaux.com/vls/v1/stations?contract=Valence&apiKey=adcac2d5b367dacef9846586d12df1bf7e8c7fcd"; // api request of all valencia stations' data


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        view = inflater.inflate(R.layout.fragment_main, container, false);
        return view;
    }
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState){
        Context context = getActivity().getApplicationContext();

        mapView = (MapView) view.findViewById(R.id.map);
        mapView.onCreate(savedInstanceState);
        mapView.onResume();

        //Check internet
        final ConnectivityManager cm =
                (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        final boolean isConnected = activeNetwork != null &&
                activeNetwork.isConnected();

        //React to the check
        if(isConnected) {
            mapView.getMapAsync(this);
        }
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {

        mMap = googleMap;

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

        //Set type of map and min zoom
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        mMap.setMinZoomPreference(12);

        // Move the camera to Valencia
        LatLng valencia = new LatLng(39.469, -0.378);
        mMap.moveCamera(CameraUpdateFactory.newLatLng(valencia));

        //Add GeoJSON layer of bike lanes and handle errors

        try {
            //lanes layer

            final Button btn_carril = (Button) view.findViewById(R.id.btnCarrilToggle);
            final GeoJsonLayer carril = new GeoJsonLayer(mMap, R.raw.oficialcarril, getActivity().getApplicationContext());
            final Drawable myDrawableLaneOn = ContextCompat.getDrawable(getActivity().getApplicationContext(), R.drawable.ic_road_variant_black_24dp);
            final Drawable myDrawableLaneOff = ContextCompat.getDrawable(getActivity().getApplicationContext(), R.drawable.ic_road_variant_off_black_24dp);

            btn_carril.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    if (carrilLayer == 0) {
                        btn_carril.setCompoundDrawablesWithIntrinsicBounds(myDrawableLaneOn, null, null, null);
                        carril.addLayerToMap();
                        carrilLayer = 1;
                    } else {
                        btn_carril.setCompoundDrawablesWithIntrinsicBounds(myDrawableLaneOff, null, null, null);
                        carril.removeLayerFromMap();
                        carrilLayer = 0;
                    }

                }
            });

        } catch (IOException e) {

            Log.e(mLogTag, "GeoJSON file could not be read");

        } catch (JSONException e) {

            Log.e(mLogTag, "GeoJSON file could not be converted to a JSONObject");
        }


        new GetStations().execute();

    }
    //React to permission dialog
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == MY_LOCATION_REQUEST_CODE) {
            if (ContextCompat.checkSelfPermission(getActivity().getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                mMap.setMyLocationEnabled(true);
            } else {
                Toast.makeText(getActivity().getApplicationContext(), R.string.no_location_permission, Toast.LENGTH_SHORT).show();

            }
        }
    }

    private class GetStations extends AsyncTask<Void, Void, GeoJsonLayer> {

        BitmapDescriptor icon_green = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN);
        BitmapDescriptor icon_orange = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE);
        BitmapDescriptor icon_yellow = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW);
        BitmapDescriptor icon_red = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED);
        final Button btn_estaciones = (Button) view.findViewById(R.id.btnEstacionesToggle);
        final Button btnOnFootToggle = (Button) view.findViewById(R.id.btnOnFootToggle);
        final Button btnRefresh = (Button) view.findViewById(R.id.btnRefresh);
        final Drawable myDrawableBike = ContextCompat.getDrawable(getActivity().getApplicationContext(), R.drawable.ic_directions_bike_black_24dp);
        final Drawable myDrawableWalk = ContextCompat.getDrawable(getActivity().getApplicationContext(), R.drawable.ic_directions_walk_black_24dp);
        final Drawable myDrawableStationsOn = ContextCompat.getDrawable(getActivity().getApplicationContext(), R.drawable.ic_place_black_24dp);
        final Drawable myDrawableStationsOff = ContextCompat.getDrawable(getActivity().getApplicationContext(), R.drawable.ic_map_marker_off_black_24dp);

        @Override
        protected GeoJsonLayer doInBackground(Void... params) {


            // Creating service handler class instance
            WebRequest webreq = new WebRequest();

            // Making a request to url and getting response
            String jsonStr = webreq.makeWebServiceCall(url, WebRequest.GET);

            try {
                if(!jsonStr.equals("")) {
                    JSONArray datos = new JSONArray(jsonStr);
                    final GeoJsonLayer layer = new GeoJsonLayer(mMap, R.raw.valencia, getActivity().getApplicationContext());

                    for (counter = 0; counter < datos.length(); counter++) {

                        JSONObject object = datos.getJSONObject(counter);


                        for (GeoJsonFeature feature : layer.getFeatures()) {  //loop through features
                            //Add each number and address to its correspondent marker

                            if (object.getString("number").equals(feature.getProperty("Number"))) {

                                GeoJsonPointStyle pointStyle = new GeoJsonPointStyle();
                                pointStyle.setTitle(object.getString("address"));
                                pointStyle.setSnippet("Huecos: " + object.getInt("available_bike_stands") +
                                        " - Bicis: " + object.getInt("available_bikes"));
                                //set markers colors
                                if (onFoot == 1) {
                                    if (object.getInt("available_bikes") == 0) {
                                        pointStyle.setIcon(icon_red);
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
                                    } else if (object.getInt("available_bike_stands") < 5) {
                                        pointStyle.setIcon(icon_orange);
                                    } else if (object.getInt("available_bike_stands") < 10) {
                                        pointStyle.setIcon(icon_yellow);
                                    } else {
                                        pointStyle.setIcon(icon_green);
                                    }
                                }
                                feature.setPointStyle(pointStyle);

                            }
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

            if (layer != null) {
                if (estacionesLayer) {
                    layer.addLayerToMap();
                }

                //Toggle Stations
                btn_estaciones.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        if (estacionesLayer) {
                            layer.removeLayerFromMap();
                            btn_estaciones.setCompoundDrawablesWithIntrinsicBounds(myDrawableStationsOff, null, null, null);
                            estacionesLayer = false;
                        } else {
                            layer.addLayerToMap();
                            btn_estaciones.setCompoundDrawablesWithIntrinsicBounds(myDrawableStationsOn, null, null, null);
                            estacionesLayer = true;
                        }

                    }
                });

                //Toggle onFoot/onBike
                btnOnFootToggle.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        layer.removeLayerFromMap();
                        if (onFoot == 1) {
                            onFoot = 0;
                            btnOnFootToggle.setCompoundDrawablesWithIntrinsicBounds(myDrawableBike, null, null, null);
                            new GetStations().execute();

                        } else {
                            onFoot = 1;
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
            }
            else{
                Toast toast = Toast.makeText(getActivity().getApplicationContext(), "No se han podido cargar los datos, prueba más tarde", Toast.LENGTH_LONG);
                toast.show();
            }
        }

    }
}
