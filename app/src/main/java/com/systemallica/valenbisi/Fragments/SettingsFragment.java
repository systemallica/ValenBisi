package com.systemallica.valenbisi.Fragments;

import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.v4.content.ContextCompat;
import android.widget.Toast;

import com.systemallica.valenbisi.R;


public class SettingsFragment extends PreferenceFragment {


    public SettingsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        //addPreferencesFromResource(R.xml.fragment_settings);

        final CheckBoxPreference navBarPref = (CheckBoxPreference) findPreference("navBar");

        navBarPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {

            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {

                if (navBarPref.isChecked()) {
                    if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        getActivity().getWindow().setNavigationBarColor(ContextCompat.getColor(getActivity().getApplicationContext(), R.color.colorAccent));

                    }
                    else{
                        Toast toast = Toast.makeText(getActivity().getApplicationContext(), "Los cambios se aplicarán al reiniciar", Toast.LENGTH_LONG);
                        toast.show();
                    }
                }
                return true;
            }
        });

        //Change toolbar title
        getActivity().setTitle("Ajustes");


    }
}


