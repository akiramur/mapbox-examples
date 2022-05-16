package com.example.simplemapapp

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.example.simplemapapp.databinding.ActivityMainBinding
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.locationcomponent.location
import java.lang.ref.WeakReference

class MainActivity : AppCompatActivity() {

    private lateinit var mapView: MapView
    private lateinit var mapboxMap: MapboxMap
    private lateinit var locationPermissionHelper: LocationPermissionHelper
    private var lastStyleUri = Style.DARK
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val styleId = Style.MAPBOX_STREETS
        //val styleId = Style.SATELLITE

        // This is how activity accesses fragment in MultiMapActivity example in mapbox-maps-android
        val fragment = supportFragmentManager.findFragmentById(R.id.map_fragment) as MapFragment
        fragment.getMapAsync { mapboxMap ->
            this.mapboxMap = mapboxMap
            this.mapView = fragment.getMapView()

            locationPermissionHelper = LocationPermissionHelper(WeakReference(this))
            locationPermissionHelper.checkPermissions {
                mapboxMap.loadStyleUri(
                    styleId
                ) {
                    mapView.location.addOnIndicatorPositionChangedListener() {
                        val cameraOptions = CameraOptions.Builder().center(it).zoom(12.0).build()
                        mapboxMap.setCamera(cameraOptions)
                    }
                    mapView.location.pulsingEnabled = true
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_pulsing_location_mode, menu)
        return true
    }

    @SuppressLint("MissingPermission")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_map_style_change -> {
                loadNewStyle()
                return true
            }
            R.id.action_component_disable -> {
                mapView.location.enabled = false
                return true
            }
            R.id.action_component_enabled -> {
                mapView.location.enabled = true
                return true
            }
            R.id.action_stop_pulsing -> {
                mapView.location.pulsingEnabled = false
                return true
            }
            R.id.action_start_pulsing -> {
                mapView.location.pulsingEnabled = true
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }
    
    private fun generateCamera(lat: Double, lng: Double, zoom: Double): CameraOptions {
        return CameraOptions.Builder().center(Point.fromLngLat(lng, lat)).zoom(zoom).build()
    }

    private fun loadNewStyle() {
        val styleUrl = if (lastStyleUri == Style.DARK) Style.LIGHT else Style.DARK
        mapboxMap.loadStyleUri(
            styleUrl
        ) { lastStyleUri = styleUrl }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        locationPermissionHelper.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
}