package com.systemallica.valenbisi.activities

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentTransaction
import androidx.preference.PreferenceManager.getDefaultSharedPreferences
import com.google.android.material.snackbar.Snackbar
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.InstallState
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.play.core.listener.StateUpdatedListener
import com.systemallica.valenbisi.R
import com.systemallica.valenbisi.R.layout.activity_main
import com.systemallica.valenbisi.fragments.AboutFragment
import com.systemallica.valenbisi.fragments.MapsFragment
import com.systemallica.valenbisi.fragments.SettingsFragment
import kotlinx.android.synthetic.main.activity_main.*
import kotlin.system.exitProcess


class MainActivity : AppCompatActivity(), StateUpdatedListener<InstallState> {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(activity_main)

        initActivity()

        //Inflate main fragment
        val fragmentTransaction = supportFragmentManager.beginTransaction()

        if (savedInstanceState == null) {
            // Change fragment
            fragmentTransaction.replace(R.id.containerView, MapsFragment()).commitNow()
            title = getString(R.string.nav_map)
            bottom_navigation_view.menu.getItem(0).isChecked = true
        } else {
            fragmentTransaction.commitNow()
        }

        //Check if current network has internet access
        val cm =
                applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = cm.activeNetwork
        val networkCapabilities = cm.getNetworkCapabilities(cm.activeNetwork)
        val isConnected = networkCapabilities?.hasCapability(NET_CAPABILITY_INTERNET)

        //React to the check
        if (activeNetwork == null || (isConnected != null && !isConnected)) {

            AlertDialog.Builder(this)
                    .setTitle(R.string.no_internet)
                    .setMessage(R.string.no_internet_message)
                    .setPositiveButton(R.string.close
                    ) { _, _ ->
                        exitProcess(0)
                    }
                    .setNegativeButton(R.string.continuer
                    ) { _, _ ->
                        // Do nothing
                    }
                    .setIcon(R.drawable.icon_alert)
                    .show()

        } else {
            getLatestVersion()
        }
    }

    private fun initActivity() {
        setSupportActionBar(toolbar)
        setOnNavigationListener()
        initNavBarColor()
    }

    private fun setOnNavigationListener() {
        bottom_navigation_view.setOnNavigationItemSelectedListener { item ->
            if (title == item.title) return@setOnNavigationItemSelectedListener false

            val id = item.itemId

            val fragmentTransaction = supportFragmentManager.beginTransaction()

            when (id) {
                R.id.nav_map -> {
                    // Set Activity title
                    title = item.title
                    fragmentTransaction.replace(R.id.containerView, MapsFragment())
                }
                R.id.nav_settings -> {
                    // Set Activity title
                    title = item.title
                    fragmentTransaction.replace(R.id.containerView, SettingsFragment())
                }
                R.id.nav_about -> {
                    // Set Activity title
                    title = item.title
                    fragmentTransaction.replace(R.id.containerView, AboutFragment())
                }
            }

            fragmentTransaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN).commitNow()
            return@setOnNavigationItemSelectedListener true
        }
    }

    private fun initNavBarColor() {
        val userSettings = getDefaultSharedPreferences(applicationContext)
        val navBar = userSettings.getBoolean("navBar", true)
        val colorPrimary = ContextCompat.getColor(applicationContext, R.color.colorPrimary)
        if (navBar) {
            window.navigationBarColor = colorPrimary
        }
    }

    private fun getLatestVersion() {
        val appUpdateManager = AppUpdateManagerFactory.create(applicationContext)

        val appUpdateInfoTask = appUpdateManager.appUpdateInfo

        // Checks that the platform will allow the specified type of update.
        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                    && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) {

                appUpdateManager.startUpdateFlowForResult(
                        appUpdateInfo,
                        AppUpdateType.FLEXIBLE,
                        this,
                        1)
            }
        }

    }

    override fun onStateUpdate(state: InstallState) {
        if (state.installStatus() == InstallStatus.DOWNLOADED) {
            popupSnackbarForCompleteUpdate()
        }
    }

    private fun popupSnackbarForCompleteUpdate() {
        Snackbar.make(
                findViewById(R.id.activity_main_layout),
                "An update has just been downloaded.",
                Snackbar.LENGTH_INDEFINITE
        ).apply {
            val appUpdateManager = AppUpdateManagerFactory.create(applicationContext)
            setAction("RESTART") { appUpdateManager.completeUpdate() }
            setActionTextColor(ContextCompat.getColor(applicationContext, R.color.white))
            show()
        }
    }


    public override fun onDestroy() {
        super.onDestroy()
    }
}
