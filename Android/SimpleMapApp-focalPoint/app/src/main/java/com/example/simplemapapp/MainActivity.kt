package com.example.simplemapapp

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.GestureDetector
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.view.doOnLayout
import com.example.simplemapapp.databinding.ActivityMainBinding
import com.mapbox.android.gestures.MoveGestureDetector
import com.mapbox.android.gestures.RotateGestureDetector
import com.mapbox.android.gestures.ShoveGestureDetector
import com.mapbox.android.gestures.StandardScaleGestureDetector
import com.mapbox.geojson.Point
import com.mapbox.maps.*
import com.mapbox.maps.extension.observable.eventdata.CameraChangedEventData
import com.mapbox.maps.extension.style.expressions.dsl.generated.interpolate
import com.mapbox.maps.plugin.LocationPuck2D
import com.mapbox.maps.plugin.animation.*
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.CircleAnnotation
import com.mapbox.maps.plugin.annotation.generated.CircleAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.CircleAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createCircleAnnotationManager
import com.mapbox.maps.plugin.delegates.listeners.OnCameraChangeListener
import com.mapbox.maps.plugin.gestures.*
import com.mapbox.maps.plugin.locationcomponent.location
import java.lang.ref.WeakReference

class MainActivity : AppCompatActivity() {

    private val TAG = "MainActivity"

    private lateinit var mapView: MapView
    private lateinit var mapboxMap: MapboxMap
    private lateinit var cameraAnimationsPlugin: CameraAnimationsPlugin

    private lateinit var gesturesPlugin: GesturesPlugin
    private lateinit var locationPermissionHelper: LocationPermissionHelper
    private var lastStyleUri = Style.DARK
    private lateinit var binding: ActivityMainBinding

    private var circleAnnotationManager: CircleAnnotationManager? = null
    private var redCircleAnnotation: CircleAnnotation? = null
    private var blueCircleAnnotation: CircleAnnotation? = null

    private lateinit var onCameraChangeListener: OnCameraChangeListener

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mapView = binding.mapView
        mapboxMap = binding.mapView.getMapboxMap()
        cameraAnimationsPlugin = binding.mapView.camera

        locationPermissionHelper = LocationPermissionHelper(WeakReference(this))
        locationPermissionHelper.checkPermissions {
            onMapReady()
        }

        // Create an instance of the Annotation API and get the CircleAnnotationManager.
        circleAnnotationManager = mapView?.annotations.createCircleAnnotationManager()

        /*
        mapView.doOnLayout {
            val centerX = it.width / 2.0
            val centerY = it.height / 2.0
            mapView.gestures.updateSettings {
                focalPoint = ScreenCoordinate(centerX, centerY)
            }
        }
        */
    }

    private fun onMapReady() {
        mapboxMap.loadStyleUri(
            Style.MAPBOX_STREETS
        ) {
            lastStyleUri = it.styleURI

            /*
            mapView.location.addOnIndicatorPositionChangedListener {
                mapView.getMapboxMap().setCamera(CameraOptions.Builder().center(it).build())
                //mapView.gestures.focalPoint = mapView.getMapboxMap().pixelForCoordinate(it)
            }
            */
            val options = CameraOptions.Builder()
                .center(Point.fromLngLat(139.6503, 35.6762))
                .build()
            mapView.getMapboxMap().setCamera(options)

            mapView.gestures.addOnMapClickListener(object : OnMapClickListener {

                override fun onMapClick(point: Point): Boolean {
                    print("map click")
                    return true
                }
            })

            mapView.gestures.addOnMapLongClickListener {
                print("map long click")
                mapView.gestures.focalPoint = mapView.getMapboxMap().pixelForCoordinate(it)

                deleteRedCircleIfExists()
                addRedCircleAnnotation(it.latitude(), it.longitude())
                return@addOnMapLongClickListener true
            }

            mapView.gestures.addOnRotateListener(object: OnRotateListener {
                override fun onRotate(detector: RotateGestureDetector) {
                    print("onRotate")
                }

                override fun onRotateBegin(detector: RotateGestureDetector) {
                    print("onRotateBegin")
                }

                override fun onRotateEnd(detector: RotateGestureDetector) {
                    mapView.gestures.focalPoint?.let { it ->
                        val coordinate = mapView.getMapboxMap().coordinateForPixel(it)

                        deleteBlueCircleIfExists()
                        addBlueCircleAnnotation(coordinate.latitude(), coordinate.longitude())
                        print("onRotateEnd")
                    };
                }
            })

            mapView.gestures.addOnMoveListener(object : OnMoveListener {
                override fun onMoveBegin(detector: MoveGestureDetector) {
                    // user started moving the map
                }

                override fun onMove(detector: MoveGestureDetector): Boolean {
                    // user is moving the map
                    return false
                }

                override fun onMoveEnd(detector: MoveGestureDetector) {
                    // user stopped moving the map
                    mapView.gestures.focalPoint?.let { it ->
                        val coordinate = mapView.getMapboxMap().coordinateForPixel(it)

                        deleteBlueCircleIfExists()
                        addBlueCircleAnnotation(coordinate.latitude(), coordinate.longitude())
                        print("onRotateEnd")
                    };
                }
            })

            mapView.gestures.addOnScaleListener(object : OnScaleListener {
                override fun onScaleBegin(detector: StandardScaleGestureDetector) {
                    print("scale begin")
                }
                override fun onScale(detector: StandardScaleGestureDetector) {
                    print("scale")
                }

                override fun onScaleEnd(detector: StandardScaleGestureDetector) {
                    mapView.gestures.focalPoint?.let { it ->
                        val coordinate = mapView.getMapboxMap().coordinateForPixel(it)

                        deleteBlueCircleIfExists()
                        addBlueCircleAnnotation(coordinate.latitude(), coordinate.longitude())
                        print("onRotateEnd")
                    };
                }
            })

            mapView.gestures.addOnShoveListener(object: OnShoveListener {
                override fun onShove(detector: ShoveGestureDetector) {

                }

                override fun onShoveBegin(detector: ShoveGestureDetector) {

                }

                override fun onShoveEnd(detector: ShoveGestureDetector) {
                    mapView.gestures.focalPoint?.let { it ->
                        val coordinate = mapView.getMapboxMap().coordinateForPixel(it)

                        deleteBlueCircleIfExists()
                        addBlueCircleAnnotation(coordinate.latitude(), coordinate.longitude())
                        print("onRotateEnd")
                    };
                }

            })

        }
    }

    private fun addRedCircleAnnotation(lat: Double, lon: Double) {

        // Set options for the resulting circle layer.
        val circleAnnotationOptions: CircleAnnotationOptions = CircleAnnotationOptions()
            // Define a geographic coordinate.
            //.withPoint(Point.fromLngLat(18.06, 59.31))
            .withPoint(Point.fromLngLat(lon, lat))
            // Style the circle that will be added to the map.
            .withCircleRadius(20.0)
            .withCircleColor("#ee4e8b")
            //.withCircleStrokeWidth(2.0)
            //.withCircleStrokeColor("#ffffff")

        // Add the resulting circle to the map.
        redCircleAnnotation = circleAnnotationManager?.create(circleAnnotationOptions)
    }

    private fun deleteRedCircleIfExists() {
        redCircleAnnotation?.let { it ->
            circleAnnotationManager?.delete(it)
        }
    }

    private fun addBlueCircleAnnotation(lat: Double, lon: Double) {
        // Set options for the resulting circle layer.
        val circleAnnotationOptions: CircleAnnotationOptions = CircleAnnotationOptions()
            // Define a geographic coordinate.
            //.withPoint(Point.fromLngLat(18.06, 59.31))
            .withPoint(Point.fromLngLat(lon, lat))
            // Style the circle that will be added to the map.
            .withCircleRadius(20.0)
            .withCircleColor("#0000ee")
            //.withCircleStrokeWidth(2.0)
            //.withCircleStrokeColor("#ffffff")

        // Add the resulting circle to the map.
        blueCircleAnnotation = circleAnnotationManager?.create(circleAnnotationOptions)
    }

    private fun deleteBlueCircleIfExists() {
        blueCircleAnnotation?.let { it ->
            circleAnnotationManager?.delete(it)
        }
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