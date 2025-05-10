package com.example.trashcashcampus_mobile.fragments

import android.Manifest
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.preference.PreferenceManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.trashcashcampus_mobile.QRScannerActivity
import com.example.trashcashcampus_mobile.R
import com.example.trashcashcampus_mobile.models.ApiClient
import com.example.trashcashcampus_mobile.models.CampusLocation
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.infowindow.InfoWindow
import android.app.AlertDialog
import androidx.activity.result.contract.ActivityResultContracts

class MapFragment : Fragment() {
    private val TAG = "MapFragment"
    // Add QR scanner request code constant
    companion object {
        const val QR_SCANNER_REQUEST_CODE = 100
        
        @JvmStatic
        fun newInstance() = MapFragment()
    }
    
    // UI elements
    // private lateinit var btnScanQR: Button
    private lateinit var mapView: MapView
    private lateinit var customInfoWindow: InfoWindow
    
    // Location permissions
    private val REQUEST_PERMISSIONS_REQUEST_CODE = 1
    
    // Campus locations loaded from backend
    private var campusLocations = listOf<CampusLocation>()
    
    // Default fallback map points in case API fails
    private val defaultPinPoints = listOf(
        MapPoint("NGE Building", 10.294460890889612, 123.881064193439),
        MapPoint("ACAD Building", 10.295721859947419, 123.88122914928363),
        MapPoint("RTL Building", 10.294840425650719, 123.88049020072194),
        MapPoint("Engineering Department", 10.294758615762326, 123.87985451718372),
        MapPoint("Junior High Building", 10.295550323525, 123.87961580058581),
        MapPoint("Gymnasium", 10.296260219787618, 123.87957020303742),
        MapPoint("Canteen", 10.296128268936334, 123.88038693566146),
        MapPoint("GLE Building", 10.295378787021845, 123.88130693334064)
    )

    // Add this property for permission launcher
    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val locationGranted = permissions.entries.all { it.value }
        if (locationGranted) {
            initMap()
        } else {
            Toast.makeText(
                requireContext(),
                "Location permission is required to show your position on the map",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // Add QR scanner launcher
    private val qrScannerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        // Handle QR scanner result
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            Log.d(TAG, "QR scan successful")
            // You can handle successful scan results here if needed
        }
    }

    // Add the missing initMap method
    private fun initMap() {
        try {
            // Configure the map
            mapView.setTileSource(TileSourceFactory.MAPNIK)
            mapView.setMultiTouchControls(true)
            
            val mapController = mapView.controller
            
            // Set initial map position to the center of the campus coordinates
            val centerLat = 10.295383 // Center of campus approximately
            val centerLon = 123.880498
            val startPoint = GeoPoint(centerLat, centerLon)
            mapController.setCenter(startPoint)
            mapController.setZoom(18.0) // Closer zoom level for campus
            
            // Enable user location on the map if permission is granted
            val myLocationOverlay = org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay(
                org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider(requireContext()),
                mapView
            )
            myLocationOverlay.enableMyLocation()
            mapView.overlays.add(myLocationOverlay)
            
            // Add markers for trash bins and collection points
            addMapMarkers()
            
            // Refresh the map
            mapView.invalidate()
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing map: ${e.message}", e)
            Toast.makeText(requireContext(), "Error initializing map", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_map, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initialize map view
        mapView = view.findViewById(R.id.map_view)
        
        // Initialize info window for markers
        customInfoWindow = object : InfoWindow(R.layout.map_info_window, mapView) {
            override fun onOpen(item: Any?) {
                val marker = item as? Marker ?: return
                val infoView = mView
                
                // Fix the IDs to match the actual ones in map_info_window.xml
                val title = infoView.findViewById<TextView>(R.id.info_title)
                val snippet = infoView.findViewById<TextView>(R.id.info_snippet)
                val scanButton = infoView.findViewById<Button>(R.id.info_button)
                
                title.text = marker.title
                snippet.text = marker.snippet
                
                scanButton.setOnClickListener {
                    if (marker.title?.contains("Recycling Bin") == true) {
                        // Launch QR scanner for this location
                        launchQRScanner(marker.position.latitude, marker.position.longitude)
                    } else {
                        // Just show a message for collection points
                        Toast.makeText(context, "Visit this location to redeem your points!", Toast.LENGTH_SHORT).show()
                    }
                    // Close the info window
                    close()
                }
            }
            
            override fun onClose() {
                // Clean up if needed
            }
        }
        
        // Configure map
        configureMap()
        
        // Add markers for trash bins and collection points
        addMapMarkers()
        
        // Animate map entrance
        animateMapEntrance()
    }
    
    private fun configureMap() {
        // Configure osmdroid
        val ctx = requireActivity().applicationContext
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx))
        
        // Initialize UI elements
        // Remove the global scan QR button - we'll keep only the ones in marker windows
        // btnScanQR = view.findViewById(R.id.btn_scan_qr)
        
        // Remove button click listeners setup since we removed the button
        // setupListeners()
        
        // Request permissions if needed
        requestPermissionsIfNecessary()
        
        // If permission is already granted, initialize the map
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED) {
            // Initialize the map with full functionality
            initMap()
        } else {
            // Initialize the map with basic configuration
            initializeMapBase()
        }
        
        // Set up tap listener on map to dismiss any open info windows
        mapView.setOnTouchListener { _, _ ->
            // Close any open info windows when tapping on the map
            InfoWindow.closeAllInfoWindowsOn(mapView)
            false // Return false to allow the event to propagate for map dragging/zooming
        }
    }
    
    private fun initializeMapBase() {
        // Configure the map
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)
        
        val mapController = mapView.controller
        
        // Set initial map position to the center of the campus coordinates
        val centerLat = 10.295383 // Center of campus approximately
        val centerLon = 123.880498
        val startPoint = GeoPoint(centerLat, centerLon)
        mapController.setCenter(startPoint)
        mapController.setZoom(18.0) // Closer zoom level for campus
    }
    
    private fun loadCampusLocations() {
        // Show a loading indicator if needed
        
        // Load campus locations from the backend API
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val locations = ApiClient.getCampusLocations()
                
                if (locations.isNotEmpty()) {
                    // Successfully loaded locations from backend
                    campusLocations = locations
                    Log.d(TAG, "Loaded ${locations.size} locations from backend")
                    
                    // Update the map with actual locations
                    addMarkersFromBackend()
                } else {
                    // Fall back to default locations if API returned empty list
                    Log.w(TAG, "No locations from backend, using default locations")
                    addDefaultMarkers()
                }
            } catch (e: Exception) {
                // Handle errors and fall back to default locations
                Log.e(TAG, "Error loading campus locations", e)
                Toast.makeText(context, "Error loading map locations", Toast.LENGTH_SHORT).show()
                addDefaultMarkers()
            }
        }
    }
    
    private fun addMarkersFromBackend() {
        // Clear existing overlays
        mapView.overlays.clear()
        
        // Add a marker for each campus location from the backend
        for (location in campusLocations) {
            val marker = Marker(mapView)
            marker.position = GeoPoint(location.latitude, location.longitude)
            marker.title = location.name
            marker.snippet = location.description
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            
            // Set custom click behavior
            marker.setOnMarkerClickListener { clickedMarker, _ ->
                // Create an alert dialog
                val dialogView = layoutInflater.inflate(R.layout.map_info_window, null)
                
                // Set marker information
                val title = dialogView.findViewById<TextView>(R.id.info_title)
                title.text = marker.title
                
                // Set up scan button
                val btnScan = dialogView.findViewById<Button>(R.id.info_button)
                btnScan.setOnClickListener {
                    // Launch QR scanner with location data
                    launchQRScanner(location.latitude, location.longitude)
                    dialogView.isPressed = false
                }
                
                // Show the dialog
                val dialog = AlertDialog.Builder(requireContext())
                    .setView(dialogView)
                    .create()
                
                dialog.show()
                
                // Return true to indicate we've handled the event
                true
            }
            
            marker.infoWindow = customInfoWindow
            mapView.overlays.add(marker)
        }
    }
    
    private fun addDefaultMarkers() {
        // Clear existing overlays
        mapView.overlays.clear()
        
        // Add a marker for each default pin point
        for (point in defaultPinPoints) {
            val marker = Marker(mapView)
            marker.position = GeoPoint(point.latitude, point.longitude)
            marker.title = point.name
            marker.snippet = "Tap to scan QR code at this location"
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            
            // Set custom click behavior
            marker.setOnMarkerClickListener { clickedMarker, _ ->
                // Show a custom info window
                val infoWindow = CustomInfoWindow(mapView, clickedMarker, point.name)
                // Use constant value instead of marker.height which is unavailable
                val markerHeight = 100 // Default height in pixels
                infoWindow.open(clickedMarker, marker.position, 0, -markerHeight)
                true
            }
            
            mapView.overlays.add(marker)
        }
        
        // Refresh the map to show changes
        mapView.invalidate()
    }
    
    // Custom InfoWindow class for the map markers
    inner class CustomInfoWindow(
        mapView: MapView, 
        private val marker: Marker,
        private val locationName: String
    ) : InfoWindow(R.layout.map_info_window, mapView) {
        override fun onOpen(item: Any?) {
            val view = mView
            
            // Set location name
            val title = view.findViewById<TextView>(R.id.info_title)
            title.text = marker.title
            
            // Set up scan button
            val btnScan = view.findViewById<Button>(R.id.info_button)
            btnScan.setOnClickListener {
                // Log the location being passed to ensure it's correct
                Log.d(TAG, "Launching QR scanner with location: $locationName")
                
                // Launch QR scanner with location data
            val intent = Intent(requireContext(), QRScannerActivity::class.java)
                intent.putExtra("LOCATION_NAME", locationName)
                // Also pass location as BUILDING_NAME for compatibility
                intent.putExtra("BUILDING_NAME", locationName)
            startActivity(intent)
                
                // Close the info window
                close()
            }
        }
        
        override fun onClose() {
            // Clean up if needed
        }
    }
    
    private fun requestPermissionsIfNecessary() {
        val permissions = mutableListOf<String>()
        
        // Only add storage permission for Android < 13
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
        
        // Always request location permissions
        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        
        val permissionsToRequest = permissions.filter {
            ContextCompat.checkSelfPermission(requireContext(), it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()
        
        if (permissionsToRequest.isNotEmpty()) {
            locationPermissionLauncher.launch(permissionsToRequest)
        }
    }
    
    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }
    
    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }
    
    // Data class for mapping locations
    data class MapPoint(val name: String, val latitude: Double, val longitude: Double, val type: String = "bin")

    private fun animateMapEntrance() {
        // Get references to view elements
        val titleText = view?.findViewById<View>(R.id.tv_map_title)
        val subtitleText = view?.findViewById<View>(R.id.tv_map_subtitle)
        val mapContainer = view?.findViewById<View>(R.id.map_container)
        
        // Define animation durations and delays
        val baseDelay = 100L
        val animDuration = 500L
        
        // Animate the title
        titleText?.apply {
            animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(animDuration)
                .setInterpolator(android.view.animation.DecelerateInterpolator())
                .setStartDelay(baseDelay)
                .start()
        }
        
        // Animate the subtitle
        subtitleText?.apply {
            animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(animDuration)
                .setInterpolator(android.view.animation.DecelerateInterpolator())
                .setStartDelay(baseDelay + 100)
                .start()
        }
        
        // Animate the map container with a bounce effect
        mapContainer?.apply {
            animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(animDuration + 100)
                .setInterpolator(android.view.animation.OvershootInterpolator(0.8f))
                .setStartDelay(baseDelay + 200)
                .withEndAction {
                    try {
                        // Add a subtle scale animation to draw attention to the map
                        val scaleX = ObjectAnimator.ofFloat(this, "scaleX", 1f, 1.02f, 1f)
                        val scaleY = ObjectAnimator.ofFloat(this, "scaleY", 1f, 1.02f, 1f)
                        
                        val animSet = AnimatorSet()
                        animSet.playTogether(scaleX, scaleY)
                        animSet.duration = 800
                        animSet.interpolator = AccelerateDecelerateInterpolator()
                        animSet.start()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error during map animation", e)
                    }
                }
                .start()
        }
    }
    
    private fun addMarkerWithAnimation(geoPoint: GeoPoint, markerType: String) {
        try {
            // Create marker
            val marker = Marker(mapView)
            marker.position = geoPoint
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            
            // Set marker appearance based on type
            when (markerType) {
                "bin" -> {
                    marker.icon = AppCompatResources.getDrawable(requireContext(), R.drawable.ic_recycling)
                    marker.title = "Recycling Bin"
                    marker.snippet = "Tap to scan QR code"
                    
                    // Apply a green tint to bin markers
                    marker.setInfoWindowAnchor(Marker.ANCHOR_CENTER, 0f)
                    marker.infoWindow = customInfoWindow
                }
                "collection" -> {
                    marker.icon = AppCompatResources.getDrawable(requireContext(), R.drawable.ic_collection_point)
                    marker.title = "Collection Point"
                    marker.snippet = "Redeem points here"
                    
                    // Apply a blue tint to collection markers
                    marker.setInfoWindowAnchor(Marker.ANCHOR_CENTER, 0f)
                    marker.infoWindow = customInfoWindow
                }
            }
            
            // Set onclick listener for marker
            marker.setOnMarkerClickListener { clickedMarker, _ ->
                // Add a pulse animation when marker is clicked
                pulseMarker(clickedMarker)
                
                clickedMarker.showInfoWindow()
                mapView.controller.animateTo(clickedMarker.position)
                true
            }
            
            // Add to map with animation
            mapView.overlays.add(marker)
            
            // Animate the marker when it's added
            val handler = Handler(Looper.getMainLooper())
            handler.postDelayed({
                try {
                    // Start with marker slightly above its position and not fully opaque
                    marker.alpha = 0f
                    marker.position = GeoPoint(geoPoint.latitude, geoPoint.longitude)
                    marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    
                    // Animate marker dropping down with a bounce effect
                    val startLat = geoPoint.latitude + 0.0002 // Slightly higher for more dramatic effect
                    val steps = 15 // More steps for smoother animation
                    val delay = 15L // milliseconds per step
                    
                    for (i in 0 until steps) {
                        handler.postDelayed({
                            try {
                                val fraction = i.toFloat() / steps
                                // Use a custom curve for bounce effect
                                val progress = if (fraction < 0.7f) {
                                    fraction
                                } else {
                                    val bounceFactor = (fraction - 0.7f) / 0.3f
                                    // Add a small bounce at the end (overshoot then settle)
                                    val bounce = 1f + 0.1f * Math.sin(bounceFactor * Math.PI).toFloat()
                                    0.7f + (0.3f * bounce)
                                }
                                
                                val newLat = startLat - (startLat - geoPoint.latitude) * progress
                                marker.position = GeoPoint(newLat, geoPoint.longitude)
                                marker.alpha = Math.min(1f, fraction * 1.5f) // Fade in faster than drop
                                mapView.invalidate()
                            } catch (e: Exception) {
                                Log.e(TAG, "Error in marker animation step", e)
                            }
                        }, i * delay)
                    }
                    
                    // Final position and add a small "settle" animation
                    handler.postDelayed({
                        try {
                            marker.position = geoPoint
                            marker.alpha = 1f
                            mapView.invalidate()
                            
                            // Add a small scale "settle" animation
                            pulseMarker(marker, 0.9f, 1.0f, 300)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error in marker final animation", e)
                        }
                    }, steps * delay)
                } catch (e: Exception) {
                    Log.e(TAG, "Error during marker animation setup", e)
                    // Fallback to show marker without animation
                    marker.position = geoPoint
                    marker.alpha = 1f
                    mapView.invalidate()
                }
            }, 300L) // Initial delay before animation
        } catch (e: Exception) {
            Log.e(TAG, "Error creating animated marker", e)
        }
    }
    
    // Add a pulse animation to a marker when clicked
    private fun pulseMarker(marker: Marker, startScale: Float = 1.0f, endScale: Float = 1.3f, duration: Long = 500) {
        try {
            val handler = Handler(Looper.getMainLooper())
            val steps = 10
            val delay = duration / steps
            
            for (i in 0 until steps) {
                handler.postDelayed({
                    try {
                        val progress = i.toFloat() / steps
                        // Create a pulse effect (grow then shrink)
                        val scaleProgress = if (progress < 0.5f) {
                            // First half: grow
                            startScale + (endScale - startScale) * (progress * 2)
                        } else {
                            // Second half: shrink
                            endScale - (endScale - startScale) * ((progress - 0.5f) * 2)
                        }
                        
                        // Apply scale to marker
                        marker.icon?.setBounds(
                            0, 0,
                            (marker.icon?.intrinsicWidth ?: 0) * scaleProgress.toInt(),
                            (marker.icon?.intrinsicHeight ?: 0) * scaleProgress.toInt()
                        )
                        mapView.invalidate()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error in pulse animation step", e)
                    }
                }, i * delay)
            }
            
            // Reset to original size
            handler.postDelayed({
                try {
                    marker.icon?.setBounds(
                        0, 0,
                        marker.icon?.intrinsicWidth ?: 0,
                        marker.icon?.intrinsicHeight ?: 0
                    )
                    mapView.invalidate()
                } catch (e: Exception) {
                    Log.e(TAG, "Error resetting marker size", e)
                }
            }, duration + 50)
        } catch (e: Exception) {
            Log.e(TAG, "Error during pulse animation", e)
        }
    }

    private fun addMapMarkers() {
        // Load campus locations from backend
        loadCampusLocations()
        
        // Add some default markers to begin with
        val defaultLocations = listOf(
            // Recycling bins (sample data)
            MapPoint("Library Recycling Bin", 21.5001, 39.2435, "bin"),
            MapPoint("Student Center Bin", 21.4997, 39.2450, "bin"),
            MapPoint("Admin Building Bin", 21.5008, 39.2445, "bin"),
            MapPoint("Engineering Bin", 21.4990, 39.2458, "bin"),
            
            // Collection points (sample data)
            MapPoint("Main Collection Center", 21.5004, 39.2440, "collection"),
            MapPoint("South Campus Collection", 21.4985, 39.2448, "collection")
        )
        
        // Add markers with animation, slightly delayed from each other
        defaultLocations.forEachIndexed { index, point ->
            val handler = Handler(Looper.getMainLooper())
            handler.postDelayed({
                addMarkerWithAnimation(
                    GeoPoint(point.latitude, point.longitude),
                    point.type
                )
            }, 300L + (index * 150L)) // Stagger the marker additions
        }
    }
    
    // Updated data class for mapping locations with type
    // data class MapPoint(val name: String, val latitude: Double, val longitude: Double, val type: String = "bin")

    private fun launchQRScanner(latitude: Double, longitude: Double) {
        try {
            // Find the location name based on coordinates
            val locationName = findLocationNameByCoordinates(latitude, longitude)
            
            // Log for debugging
            Log.d(TAG, "Launching QR scanner for coordinates: $latitude, $longitude, location: $locationName")
            
            // Launch QR scanner activity with location data
            val intent = Intent(requireContext(), QRScannerActivity::class.java)
            intent.putExtra("latitude", latitude)
            intent.putExtra("longitude", longitude)
            intent.putExtra("source", "map")
            // Add the location name
            intent.putExtra("LOCATION_NAME", locationName)
            intent.putExtra("BUILDING_NAME", locationName)
            qrScannerLauncher.launch(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error launching QR scanner", e)
            Toast.makeText(requireContext(), "Could not open QR scanner", Toast.LENGTH_SHORT).show()
        }
    }
    
    // Helper method to find location name based on coordinates
    private fun findLocationNameByCoordinates(latitude: Double, longitude: Double): String {
        // First check if we have campus locations loaded from backend
        if (campusLocations.isNotEmpty()) {
            // Find the closest location
            val closestLocation = campusLocations.minByOrNull { location ->
                val latDiff = location.latitude - latitude
                val lonDiff = location.longitude - longitude
                // Simple distance calculation (not exact but good enough for comparison)
                Math.sqrt((latDiff * latDiff) + (lonDiff * lonDiff))
            }
            
            if (closestLocation != null) {
                return closestLocation.name
            }
        }
        
        // If no match from backend, try default pin points
        val closestPoint = defaultPinPoints.minByOrNull { point ->
            val latDiff = point.latitude - latitude
            val lonDiff = point.longitude - longitude
            Math.sqrt((latDiff * latDiff) + (lonDiff * lonDiff))
        }
        
        return closestPoint?.name ?: "Unknown Location"
    }
} 