package com.systemallica.valenbisi.fragments

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.preference.CheckBoxPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.core.content.ContextCompat
import android.widget.Toast

import com.systemallica.valenbisi.R


class SettingsFragment : PreferenceFragmentCompat() {

    private var mContext: Context? = null

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        // Load the preferences from an XML resource
        setPreferencesFromResource(R.xml.fragment_settings, rootKey)

        mContext = activity!!.applicationContext

        //NavBar stuff
        val navBarPref = findPreference("navBar") as CheckBoxPreference

        navBarPref.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, _ ->
            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

                if (!navBarPref.isChecked) {
                    activity!!.window.navigationBarColor =
                            ContextCompat.getColor(context!!, R.color.colorPrimary)
                } else {
                    activity!!.window.navigationBarColor =
                            ContextCompat.getColor(context!!, R.color.black)
                }
            } else {
                if (isAdded) {
                    val toast = Toast.makeText(
                        context,
                        "Tu versión de Android no es compatible con esto :(",
                        Toast.LENGTH_LONG
                    )
                    toast.show()
                }
            }
            true
        }


        //Voronoi info
        val infoVoronoi = findPreference("infoVoronoi")
        infoVoronoi.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            val browserIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://en.wikipedia.org/wiki/Voronoi_diagram")
            )
            startActivity(browserIntent)
            true
        }

        //Language stuff
        val languagePref = findPreference("language") as ListPreference
        languagePref.onPreferenceChangeListener =
                Preference.OnPreferenceChangeListener { _, newValue ->
                    val userSelectedValue = newValue as String

                    val settings = context!!.getSharedPreferences("MyPrefsFile", 0)
                    val editor = settings.edit()
                    editor.putString("locale", userSelectedValue)
                    editor.apply()

                    val i =
                        activity!!.packageManager.getLaunchIntentForPackage(activity!!.packageName)
                    if (i != null) {
                        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        startActivity(i)
                        activity!!.finish()
                    }

                    true
                }
    }
}


