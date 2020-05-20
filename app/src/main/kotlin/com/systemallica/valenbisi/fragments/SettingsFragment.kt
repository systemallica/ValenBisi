package com.systemallica.valenbisi.fragments

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.preference.CheckBoxPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.core.content.ContextCompat

import com.systemallica.valenbisi.R


class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        // Load the preferences from an XML resource
        setPreferencesFromResource(R.xml.fragment_settings, rootKey)

        //NavBar stuff
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

        //Voronoi info
        val infoVoronoi = findPreference<Preference>("infoVoronoi")
        infoVoronoi?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            val browserIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://en.wikipedia.org/wiki/Voronoi_diagram")
            )
            startActivity(browserIntent)
            true
        }
    }
}


