package com.systemallica.valenbisi.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.v4.content.ContextCompat;
import android.widget.Toast;

import com.systemallica.valenbisi.R;


public class SettingsFragment extends PreferenceFragment {

    public static final String PREFS_NAME = "MyPrefsFile";
    private Context context;

    public SettingsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        context = getActivity().getApplicationContext();
        final SharedPreferences settings = context.getSharedPreferences(PREFS_NAME, 0);
        final SharedPreferences.Editor editor = settings.edit();

        //Change toolbar title
        getActivity().setTitle(R.string.nav_settings);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.fragment_settings);

        //NavBar stuff
        final CheckBoxPreference navBarPref = (CheckBoxPreference) findPreference("navBar");

        navBarPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {

            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {

                if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

                    if (!navBarPref.isChecked()) {
                        editor.putBoolean("navBar", true);
                        editor.apply();
                        getActivity().getWindow().setNavigationBarColor(ContextCompat.getColor(context, R.color.colorPrimary));
                    }
                    else{
                        editor.putBoolean("navBar", false);
                        editor.apply();
                        getActivity().getWindow().setNavigationBarColor(ContextCompat.getColor(context, R.color.black));
                    }
                }
                else{
                    if(isAdded()) {
                        Toast toast = Toast.makeText(context, "Tu versión de Android no es compatible con esto :(", Toast.LENGTH_LONG);
                        toast.show();
                    }
                }
                return true;
            }
        });

        //Satellite stuff
        final CheckBoxPreference satelliteViewPref = (CheckBoxPreference) findPreference("satellite");

        satelliteViewPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {

            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (!satelliteViewPref.isChecked()) {
                    editor.putBoolean("satellite", true);
                    editor.apply();
                }
                else{
                    editor.putBoolean("satellite", false);
                    editor.apply();
                }

                return true;
            }

        });

        //Bike lanes stuff
        final CheckBoxPreference bikeLanesPref = (CheckBoxPreference) findPreference("bikeLanes");

        bikeLanesPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {

            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (!bikeLanesPref.isChecked()) {
                    editor.putBoolean("bikeLanes", true);
                    editor.apply();
                }
                else{
                    editor.putBoolean("bikeLanes", false);
                    editor.apply();
                }

                return true;
            }

        });

        //Ciclocalles stuff
        final CheckBoxPreference ciclocallesPref = (CheckBoxPreference) findPreference("cicloCalles");

        ciclocallesPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {

            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (!ciclocallesPref.isChecked()) {
                    editor.putBoolean("cicloCalles", true);
                    editor.apply();
                }
                else{
                    editor.putBoolean("cicloCalles", false);
                    editor.apply();
                }

                return true;
            }

        });

        //Favorite stuff
        final CheckBoxPreference showFavoritesPref = (CheckBoxPreference) findPreference("showFavorites");
        showFavoritesPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {

            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (!showFavoritesPref.isChecked()) {
                    editor.putBoolean("showFavorites", true);
                    editor.apply();
                }
                else{
                    editor.putBoolean("showFavorites", false);
                    editor.apply();
                }
                return true;
            }
        });

        //Available stuff
        final CheckBoxPreference showAvailablesPref = (CheckBoxPreference) findPreference("showAvailable");
        showAvailablesPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {

            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (!showAvailablesPref.isChecked()) {
                    editor.putBoolean("showAvailable", true);
                    editor.apply();
                }
                else{
                    editor.putBoolean("showAvailable", false);
                    editor.apply();
                }
                return true;
            }
        });

        //Zoom stuff
        final CheckBoxPreference initialZoomPref = (CheckBoxPreference) findPreference("initialZoom");
        initialZoomPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {

            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (!initialZoomPref.isChecked()) {
                    editor.putBoolean("initialZoom", true);
                    editor.apply();
                }
                else{
                    editor.putBoolean("initialZoom", false);
                    editor.apply();
                }
                return true;
            }
        });

        //LastUpdated stuff
        final CheckBoxPreference lastUpdatedPref = (CheckBoxPreference) findPreference("lastUpdated");
        lastUpdatedPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {

            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (!lastUpdatedPref.isChecked()) {
                    editor.putBoolean("lastUpdated", true);
                    editor.apply();
                }
                else{
                    editor.putBoolean("lastUpdated", false);
                    editor.apply();
                }
                return true;
            }
        });

        //Clustering stuff
        final CheckBoxPreference clusteringPref = (CheckBoxPreference) findPreference("clustering");
        clusteringPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {

            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (!clusteringPref.isChecked()) {
                    editor.putBoolean("clustering", true);
                    editor.apply();
                }
                else{
                    editor.putBoolean("clustering", false);
                    editor.apply();
                }
                return true;
            }
        });


        //Voronoi stuff
        final CheckBoxPreference voronoiPref = (CheckBoxPreference) findPreference("voronoiCell");
        voronoiPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {

            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (!voronoiPref.isChecked()) {
                    editor.putBoolean("voronoiCell", true);
                    editor.apply();
                }
                else{
                    editor.putBoolean("voronoiCell", false);
                    editor.apply();
                }
                return true;
            }
        });

        //Voronoi info
        Preference infoVoronoi = findPreference("infoVoronoi");
        infoVoronoi.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://en.wikipedia.org/wiki/Voronoi_diagram"));
                startActivity(browserIntent);
                return true;
            }
        });

        //Language stuff
        final ListPreference languagepref = (ListPreference) findPreference("language");
        languagepref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                String userSelectedValue = (String) newValue;
                editor.putString("locale", userSelectedValue);
                editor.apply();

                Intent i = getActivity().getPackageManager().getLaunchIntentForPackage(getActivity().getPackageName());
                if(i != null) {
                    i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(i);
                    getActivity().finish();
                }

               return true;
            }
        });
    }
}


