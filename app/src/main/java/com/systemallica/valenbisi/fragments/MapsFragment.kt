package com.systemallica.valenbisi.fragments

import android.Manifest
import android.annotation.TargetApi
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.UiSettings
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.maps.android.clustering.ClusterManager
import com.google.maps.android.data.geojson.GeoJsonFeature
import com.google.maps.android.data.geojson.GeoJsonLayer
import com.google.maps.android.data.geojson.GeoJsonLineStringStyle
import com.google.maps.android.data.geojson.GeoJsonPoint
import com.google.maps.android.data.geojson.GeoJsonPointStyle
import com.systemallica.valenbisi.BikeStation
import com.systemallica.valenbisi.R
import com.systemallica.valenbisi.clustering.ClusterPoint
import com.systemallica.valenbisi.clustering.IconRenderer
import com.systemallica.valenbisi.services.TrackGPSService

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

import java.io.IOException
import java.util.Formatter
import java.util.GregorianCalendar
import java.util.HashMap

import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response

import android.support.v7.preference.PreferenceManager.getDefaultSharedPreferences
import kotlinx.android.synthetic.main.fragment_main.*

const val PREFS_NAME = "MyPrefsFile"
const val LOG_TAG = "Valenbisi error"

class MapsFragment : Fragment(), OnMapReadyCallback {
    private var settings: SharedPreferences? = null
    private var userSettings: SharedPreferences? = null
    private var settingsEditor: SharedPreferences.Editor? = null
    private var stations: GeoJsonLayer? = null
    private var lanes: GeoJsonLayer? = null
    private var parking: GeoJsonLayer? = null
    private var mMap: GoogleMap? = null
    private var mClusterManager: ClusterManager<ClusterPoint>? = null

    private val isApplicationReady: Boolean
        get() = isAdded && activity != null

    private val isLocationPermissionGranted: Boolean
        @TargetApi(23)
        get() = !isSdkHigherThanLollipop || activity!!.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    private val isSdkHigherThanLollipop: Boolean
        get() = Build.VERSION.SDK_INT >= 23

    private val isMapReady: Boolean
        get() = mMap != null

    private val currentLocation: LatLng?
        get() {
            val gps = TrackGPSService(context!!)

            return if (gps.canGetLocation()) {
                val longitude = gps.longitude
                val latitude = gps.latitude
                gps.stopUsingGPS()
                LatLng(latitude, longitude)
            } else {
                gps.stopUsingGPS()
                null
            }
        }

    private// Add warning that data may be unreliable
    val warningMessage: String
        get() = "\n\n" +
                this@MapsFragment.resources.getString(R.string.data_old) +
                "\n" +
                this@MapsFragment.resources.getString(R.string.data_unreliable)


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_main, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Change toolbar title
        activity!!.setTitle(R.string.nav_map)

        mapView.onCreate(savedInstanceState)
        mapView.onResume()
        mapView.getMapAsync(this)
    }

    override fun onMapReady(map: GoogleMap) {
        // Store map in member variable
        mMap = map
        onMapReadyHandler()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == 1) {
            if (isLocationPermissionGranted) {
                setLocationButtonEnabled(true)
            } else {
                setLocationButtonEnabled(false)
                Snackbar.make(mainView!!, R.string.no_location_permission, Snackbar.LENGTH_SHORT)
                    .show()
            }
        }
    }

    private fun onMapReadyHandler() {
        if (isApplicationReady) {

            initPreferences()

            initClusterManager()

            initMap()

            setInitialPosition()

            setButtonIconsAndListeners()

            restoreOptionalLayers()

            getStations()
        }
    }

    private fun initClusterManager() {
        // Load ClusterManager to the Map
        mClusterManager = ClusterManager(context, mMap)
        // Set custom renderer
        mClusterManager!!.renderer =
                IconRenderer(
                    context!!,
                    mMap!!,
                    mClusterManager!!
                )
    }

    private fun initPreferences() {
        settings = context!!.getSharedPreferences(PREFS_NAME, 0)
        settingsEditor = settings!!.edit()
        userSettings = getDefaultSharedPreferences(context)
    }

    private fun initMap() {
        mMap!!.setOnInfoWindowClickListener(mClusterManager)
        mMap!!.setOnCameraIdleListener(mClusterManager)
        mMap!!.setOnMarkerClickListener(mClusterManager)

        mMap!!.setInfoWindowAdapter(mClusterManager!!.markerManager)

        mMap!!.setMinZoomPreference(10f)

        setMapSettings()

        setMapBasemap()

        initLocationButton()
    }

    private fun setMapSettings() {
        val mapSettings: UiSettings = mMap!!.uiSettings
        mapSettings.isZoomControlsEnabled = true
        mapSettings.isCompassEnabled = false
        mapSettings.isRotateGesturesEnabled = false
    }

    private fun setMapBasemap() {
        val isSatellite = userSettings!!.getBoolean("isSatellite", false)
        if (!isSatellite) {
            mMap!!.mapType = GoogleMap.MAP_TYPE_NORMAL
        } else {
            mMap!!.mapType = GoogleMap.MAP_TYPE_HYBRID
        }
    }

    private fun initLocationButton() {
        if (isLocationPermissionGranted) {
            setLocationButtonEnabled(true)
        } else {
            requestPermissions(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                1
            )
        }
    }

    private fun setLocationButtonEnabled(mode: Boolean) {
        try {
            mMap!!.isMyLocationEnabled = mode
        } catch (e: SecurityException) {
            Log.e(LOG_TAG, e.message)
        }

    }

    private fun isValencia(location: LatLng): Boolean {
        return (location.latitude in 39.420..39.515) && (location.longitude in -0.572..-0.272)
    }

    private fun setInitialPosition() {

        val currentLocation = currentLocation
        val valencia = LatLng(39.479, -0.372)

        val initialZoom = userSettings!!.getBoolean("initialZoom", true)

        if (isLocationPermissionGranted && initialZoom && currentLocation != null) {
            if (isValencia(currentLocation)) {
                mMap!!.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 16.0f))
            } else {
                mMap!!.moveCamera(CameraUpdateFactory.newLatLngZoom(valencia, 13.0f))
            }
        } else {
            mMap!!.moveCamera(CameraUpdateFactory.newLatLngZoom(valencia, 13.0f))
        }
    }

    private fun restoreOptionalLayers() {
        val isDrawVoronoiCellsChecked = userSettings!!.getBoolean("voronoiCell", false)
        if (isDrawVoronoiCellsChecked) {
            drawBoronoiCells()
        }

        val isCarrilLayerAdded = settings!!.getBoolean("isCarrilLayerAdded", false)
        if (isCarrilLayerAdded) {
            GetLanes().execute()
        }

        val isDrawParkingSpotsChecked = settings!!.getBoolean("isParkingLayerAdded", false)
        if (isDrawParkingSpotsChecked) {
            GetParking().execute()
        }
    }

    private fun drawBoronoiCells() {
        try {
            val voronoi = GeoJsonLayer(mMap, R.raw.voronoi, context)
            for (feature in voronoi.features) {
                val stringStyle = voronoi.defaultLineStringStyle
                stringStyle.color = -16776961
                stringStyle.width = 2f
                feature.lineStringStyle = stringStyle
            }
            voronoi.addLayerToMap()
        } catch (e: JSONException) {
            Log.e(LOG_TAG, "JSONArray could not be created")
        } catch (e: IOException) {
            Log.e(LOG_TAG, "GeoJSON file could not be read")
        }

    }

    private fun getStations() {
        val client = OkHttpClient()
        val url =
            "https://api.jcdecaux.com/vls/v1/stations?contract=Valence&apiKey=adcac2d5b367dacef9846586d12df1bf7e8c7fcd"

        val request = Request.Builder()
            .url(url)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(LOG_TAG, "error with http call(no internet?)")
            }

            override fun onResponse(call: Call, response: Response?) {
                try {
                    handleApiResponse(response!!)
                } catch (e: IOException) {
                    Log.e(LOG_TAG, "error with http request")
                } finally {
                    response?.close()
                }
            }
        })
    }

    private fun resetStationsLayer() {
        val isClusteringActivated = userSettings!!.getBoolean("isClusteringActivated", true)

        if (isClusteringActivated) {
            mClusterManager!!.clearItems()
            mClusterManager!!.cluster()
        } else if (stations != null) {
            stations!!.removeLayerFromMap()
        }
    }

    @Throws(IOException::class)
    private fun handleApiResponse(response: Response) {
        if (!response.isSuccessful)
            throw IOException("Unexpected code $response")
        val responseBody = response.body()

        if (responseBody != null) {
            // Show loading message
            Snackbar.make(mainView!!, R.string.load_stations, Snackbar.LENGTH_LONG).show()
            addDataToMap(responseBody.string())
        } else {
            Log.e(LOG_TAG, "Empty server response")
            activity!!.runOnUiThread {
                // Show message if API response is empty
                Snackbar.make(mainView!!, R.string.no_data, Snackbar.LENGTH_LONG).show()
            }
        }
    }

    private fun addDataToMap(jsonData: String) {
        val showStationsLayer = settings!!.getBoolean("showStationsLayer", true)
        val isClusteringActivated = userSettings!!.getBoolean("isClusteringActivated", true)

        if (isApplicationReady && showStationsLayer) {
            try {
                val jsonDataArray = JSONArray(jsonData)

                if (isClusteringActivated) {
                    addPointsToCluster(jsonDataArray)
                    activity!!.runOnUiThread { mClusterManager!!.cluster() }
                } else {
                    addPointsToLayer(jsonDataArray)
                    activity!!.runOnUiThread { stations!!.addLayerToMap() }
                }
            } catch (e: JSONException) {
                Log.e(LOG_TAG, "JSONArray could not be created")
            }

        }
    }

    @Throws(JSONException::class)
    private fun addPointsToCluster(jsonDataArray: JSONArray) {
        val showOnlyFavoriteStations = userSettings!!.getBoolean("showFavorites", false)

        // Parse data from API
        for (i in 0 until jsonDataArray.length()) {
            // Get current station
            val jsonStation = jsonDataArray.getJSONObject(i)
            var station = BikeStation(jsonStation)
            station = generateCompleteStationData(station)

            // If station is not favourite and "Display only favourites" is enabled-> Do not add station
            if (showOnlyFavoriteStations && !station.isFavourite) {
                continue
            }

            val clusterPoint = ClusterPoint(station)
            mClusterManager!!.addItem(clusterPoint)
        }
    }

    @Throws(JSONException::class)
    private fun addPointsToLayer(jsonDataArray: JSONArray) {
        val showOnlyFavoriteStations = userSettings!!.getBoolean("showFavorites", false)

        val dummy = JSONObject()
        stations = GeoJsonLayer(mMap, dummy)

        // Parse data from API
        for (i in 0 until jsonDataArray.length()) {
            // Get current station
            val jsonStation = jsonDataArray.getJSONObject(i)
            var station = BikeStation(jsonStation)
            station = generateCompleteStationData(station)

            // If station is not favourite and "Display only favourites" is enabled-> Do not add station
            if (showOnlyFavoriteStations && !station.isFavourite) {
                continue
            }

            // Create Point
            val point = GeoJsonPoint(LatLng(station.lat!!, station.lng!!))
            // Add properties
            val properties = getStationProperties(station)
            // Create feature
            val pointFeature = GeoJsonFeature(point, "Origin", properties, null)

            val pointStyle = generatePointStyle(station)
            pointFeature.pointStyle = pointStyle

            stations!!.addFeature(pointFeature)
        }
    }

    private fun generateCompleteStationData(station: BikeStation): BikeStation {
        val showOnlyAvailableStations = userSettings!!.getBoolean("showAvailable", false)
        val isOnFoot = settings!!.getBoolean("isOnFoot", true)

        station.isFavourite = settings!!.getBoolean(station.address, false)

        if (station.status == "OPEN") {
            // Set markers colors depending on station availability
            station.icon = getMarkerIcon(isOnFoot, station.bikes, station.spots)

            // Get markers visibility depending on station availability
            if (showOnlyAvailableStations) {
                station.visibility = getMarkerVisibility(isOnFoot, station.bikes, station.spots)
            }

            station.snippet = getMarkerSnippet(station.bikes, station.spots, station.lastUpdate)

        } else {
            station.icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET)
            station.snippet = this@MapsFragment.resources.getString(R.string.closed)
            if (showOnlyAvailableStations) {
                station.visibility = false
            }
        }
        station.alpha = getMarkerAlpha(station.isFavourite)

        return station
    }

    private fun getStationProperties(station: BikeStation): HashMap<String, String> {
        val properties = HashMap<String, String>()

        properties["name"] = station.name
        properties["number"] = station.number
        properties["address"] = station.address
        properties["status"] = station.status
        properties["available_bike_stands"] = Integer.toString(station.spots)
        properties["available_bikes"] = Integer.toString(station.bikes)
        properties["last_updated"] = station.lastUpdate

        return properties
    }

    private fun generatePointStyle(station: BikeStation): GeoJsonPointStyle {
        val pointStyle = GeoJsonPointStyle()

        pointStyle.title = station.address
        pointStyle.snippet = station.snippet
        pointStyle.icon = station.icon
        pointStyle.alpha = station.alpha!!
        pointStyle.isVisible = station.visibility!!

        return pointStyle
    }

    private fun getMarkerIcon(isOnFoot: Boolean, bikes: Int, spots: Int): BitmapDescriptor {
        // Load default marker icons
        val iconGreen = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)
        val iconOrange = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)
        val iconYellow = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW)
        val iconRed = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)

        return if (isOnFoot) {
            when (bikes) {
                0 -> iconRed
                in 1..4 -> iconOrange
                in 5..9 -> iconYellow
                else -> iconGreen
            }
        } else {
            when (spots) {
                0 -> iconRed
                in 1..4 -> iconOrange
                in 5..9 -> iconYellow
                else -> iconGreen
            }
        }
    }

    private fun getMarkerVisibility(isOnFoot: Boolean, bikes: Int, spots: Int): Boolean {
        var visibility: Boolean? = true

        if (isOnFoot && bikes == 0) {
            visibility = false
        } else if (spots == 0) {
            visibility = false
        }

        return visibility!!
    }

    private fun getMarkerSnippet(bikes: Int, spots: Int, lastUpdate: String): String {
        val showLastUpdatedInfo = userSettings!!.getBoolean("lastUpdated", true)

        // Add number of available bikes/stands
        var snippet: String = this@MapsFragment.resources.getString(R.string.spots) + " " +
                spots + " - " +
                this@MapsFragment.resources.getString(R.string.bikes) + " " +
                bikes

        // Add last updated time if user has checked that option
        if (showLastUpdatedInfo) {
            snippet += getLastUpdatedInfo(lastUpdate)
        }

        // If data has not been updated for more than 1 hour
        if (millisecondsFrom(lastUpdate) > 3600000) {
            snippet += warningMessage
        }

        return snippet
    }

    private fun millisecondsFrom(event: String): Long {
        val eventTime = java.lang.Long.parseLong(event)
        val date = GregorianCalendar()
        val currentTime = date.timeInMillis

        return currentTime - eventTime
    }

    private fun getLastUpdatedInfo(lastUpdate: String): String {
        val snippet: String
        val apiTime = java.lang.Long.parseLong(lastUpdate)
        val date = GregorianCalendar()

        // Set API time
        date.timeInMillis = apiTime
        // Format time as HH:mm:ss
        val sbu = StringBuilder()
        val fmt = Formatter(sbu)
        fmt.format("%tT", date.time)
        // Add to pointStyle
        snippet = "\n" +
                this@MapsFragment.resources.getString(R.string.last_updated) + " " +
                sbu

        return snippet
    }

    private fun getMarkerAlpha(currentStationIsFav: Boolean): Float {
        // Apply full opacity only to favourite stations
        return when (currentStationIsFav) {
            true -> 1.0.toFloat()
            else -> 0.5.toFloat()
        }
    }

    private fun setButtonIconsAndListeners() {
        setInitialButtonState()
        addListeners()
    }

    private fun addListeners(){
        setOfflineListeners()
        setOnlineListeners()
    }

    private fun setInitialButtonState() {
        val stationsOn = ContextCompat.getDrawable(context!!, R.drawable.icon_map_marker)
        val stationsOff =
            ContextCompat.getDrawable(context!!, R.drawable.icon_map_marker_off)
        val bike = ContextCompat.getDrawable(context!!, R.drawable.icon_on_bike)
        val walk = ContextCompat.getDrawable(context!!, R.drawable.icon_walk)
        val bikeLanesOn = ContextCompat.getDrawable(context!!, R.drawable.icon_road)
        val bikeLanesOff = ContextCompat.getDrawable(context!!, R.drawable.icon_road_off)
        val parkingOn = ContextCompat.getDrawable(context!!, R.drawable.icon_parking)
        val parkingOff = ContextCompat.getDrawable(context!!, R.drawable.icon_parking_off)

        val showStationsLayer = settings!!.getBoolean("showStationsLayer", true)
        if (showStationsLayer) {
            btnStationsToggle!!.setCompoundDrawablesWithIntrinsicBounds(
                stationsOn,
                null,
                null,
                null
            )
        } else {
            btnStationsToggle!!.setCompoundDrawablesWithIntrinsicBounds(
                stationsOff,
                null,
                null,
                null
            )
        }

        val isCarrilLayerAdded = settings!!.getBoolean("isCarrilLayerAdded", true)
        if (isCarrilLayerAdded) {
            btnLanesToggle!!.setCompoundDrawablesWithIntrinsicBounds(
                bikeLanesOn,
                null,
                null,
                null
            )
        } else {
            btnLanesToggle!!.setCompoundDrawablesWithIntrinsicBounds(
                bikeLanesOff,
                null,
                null,
                null
            )
        }

        val isOnFoot = settings!!.getBoolean("isOnFoot", false)
        if (isOnFoot) {
            btnOnFootToggle!!.setCompoundDrawablesWithIntrinsicBounds(
                walk,
                null,
                null,
                null
            )
        } else {
            btnOnFootToggle!!.setCompoundDrawablesWithIntrinsicBounds(
                bike,
                null,
                null,
                null
            )
        }

        val isParkingLayerAdded = settings!!.getBoolean("isParkingLayerAdded", false)
        if (isParkingLayerAdded) {
            btnParkingToggle!!.setCompoundDrawablesWithIntrinsicBounds(
                parkingOn,
                null,
                null,
                null
            )
        } else {
            btnParkingToggle!!.setCompoundDrawablesWithIntrinsicBounds(
                parkingOff,
                null,
                null,
                null
            )
        }
    }

    private fun setOfflineListeners() {
        val bikeLanesOn = ContextCompat.getDrawable(context!!, R.drawable.icon_road)
        val bikeLanesOff = ContextCompat.getDrawable(context!!, R.drawable.icon_road_off)
        val parkingOn = ContextCompat.getDrawable(context!!, R.drawable.icon_parking)
        val parkingOff = ContextCompat.getDrawable(context!!, R.drawable.icon_parking_off)

        btnLanesToggle!!.setOnClickListener {
            if (!settings!!.getBoolean("isCarrilLayerAdded", false)) {
                btnLanesToggle!!.setCompoundDrawablesWithIntrinsicBounds(
                    bikeLanesOn,
                    null,
                    null,
                    null
                )
                GetLanes().execute()
            } else {
                btnLanesToggle!!.setCompoundDrawablesWithIntrinsicBounds(
                    bikeLanesOff,
                    null,
                    null,
                    null
                )
                settingsEditor!!.putBoolean("isCarrilLayerAdded", false).apply()
                if (lanes != null) {
                    lanes!!.removeLayerFromMap()
                }
            }
        }

        btnParkingToggle!!.setOnClickListener {
            if (!settings!!.getBoolean("isParkingLayerAdded", false)) {
                btnParkingToggle!!.setCompoundDrawablesWithIntrinsicBounds(
                    parkingOn,
                    null,
                    null,
                    null
                )
                GetParking().execute()
            } else {
                btnParkingToggle!!.setCompoundDrawablesWithIntrinsicBounds(
                    parkingOff,
                    null,
                    null,
                    null
                )
                settingsEditor!!.putBoolean("isParkingLayerAdded", false).apply()
                if (parking != null) {
                    parking!!.removeLayerFromMap()
                }
            }
        }
    }

    private fun setOnlineListeners() {
        val myDrawableBike = ContextCompat.getDrawable(context!!, R.drawable.icon_on_bike)
        val myDrawableWalk = ContextCompat.getDrawable(context!!, R.drawable.icon_walk)
        val myDrawableStationsOn = ContextCompat.getDrawable(context!!, R.drawable.icon_map_marker)
        val myDrawableStationsOff =
            ContextCompat.getDrawable(context!!, R.drawable.icon_map_marker_off)
        val isClusteringActivated = userSettings!!.getBoolean("isClusteringActivated", true)

        // Toggle Stations
        btnStationsToggle!!.setOnClickListener {
            val showStationsLayer = settings!!.getBoolean("showStationsLayer", true)
            if (showStationsLayer) {
                resetStationsLayer()
                btnStationsToggle!!.setCompoundDrawablesWithIntrinsicBounds(
                    myDrawableStationsOff,
                    null,
                    null,
                    null
                )
                settingsEditor!!.putBoolean("showStationsLayer", false).apply()
            } else {
                getStations()
                btnStationsToggle!!.setCompoundDrawablesWithIntrinsicBounds(
                    myDrawableStationsOn,
                    null,
                    null,
                    null
                )
                settingsEditor!!.putBoolean("showStationsLayer", true).apply()
            }
        }

        // Toggle onFoot/onBike
        btnOnFootToggle!!.setOnClickListener {
            val isOnFoot = settings!!.getBoolean("isOnFoot", false)
            resetStationsLayer()
            if (isOnFoot) {
                settingsEditor!!.putBoolean("isOnFoot", false).apply()
                btnOnFootToggle!!.setCompoundDrawablesWithIntrinsicBounds(
                    myDrawableBike,
                    null,
                    null,
                    null
                )
                getStations()
            } else {
                settingsEditor!!.putBoolean("isOnFoot", true).apply()
                btnOnFootToggle!!.setCompoundDrawablesWithIntrinsicBounds(
                    myDrawableWalk,
                    null,
                    null,
                    null
                )
                getStations()
            }
        }

        // Reload data
        btnRefresh!!.setOnClickListener {
            resetStationsLayer()
            getStations()
        }

        mClusterManager!!.setOnClusterClickListener { cluster ->
            val zoom = mMap!!.cameraPosition.zoom
            val position = cluster.position
            mMap!!.animateCamera(
                CameraUpdateFactory.newLatLngZoom(position, zoom + 1.0.toFloat()),
                250,
                null
            )
            true
        }

        if (isClusteringActivated) {
            setClusteredInfoWindow()
        } else {
            setNormalInfoWindow()
        }
    }

    private fun removeListeners(){
        btnLanesToggle.setOnClickListener(null)
        btnParkingToggle.setOnClickListener(null)
        btnStationsToggle.setOnClickListener(null)
        btnOnFootToggle.setOnClickListener(null)
        btnRefresh.setOnClickListener(null)
        mClusterManager!!.setOnClusterClickListener(null)
        mMap!!.setOnInfoWindowClickListener(null)
        mClusterManager!!.setOnClusterItemInfoWindowClickListener(null)
    }

    private fun setNormalInfoWindow() {
        mMap!!.setInfoWindowAdapter(object : GoogleMap.InfoWindowAdapter {
            // Use default InfoWindow frame
            override fun getInfoWindow(marker: Marker): View? {
                return null
            }

            // Defines the contents of the InfoWindow
            override fun getInfoContents(marker: Marker): View {
                return getInfoWindowCommonInfo(marker)
            }
        })

        mMap!!.setOnInfoWindowClickListener { clickedMarker ->
            val currentStationIsFav = settings!!.getBoolean(clickedMarker.title, false)
            val showFavorites = userSettings!!.getBoolean("showFavorites", false)

            if (currentStationIsFav) {
                clickedMarker.alpha = 0.5.toFloat()
                if (showFavorites) {
                    clickedMarker.isVisible = false
                }
                settingsEditor!!.putBoolean(clickedMarker.title, false).apply()
            } else {
                clickedMarker.alpha = 1f
                settingsEditor!!.putBoolean(clickedMarker.title, true).apply()
            }
            clickedMarker.showInfoWindow()
        }
    }

    private fun setClusteredInfoWindow() {
        mClusterManager!!.markerCollection.setOnInfoWindowAdapter(object :
            GoogleMap.InfoWindowAdapter {
            // Use default InfoWindow frame
            override fun getInfoWindow(marker: Marker): View? {
                return null
            }

            // Defines the contents of the InfoWindow
            override fun getInfoContents(marker: Marker): View {
                // Getting view from the layout file info_window_layout
                val popup = getInfoWindowCommonInfo(marker)

                mClusterManager!!.setOnClusterItemInfoWindowClickListener { item ->
                    val currentStationIsFav = settings!!.getBoolean(item.title, false)
                    val showFavorites = userSettings!!.getBoolean("showFavorites", false)

                    if (currentStationIsFav) {
                        item.alpha = 0.5.toFloat()
                        marker.alpha = 0.5.toFloat()
                        if (showFavorites) {
                            item.visibility = false
                        }
                        settingsEditor!!.putBoolean(item.title, false).apply()
                    } else {
                        item.alpha = 1.0.toFloat()
                        marker.alpha = 1.0.toFloat()
                        settingsEditor!!.putBoolean(item.title, true).apply()
                    }
                    marker.showInfoWindow()
                    mClusterManager!!.cluster()
                }
                return popup
            }
        })
    }

    private fun getInfoWindowCommonInfo(marker: Marker): View {
        val myDrawableFavOff = ContextCompat.getDrawable(context!!, R.drawable.icon_star_outline)
        val myDrawableFavOn = ContextCompat.getDrawable(context!!, R.drawable.icon_star)

        // Getting view from the layout file info_window_layout
        val popup = activity!!.layoutInflater.inflate(R.layout.marker_popup, null)

        // Getting reference to the ImageView/title/snippet
        val title = popup.findViewById<TextView>(R.id.title)
        val snippet = popup.findViewById<TextView>(R.id.snippet)
        val btnStar = popup.findViewById<ImageView>(R.id.btn_star)

        if (marker.snippet.contains("\n\n")) {
            snippet.setTextColor(ContextCompat.getColor(context!!, R.color.red))
            snippet.setTypeface(null, Typeface.BOLD)
            snippet.text = marker.snippet
        } else {
            snippet.text = marker.snippet
        }
        title.text = marker.title

        // Checking if current station is favorite
        val currentStationIsFav = settings!!.getBoolean(marker.title, false)

        // Setting correspondent icon
        if (currentStationIsFav) {
            btnStar.setImageDrawable(myDrawableFavOn)
        } else {
            btnStar.setImageDrawable(myDrawableFavOff)
        }
        return popup
    }

    private inner class GetLanes : AsyncTask<Void, Void, GeoJsonLayer>() {

        override fun onPreExecute() {
            Snackbar.make(mainView!!, R.string.load_lanes, Snackbar.LENGTH_LONG).show()
        }

        override fun doInBackground(vararg params: Void): GeoJsonLayer {
            try {
                lanes = GeoJsonLayer(mMap, R.raw.bike_lanes_0618, context)
                for (feature in lanes!!.features) {
                    val stringStyle = GeoJsonLineStringStyle()
                    stringStyle.width = 5f
                    if (userSettings!!.getBoolean("cicloCalles", true)) {
                        stringStyle.color = getLaneColor(feature)
                    }
                    feature.lineStringStyle = stringStyle
                }
            } catch (e: IOException) {
                Log.e(LOG_TAG, "GeoJSON file could not be read")
            } catch (e: JSONException) {
                Log.e(LOG_TAG, "GeoJSON file could not be converted to a JSONObject")
            }

            return lanes!!
        }

        private fun getLaneColor(feature: GeoJsonFeature): Int {
            return when (feature.getProperty("estado")) {
                // Normal bike lane
                "1" -> Color.BLACK
                // Ciclocalle
                "2" -> Color.BLUE
                // Weird fragments
                "3" -> Color.BLUE
                // Rio
                "4" -> Color.BLACK
                else -> Color.RED
            }
        }

        override fun onPostExecute(lanes: GeoJsonLayer?) {
            lanes!!.addLayerToMap()
            settingsEditor!!.putBoolean("isCarrilLayerAdded", true).apply()
        }
    }

    private inner class GetParking : AsyncTask<Void, Void, GeoJsonLayer>() {
        override fun onPreExecute() {
            Snackbar.make(mainView!!, R.string.load_parking, Snackbar.LENGTH_SHORT).show()
        }

        override fun doInBackground(vararg params: Void): GeoJsonLayer {
            val showFavorites = userSettings!!.getBoolean("showFavorites", false)
            var bitmap = BitmapFactory.decodeResource(resources, R.drawable.ic_map_marker_circle)
            bitmap = Bitmap.createScaledBitmap(bitmap, 50, 50, false)
            val iconParking = BitmapDescriptorFactory.fromBitmap(bitmap)

            if (isApplicationReady) {
                try {
                    parking = GeoJsonLayer(mMap, R.raw.aparcabicis, context)
                    for (feature in parking!!.features) {
                        val pointStyle = GeoJsonPointStyle()
                        pointStyle.title = this@MapsFragment.resources.getString(R.string.parking) +
                                " " + feature.getProperty("id")
                        pointStyle.snippet = this@MapsFragment.resources.getString(R.string.plazas) +
                                " " + feature.getProperty("plazas")
                        pointStyle.alpha = 0.5.toFloat()
                        pointStyle.icon = iconParking

                        val currentStationIsFav = settings!!.getBoolean(pointStyle.title, false)

                        // Apply full opacity to fav stations
                        if (currentStationIsFav) {
                            pointStyle.alpha = 1f
                        }

                        // If favorites are selected, hide the rest
                        if (showFavorites) {
                            if (!currentStationIsFav) {
                                pointStyle.isVisible = false
                            }
                        }
                        feature.pointStyle = pointStyle
                    }
                } catch (e: IOException) {
                    Log.e(LOG_TAG, "GeoJSON file could not be read")
                } catch (e: JSONException) {
                    Log.e(LOG_TAG, "GeoJSON file could not be converted to a JSONObject")
                }

            }
            return parking!!
        }

        override fun onPostExecute(parking: GeoJsonLayer?) {
            parking!!.addLayerToMap()
            settingsEditor!!.putBoolean("isParkingLayerAdded", true).apply()
        }

    }

    override fun onPause() {
        super.onPause()
        if (isApplicationReady && isMapReady) {
            removeListeners()
            if(isLocationPermissionGranted) {
                // Disable location to avoid battery drain
                setLocationButtonEnabled(false)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (isApplicationReady && isMapReady) {
            addListeners()
            if(isLocationPermissionGranted) {
                // Disable location to avoid battery drain
                setLocationButtonEnabled(false)
            }
        }
    }
}
