package com.example.simplemapapp

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.simplemapapp.databinding.ActivitySimpleMapBinding
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.scalebar.scalebar

class SimpleMapActivity : AppCompatActivity() {

    lateinit var mapView: MapView
    private lateinit var binding: ActivitySimpleMapBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySimpleMapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mapView = binding.mapView
        mapView.mapboxMap
            .apply {
                setCamera(
                    CameraOptions.Builder()
                        .center(Point.fromLngLat(LONGITUDE, LATITUDE))
                        .zoom(9.0)
                        .build()
                )
            }

        mapView.scalebar.enabled = false
        mapView.mapboxMap.loadStyle(Style.STANDARD)

        binding.button.setOnClickListener {
            Log.d(TAG, "button clicked")
        }
    }

    companion object {
        private val TAG = "SimpleMapActivity"
        private const val LATITUDE = 40.0
        private const val LONGITUDE = -74.5
    }
}