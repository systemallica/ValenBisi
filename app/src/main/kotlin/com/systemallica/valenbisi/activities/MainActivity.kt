package com.systemallica.valenbisi.activities

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.FragmentTransaction
import androidx.preference.PreferenceManager.getDefaultSharedPreferences
import com.google.android.material.navigation.NavigationView
import com.systemallica.valenbisi.BuildConfig
import com.systemallica.valenbisi.ContextWrapper
import com.systemallica.valenbisi.R
import com.systemallica.valenbisi.R.layout.activity_main
import com.systemallica.valenbisi.fragments.AboutFragment
import com.systemallica.valenbisi.fragments.MapsFragment
import com.systemallica.valenbisi.fragments.SettingsFragment
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*
import okhttp3.*
import java.io.IOException
import java.util.*
import kotlin.system.exitProcess

const val PREFS_NAME = "MyPrefsFile"

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(activity_main)

        initActivity()

        //Inflate main fragment
        val fragmentTransaction = supportFragmentManager.beginTransaction()

        if (savedInstanceState == null) {
            // Change fragment
            fragmentTransaction.replace(R.id.containerView, MapsFragment()).commitNow()

            nav_view.menu.getItem(0).isChecked = true
        } else {
            fragmentTransaction.commitNow()
        }

        //Check internet
        val cm =
            applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val activeNetwork = cm.activeNetworkInfo
        val isConnected = activeNetwork != null && activeNetwork.isConnected

        //React to the check
        if (!isConnected) {
            //Prompt an alert dialog to the user
            AlertDialog.Builder(this)
                .setTitle(R.string.no_internet)
                .setMessage(R.string.no_internet_message)
                .setPositiveButton(R.string.close) { _, _ -> exitProcess(0) }

                .setNegativeButton(R.string.continuer) { _, _ ->
                    //Do nothing
                }

                .setIcon(R.drawable.icon_alert)
                .show()
        } else {
            getLatestVersion()
        }
    }

    override fun attachBaseContext(newBase: Context) {
        // Changing language
        val settings = newBase.getSharedPreferences(PREFS_NAME, 0)
        val locale = settings.getString("locale", "default_locale")

        // Get default system locale
        val config = newBase.resources.configuration
        val sysLocale: Locale
        sysLocale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            ContextWrapper.getSystemLocale(config)
        } else {
            ContextWrapper.getSystemLocaleLegacy(config)
        }

        // Apply default locale if user didn't specify a locale
        if (locale == "default_locale") {
            super.attachBaseContext(ContextWrapper.wrap(newBase, sysLocale.language))
            // Else apply user choice
        } else {
            super.attachBaseContext(ContextWrapper.wrap(newBase, locale!!))
        }
    }


    override fun onBackPressed() {
        val drawer = findViewById<DrawerLayout>(R.id.drawer_layout)
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
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
            R.id.nav_share -> {
                try {
                    shareApplication()
                    fragmentTransaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                        .commitNow()
                    drawer_layout.closeDrawers()
                    return false
                } catch (e: Exception) {
                    e.toString()
                }
            }
            R.id.nav_about -> {
                // Set Activity title
                title = item.title
                fragmentTransaction.replace(R.id.containerView, AboutFragment())
            }
        }

        fragmentTransaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN).commitNow()

        drawer_layout.closeDrawers()
        return true
    }

    private fun shareApplication() {
        val sendIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(
                Intent.EXTRA_TEXT,
                "https://play.google.com/store/apps/details?id=com.systemallica.valenbisi"
            )
            type = "text/plain"
        }
        startActivity(Intent.createChooser(sendIntent, getString(R.string.nav_share)))
    }

    private fun initActivity() {
        setSupportActionBar(toolbar)
        initDrawerToggle()
        initNavigationView()
        initNavBarColor()
        initRecentsIconAndColor()
    }

    private fun initDrawerToggle() {
        val toggle = ActionBarDrawerToggle(
            this,
            drawer_layout,
            toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()
    }

    private fun initNavigationView() {
        nav_view.setNavigationItemSelectedListener(this)
    }

    private fun initNavBarColor() {
        val userSettings = getDefaultSharedPreferences(applicationContext)
        val navBar = userSettings.getBoolean("navBar", true)
        val colorPrimary = ContextCompat.getColor(applicationContext, R.color.colorPrimary)
        if (navBar && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.navigationBarColor = colorPrimary
        }
    }

    private fun initRecentsIconAndColor() {
        val colorPrimary = ContextCompat.getColor(applicationContext, R.color.colorPrimary)

        val recentsIcon = BitmapFactory.decodeResource(
            applicationContext.resources,
            R.drawable.splash_inverted
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val description = ActivityManager.TaskDescription(null, recentsIcon, colorPrimary)
            this.setTaskDescription(description)
        }
    }


    private fun getLatestVersion() {
        val client = OkHttpClient()

        val request = Request.Builder()
            .url("https://raw.githubusercontent.com/systemallica/ValenBisi/master/VersionCode")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
            }

            @Throws(IOException::class)
            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val responseBody = response.body
                    if (!response.isSuccessful)
                        throw IOException("Unexpected code $response")

                    var latestVersionTemp = ""

                    if (responseBody != null) {
                        latestVersionTemp = responseBody.string()
                    }

                    val latestVersion = latestVersionTemp
                    checkUpdate(latestVersion.trim())
                }
            }
        })
    }

    private fun checkUpdate(latestVersion: String) {
        val versionCode = BuildConfig.VERSION_CODE
        val versionGit = Integer.parseInt(latestVersion)

        if (versionCode < versionGit) {

            val settings = getSharedPreferences(PREFS_NAME, 0)
            val noUpdate = settings.getBoolean("noUpdate", false)

            if (!noUpdate) {
                runOnUiThread {
                    val builder = AlertDialog.Builder(this@MainActivity)
                    builder.setTitle(R.string.update_available)
                        .setMessage(R.string.update_message)
                        .setIcon(R.drawable.icon_update)
                        .setPositiveButton(R.string.update_ok) { _, _ ->
                            val browserIntent = Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://play.google.com/store/apps/details?id=com.systemallica.valenbisi")
                            )
                            startActivity(browserIntent)
                        }
                        .setNegativeButton(R.string.update_not_now) { _, _ ->
                            // Do nothing
                        }
                        .setNeutralButton(R.string.update_never) { _, _ ->
                            val editor = settings.edit()
                            editor.putBoolean("noUpdate", true)
                            editor.apply()
                        }
                    val dialog = builder.create()
                    dialog.show()
                }
            }
        } else if (versionCode > versionGit) {
            runOnUiThread {
                val builder = AlertDialog.Builder(this@MainActivity)
                builder.setTitle(R.string.alpha_title)
                    .setMessage(R.string.alpha_message)
                    .setPositiveButton(R.string.update_ok) { _, _ ->
                        // Do nothing
                    }
                val dialog = builder.create()
                dialog.show()
            }
        }
    }

    public override fun onDestroy() {
        super.onDestroy()
    }
}
