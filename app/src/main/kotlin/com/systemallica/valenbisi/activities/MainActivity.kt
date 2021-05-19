package com.systemallica.valenbisi.activities

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentTransaction
import androidx.preference.PreferenceManager.getDefaultSharedPreferences
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.InstallState
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.systemallica.valenbisi.R
import com.systemallica.valenbisi.databinding.ActivityMainBinding
import com.systemallica.valenbisi.fragments.AboutFragment
import com.systemallica.valenbisi.fragments.MapsFragment
import com.systemallica.valenbisi.fragments.SettingsFragment
import kotlin.system.exitProcess


class MainActivity : AppCompatActivity() {

    private lateinit var inflatedFragment: CharSequence
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        initActivity()

        if (savedInstanceState == null) {
            //Inflate main fragment
            val fragmentTransaction = supportFragmentManager.beginTransaction()
            // Change fragment
            fragmentTransaction.replace(R.id.containerView, MapsFragment()).commitNow()
            title = getString(R.string.app_name)
            inflatedFragment = getString(R.string.app_name)
            binding.bottomNavigationView.menu.getItem(0).isChecked = true
        } else {
            inflatedFragment = savedInstanceState.getCharSequence("inflatedFragment")!!
        }

        checkInternetAccess()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putCharSequence("inflatedFragment", inflatedFragment)
    }

    private fun initActivity() {
        setSupportActionBar(binding.toolbar)
        setOnNavigationListener()
        initNavBarColor()
    }

    private fun checkInternetAccess() {
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
            // Check for updates if there's an internet connection
            getLatestVersion()
        }
    }

    private fun setOnNavigationListener() {
        binding.bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            if (inflatedFragment == item.title) return@setOnNavigationItemSelectedListener false

            val id = item.itemId

            val fragmentTransaction = supportFragmentManager.beginTransaction()

            when (id) {
                R.id.nav_map -> {
                    inflatedFragment = item.title
                    fragmentTransaction.replace(R.id.containerView, MapsFragment())
                }
                R.id.nav_settings -> {
                    inflatedFragment = item.title
                    fragmentTransaction.replace(R.id.containerView, SettingsFragment())
                }
                R.id.nav_about -> {
                    inflatedFragment = item.title
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

    // Create a listener to track request state updates.
    private val updateStateListener = { state: InstallState ->
        Log.i("valenbisi", state.installStatus().toString())
        if (state.installStatus() == InstallStatus.DOWNLOADING) {
            val bytesDownloaded = state.bytesDownloaded()
            val totalBytesToDownload = state.totalBytesToDownload()
            Log.i("valenbisi", bytesDownloaded.toString())
            Log.i("valenbisi", totalBytesToDownload.toString())
        }

        if (state.installStatus() == InstallStatus.DOWNLOADED) {
            Log.i("valenbisi", "launch snackbar")
            popupForCompleteUpdate()
        }
    }

    private fun getLatestVersion() {
        val appUpdateManager = AppUpdateManagerFactory.create(this)

        val appUpdateInfoTask = appUpdateManager.appUpdateInfo

        appUpdateInfoTask.addOnFailureListener { appUpdateInfo ->
            Log.e("valenbisi update error", appUpdateInfo.toString())
        }

        // Checks that the platform will allow the specified type of update.
        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
            Log.i("valenbisi update", appUpdateInfo.toString())
            // Before starting an update, register a listener for updates.
            appUpdateManager.registerListener(updateStateListener)
            Log.i("valenbisi update type", appUpdateInfo.updateAvailability().toString())
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) {
                appUpdateManager.startUpdateFlowForResult(
                        appUpdateInfo,
                        AppUpdateType.FLEXIBLE,
                        this,
                        1)
            }
        }
    }

    private fun popupForCompleteUpdate() {
        runOnUiThread {
            val builder = AlertDialog.Builder(this@MainActivity)
            builder.setTitle(R.string.update_download)
                    .setMessage(R.string.update_download)
                    .setIcon(R.drawable.icon_update)
                    .setPositiveButton(R.string.update_ok) { _, _ ->
                        val appUpdateManager = AppUpdateManagerFactory.create(applicationContext)
                        appUpdateManager.completeUpdate()
                        appUpdateManager.unregisterListener(updateStateListener)
                    }
                    .setNegativeButton(R.string.update_not_now) { _, _ ->
                        // Do nothing
                    }
            val dialog = builder.create()
            dialog.show()
        }
    }


    public override fun onDestroy() {
        super.onDestroy()
    }
}
