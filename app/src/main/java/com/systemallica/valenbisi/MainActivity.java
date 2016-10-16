package com.systemallica.valenbisi;

import android.content.Context;
import android.content.DialogInterface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.UiSettings;
import com.systemallica.valenbisi.Fragments.AboutFragment;
import com.systemallica.valenbisi.Fragments.MainFragment;
import com.systemallica.valenbisi.Fragments.SettingsFragment;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private GoogleMap mMap;
    UiSettings mapSettings;
    NavigationView navigationView;
    int MY_LOCATION_REQUEST_CODE = 1;
    int carrilLayer = 0;
    boolean estacionesLayer = true;
    int onFoot=1;
    int counter = 0;
    private final static String mLogTag = "GeoJsonDemo";
    private final static String url = "https://api.jcdecaux.com/vls/v1/stations?contract=Valence&apiKey=adcac2d5b367dacef9846586d12df1bf7e8c7fcd"; // api request of all valencia stations' data
    //private ArrayList<MenuItem> items = new ArrayList<>();
    //HashMap<Integer, Fragment> cache = new HashMap<>();
    FragmentManager mFragmentManager;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Context context = this;

        //set view to main
        setContentView(R.layout.activity_main);

        //init toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //init drawer
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
            this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        //init navigation view, and set the first item as checked
        navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        mFragmentManager = getSupportFragmentManager();
        mFragmentManager.beginTransaction().replace(R.id.containerView, new MainFragment()).commit();
        navigationView.getMenu().getItem(0).setChecked(true);
        //items.add(navigationView.getMenu().getItem(0));

        //Check internet

        final ConnectivityManager cm =
                (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        final boolean isConnected = activeNetwork != null &&
                activeNetwork.isConnected();

        //React to the check
        if(isConnected) {
            //Do nothing
        }
        else{
            //Prompt an alert dialog to the user
            new AlertDialog.Builder(context)
                    .setTitle("No hay conexión de Internet")
                    .setMessage("La aplicación no funciona sin Internet. Conéctate y reiníciala.")
                    .setPositiveButton("Cerrar", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            System.exit(0);
                        }
                    })

                    .setNegativeButton("Continuar de todas formas", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            //Do nothing
                        }
                    })

                    .setIcon(R.drawable.ic_report_problem_black_24dp)
                    .show();
        }

    }
/*
    @Override
    public void onMapReady(GoogleMap googleMap) {

        //Add GeoJSON layer(with data) and handle errors

        try {
            //lanes layer
            final Button btn_carril = (Button) findViewById(R.id.btnCarrilToggle);
            final GeoJsonLayer carril = new GeoJsonLayer(mMap, R.raw.oficialcarril, this);
            final Drawable myDrawableLaneOn = ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_road_variant_black_24dp);
            final Drawable myDrawableLaneOff = ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_road_variant_off_black_24dp);

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


        new MainActivity.GetStations().execute();


    }

    private class GetStations extends AsyncTask<Void, Void, GeoJsonLayer> {

        BitmapDescriptor icon_green = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN);
        BitmapDescriptor icon_orange = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE);
        BitmapDescriptor icon_yellow = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW);
        BitmapDescriptor icon_red = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED);
        final Button btn_estaciones = (Button) findViewById(R.id.btnEstacionesToggle);
        final Button btnOnFootToggle = (Button) findViewById(R.id.btnOnFootToggle);
        final Button btnRefresh = (Button) findViewById(R.id.btnRefresh);
        final Drawable myDrawableBike = ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_directions_bike_black_24dp);
        final Drawable myDrawableWalk = ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_directions_walk_black_24dp);
        final Drawable myDrawableStationsOn = ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_place_black_24dp);
        final Drawable myDrawableStationsOff = ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_map_marker_off_black_24dp);

        @Override
        protected GeoJsonLayer doInBackground(Void... params) {
            // Creating service handler class instance
            WebRequest webreq = new WebRequest();

            // Making a request to url and getting response
            String jsonStr = webreq.makeWebServiceCall(url, WebRequest.GET);

            try {
                JSONArray datos = new JSONArray(jsonStr);
                final GeoJsonLayer layer = new GeoJsonLayer(mMap, R.raw.valencia, getApplicationContext());

                for(counter = 0; counter < datos.length(); counter++ ) {

                    JSONObject object = datos.getJSONObject(counter);


                    for (GeoJsonFeature feature : layer.getFeatures()) {  //loop through features
                        //Add each number and address to its correspondent marker

                        if(object.getString("number").equals(feature.getProperty("Number"))){

                            GeoJsonPointStyle pointStyle = new GeoJsonPointStyle();
                            pointStyle.setTitle(object.getString("address"));
                            pointStyle.setSnippet("Huecos: " + object.getInt("available_bike_stands") +
                                    " - Bicis: " + object.getInt("available_bikes"));
                            //set markers colors
                            if(onFoot==1) {
                                if (object.getInt("available_bikes") == 0) {
                                    pointStyle.setIcon(icon_red);
                                } else if (object.getInt("available_bikes") < 5) {
                                    pointStyle.setIcon(icon_orange);
                                } else if (object.getInt("available_bikes") < 10) {
                                    pointStyle.setIcon(icon_yellow);
                                } else {
                                    pointStyle.setIcon(icon_green);
                                }
                            }
                            else{
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
            } catch (JSONException e) {

                Log.e(mLogTag, "JSONArray could not be created");
            } catch (IOException e) {

                Log.e(mLogTag, "GeoJSON file could not be read");

            }

            return null;
        }

        protected void onPostExecute(final GeoJsonLayer layer) {

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
                    if (onFoot==1) {
                        onFoot=0;
                        btnOnFootToggle.setCompoundDrawablesWithIntrinsicBounds(myDrawableBike, null, null, null);
                        new MainActivity.GetStations().execute();

                    } else {
                        onFoot=1;
                        btnOnFootToggle.setCompoundDrawablesWithIntrinsicBounds(myDrawableWalk, null, null, null);
                        new MainActivity.GetStations().execute();
                    }

                }
            });

            //Reload data
            btnRefresh.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    layer.removeLayerFromMap();
                    new MainActivity.GetStations().execute();
                }
            });
        }

    }*/

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_map) {

            mFragmentManager.beginTransaction().replace(R.id.containerView, new MainFragment()).commit();
            navigationView.getMenu().getItem(0).setChecked(true);

        } else if (id == R.id.nav_settings) {

            mFragmentManager.beginTransaction().replace(R.id.containerView, new SettingsFragment()).commit();
            navigationView.getMenu().getItem(1).setChecked(true);

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_about) {

            mFragmentManager.beginTransaction().replace(R.id.containerView, new AboutFragment()).commit();
            navigationView.getMenu().getItem(3).setChecked(true);

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

}
