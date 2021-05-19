package com.systemallica.valenbisi.fragments

import android.Manifest
import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.content.res.Resources
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
import androidx.activity.result.contract.ActivityResultContracts
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
import com.google.android.gms.maps.model.*
import com.google.android.material.snackbar.Snackbar
import com.google.maps.android.clustering.ClusterManager
import com.google.maps.android.data.geojson.*
import com.systemallica.valenbisi.BikeStation
import com.systemallica.valenbisi.R
import com.systemallica.valenbisi.clustering.ClusterPoint
import com.systemallica.valenbisi.clustering.IconRenderer
import com.systemallica.valenbisi.databinding.FragmentMainBinding
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
    private var _binding: FragmentMainBinding? = null
    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!


    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    private lateinit var settings: SharedPreferences
    private lateinit var userSettings: SharedPreferences
    private lateinit var mClusterManager: ClusterManager<ClusterPoint>
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var stations: GeoJsonLayer? = null
    private var lanes: GeoJsonLayer? = null
    private var parking: GeoJsonLayer? = null
    private var mMap: GoogleMap? = null

    private val isApplicationReady: Boolean
        get() = isAdded && activity != null

    private val isLocationPermissionGranted: Boolean
        get() = activity?.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    // Add warning that data may be unreliable
    private val warningMessage: String
        get() = "\n\n" +
                getString(R.string.data_old) +
                "\n" +
                getString(R.string.data_unreliable)

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!isLocationPermissionGranted) {
            requestLocationPermission()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.mapView.onCreate(savedInstanceState)
        binding.mapView.onResume()
        binding.mapView.getMapAsync(this)
    }

    override fun onMapReady(map: GoogleMap) {
        // Store map in member variable
        mMap = map
        onMapReadyHandler()
    }

    private fun onMapReadyHandler() {
        if (isApplicationReady) {

            initPreferences()

            initMap()

            val isClusteringActivated = userSettings.getBoolean("isClusteringActivated", true)
            if (isClusteringActivated) {
                initClusterManager()
            }

            setInitialPosition()

            setInitialButtonState()

            setButtonBackground()

            restoreOptionalLayers()
        }
    }

    private fun initClusterManager() {
        // Load ClusterManager to the Map
        mClusterManager = ClusterManager(context, mMap)
        // Set custom renderer
        mClusterManager.renderer =
                IconRenderer(
                        requireContext(),
                        mMap!!,
                        mClusterManager
                )
    }

    private fun initPreferences() {
        // Settings from the map buttons
        settings = requireContext().getSharedPreferences(PREFS_NAME, 0)
        // Settings from the menu
        userSettings = getDefaultSharedPreferences(context)
    }

    private fun initMap() {

        mMap!!.setMinZoomPreference(10f)

        setMapSettings()

        setMapBasemap()

        setMapTheme()

        initLocationButton()
    }

    private fun setMapTheme(){
        when (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
            Configuration.UI_MODE_NIGHT_NO -> {
                // Night mode is not active, we're using the light theme
                try {
                    mMap!!.setMapStyle(MapStyleOptions.loadRawResourceStyle(requireContext(), R.raw.light_style))
                } catch (e: Resources.NotFoundException) {
                    Log.e("Valenbisi", "Error parsing light map style", e)
                }
            }
            Configuration.UI_MODE_NIGHT_YES -> {
                // Night mode is active, we're using dark theme
                try {
                    mMap!!.setMapStyle(MapStyleOptions.loadRawResourceStyle(requireContext(), R.raw.dark_style))
                } catch (e: Resources.NotFoundException) {
                    Log.e("Valenbisi", "Error parsing dark map style", e)
                }
            }
        }
    }

    private fun setMapSettings() {
        val mapSettings: UiSettings = mMap!!.uiSettings
        mapSettings.isZoomControlsEnabled = true
        mapSettings.isCompassEnabled = false
        mapSettings.isRotateGesturesEnabled = false
        mapSettings.isScrollGesturesEnabledDuringRotateOrZoom = false
    }

    private fun setMapBasemap() {
        val isSatellite = userSettings.getBoolean("isSatellite", false)
        if (!isSatellite) {
            mMap!!.mapType = GoogleMap.MAP_TYPE_NORMAL
        } else {
            mMap!!.mapType = GoogleMap.MAP_TYPE_HYBRID
        }
    }

    private fun requestLocationPermission(){
        val requestPermissionLauncher =
            registerForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { isGranted: Boolean ->
                if (isGranted) {
                    setLocationButtonEnabled(true)
                } else {
                    setLocationButtonEnabled(false)
                    safeSnackBar(R.string.no_location_permission)
                }
            }

        requestPermissionLauncher.launch(
            Manifest.permission.ACCESS_FINE_LOCATION)
    }

    private fun initLocationButton() {
        if (isLocationPermissionGranted) {
            setLocationButtonEnabled(true)
        } else {
            setLocationButtonEnabled(false)
            safeSnackBar(R.string.no_location_permission)
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
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())
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
        val initialZoom = userSettings.getBoolean("initialZoom", true)

        if (isLocationPermissionGranted && initialZoom && isValenciaArea(currentLocation)) {
            mMap!!.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 16.0f))
        } else {
            mMap!!.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(39.479, -0.372), 13.0f))
        }
    }

    private fun restoreOptionalLayers() {
        val isStationsLayerAdded = settings.getBoolean("showStationsLayer", true)
        if (isStationsLayerAdded) {
            getStations()
        }

        val isDrawVoronoiCellsChecked = userSettings.getBoolean("voronoiCell", false)
        if (isDrawVoronoiCellsChecked) {
            drawVoronoiCells()
        }

        val isCarrilLayerAdded = settings.getBoolean("isCarrilLayerAdded", false)
        if (isCarrilLayerAdded) {
            getLanes()
        }

        val isDrawParkingSpotsChecked = settings.getBoolean("isParkingLayerAdded", false)
        if (isDrawParkingSpotsChecked) {
            getParkings()
        }

        if (!isStationsLayerAdded && !isCarrilLayerAdded && !isDrawParkingSpotsChecked) {
            setListeners()
        }
    }

    private fun drawVoronoiCells() {
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
        val isClusteringActivated = userSettings.getBoolean("isClusteringActivated", true)

        if (isClusteringActivated) {
            mClusterManager.clearItems()
            mClusterManager.cluster()
        } else {
            stations?.removeLayerFromMap()
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
            requireActivity().runOnUiThread {
                // Show message if API response is empty
                safeSnackBar(R.string.no_data)
            }
        }
    }

    private fun addDataToMap(jsonData: String) {
        val isClusteringActivated = userSettings.getBoolean("isClusteringActivated", true)

        if (isApplicationReady) {
            try {
                val jsonDataArray = JSONArray(jsonData)

                if (isClusteringActivated) {
                    addPointsToCluster(jsonDataArray)
                    requireActivity().runOnUiThread { mClusterManager.cluster() }
                } else {
                    addPointsToLayer(jsonDataArray)
                    requireActivity().runOnUiThread { stations?.addLayerToMap() }
                }
                settings.edit().putBoolean("showStationsLayer", true).apply()
                setListeners()
            } catch (e: JSONException) {
                Log.e(LOG_TAG, "JSONArray could not be created")
            }

        }
    }

    @Throws(JSONException::class)
    private fun addPointsToCluster(jsonDataArray: JSONArray) {
        val showOnlyFavoriteStations = userSettings.getBoolean("showFavorites", false)

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
            mClusterManager.addItem(clusterPoint)
        }
    }

    @Throws(JSONException::class)
    private fun addPointsToLayer(jsonDataArray: JSONArray) {
        val showOnlyFavoriteStations = userSettings.getBoolean("showFavorites", false)

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
            val point = GeoJsonPoint(LatLng(station.lat, station.lng))
            // Add properties
            val properties = getStationProperties(station)
            // Create feature
            val pointFeature = GeoJsonFeature(point, "Origin", properties, null)

            val pointStyle = generatePointStyle(station)
            pointFeature.pointStyle = pointStyle

            stations?.addFeature(pointFeature)
        }
    }

    private fun generateCompleteStationData(station: BikeStation): BikeStation {
        val showOnlyAvailableStations = userSettings.getBoolean("showAvailable", false)
        val isOnFoot = settings.getBoolean("isOnFoot", true)

        station.isFavourite = settings.getBoolean(station.address, false)

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
            station.snippet = if (isAdded) getString(R.string.closed) else ""
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
        pointStyle.isVisible = station.visibility

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
            val showLastUpdatedInfo = userSettings.getBoolean("lastUpdated", true)

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
            true -> 1.0f
            else -> 0.5f
        }
    }

    private fun setListeners() {
        setButtonListeners()
        setMapListeners()
    }

    private fun setInitialButtonState() {
        val stationsOn = ContextCompat.getDrawable(requireContext(), R.drawable.icon_map_marker)
        val stationsOff =
                ContextCompat.getDrawable(requireContext(), R.drawable.icon_map_marker_off)
        val bike = ContextCompat.getDrawable(requireContext(), R.drawable.icon_on_bike)
        val walk = ContextCompat.getDrawable(requireContext(), R.drawable.icon_walk)
        val bikeLanesOn = ContextCompat.getDrawable(requireContext(), R.drawable.icon_road)
        val bikeLanesOff = ContextCompat.getDrawable(requireContext(), R.drawable.icon_road_off)
        val parkingOn = ContextCompat.getDrawable(requireContext(), R.drawable.icon_parking)
        val parkingOff = ContextCompat.getDrawable(requireContext(), R.drawable.icon_parking_off)

        val showStationsLayer = settings.getBoolean("showStationsLayer", true)
        if (showStationsLayer) {
            binding.btnStationsToggle.icon = stationsOn
        } else {
            binding.btnStationsToggle.icon = stationsOff
        }

        val isCarrilLayerAdded = settings.getBoolean("isCarrilLayerAdded", false)
        if (isCarrilLayerAdded) {
            binding.btnLanesToggle.icon = bikeLanesOn
        } else {
            binding.btnLanesToggle.icon = bikeLanesOff
        }

        val isOnFoot = settings.getBoolean("isOnFoot", false)
        if (isOnFoot) {
            binding.btnOnFootToggle.icon = walk
        } else {
            binding.btnOnFootToggle.icon = bike
        }

        val isParkingLayerAdded = settings.getBoolean("isParkingLayerAdded", false)
        if (isParkingLayerAdded) {
            binding.btnParkingToggle.icon = parkingOn
        } else {
            binding.btnParkingToggle.icon = parkingOff
        }
    }

    private fun setButtonBackground() {
        binding.btnLanesToggle.background = ContextCompat.getDrawable(requireContext(), R.drawable.mapbutton_background)
        binding.btnStationsToggle.background = ContextCompat.getDrawable(requireContext(), R.drawable.mapbutton_background)
        binding.btnOnFootToggle.background = ContextCompat.getDrawable(requireContext(), R.drawable.mapbutton_background)
        binding.btnRefresh.background = ContextCompat.getDrawable(requireContext(), R.drawable.mapbutton_background)
        binding.btnParkingToggle.background = ContextCompat.getDrawable(requireContext(), R.drawable.mapbutton_background)
    }

    private fun setButtonListeners() {
        val bikeLanesOn = ContextCompat.getDrawable(requireContext(), R.drawable.icon_road)
        val bikeLanesOff = ContextCompat.getDrawable(requireContext(), R.drawable.icon_road_off)
        val parkingOn = ContextCompat.getDrawable(requireContext(), R.drawable.icon_parking)
        val parkingOff = ContextCompat.getDrawable(requireContext(), R.drawable.icon_parking_off)
        val bike = ContextCompat.getDrawable(requireContext(), R.drawable.icon_on_bike)
        val walk = ContextCompat.getDrawable(requireContext(), R.drawable.icon_walk)
        val stationsOn = ContextCompat.getDrawable(requireContext(), R.drawable.icon_map_marker)
        val stationsOff =
                ContextCompat.getDrawable(requireContext(), R.drawable.icon_map_marker_off)

        // Toggle bike lanes
        binding.btnLanesToggle.setOnClickListener {
            removeButtonListeners()
            if (!settings.getBoolean("isCarrilLayerAdded", false)) {
                binding.btnLanesToggle.icon = bikeLanesOn
                getLanes()
            } else {
                binding.btnLanesToggle.icon = bikeLanesOff
                lanes?.removeLayerFromMap()
                settings.edit().putBoolean("isCarrilLayerAdded", false).apply()
                setButtonListeners()
            }
        }

        // Toggle parking
        binding.btnParkingToggle.setOnClickListener {
            removeButtonListeners()
            if (!settings.getBoolean("isParkingLayerAdded", false)) {
                binding.btnParkingToggle.icon = parkingOn
                getParkings()
            } else {
                binding.btnParkingToggle.icon = parkingOff
                parking?.removeLayerFromMap()
                settings.edit().putBoolean("isParkingLayerAdded", false).apply()
                setButtonListeners()
            }
        }

        // Toggle stations
        binding.btnStationsToggle.setOnClickListener {
            removeButtonListeners()
            resetStationsLayer()
            if (settings.getBoolean("showStationsLayer", true)) {
                binding.btnStationsToggle.icon = stationsOff
                settings.edit().putBoolean("showStationsLayer", false).apply()
                setButtonListeners()
            } else {
                getStations()
                binding.btnStationsToggle.icon = stationsOn
            }
        }

        // Toggle onFoot/onBike
        binding.btnOnFootToggle.setOnClickListener {
            // If stations are visible, recalculate layer
            if (settings.getBoolean("showStationsLayer", true)) {
                removeButtonListeners()
                resetStationsLayer()
                if (settings.getBoolean("isOnFoot", false)) {
                    settings.edit().putBoolean("isOnFoot", false).apply()
                    binding.btnOnFootToggle.icon = bike
                    getStations()
                } else {
                    settings.edit().putBoolean("isOnFoot", true).apply()
                    binding.btnOnFootToggle.icon = walk
                    getStations()
                }
                // Else just change the button icon and the setting
            } else {
                if (settings.getBoolean("isOnFoot", false)) {
                    settings.edit().putBoolean("isOnFoot", false).apply()
                    binding.btnOnFootToggle.icon = bike
                } else {
                    settings.edit().putBoolean("isOnFoot", true).apply()
                    binding.btnOnFootToggle.icon = walk
                }
            }
        }

        // Reload bike station data
        binding.btnRefresh.setOnClickListener {
            removeButtonListeners()
            binding.btnStationsToggle.icon = stationsOn
            if (settings.getBoolean("showStationsLayer", true)) {
                resetStationsLayer()
            }
            getStations()
        }
    }

    private fun removeButtonListeners() {
        binding.btnLanesToggle.setOnClickListener(null)
        binding.btnOnFootToggle.setOnClickListener(null)
        binding.btnParkingToggle.setOnClickListener(null)
        binding.btnRefresh.setOnClickListener(null)
        binding.btnStationsToggle.setOnClickListener(null)
    }

    private fun setMapListeners() {

        val isClusteringActivated = userSettings.getBoolean("isClusteringActivated", true)
        requireActivity().runOnUiThread {

            if (isClusteringActivated) {
                mMap!!.apply {
                    setOnInfoWindowClickListener(mClusterManager)
                    setOnCameraIdleListener(mClusterManager)
                    setOnMarkerClickListener(mClusterManager)
                    setInfoWindowAdapter(mClusterManager.markerManager)
                }

                setClusteredInfoWindow()

                mClusterManager.setOnClusterClickListener { cluster ->
                    val zoom = mMap!!.cameraPosition.zoom
                    val position = cluster.position
                    mMap!!.animateCamera(
                            CameraUpdateFactory.newLatLngZoom(position, zoom + 1.0.toFloat()),
                            250,
                            null
                    )
                    true
                }
            } else {
                setNormalInfoWindow()
            }
        }
    }

    private fun setNormalInfoWindow() {
        mMap!!.setInfoWindowAdapter(object : GoogleMap.InfoWindowAdapter {
            // Use default InfoWindow frame
            override fun getInfoWindow(marker: Marker): View {
                return getInfoWindowCommonInfo(marker)
            }

            // Defines the contents of the InfoWindow
            override fun getInfoContents(marker: Marker): View? {
                return null
            }
        })

        mMap!!.setOnInfoWindowClickListener { clickedMarker ->
            val currentStationIsFav = settings.getBoolean(clickedMarker.title, false)
            val showFavorites = userSettings.getBoolean("showFavorites", false)

            if (currentStationIsFav) {
                clickedMarker.alpha = 0.5f
                if (showFavorites) {
                    clickedMarker.isVisible = false
                }
                settings.edit().putBoolean(clickedMarker.title, false).apply()
            } else {
                clickedMarker.alpha = 1f
                settings.edit().putBoolean(clickedMarker.title, true).apply()
            }
            clickedMarker.showInfoWindow()
        }
    }

    private fun setClusteredInfoWindow() {
        mClusterManager.markerCollection.setInfoWindowAdapter(object :
                GoogleMap.InfoWindowAdapter {
            // Use default InfoWindow frame
            override fun getInfoWindow(marker: Marker): View {
                // Getting view from the layout file info_window_layout
                val popup = getInfoWindowCommonInfo(marker)

                mClusterManager.setOnClusterItemInfoWindowClickListener { item ->
                    val currentStationIsFav = settings.getBoolean(item.title, false)
                    val showFavorites = userSettings.getBoolean("showFavorites", false)

                    if (currentStationIsFav) {
                        item.alpha = 0.5f
                        marker.alpha = 0.5f
                        if (showFavorites) {
                            item.visibility = false
                        }
                        settings.edit().putBoolean(item.title, false).apply()
                    } else {
                        item.alpha = 1.0f
                        marker.alpha = 1.0f
                        settings.edit().putBoolean(item.title, true).apply()
                    }
                    marker.showInfoWindow()
                    mClusterManager.cluster()
                }
                return popup
            }

            // Defines the contents of the InfoWindow
            override fun getInfoContents(marker: Marker): View? {
                return null
            }
        })
    }

    private fun getBackgroundColor(): Int {
        var color = R.color.white
        when ((resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK)) {
            Configuration.UI_MODE_NIGHT_NO -> {
                color = R.color.white
            }
            Configuration.UI_MODE_NIGHT_YES -> {
                color = R.color.black
            }
        }
        return color
    }

    @SuppressLint("InflateParams")
    private fun getInfoWindowCommonInfo(marker: Marker): View {
        val starIconOff = ContextCompat.getDrawable(requireContext(), R.drawable.icon_star_outline)
        val startIconOn = ContextCompat.getDrawable(requireContext(), R.drawable.icon_star)

        // Getting view from the layout file info_window_layout
        val popup = requireActivity().layoutInflater.inflate(R.layout.marker_popup, null)
        popup.setBackgroundColor(ContextCompat.getColor(requireContext(), getBackgroundColor()))

        // Getting reference to the ImageView/title/snippet
        val title = popup.findViewById<TextView>(R.id.title)
        val snippet = popup.findViewById<TextView>(R.id.snippet)
        val btnStar = popup.findViewById<ImageView>(R.id.btn_star)

        if (marker.snippet!!.contains("\n\n")) {
            snippet.setTextColor(ContextCompat.getColor(requireContext(), R.color.red))
            snippet.setTypeface(null, Typeface.BOLD)
            snippet.text = marker.snippet
        } else {
            snippet.text = marker.snippet
        }
        title.text = marker.title

        // Checking if current station is favorite
        val currentStationIsFav = settings.getBoolean(marker.title, false)

        // Setting correspondent icon
        if (currentStationIsFav) {
            btnStar.setImageDrawable(startIconOn)
        } else {
            btnStar.setImageDrawable(starIconOff)
        }
        return popup
    }

    private fun safeSnackBar(stringReference: Int) {
        binding.mainView.let {
            val snack = Snackbar.make(binding.mainView, stringReference, Snackbar.LENGTH_SHORT)

            // Necessary to display snackbar over bottom navigation bar
            val params = snack.view.layoutParams as CoordinatorLayout.LayoutParams
            params.anchorId = R.id.bottom_navigation_view
            params.anchorGravity = Gravity.TOP
            params.gravity = Gravity.TOP
            snack.view.layoutParams = params
            snack.show()
        }
    }

    private fun getLanes() {
        safeSnackBar(R.string.load_lanes)

        launch {
            getLanesAsync()
            // Has to run on the main thread
            lanes?.addLayerToMap()
            settings.edit().putBoolean("isCarrilLayerAdded", true).apply()
            setListeners()
        }
    }

    private suspend fun getLanesAsync() {
        try {
            withContext(Dispatchers.IO) {
                lanes = GeoJsonLayer(mMap, R.raw.bike_lanes, context)
                for (feature in lanes!!.features) {
                    val stringStyle = GeoJsonLineStringStyle()
                    stringStyle.width = 5f
                    if (userSettings.getBoolean("cicloCalles", true)) {
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

        var color = Color.BLACK
        if(resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES){
            color = Color.WHITE
        }

        return when (feature.getProperty("estado")) {
            // Normal bike lane
            "1" -> color
            // Ciclocalle
            "2" -> Color.BLUE
            // Weird fragments
            "3" -> Color.BLUE
            // River
            "4" -> color
            else -> Color.RED
        }
    }

    private fun getParkings() {
        safeSnackBar(R.string.load_parking)

        launch {
            getParkingsAsync()
            // Has to run on the main thread
            parking?.addLayerToMap()
            settings.edit().putBoolean("isParkingLayerAdded", true).apply()
            setListeners()
        }
    }

    private suspend fun getParkingsAsync() {

        val showFavorites = userSettings.getBoolean("showFavorites", false)
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
                        pointStyle.alpha = 0.5f
                        pointStyle.icon = iconParking

                        val currentStationIsFav = settings.getBoolean(pointStyle.title, false)

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
        if (isApplicationReady && mMap != null) {
            // Disable location to avoid battery drain
            setLocationButtonEnabled(false)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isApplicationReady && mMap != null) {
            // Disable location to avoid battery drain
            setLocationButtonEnabled(false)
        }
    }

    override fun onResume() {
        super.onResume()
        if (isApplicationReady && isLocationPermissionGranted && mMap != null) {
            // Re-enable location
            setLocationButtonEnabled(true)
        }
    }
}
