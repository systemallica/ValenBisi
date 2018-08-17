package com.systemallica.valenbisi.fragments

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.support.v7.preference.CheckBoxPreference
import android.support.v7.preference.ListPreference
import android.support.v7.preference.Preference
import android.support.v7.preference.PreferenceFragmentCompat
import android.support.v4.content.ContextCompat
import android.widget.Toast

import com.systemallica.valenbisi.R


class SettingsFragment : PreferenceFragmentCompat() {

    private var mContext: Context? = null

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        // Load the preferences from an XML resource
        setPreferencesFromResource(R.xml.fragment_settings, rootKey)

        mContext = activity!!.applicationContext

        //Change toolbar title
        activity!!.setTitle(R.string.nav_settings)

        //NavBar stuff
        val navBarPref = findPreference("navBar") as CheckBoxPreference

        navBarPref.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, _ ->
            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

                if (!navBarPref.isChecked) {
                    activity!!.window.navigationBarColor = ContextCompat.getColor(context!!, R.color.colorPrimary)
                } else {
                    activity!!.window.navigationBarColor = ContextCompat.getColor(context!!, R.color.black)
                }
            } else {
                if (isAdded) {
                    val toast = Toast.makeText(context, "Tu versión de Android no es compatible con esto :(", Toast.LENGTH_LONG)
                    toast.show()
                }
            }
            true
        }


        //Voronoi info
        val infoVoronoi = findPreference("infoVoronoi")
        infoVoronoi.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://en.wikipedia.org/wiki/Voronoi_diagram"))
            startActivity(browserIntent)
            true
        }

        //Language stuff
        val languagePref = findPreference("language") as ListPreference
        languagePref.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
            val userSelectedValue = newValue as String

            val settings = context!!.getSharedPreferences("MyPrefsFile", 0)
            val editor = settings.edit()
            editor.putString("locale", userSelectedValue)
            editor.apply()

            val i = activity!!.packageManager.getLaunchIntentForPackage(activity!!.packageName)
            if (i != null) {
                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                startActivity(i)
                activity!!.finish()
            }

            true
        }
    }
}


