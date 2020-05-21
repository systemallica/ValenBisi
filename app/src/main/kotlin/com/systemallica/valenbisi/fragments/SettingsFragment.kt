package com.systemallica.valenbisi.fragments

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.preference.*

import com.systemallica.valenbisi.R


class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        // Load the preferences from an XML resource
        setPreferencesFromResource(R.xml.fragment_settings, rootKey)

        // Settings
        val settings = requireContext().getSharedPreferences(PREFS_NAME, 0)

        // NavBar stuff
        val navBarPref = findPreference<CheckBoxPreference>("navBar")

        navBarPref?.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, _ ->

            if (!navBarPref!!.isChecked) {
                requireActivity().window.navigationBarColor =
                        ContextCompat.getColor(requireContext(), R.color.colorPrimary)
            } else {
                requireActivity().window.navigationBarColor =
                        ContextCompat.getColor(requireContext(), R.color.black)
            }
            true
        }

        // Voronoi info
        val infoVoronoi = findPreference<Preference>("infoVoronoi")
        infoVoronoi?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            val browserIntent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://en.wikipedia.org/wiki/Voronoi_diagram")
            )
            startActivity(browserIntent)
            true
        }

        // Theme selection
        val theme = findPreference<ListPreference>("theme")
        theme?.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
            when (newValue) {
                "light" -> {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                    settings.edit().putInt("theme", 1).apply()
                }
                "dark" -> {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                    settings.edit().putInt("theme", 2).apply()
                }
                else -> {
                    if (Build.VERSION.SDK_INT < 29) {
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY)
                    } else {
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                    }
                    settings.edit().putInt("theme", 3).apply()
                }
            }
            true
        }
    }
}


