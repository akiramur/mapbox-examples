package com.example.simplemapapp

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.locationcomponent.location

import com.example.simplemapapp.databinding.ActivityMainBinding

import java.lang.ref.WeakReference

class MainActivity : AppCompatActivity() {

    private lateinit var mapboxMap: MapboxMap
    private lateinit var locationPermissionHelper: LocationPermissionHelper
    private var lastStyleUri = Style.DARK
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mapboxMap = binding.mapView.getMapboxMap()

        binding.mapView.location.addOnIndicatorPositionChangedListener() {
            mapboxMap.setCamera(CameraOptions.Builder().center(it).zoom(12.0).build())
        }

        locationPermissionHelper = LocationPermissionHelper(WeakReference(this))
        locationPermissionHelper.checkPermissions {
            onMapReady()
        }
    }

    private fun onMapReady() {
        mapboxMap.loadStyleUri(
            Style.MAPBOX_STREETS
        ) {
            lastStyleUri = it.styleURI

            //initLocationComponent()
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
                binding.mapView.location.enabled = false
                return true
            }
            R.id.action_component_enabled -> {
                binding.mapView.location.enabled = true
                return true
            }
            R.id.action_stop_pulsing -> {
                binding.mapView.location.pulsingEnabled = false
                return true
            }
            R.id.action_start_pulsing -> {
                binding.mapView.location.pulsingEnabled = true
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
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