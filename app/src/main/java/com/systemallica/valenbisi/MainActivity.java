package com.systemallica.valenbisi;

import android.app.FragmentManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.systemallica.valenbisi.Fragments.AboutFragment;
import com.systemallica.valenbisi.Fragments.MainFragment;
import com.systemallica.valenbisi.Fragments.SettingsFragment;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener{


    NavigationView navigationView;
    FragmentManager mFragmentManager;
    TextView version2;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Context context = this;

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

        View headerView = navigationView.getHeaderView(0);
        version2 = (TextView) headerView.findViewById(R.id.version2);
        version2.setText(BuildConfig.VERSION_NAME);

        //Inflate main fragment
        mFragmentManager = getFragmentManager();

        if (savedInstanceState == null) {
            mFragmentManager.beginTransaction().replace(R.id.containerView, new MainFragment()).commit();
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
                    .setTitle("No hay conexión de Internet")
                    .setMessage("La aplicación no funcionará sin Internet. Conéctate y reiníciala.")
                    .setPositiveButton("Cerrar", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            System.exit(0);
                        }
                    })

                    .setNegativeButton("Continuar de todas formas", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            //Do nothing
                        }
                    })

                    .setIcon(R.drawable.ic_report_problem_black_24dp)
                    .show();
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

        if (id == R.id.nav_map) {

            mFragmentManager.beginTransaction().replace(R.id.containerView, new MainFragment()).commit();

        } else if (id == R.id.nav_settings) {

            mFragmentManager.beginTransaction().replace(R.id.containerView, new SettingsFragment()).commit();

        } else if (id == R.id.nav_share) {

            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_TEXT,
                    "Descarga la mejor app de ValenBisi en: https://github.com/systemallica/ValenBisi/releases");
            sendIntent.setType("text/plain");
            startActivity(sendIntent);

        } else if (id == R.id.nav_about) {

            mFragmentManager.beginTransaction().replace(R.id.containerView, new AboutFragment()).commit();

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

}
