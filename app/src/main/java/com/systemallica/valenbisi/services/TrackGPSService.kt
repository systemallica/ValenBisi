package com.systemallica.valenbisi.services

import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.IBinder
import android.util.Log

const val MIN_DISTANCE_CHANGE_FOR_UPDATES: Long = 10
const val MIN_TIME_BW_UPDATES = (1000 * 60).toLong()

class TrackGPSService(mContext: Context) : Service(), LocationListener {

    private val mContext: Context?
    private var checkGPS = false
    private var checkNetwork = false
    private var canGetLocation = false
    private var loc: Location? = null
    private var locationManager: LocationManager? = null
    var latitude: Double = 0.toDouble()
    var longitude: Double = 0.toDouble()

    init {
        this.mContext = mContext
        getLocation()
    }

    private fun getLocation() {

        try {
            locationManager = mContext!!
                    .getSystemService(Context.LOCATION_SERVICE) as LocationManager

            // getting GPS status
            if (locationManager != null) {
                checkGPS = locationManager!!
                        .isProviderEnabled(LocationManager.GPS_PROVIDER)

                // getting network status
                checkNetwork = locationManager!!
                        .isProviderEnabled(LocationManager.NETWORK_PROVIDER)


                if (checkGPS || checkNetwork) {
                    this.canGetLocation = true
                    // First get location from Network Provider
                    if (checkNetwork) {

                        try {
                            locationManager!!.requestLocationUpdates(
                                    LocationManager.NETWORK_PROVIDER,
                                    MIN_TIME_BW_UPDATES,
                                    MIN_DISTANCE_CHANGE_FOR_UPDATES.toFloat(), this)
                            Log.d("Network", "Network")
                            if (locationManager != null) {
                                loc = locationManager!!
                                        .getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

                            }

                            if (loc != null) {
                                latitude = loc!!.latitude
                                longitude = loc!!.longitude
                            }
                        } catch (e: SecurityException) {
                            e.printStackTrace()
                        }

                    }
                }

                // if GPS Enabled get lat/long using GPS Services
                if (checkGPS) {
                    //Toast.makeText(mContext,"GPS",Toast.LENGTH_SHORT).show();
                    if (loc == null) {
                        try {
                            locationManager!!.requestLocationUpdates(
                                    LocationManager.GPS_PROVIDER,
                                    MIN_TIME_BW_UPDATES,
                                    MIN_DISTANCE_CHANGE_FOR_UPDATES.toFloat(), this)
                            Log.d("GPS Enabled", "GPS Enabled")
                            if (locationManager != null) {
                                loc = locationManager!!
                                        .getLastKnownLocation(LocationManager.GPS_PROVIDER)
                                if (loc != null) {
                                    latitude = loc!!.latitude
                                    longitude = loc!!.longitude
                                }
                            }
                        } catch (e: SecurityException) {
                            e.printStackTrace()
                        }

                    }
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    fun canGetLocation(): Boolean {
        return this.canGetLocation
    }

    fun stopUsingGPS() {
        if (locationManager != null) {

            locationManager!!.removeUpdates(this@TrackGPSService)

        }
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onLocationChanged(location: Location) {

    }

    override fun onStatusChanged(s: String, i: Int, bundle: Bundle) {

    }

    override fun onProviderEnabled(s: String) {

    }

    override fun onProviderDisabled(s: String) {

    }
}