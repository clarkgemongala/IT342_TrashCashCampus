package com.example.trashcashcampus_mobile.fragments

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
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

class MapFragment : Fragment() {
    private val TAG = "MapFragment"
    
    // UI elements
    // private lateinit var btnScanQR: Button
    private lateinit var mapView: MapView
    
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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_map, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Configure osmdroid
        val ctx = requireActivity().applicationContext
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx))
        
        // Initialize UI elements
        // Remove the global scan QR button - we'll keep only the ones in marker windows
        // btnScanQR = view.findViewById(R.id.btn_scan_qr)
        mapView = view.findViewById(R.id.map_view)
        
        // Remove button click listeners setup since we removed the button
        // setupListeners()
        
        // Request permissions if needed
        requestPermissionsIfNecessary()
        
        // Initialize the map with basic configuration
        initializeMapBase()
        
        // Load campus locations from backend
        loadCampusLocations()
        
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
                // Show a custom info window with the scan button
                val infoWindow = CustomInfoWindow(mapView, clickedMarker, location.name)
                // Use constant value instead of marker.height which is unavailable
                val markerHeight = 100 // Default height in pixels
                infoWindow.open(clickedMarker, marker.position, 0, -markerHeight)
                true
            }
            
            // Set icon based on bin type if needed
            /*when (location.binType.toLowerCase()) {
                "recyclable" -> marker.icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_bin_recyclable)
                "paper" -> marker.icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_bin_paper)
                "plastic" -> marker.icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_bin_plastic)
                "food" -> marker.icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_bin_food)
                else -> marker.icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_bin_general)
            }*/
            
            mapView.overlays.add(marker)
        }
        
        // Refresh the map to show changes
        mapView.invalidate()
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
            val title = view.findViewById<TextView>(R.id.info_window_title)
            title.text = marker.title
            
            // Set up scan button
            val btnScan = view.findViewById<Button>(R.id.btn_scan_at_location)
            btnScan.setOnClickListener {
                // Launch QR scanner with location data
            val intent = Intent(requireContext(), QRScannerActivity::class.java)
                intent.putExtra("LOCATION_NAME", locationName)
            startActivity(intent)
                
                // Close the info window
                close()
            }
            
            // Add a close button to the info window
            val btnClose = view.findViewById<View>(R.id.btn_close_info)
            if (btnClose != null) {
                btnClose.setOnClickListener {
                    // Close this info window
                    close()
                }
            }
        }
        
        override fun onClose() {
            // Clean up if needed
        }
    }
    
    private fun requestPermissionsIfNecessary() {
        val permissions = arrayOf(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        
        val permissionsToRequest = permissions.filter {
            ContextCompat.checkSelfPermission(requireContext(), it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()
        
        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                permissionsToRequest,
                REQUEST_PERMISSIONS_REQUEST_CODE
            )
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
    data class MapPoint(val name: String, val latitude: Double, val longitude: Double)

    companion object {
        @JvmStatic
        fun newInstance() = MapFragment()
    }
} 