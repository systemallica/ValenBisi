package com.systemallica.valenbisi.fragments

import android.Manifest
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager.getDefaultSharedPreferences
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.UiSettings
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.material.snackbar.Snackbar
import com.google.maps.android.clustering.ClusterManager
import com.google.maps.android.data.geojson.*
import com.systemallica.valenbisi.BikeStation
import com.systemallica.valenbisi.R
import com.systemallica.valenbisi.clustering.ClusterPoint
import com.systemallica.valenbisi.clustering.IconRenderer
import kotlinx.android.synthetic.main.fragment_main.*
import kotlinx.coroutines.*
import okhttp3.*
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.util.*
import kotlin.coroutines.CoroutineContext

const val PREFS_NAME = "MyPrefsFile"
const val LOG_TAG = "Valenbisi error"

class MapsFragment : Fragment(), OnMapReadyCallback, CoroutineScope {

    private var job: Job = Job()

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    private var settings: SharedPreferences? = null
    private var userSettings: SharedPreferences? = null
    private var settingsEditor: SharedPreferences.Editor? = null
    private var stations: GeoJsonLayer? = null
    private var lanes: GeoJsonLayer? = null
    private var parking: GeoJsonLayer? = null
    private var mMap: GoogleMap? = null
    private var mClusterManager: ClusterManager<ClusterPoint>? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val isApplicationReady: Boolean
        get() = isAdded && activity != null

    private val isLocationPermissionGranted: Boolean
        get() = activity?.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    private val isMapReady: Boolean
        get() = mMap != null

    private// Add warning that data may be unreliable
    val warningMessage: String
        get() = "\n\n" +
                getString(R.string.data_old) +
                "\n" +
                getString(R.string.data_unreliable)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_main, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
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
                safeSnackBar(R.string.no_location_permission)

            }
        }
    }

    private fun onMapReadyHandler() {
        if (isApplicationReady) {

            initPreferences()

            initClusterManager()

            initMap()

            setInitialPosition()

            setInitialButtonState()

            setButtonBackground()

            setButtonListeners()

            restoreOptionalLayers()
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
        mapSettings.isScrollGesturesEnabledDuringRotateOrZoom = false
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
            Log.e(LOG_TAG, e.message!!)
        }
    }

    private fun isValenciaArea(location: LatLng): Boolean {
        return (location.latitude in 39.420..39.515) && (location.longitude in -0.572..-0.272)
    }

    private fun setInitialPosition() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context!!)
        if (isLocationPermissionGranted) {
            try {
                fusedLocationClient.lastLocation
                    .addOnSuccessListener { location: Location? ->
                        // Got last known location. In some rare situations this can be null.
                        if (location != null) {
                            val longitude = location.longitude
                            val latitude = location.latitude
                            val currentLocation = LatLng(latitude, longitude)
                            moveToLocationOrValencia(currentLocation)
                        } else {
                            moveToLocationOrValencia()
                        }
                    }
                    .addOnFailureListener {
                        moveToLocationOrValencia()
                    }
            } catch (e: SecurityException) {
                Log.e(LOG_TAG, e.message!!)
            }
        } else {
            moveToLocationOrValencia()
        }
    }

    private fun moveToLocationOrValencia(currentLocation: LatLng = LatLng(39.479, -0.372)) {
        val initialZoom = userSettings!!.getBoolean("initialZoom", true)

        if (isLocationPermissionGranted && initialZoom && isValenciaArea(currentLocation)) {
            mMap!!.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 16.0f))
        } else {
            mMap!!.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(39.479, -0.372), 13.0f))
        }
    }

    private fun restoreOptionalLayers() {
        val isStationsLayerAdded = settings!!.getBoolean("showStationsLayer", true)
        if(isStationsLayerAdded){
            getStations()
        }

        val isDrawVoronoiCellsChecked = userSettings!!.getBoolean("voronoiCell", false)
        if (isDrawVoronoiCellsChecked) {
            drawBoronoiCells()
        }

        val isCarrilLayerAdded = settings!!.getBoolean("isCarrilLayerAdded", false)
        if (isCarrilLayerAdded) {
            getLanes()
        }

        val isDrawParkingSpotsChecked = settings!!.getBoolean("isParkingLayerAdded", false)
        if (isDrawParkingSpotsChecked) {
            getParkings()
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
        // Show loading message
        safeSnackBar(R.string.load_stations)

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

            override fun onResponse(call: Call, response: Response) {
                try {
                    handleApiResponse(response)
                } catch (e: IOException) {
                    Log.e(LOG_TAG, "error with http request")
                } finally {
                    response.close()
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
        val responseBody = response.body

        if (responseBody != null) {
            addDataToMap(responseBody.string())
        } else {
            Log.e(LOG_TAG, "Empty server response")
            activity!!.runOnUiThread {
                // Show message if API response is empty
                safeSnackBar(R.string.no_data)
            }
        }
    }

    private fun addDataToMap(jsonData: String) {
        val isClusteringActivated = userSettings!!.getBoolean("isClusteringActivated", true)

        if (isApplicationReady) {
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
            station.snippet = getString(R.string.closed)
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
        properties["available_bike_stands"] = station.spots.toString()
        properties["available_bikes"] = station.bikes.toString()
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
        if (isApplicationReady) {
            val showLastUpdatedInfo = userSettings!!.getBoolean("lastUpdated", true)

            // Add number of available bikes/stands
            var snippet: String = getString(R.string.spots) + " " +
                    spots + " - " +
                    getString(R.string.bikes) + " " +
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

        return ""
    }

    private fun millisecondsFrom(event: String): Long {
        val eventTime = java.lang.Long.parseLong(event)
        val date = GregorianCalendar()
        val currentTime = date.timeInMillis

        return currentTime - eventTime
    }

    private fun getLastUpdatedInfo(lastUpdate: String): String {
        if (isApplicationReady) {
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
                    getString(R.string.last_updated) + " " +
                    sbu

            return snippet
        }

        return ""
    }

    private fun getMarkerAlpha(currentStationIsFav: Boolean): Float {
        // Apply full opacity only to favourite stations
        return when (currentStationIsFav) {
            true -> 1.0.toFloat()
            else -> 0.5.toFloat()
        }
    }

    private fun setButtonListeners() {
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
            btnStationsToggle!!.icon = stationsOn
        } else {
            btnStationsToggle!!.icon = stationsOff
        }

        val isCarrilLayerAdded = settings!!.getBoolean("isCarrilLayerAdded", false)
        if (isCarrilLayerAdded) {
            btnLanesToggle!!.icon = bikeLanesOn
        } else {
            btnLanesToggle!!.icon = bikeLanesOff
        }

        val isOnFoot = settings!!.getBoolean("isOnFoot", false)
        if (isOnFoot) {
            btnOnFootToggle!!.icon = walk
        } else {
            btnOnFootToggle!!.icon = bike
        }

        val isParkingLayerAdded = settings!!.getBoolean("isParkingLayerAdded", false)
        if (isParkingLayerAdded) {
            btnParkingToggle!!.icon = parkingOn
        } else {
            btnParkingToggle!!.icon = parkingOff
        }
    }

    private fun setButtonBackground(){
        btnLanesToggle.background = ContextCompat.getDrawable(context!!, R.drawable.mapbutton_background)
        btnStationsToggle.background = ContextCompat.getDrawable(context!!, R.drawable.mapbutton_background)
        btnOnFootToggle.background = ContextCompat.getDrawable(context!!, R.drawable.mapbutton_background)
        btnRefresh.background = ContextCompat.getDrawable(context!!, R.drawable.mapbutton_background)
        btnParkingToggle.background = ContextCompat.getDrawable(context!!, R.drawable.mapbutton_background)
    }

    private fun setOfflineListeners() {
        val bikeLanesOn = ContextCompat.getDrawable(context!!, R.drawable.icon_road)
        val bikeLanesOff = ContextCompat.getDrawable(context!!, R.drawable.icon_road_off)
        val parkingOn = ContextCompat.getDrawable(context!!, R.drawable.icon_parking)
        val parkingOff = ContextCompat.getDrawable(context!!, R.drawable.icon_parking_off)

        btnLanesToggle!!.setOnClickListener {
            if (!settings!!.getBoolean("isCarrilLayerAdded", false)) {
                btnLanesToggle!!.icon = bikeLanesOn
                getLanes()
            } else {
                btnLanesToggle!!.icon = bikeLanesOff
                settingsEditor!!.putBoolean("isCarrilLayerAdded", false).apply()
                if (lanes != null) {
                    lanes!!.removeLayerFromMap()
                }
            }
        }

        btnParkingToggle!!.setOnClickListener {
            if (!settings!!.getBoolean("isParkingLayerAdded", false)) {
                btnParkingToggle!!.icon = parkingOn
                getParkings()
            } else {
                btnParkingToggle!!.icon = parkingOff
                settingsEditor!!.putBoolean("isParkingLayerAdded", false).apply()
                if (parking != null) {
                    parking!!.removeLayerFromMap()
                }
            }
        }
    }

    private fun setOnlineListeners() {
        val bike = ContextCompat.getDrawable(context!!, R.drawable.icon_on_bike)
        val walk = ContextCompat.getDrawable(context!!, R.drawable.icon_walk)
        val stationsOn = ContextCompat.getDrawable(context!!, R.drawable.icon_map_marker)
        val stationsOff =
            ContextCompat.getDrawable(context!!, R.drawable.icon_map_marker_off)
        val isClusteringActivated = userSettings!!.getBoolean("isClusteringActivated", true)

        // Toggle Stations
        btnStationsToggle!!.setOnClickListener {
            val showStationsLayer = settings!!.getBoolean("showStationsLayer", true)
            if (showStationsLayer) {
                resetStationsLayer()
                btnStationsToggle!!.icon = stationsOff
                settingsEditor!!.putBoolean("showStationsLayer", false).apply()
            } else {
                getStations()
                btnStationsToggle!!.icon = stationsOn
                settingsEditor!!.putBoolean("showStationsLayer", true).apply()
            }
        }

        // Toggle onFoot/onBike
        btnOnFootToggle!!.setOnClickListener {
            val isOnFoot = settings!!.getBoolean("isOnFoot", false)
            resetStationsLayer()
            if (isOnFoot) {
                settingsEditor!!.putBoolean("isOnFoot", false).apply()
                btnOnFootToggle!!.icon = bike
                getStations()
            } else {
                settingsEditor!!.putBoolean("isOnFoot", true).apply()
                btnOnFootToggle!!.icon = walk
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
        mClusterManager!!.markerCollection.setInfoWindowAdapter(object :
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

    private fun safeSnackBar(stringReference: Int) {
        mainView?.let {
            val snack = Snackbar.make(mainView, stringReference, Snackbar.LENGTH_LONG)

            // Necessary to display snackbar over bottom navigation bar
            val params = snack.view.layoutParams as CoordinatorLayout.LayoutParams
            params.anchorId = R.id.bottom_navigation_view
            params.anchorGravity = Gravity.TOP
            params.gravity = Gravity.TOP
            snack.view.layoutParams = params
            snack.show()
        }
    }

    private fun getLanes(){
        safeSnackBar(R.string.load_lanes)

        launch {
            getLanesAsync()
            // Has to run on the main thread
            lanes!!.addLayerToMap()
            settingsEditor!!.putBoolean("isCarrilLayerAdded", true).apply()
        }
    }

    private suspend fun getLanesAsync(){

        try {
            withContext(Dispatchers.IO) {
                lanes = GeoJsonLayer(mMap, R.raw.bike_lanes, context)
                for (feature in lanes!!.features) {
                    val stringStyle = GeoJsonLineStringStyle()
                    stringStyle.width = 5f
                    if (userSettings!!.getBoolean("cicloCalles", true)) {
                        stringStyle.color = getLaneColor(feature)
                    }
                    feature.lineStringStyle = stringStyle
                }
            }


        } catch (e: IOException) {
            Log.e(LOG_TAG, "GeoJSON file could not be read")

        } catch (e: JSONException) {
            Log.e(LOG_TAG, "GeoJSON file could not be converted to a JSONObject")
        }
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

    private fun getParkings(){
        safeSnackBar(R.string.load_parking)

        launch {
            getParkingsAsync()
            // Has to run on the main thread
            parking!!.addLayerToMap()
            settingsEditor!!.putBoolean("isParkingLayerAdded", true).apply()
        }
    }

    private suspend fun getParkingsAsync(){

        val showFavorites = userSettings!!.getBoolean("showFavorites", false)
        var bitmap = BitmapFactory.decodeResource(resources, R.drawable.ic_map_marker_circle)
        bitmap = Bitmap.createScaledBitmap(bitmap, 50, 50, false)
        val iconParking = BitmapDescriptorFactory.fromBitmap(bitmap)

        if (isApplicationReady) {
            try {
                withContext(Dispatchers.IO) {
                    parking = GeoJsonLayer(mMap, R.raw.aparcabicis, context)
                    for (feature in parking!!.features) {
                        val pointStyle = GeoJsonPointStyle()
                        pointStyle.title = getString(R.string.parking) +
                                " " + feature.getProperty("id")
                        pointStyle.snippet = getString(R.string.plazas) +
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
                }
            } catch (e: IOException) {
                Log.e(LOG_TAG, "GeoJSON file could not be read")
            } catch (e: JSONException) {
                Log.e(LOG_TAG, "GeoJSON file could not be converted to a JSONObject")
            }
        }
    }

    override fun onPause() {
        super.onPause()
        if (isApplicationReady && isMapReady) {
            // Disable location to avoid battery drain
            setLocationButtonEnabled(false)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isApplicationReady && isMapReady) {
            // Disable location to avoid battery drain
            setLocationButtonEnabled(false)
        }
    }

    override fun onResume() {
        super.onResume()
        if (isApplicationReady && isMapReady && isLocationPermissionGranted) {
            // Re-enable location
            setLocationButtonEnabled(true)
        }
    }
}
