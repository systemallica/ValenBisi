package com.systemallica.valenbisi;

import android.app.ActivityManager;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ContextThemeWrapper;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.systemallica.valenbisi.Fragments.AboutFragment;
import com.systemallica.valenbisi.Fragments.DonateFragment;
import com.systemallica.valenbisi.Fragments.MainFragment;
import com.systemallica.valenbisi.Fragments.MyContextWrapper;
import com.systemallica.valenbisi.Fragments.SettingsFragment;

import java.util.Locale;

import static com.systemallica.valenbisi.Fragments.MyContextWrapper.getSystemLocale;
import static com.systemallica.valenbisi.Fragments.MyContextWrapper.getSystemLocaleLegacy;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    NavigationView navigationView;
    FragmentManager mFragmentManager;
    public static final String PREFS_NAME = "MyPrefsFile";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Context context = this;
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        boolean navBar = settings.getBoolean("navBar", true);

        int colorPrimary = ContextCompat.getColor(context, R.color.colorPrimary);

        //Apply preferences navBar preference
        if(navBar && android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
            getWindow().setNavigationBarColor(colorPrimary);
        }

        //Recents implementation
        //String title = null;  // You can either set the title to whatever you want or just use null and it will default to your app/activity name
        Bitmap recentsIcon = BitmapFactory.decodeResource(context.getResources(), R.drawable.splash_inverted);//Choose the icon

        if(android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ActivityManager.TaskDescription description = new ActivityManager.TaskDescription(null, recentsIcon, colorPrimary);
            this.setTaskDescription(description);
        }

        //set view to main
        setContentView(R.layout.activity_main);

        //init toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //init drawer
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
            this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        //init navigation view
        navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        //Inflate main fragment
        mFragmentManager = getFragmentManager();
        FragmentTransaction ft = mFragmentManager.beginTransaction();

        if (savedInstanceState == null) {
            ft.replace(R.id.containerView, new MainFragment(), "mainFragment").commit();
            navigationView.getMenu().getItem(0).setChecked(true);
        }

        //Check internet
        final ConnectivityManager cm =
                (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        final boolean isConnected = activeNetwork != null &&
                activeNetwork.isConnected();

        //React to the check
        if(!isConnected) {
            //Prompt an alert dialog to the user
            new AlertDialog.Builder(context)
                    .setTitle(R.string.no_internet)
                    .setMessage(R.string.no_internet_message)
                    .setPositiveButton(R.string.close, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            System.exit(0);
                        }
                    })

                    .setNegativeButton(R.string.continuer, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            //Do nothing
                        }
                    })

                    .setIcon(R.drawable.ic_report_problem_black_24dp)
                    .show();
        }else{
            boolean noUpdate = settings.getBoolean("noUpdate", false);

            if(!noUpdate) {
                new CheckVersion().execute();
            }
        }

        boolean removedAds = settings.getBoolean("removedAds", false);

        //Ads management
        AdView mAdView = (AdView) findViewById(R.id.adView);

        if(removedAds){
            mAdView.destroy();
            mAdView.setVisibility(View.GONE);
        }
        else {
        //Ad request and load
        //mAdView.setVisibility(View.VISIBLE);
        AdRequest adRequest = new AdRequest.Builder()
                //.addTestDevice("6BB60830FFEC02110221CD0A1878D464")
                .build();
        mAdView.loadAd(adRequest);
        mAdView.setVisibility(View.GONE);
        }
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        final SharedPreferences settings = newBase.getSharedPreferences(PREFS_NAME, 0);
        String locale = settings.getString("locale", "default_locale");

        //Get default system locale
        Configuration config = newBase.getResources().getConfiguration();
        Locale sysLocale = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            sysLocale = getSystemLocale(config);
        } else {
            sysLocale = getSystemLocaleLegacy(config);
        }

        //Apply it if user didn't specify a locale
        if (locale.equals("default_locale")){
            super.attachBaseContext(MyContextWrapper.wrap(newBase,sysLocale.getLanguage()));
        //Else apply user choice
        }else{
            super.attachBaseContext(MyContextWrapper.wrap(newBase,locale));
        }
    }


    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();
        AdView mAdView = (AdView) findViewById(R.id.adView);

        if (id == R.id.nav_map) {

            mAdView.setVisibility(View.GONE);

            //Change toolbar title
            this.setTitle(R.string.nav_map);
            SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
            SharedPreferences.Editor editor = settings.edit();
            boolean isChanged = settings.getBoolean("isChanged", false);

            FragmentTransaction ft = mFragmentManager.beginTransaction();
            if(isChanged){
                ft.replace(R.id.containerView, new MainFragment(), "mainFragment");
                editor.putBoolean("isChanged", false);
                editor.apply();
            }
            else {
                ft.show(getFragmentManager().findFragmentByTag("mainFragment"));
            }
            if(getFragmentManager().findFragmentByTag("aboutFragment")!=null) {
                ft.remove(getFragmentManager().findFragmentByTag("aboutFragment"));
            }
            if(getFragmentManager().findFragmentByTag("settingsFragment")!=null){
                ft.remove(getFragmentManager().findFragmentByTag("settingsFragment"));
            }
            if(getFragmentManager().findFragmentByTag("donateFragment")!=null){
                ft.remove(getFragmentManager().findFragmentByTag("donateFragment"));
            }
            ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
            ft.commit();

        } else if (id == R.id.nav_settings) {

            mAdView.setVisibility(View.VISIBLE);

            FragmentTransaction ft = mFragmentManager.beginTransaction();
            ft.hide(getFragmentManager().findFragmentByTag("mainFragment"));
            if(getFragmentManager().findFragmentByTag("aboutFragment")!=null) {
                ft.remove(getFragmentManager().findFragmentByTag("aboutFragment"));
            }
            if(getFragmentManager().findFragmentByTag("donateFragment")!=null){
                ft.remove(getFragmentManager().findFragmentByTag("donateFragment"));
            }
            if(getFragmentManager().findFragmentByTag("settingsFragment")!=null){
                ft.remove(getFragmentManager().findFragmentByTag("settingsFragment"));
            }
            ft.add(R.id.containerView, new SettingsFragment(), "settingsFragment");
            ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
            ft.commit();

        } else if (id == R.id.nav_donate) {

            mAdView.setVisibility(View.VISIBLE);

            FragmentTransaction ft = mFragmentManager.beginTransaction();
            ft.hide(getFragmentManager().findFragmentByTag("mainFragment"));
            if(getFragmentManager().findFragmentByTag("settingsFragment")!=null){
                ft.remove(getFragmentManager().findFragmentByTag("settingsFragment"));
            }
            if(getFragmentManager().findFragmentByTag("aboutFragment")!=null){
                ft.remove(getFragmentManager().findFragmentByTag("aboutFragment"));
            }
            if(getFragmentManager().findFragmentByTag("donateFragment")!=null){
                ft.remove(getFragmentManager().findFragmentByTag("donateFragment"));
            }
            ft.add(R.id.containerView, new DonateFragment(), "donateFragment");
            ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
            ft.commit();

        } else if (id == R.id.nav_share) {

            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_TEXT,
                    R.string.share);
            sendIntent.setType("text/plain");
            startActivity(sendIntent);

        } else if (id == R.id.nav_about) {

            mAdView.setVisibility(View.VISIBLE);

            FragmentTransaction ft = mFragmentManager.beginTransaction();
            ft.hide(getFragmentManager().findFragmentByTag("mainFragment"));
            if(getFragmentManager().findFragmentByTag("settingsFragment")!=null){
                ft.remove(getFragmentManager().findFragmentByTag("settingsFragment"));
            }
            if(getFragmentManager().findFragmentByTag("donateFragment")!=null){
                ft.remove(getFragmentManager().findFragmentByTag("donateFragment"));
            }
            if(getFragmentManager().findFragmentByTag("aboutFragment")!=null){
                ft.remove(getFragmentManager().findFragmentByTag("aboutFragment"));
            }
            ft.add(R.id.containerView, new AboutFragment(), "aboutFragment");
            ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
            ft.commit();

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

   private class CheckVersion extends AsyncTask<Void, Void, String> {

        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(Void... params) {

            // Creating service handler class instance
            WebRequest webreq = new WebRequest();
            // Making a request to url and getting response
            return webreq.makeWebServiceCall("https://systemallica.000webhostapp.com/version.html", WebRequest.GET);
   }

        protected void onPostExecute(final String latestVersion) {

            //Check for updates
            String versionName = BuildConfig.VERSION_NAME;

            //Log.e("versionName", versionName);
            //Log.e("latestVersion", latestVersion);

            if (!versionName.equals(latestVersion)) {
                new AlertDialog.Builder(new ContextThemeWrapper(MainActivity.this, R.style.myDialog))
                        .setTitle(R.string.update_available)
                        .setMessage(R.string.update_message)
                        .setPositiveButton(R.string.update_ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.systemallica.valenbisi"));
                                startActivity(browserIntent);
                            }
                        })

                        .setNegativeButton(R.string.update_not_now, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                //Do nothing
                            }
                        })

                        .setNeutralButton(R.string.update_never, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                final SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
                                SharedPreferences.Editor editor = settings.edit();
                                editor.putBoolean("noUpdate", true);
                                editor.apply();
                            }
                        })

                        .setIcon(R.drawable.ic_system_update_black_24dp)
                        .show();
            }
        }
    }
}
