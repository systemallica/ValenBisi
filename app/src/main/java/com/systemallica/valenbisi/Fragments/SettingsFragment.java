package com.systemallica.valenbisi.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.preference.CheckBoxPreference;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v4.content.ContextCompat;
import android.widget.Toast;

import com.systemallica.valenbisi.R;


public class SettingsFragment extends PreferenceFragmentCompat {

    private Context context;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        // Load the preferences from an XML resource
        setPreferencesFromResource(R.xml.fragment_settings, rootKey);

        context = getActivity().getApplicationContext();

        //Change toolbar title
        getActivity().setTitle(R.string.nav_settings);

        //NavBar stuff
        final CheckBoxPreference navBarPref = (CheckBoxPreference) findPreference("navBar");

        navBarPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {

            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {

                if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

                    if (!navBarPref.isChecked()) {
                        getActivity().getWindow().setNavigationBarColor(ContextCompat.getColor(context, R.color.colorPrimary));
                    }
                    else{
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
        final ListPreference languagePref = (ListPreference) findPreference("language");
        languagePref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                String userSelectedValue = (String) newValue;

                final SharedPreferences settings = context.getSharedPreferences("MyPrefsFile", 0);
                final SharedPreferences.Editor editor = settings.edit();
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


