package com.systemallica.valenbisi.Fragments;

import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.v4.content.ContextCompat;
import android.widget.Toast;

import com.systemallica.valenbisi.R;


public class SettingsFragment extends PreferenceFragment {
    public static final String PREFS_NAME = "MyPrefsFile";

    public SettingsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final SharedPreferences settings = getActivity().getApplicationContext().getSharedPreferences(PREFS_NAME, 0);

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

                        SharedPreferences.Editor editor = settings.edit();
                        editor.putBoolean("navBar", true);
                        editor.apply();
                        getActivity().getWindow().setNavigationBarColor(ContextCompat.getColor(getActivity().getApplicationContext(), R.color.colorPrimary));
                    }
                    else{
                        SharedPreferences.Editor editor = settings.edit();
                        editor.putBoolean("navBar", false);
                        editor.apply();
                        getActivity().getWindow().setNavigationBarColor(ContextCompat.getColor(getActivity().getApplicationContext(), R.color.black));
                    }
                }
                else{
                    if(isAdded()) {
                        Toast toast = Toast.makeText(getActivity().getApplicationContext(), "Tu versión de Android no es compatible con esto :(", Toast.LENGTH_LONG);
                        toast.show();
                    }
                }
                return true;
            }
        });

        //Satellite stuff
        final CheckBoxPreference satelliteViewPref = (CheckBoxPreference) findPreference("mapView");

        satelliteViewPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {

            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (!satelliteViewPref.isChecked()) {
                    SharedPreferences.Editor editor = settings.edit();
                    editor.putBoolean("mapView", true);
                    editor.putBoolean("isChanged", true);
                    editor.apply();
                }
                else{
                    SharedPreferences.Editor editor = settings.edit();
                    editor.putBoolean("mapView", false);
                    editor.putBoolean("isChanged", true);
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
                    SharedPreferences.Editor editor = settings.edit();
                    editor.putBoolean("showFavorites", true);
                    editor.putBoolean("isChanged", true);
                    editor.apply();
                }
                else{
                    SharedPreferences.Editor editor = settings.edit();
                    editor.putBoolean("showFavorites", false);
                    editor.putBoolean("isChanged", true);
                    editor.apply();
                }
                return true;
            }
        });
    }
}


