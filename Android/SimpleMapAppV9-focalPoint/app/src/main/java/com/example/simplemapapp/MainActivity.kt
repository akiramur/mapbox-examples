package com.example.simplemapapp

import android.graphics.PointF
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.simplemapapp.R
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.maps.Style

import com.example.simplemapapp.databinding.ActivityMainBinding
import com.mapbox.android.gestures.MoveGestureDetector
import com.mapbox.android.gestures.RotateGestureDetector
import com.mapbox.android.gestures.ShoveGestureDetector
import com.mapbox.android.gestures.StandardScaleGestureDetector
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.plugins.annotation.Circle
import com.mapbox.mapboxsdk.plugins.annotation.CircleManager
import com.mapbox.mapboxsdk.plugins.annotation.CircleOptions

/**
 * The most basic example of adding a map to an activity with Kotlin code
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private lateinit var circleManager: CircleManager
    private var redCircle: Circle? = null
    private var blueCircle: Circle? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Mapbox access token is configured here. This needs to be called either in your application
        // object or in the same activity which contains the mapview.
        Mapbox.getInstance(this, getString(R.string.mapbox_access_token))

        // This contains the MapView in XML and needs to be called after the access token is configured.
        //setContentView(R.layout.activity_main)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)


        binding.mapView.onCreate(savedInstanceState)
        binding.mapView.getMapAsync { mapboxMap ->
            mapboxMap.setStyle(Style.MAPBOX_STREETS) {

                // Map is set up and the style has loaded. Now you can add data or make other map adjustments.
                circleManager = CircleManager(
                    binding.mapView, mapboxMap, it
                )
            }

            mapboxMap.addOnMapLongClickListener(object: MapboxMap.OnMapLongClickListener {
                override fun onMapLongClick(point: LatLng): Boolean {
                    val pointF = mapboxMap.projection.toScreenLocation(point);
                    mapboxMap.uiSettings.focalPoint = pointF;


                    deleteRedCircleIfExists()
                    addRedCircle(point)

                    return true;
                }
            });

            mapboxMap.addOnRotateListener(object: MapboxMap.OnRotateListener {
                override fun onRotateBegin(detector: RotateGestureDetector) {

                }

                override fun onRotate(detector: RotateGestureDetector) {

                }

                override fun onRotateEnd(detector: RotateGestureDetector) {

                    mapboxMap.uiSettings.focalPoint?.let { it ->
                        val coordinate = mapboxMap.projection.fromScreenLocation(it)

                        deleteBlueCircleIfExists()
                        addBlueCircle(coordinate)
                    }
                }
            })

            mapboxMap.addOnMoveListener(object : MapboxMap.OnMoveListener {
                override fun onMoveBegin(detector: MoveGestureDetector) {
                    // user started moving the map
                }

                override fun onMove(detector: MoveGestureDetector) {
                    // user is moving the map
                }

                override fun onMoveEnd(detector: MoveGestureDetector) {
                    mapboxMap.uiSettings.focalPoint?.let { it ->
                        val coordinate = mapboxMap.projection.fromScreenLocation(it)

                        deleteBlueCircleIfExists()
                        addBlueCircle(coordinate)
                    }
                }
            })

            mapboxMap.addOnScaleListener(object: MapboxMap.OnScaleListener {
                override fun onScaleBegin(detector: StandardScaleGestureDetector) {

                }

                override fun onScale(detector: StandardScaleGestureDetector) {

                }

                override fun onScaleEnd(detector: StandardScaleGestureDetector) {
                    mapboxMap.uiSettings.focalPoint?.let { it ->
                        val coordinate = mapboxMap.projection.fromScreenLocation(it)

                        deleteBlueCircleIfExists()
                        addBlueCircle(coordinate)
                    }
                }
            })

            mapboxMap.addOnShoveListener(object: MapboxMap.OnShoveListener {
                override fun onShoveBegin(detector: ShoveGestureDetector) {

                }

                override fun onShove(detector: ShoveGestureDetector) {

                }

                override fun onShoveEnd(detector: ShoveGestureDetector) {
                    mapboxMap.uiSettings.focalPoint?.let { it ->
                        val coordinate = mapboxMap.projection.fromScreenLocation(it)

                        deleteBlueCircleIfExists()
                        addBlueCircle(coordinate)
                    }
                }

            })
        }
    }

    private fun addRedCircle(coordinate: LatLng) {
        val options = CircleOptions()
            .withLatLng(coordinate)
            .withCircleRadius(20F)
            .withCircleColor("#FF0000")

        redCircle = circleManager.create(options)
    }

    private fun deleteRedCircleIfExists() {
        if (redCircle != null) {
            if (circleManager.annotations.containsValue(redCircle)) {
                circleManager.delete(redCircle)
            }
        }
    }

    private fun addBlueCircle(coordinate: LatLng) {
        val options = CircleOptions()
            .withLatLng(coordinate)
            .withCircleRadius(20F)
            .withCircleColor("#0000FF")

        blueCircle = circleManager.create(options)
    }

    private fun deleteBlueCircleIfExists() {
        if (blueCircle != null) {
            if (circleManager.annotations.containsValue(blueCircle)) {
                circleManager.delete(blueCircle)
            }
        }
    }

    // Add the mapView lifecycle to the activity's lifecycle methods
    public override fun onResume() {
        super.onResume()
        binding.mapView.onResume()
    }

    override fun onStart() {
        super.onStart()
        binding.mapView.onStart()
    }

    override fun onStop() {
        super.onStop()
        binding.mapView.onStop()
    }

    public override fun onPause() {
        super.onPause()
        binding.mapView.onPause()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        binding.mapView.onLowMemory()
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.mapView.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        binding.mapView.onSaveInstanceState(outState)
    }
}