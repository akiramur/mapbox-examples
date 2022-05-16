package com.example.simplemapapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.simplemapapp.databinding.ActivityMainBinding
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapInitOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.plugin.locationcomponent.location

import com.example.simplemapapp.databinding.FragmentMapBinding

class MapFragment : Fragment() {

    private lateinit var mapView: MapView
    private lateinit var mapboxMap: MapboxMap
    private lateinit var onMapReady: (MapboxMap) -> Unit

    //private lateinit var locationPermissionHelper: LocationPermissionHelper

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        /*
        mapView = MapView(
            inflater.context,
            MapInitOptions(inflater.context)
        )
        return mapView
         */
        _binding = FragmentMapBinding.inflate(layoutInflater, container, false)
        //mapView = binding.getRoot()
        mapView = binding.root
        return mapView
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mapboxMap = mapView.getMapboxMap()
        if (::onMapReady.isInitialized) {
            onMapReady.invoke(mapboxMap)
        }

        /*
        mapView.location.addOnIndicatorPositionChangedListener() {
            mapboxMap.setCamera(CameraOptions.Builder().center(it).zoom(12.0).build())
        }
        */
        /*
        locationPermissionHelper = LocationPermissionHelper(WeakReference(this))
        locationPermissionHelper.checkPermissions {
            mapboxMap.loadStyleUri(styleId)
        }
         */
    }

    fun getMapAsync(callback: (MapboxMap) -> Unit) = if (::mapboxMap.isInitialized) {
        callback.invoke(mapboxMap)
    } else this.onMapReady = callback

    fun getMapView(): MapView {
        return mapView
    }
}