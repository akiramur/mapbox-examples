package com.example.simplemapapp

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.simplemapapp.databinding.ActivityOfflineBinding
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.bindgen.Value
import com.mapbox.common.Cancelable
import com.mapbox.common.MapboxOptions
import com.mapbox.common.NetworkRestriction
import com.mapbox.common.OfflineSwitch
import com.mapbox.common.TileRegionLoadOptions
import com.mapbox.common.TileStore
import com.mapbox.common.TilesetDescriptor
import com.mapbox.geojson.BoundingBox
import com.mapbox.geojson.Feature
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.GlyphsRasterizationMode
import com.mapbox.maps.MapInitOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.MapboxMapsOptions
import com.mapbox.maps.OfflineManager
import com.mapbox.maps.Style
import com.mapbox.maps.StylePackLoadOptions
import com.mapbox.maps.TileStoreUsageMode
import com.mapbox.maps.TilesetDescriptorOptions
import com.mapbox.maps.mapsOptions
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.CircleAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createCircleAnnotationManager
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.navigation.base.extensions.applyDefaultNavigationOptions
import com.mapbox.navigation.base.extensions.applyLanguageAndVoiceUnitOptions
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.options.RoutingTilesOptions
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.NavigationRouterCallback
import com.mapbox.navigation.base.route.RouterFailure
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.core.lifecycle.requireMapboxNavigation
import com.mapbox.turf.TurfMeasurement
import org.intellij.lang.annotations.Language


/**
 * Example app that shows how to use OfflineManager and TileStore to
 * download regions for offline use.
 *
 * By default, users may download up to 250MB of data for offline
 * use without incurring additional charges. This limit is subject
 * to change during the beta.
 */
class OfflineActivity : AppCompatActivity() {
    private val style = Style.MAPBOX_STREETS

    private val tileStore: TileStore by lazy { TileStore.create() }
    private val offlineManager: OfflineManager by lazy {
        // Set application-scoped tile store so that all MapViews created from now on will apply these
        // settings.
        MapboxOptions.mapsOptions.tileStore = tileStore
        MapboxOptions.mapsOptions.tileStoreUsageMode = TileStoreUsageMode.READ_ONLY
        OfflineManager().also {
            // Revert setting custom tile store
            MapboxOptions.mapsOptions.tileStore = null
        }
    }
    private val offlineLogsAdapter: OfflineLogsAdapter by lazy {
        OfflineLogsAdapter()
    }
    private var mapView: MapView? = null
    private lateinit var handler: Handler
    private lateinit var binding: ActivityOfflineBinding
    private var stylePackCancelable: Cancelable? = null
    private var tilePackCancelable: Cancelable? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOfflineBinding.inflate(layoutInflater)
        setContentView(binding.root)
        handler = Handler(Looper.getMainLooper())

        // Initialize a logger that writes into the recycler view
        binding.recycler.layoutManager = LinearLayoutManager(this)
        binding.recycler.adapter = offlineLogsAdapter

        prepareDownloadButton()

        mapView = MapView(
            this,
            MapInitOptions(context = this, styleUri = BLANK_STYLE)
        )

        binding.container.addView(mapView)

//        updateButton2() {
//            toggleMapboxStackConnected()
//        }
    }

    private fun prepareDownloadButton() {
        updateButton("DOWNLOAD") {
            downloadOfflineRegion()
        }
    }

    private fun prepareCancelButton() {
        updateButton("CANCEL DOWNLOAD") {
            stylePackCancelable?.cancel()
            tilePackCancelable?.cancel()
            prepareDownloadButton()
        }
    }

    private fun prepareViewMapButton() {
        // Disable network stack, so that the map can only load from downloaded region.
        OfflineSwitch.getInstance().isMapboxStackConnected = false
        //binding.button2.text = "Connected: ${OfflineSwitch.getInstance().isMapboxStackConnected}"

        logInfoMessage("Mapbox network stack disabled.")
        handler.post {
            updateButton("VIEW MAP") {
                // create a Mapbox MapView

                // Note that the MapView must be initialised with the same TileStore that is used to create
                // the tile regions. (i.e. the tileStorePath must be consistent).

                // If user did not assign the tile store path specifically during the tile region download
                // and the map initialisation period, the default tile store path will be used and
                // no extra action is needed.
                mapView?.run {
                    mapboxMap.loadStyle(style) { _ ->
                        mapboxMap.setCamera(CameraOptions.Builder().zoom(ZOOM).center(SANFRANCISCO1).build())
                        // Add a circle annotation to the offline geometry.
                        annotations.createCircleAnnotationManager().create(
                            CircleAnnotationOptions()
                                .withPoint(SANFRANCISCO1)
                                .withCircleColor(Color.RED)
                        )

                        mapView?.gestures?.addOnMapLongClickListener { _ ->
                            findRoute()
                            true
                        }
                    }
                }
                prepareShowDownloadedRegionButton()
            }
        }
    }

    private fun prepareShowDownloadedRegionButton() {
        updateButton("SHOW DOWNLOADED REGIONS") {
            showDownloadedRegions()
            prepareRemoveOfflineRegionButton()
        }
    }

    private fun prepareRemoveOfflineRegionButton() {
        updateButton("REMOVE DOWNLOADED REGIONS") {
            removeOfflineRegions()
            showDownloadedRegions()
            binding.container.removeAllViews()

            // Re-enable the Mapbox network stack, so that the new offline region download can succeed.
            OfflineSwitch.getInstance().isMapboxStackConnected = true
            //binding.button2.text = "Connected: ${OfflineSwitch.getInstance().isMapboxStackConnected}"

            logInfoMessage("Mapbox network stack enabled.")

            prepareDownloadButton()
        }
    }

    private fun updateButton(text: String, listener: View.OnClickListener) {
        binding.button.text = text
        binding.button.setOnClickListener(listener)
    }

//    private fun updateButton2(listener: View.OnClickListener) {
//        binding.button2.setOnClickListener(listener)
//    }

//    private fun toggleMapboxStackConnected() {
//        val isMapboxStackConnected = OfflineSwitch.getInstance().isMapboxStackConnected
//        OfflineSwitch.getInstance().isMapboxStackConnected = !isMapboxStackConnected
//        binding.button2.text = "Connected: ${OfflineSwitch.getInstance().isMapboxStackConnected}"
//    }

    private fun downloadOfflineRegion() {
        // By default, users may download up to 250MB of data for offline use without incurring
        // additional charges. This limit is subject to change during the beta.

        // - - - - - - - -

        // 1. Create style package with loadStylePack() call.

        // A style pack (a Style offline package) contains the loaded style and its resources: loaded
        // sources, fonts, sprites. Style packs are identified with their style URI.

        // Style packs are stored in the disk cache database, but their resources are not subject to
        // the data eviction algorithm and are not considered when calculating the disk cache size.
        stylePackCancelable = offlineManager.loadStylePack(
            style,
            // Build Style pack load options
            StylePackLoadOptions.Builder()
                .glyphsRasterizationMode(GlyphsRasterizationMode.IDEOGRAPHS_RASTERIZED_LOCALLY)
                .metadata(Value(STYLE_PACK_METADATA))
                .build(),
            { progress ->
                // Update the download progress to UI
                updateStylePackDownloadProgress(
                    progress.completedResourceCount,
                    progress.requiredResourceCount,
                    "StylePackLoadProgress: $progress"
                )
            },
            { expected ->
                if (expected.isValue) {
                    expected.value?.let { stylePack ->
                        // Style pack download finishes successfully
                        logSuccessMessage("StylePack downloaded: $stylePack")
                        if (binding.tilePackDownloadProgress.progress == binding.tilePackDownloadProgress.max) {
                            prepareViewMapButton()
                        } else {
                            logInfoMessage("Waiting for tile region download to be finished.")
                        }
                    }
                }
                expected.error?.let {
                    // Handle error occurred during the style pack download.
                    logErrorMessage("StylePackError: $it")
                }
            }
        )

        // - - - - - - - -

        // 2. Create a tile region with tiles for the satellite street style

        // A Tile Region represents an identifiable geographic tile region with metadata, consisting of
        // a set of tiles packs that cover a given area (a polygon). Tile Regions allow caching tiles
        // packs in an explicit way: By creating a Tile Region, developers can ensure that all tiles in
        // that region will be downloaded and remain cached until explicitly deleted.

        // Creating a Tile Region requires supplying a description of the area geometry, the tilesets
        // and zoom ranges of the tiles within the region.

        // The tileset descriptor encapsulates the tile-specific data, such as which tilesets, zoom ranges,
        // pixel ratio etc. the cached tile packs should have. It is passed to the Tile Store along with
        // the region area geometry to load a new Tile Region.

        // The OfflineManager is responsible for creating tileset descriptors for the given style and zoom range.
        val mapsTilesetDescriptor = offlineManager.createTilesetDescriptor(
            TilesetDescriptorOptions.Builder()
                .styleURI(style)
                .pixelRatio(this.resources.displayMetrics.density)
                .minZoom(0)
                .maxZoom(16)
                .build()
        )

        val navTilesetDescriptor = mapboxNavigation.tilesetDescriptorFactory.getLatest()
        val tilesetDescriptors: MutableList<TilesetDescriptor> = ArrayList()
        tilesetDescriptors.add(mapsTilesetDescriptor)
        tilesetDescriptors.add(navTilesetDescriptor)

        // Use the the default TileStore to load this region. You can create custom TileStores are are
        // unique for a particular file path, i.e. there is only ever one TileStore per unique path.

        val boundingBox = BoundingBox.fromPoints(
            Point.fromLngLat(-122.6, 37.9), Point.fromLngLat(-122.3, 37.5)
        )
        val bboxPolygon: Feature = TurfMeasurement.bboxPolygon(boundingBox)

        // Note that the TileStore path must be the same with the TileStore used when initialise the MapView.
        tilePackCancelable = tileStore.loadTileRegion(
            TILE_REGION_ID,
            TileRegionLoadOptions.Builder()
                .geometry(SANFRANCISCO1)
                //.geometry(bboxPolygon.geometry())
                .descriptors(tilesetDescriptors)
                .metadata(Value(TILE_REGION_METADATA))
                .acceptExpired(true)
                .networkRestriction(NetworkRestriction.NONE)
                .build(),
            { progress ->
                updateTileRegionDownloadProgress(
                    progress.completedResourceCount,
                    progress.requiredResourceCount,
                    "TileRegionLoadProgress: $progress"
                )
            }
        ) { expected ->
            if (expected.isValue) {
                // Tile pack download finishes successfully
                expected.value?.let { region ->
                    logSuccessMessage("TileRegion downloaded: $region")
                    if (binding.stylePackDownloadProgress.progress == binding.stylePackDownloadProgress.max) {
                        prepareViewMapButton()
                    } else {
                        logInfoMessage("Waiting for style pack download to be finished.")
                    }
                }
            }
            expected.error?.let {
                // Handle error occurred during the tile region download.
                logErrorMessage("TileRegionError: $it")
            }
        }
        prepareCancelButton()
    }

    private fun showDownloadedRegions() {
        // Get a list of tile regions that are currently available.
        tileStore.getAllTileRegions { expected ->
            if (expected.isValue) {
                expected.value?.let { tileRegionList ->
                    logInfoMessage("Existing tile regions: $tileRegionList")
                }
            }
            expected.error?.let { tileRegionError ->
                logErrorMessage("TileRegionError: $tileRegionError")
            }
        }
        // Get a list of style packs that are currently available.
        offlineManager.getAllStylePacks { expected ->
            if (expected.isValue) {
                expected.value?.let { stylePackList ->
                    logInfoMessage("Existing style packs: $stylePackList")
                }
            }
            expected.error?.let { stylePackError ->
                logErrorMessage("StylePackError: $stylePackError")
            }
        }
    }

    private fun removeOfflineRegions() {
        // Remove the tile region with the tile region ID.
        // Note this will not remove the downloaded tile packs, instead, it will just mark the tileset
        // not a part of a tile region. The tiles still exists as a predictive cache in TileStore.
        tileStore.removeTileRegion(TILE_REGION_ID)

        // Remove the style pack with the style url.
        // Note this will not remove the downloaded style pack, instead, it will just mark the resources
        // not a part of the existing style pack. The resources still exists as disk cache.
        offlineManager.removeStylePack(style)

        MapboxMap.clearData {
            it.error?.let { error ->
                logErrorMessage(error)
            }
        }

        // Explicitly clear ambient cache data (so that if we try to download tile store regions again - it would actually truly download it from network).
        // Ambient cache data is anything not associated with an offline region or a style pack, including predictively cached data.
        // Note that it is advisable to rely on internal TileStore implementation to clear cache when needed.
        tileStore.clearAmbientCache {
            it.error?.let { error ->
                logErrorMessage(error.message)
            }
        }

        // Reset progressbar.
        updateStylePackDownloadProgress(0, 0)
        updateTileRegionDownloadProgress(0, 0)
    }

    private fun updateStylePackDownloadProgress(progress: Long, max: Long, message: String? = null) {
        binding.stylePackDownloadProgress.max = max.toInt()
        binding.stylePackDownloadProgress.progress = progress.toInt()
        message?.let {
            offlineLogsAdapter.addLog(OfflineLog.StylePackProgress(it))
        }
    }

    private fun updateTileRegionDownloadProgress(progress: Long, max: Long, message: String? = null) {
        binding.tilePackDownloadProgress.max = max.toInt()
        binding.tilePackDownloadProgress.progress = progress.toInt()
        message?.let {
            offlineLogsAdapter.addLog(OfflineLog.TilePackProgress(it))
        }
    }

    private fun logInfoMessage(message: String) {
        offlineLogsAdapter.addLog(OfflineLog.Info(message))
    }

    private fun logErrorMessage(message: String) {
        offlineLogsAdapter.addLog(OfflineLog.Error(message))
    }

    private fun logSuccessMessage(message: String) {
        offlineLogsAdapter.addLog(OfflineLog.Success(message))
    }

    override fun onStart() {
        super.onStart()
        mapView?.onStart()
    }

    override fun onStop() {
        super.onStop()
        mapView?.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Cancel the current downloading jobs
        stylePackCancelable?.cancel()
        tilePackCancelable?.cancel()
        // Remove downloaded style packs and tile regions.
        removeOfflineRegions()
        // Bring back the network connectivity when exiting the OfflineActivity.
        OfflineSwitch.getInstance().isMapboxStackConnected = true
        mapView?.onDestroy()
    }

    private class OfflineLogsAdapter : RecyclerView.Adapter<OfflineLogsAdapter.ViewHolder>() {
        private var isUpdating: Boolean = false
        private val updateHandler = Handler(Looper.getMainLooper())
        private val logs = ArrayList<OfflineLog>()

        @SuppressLint("NotifyDataSetChanged")
        private val updateRunnable = Runnable {
            notifyDataSetChanged()
            isUpdating = false
        }

        class ViewHolder internal constructor(view: View) : RecyclerView.ViewHolder(view) {
            internal var alertMessageTv: TextView = view.findViewById(R.id.alert_message)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view =
                LayoutInflater.from(parent.context).inflate(R.layout.item_gesture_alert, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val alert = logs[position]
            holder.alertMessageTv.text = alert.message
            holder.alertMessageTv.setTextColor(
                ContextCompat.getColor(holder.alertMessageTv.context, alert.color)
            )
        }

        override fun getItemCount(): Int {
            return logs.size
        }

        fun addLog(alert: OfflineLog) {
            when (alert) {
                is OfflineLog.Error -> Log.e(TAG, alert.message)
                else -> Log.d(TAG, alert.message)
            }
            logs.add(0, alert)
            if (!isUpdating) {
                isUpdating = true
                updateHandler.postDelayed(updateRunnable, 250)
            }
        }
    }

    private sealed class OfflineLog(val message: String, val color: Int) {
        class Info(message: String) : OfflineLog(message, android.R.color.black)
        class Error(message: String) : OfflineLog(message, android.R.color.holo_red_dark)
        class Success(message: String) : OfflineLog(message, android.R.color.holo_green_dark)
        class TilePackProgress(message: String) : OfflineLog(message, android.R.color.holo_purple)
        class StylePackProgress(message: String) : OfflineLog(message, android.R.color.holo_orange_dark)
    }

    // Navigation SDK
    private fun initNavigation() {
        val routingTilesOptions= RoutingTilesOptions.Builder()
            .tileStore(tileStore)
            .build()

        MapboxNavigationApp.setup(
            NavigationOptions.Builder(this)
                .routingTilesOptions(routingTilesOptions)
                .build()
        )

        MapboxMapsOptions.tileStore = tileStore
    }

    private val mapboxNavigation: MapboxNavigation by requireMapboxNavigation(
        onResumedObserver = object : MapboxNavigationObserver {
            @SuppressLint("MissingPermission")
            override fun onAttached(mapboxNavigation: MapboxNavigation) {

            }

            override fun onDetached(mapboxNavigation: MapboxNavigation) {

            }
        },
        onInitialize = this::initNavigation
    )

    private fun findRoute() {
        // execute a route request
        // it's recommended to use the
        // applyDefaultNavigationOptions and applyLanguageAndVoiceUnitOptions
        // that make sure the route request is optimized
        // to allow for support of all of the Navigation SDK features
        mapboxNavigation.requestRoutes(
            RouteOptions.builder()
                .applyDefaultNavigationOptions()
                .applyLanguageAndVoiceUnitOptions(this)
                .coordinatesList(listOf(SANFRANCISCO1, SANFRANCISCO2))
                .build(),
            object : NavigationRouterCallback {
                override fun onCanceled(routeOptions: RouteOptions, routerOrigin: String) {
                    // no impl
                    logInfoMessage("onCanceled")
                }

                override fun onFailure(reasons: List<RouterFailure>, routeOptions: RouteOptions) {
                    // no impl
                    logInfoMessage("onFailure reasons: $reasons")
                }

                override fun onRoutesReady(
                    routes: List<NavigationRoute>,
                    routerOrigin: String
                ) {
                    //setRouteAndStartNavigation(routes)
                    logInfoMessage("onRoutesReady routes: $routes")
                }
            }
        )
    }

    companion object {
        private const val TAG = "OfflineActivity"
        private const val ZOOM = 12.0
        val SANFRANCISCO1 = Point.fromLngLat(-122.4151361718713,37.763851086703504)
        val SANFRANCISCO2 = Point.fromLngLat(-122.41533643354823,37.76578582012118)
        private const val TILE_REGION_ID = "myTileRegion"
        private const val STYLE_PACK_METADATA = "my-satellite-street-style-pack"
        private const val TILE_REGION_METADATA = "my-satellite-street-region"
        @Language("JSON")
        private const val BLANK_STYLE = """
{
  "layers": [
    {
      "id": "background",
      "type": "background",
      "paint": {"background-color": "hsl(100, 50%, 50%)"}
    }
  ]
}
"""
    }
}