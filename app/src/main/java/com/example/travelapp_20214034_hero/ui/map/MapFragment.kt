package com.example.travelapp_20214034_hero.ui.map

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.travelapp_20214034_hero.BuildConfig
import com.example.travelapp_20214034_hero.R
import android.content.Intent
import com.example.travelapp_20214034_hero.common.KakaoMapInitializer
import com.example.travelapp_20214034_hero.common.TravelExtras
import com.example.travelapp_20214034_hero.data.TravelDbHelper
import com.example.travelapp_20214034_hero.data.TravelItem
import com.example.travelapp_20214034_hero.databinding.FragmentMapBinding
import com.example.travelapp_20214034_hero.ui.detail.DetailActivity
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.KakaoMapReadyCallback
import com.kakao.vectormap.LatLng
import com.kakao.vectormap.MapLifeCycleCallback
import com.kakao.vectormap.MapType
import com.kakao.vectormap.MapView
import com.kakao.vectormap.camera.CameraUpdateFactory
import com.kakao.vectormap.label.Label
import com.kakao.vectormap.label.LabelLayer
import com.kakao.vectormap.label.LabelOptions
import com.kakao.vectormap.label.LabelStyle
import com.kakao.vectormap.label.LabelStyles
import com.kakao.vectormap.label.LabelTextBuilder
import com.kakao.vectormap.label.LabelTextStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MapFragment : Fragment() {

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!

    private var mapView: MapView? = null
    private var kakaoMap: KakaoMap? = null
    private var labelLayer: LabelLayer? = null
    private var textLabelStyles: LabelStyles? = null
    private var mapStartRequested = false

    private val markerPositions = mutableListOf<LatLng>()
    private val activeLabels = mutableListOf<Label>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mapView = binding.mapView

        if (BuildConfig.KAKAO_NATIVE_APP_KEY.isBlank()) {
            binding.textMapHint.visibility = View.VISIBLE
            binding.textMapHint.text = getString(R.string.map_kakao_setup_hint)
            binding.mapView.visibility = View.GONE
            return
        }

        binding.textMapHint.visibility = View.GONE
        binding.mapView.visibility = View.VISIBLE
    }

    override fun onResume() {
        super.onResume()
        if (BuildConfig.KAKAO_NATIVE_APP_KEY.isBlank()) return

        val view = mapView ?: return
        view.resume()
        if (!mapStartRequested) {
            mapStartRequested = true
            startKakaoMap()
        } else if (kakaoMap != null && pendingMarkerRefresh) {
            pendingMarkerRefresh = false
            loadMarkers(showEmptyHints = false)
        }
    }

    override fun onPause() {
        mapView?.pause()
        super.onPause()
    }

    private fun showMapAuthError(message: String?) {
        if (_binding == null) return
        binding.textMapHint.visibility = View.VISIBLE
        binding.textMapHint.text = getString(R.string.map_auth_error, message ?: "")
        binding.mapView.visibility = View.VISIBLE
    }

    private fun startKakaoMap() {
        val view = mapView ?: return
        if (!KakaoMapInitializer.ensureInitialized(requireContext())) {
            showMapAuthError(getString(R.string.map_kakao_setup_hint))
            return
        }
        try {
            view.start(
                object : MapLifeCycleCallback() {
                    override fun onMapDestroy() {
                        kakaoMap = null
                        labelLayer = null
                        textLabelStyles = null
                    }

                    override fun onMapError(error: Exception) {
                        Log.e(TAG, "Kakao map error", error)
                        showMapAuthError(error.message)
                        Toast.makeText(requireContext(), R.string.map_load_error, Toast.LENGTH_LONG).show()
                    }
                },
                object : KakaoMapReadyCallback() {
                    override fun onMapReady(map: KakaoMap) {
                        kakaoMap = map
                        setupLabelStyles(map)
                        map.setOnLabelClickListener { _, _, label ->
                            val travelId = label.tag as? Long
                            if (travelId != null && travelId > 0) {
                                startActivity(
                                    Intent(requireContext(), DetailActivity::class.java).apply {
                                        putExtra(TravelExtras.EXTRA_TRAVEL_ID, travelId)
                                    }
                                )
                                return@setOnLabelClickListener true
                            }
                            val title = label.texts?.firstOrNull().orEmpty()
                            if (title.isNotEmpty()) {
                                Toast.makeText(requireContext(), title, Toast.LENGTH_SHORT).show()
                            }
                            false
                        }
                        if (_binding != null) {
                            binding.textMapHint.visibility = View.GONE
                        }
                        showDefaultMapArea(map)
                        loadMarkers()
                    }

                    override fun getPosition(): LatLng = LatLng.from(36.5, 127.5)

                    override fun getZoomLevel(): Int = 7

                    override fun isVisible(): Boolean = true
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Kakao map start failed", e)
            showMapAuthError(e.message)
        }
    }

    private fun setupLabelStyles(map: KakaoMap) {
        try {
            val manager = map.labelManager ?: return
            textLabelStyles = manager.addLabelStyles(
                LabelStyles.from(
                    LabelStyle.from(LabelTextStyle.from(28, Color.BLACK))
                )
            )
            labelLayer = manager.layer
        } catch (e: Exception) {
            Log.w(TAG, "Label setup skipped", e)
        }
    }

    private fun addMarkerForTravel(item: TravelItem, layer: LabelLayer, styles: LabelStyles) {
        val lat = item.latitude ?: return
        val lng = item.longitude ?: return
        val position = LatLng.from(lat, lng)
        markerPositions.add(position)
        val label = layer.addLabel(
            LabelOptions.from(position)
                .setStyles(styles)
                .setTexts(LabelTextBuilder().setTexts(item.place))
                .setTag(item.id)
        )
        activeLabels.add(label)
    }

    private fun showDefaultMapArea(map: KakaoMap) {
        map.moveCamera(
            CameraUpdateFactory.newCenterPosition(LatLng.from(36.5, 127.5), 7)
        )
    }

    fun loadMarkers(showEmptyHints: Boolean = true) {
        val map = kakaoMap ?: return
        val layer = labelLayer
        val styles = textLabelStyles

        viewLifecycleOwner.lifecycleScope.launch {
            if (!isAdded) return@launch
            try {
                val travels = withContext(Dispatchers.IO) {
                    TravelDbHelper.getInstance(requireContext()).getTravelsWithLocation()
                }

                markerPositions.clear()
                activeLabels.forEach { it.remove() }
                activeLabels.clear()

                if (layer != null && styles != null) {
                    travels.forEach { item ->
                        addMarkerForTravel(item, layer, styles)
                    }
                }

                if (markerPositions.isEmpty()) {
                    showDefaultMapArea(map)
                    if (showEmptyHints) {
                        when {
                            travels.isEmpty() -> Toast.makeText(
                                requireContext(),
                                R.string.map_no_markers,
                                Toast.LENGTH_LONG
                            ).show()
                            layer == null || styles == null -> Toast.makeText(
                                requireContext(),
                                R.string.map_markers_setup_failed,
                                Toast.LENGTH_SHORT
                            ).show()
                            else -> Toast.makeText(
                                requireContext(),
                                R.string.map_no_location_data,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                    return@launch
                }

                moveCameraToMarkers(map)
            } catch (e: Exception) {
                Log.e(TAG, "loadMarkers failed", e)
                Toast.makeText(requireContext(), R.string.map_load_error, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun moveCameraToMarkers(map: KakaoMap) {
        when (markerPositions.size) {
            1 -> map.moveCamera(
                CameraUpdateFactory.newCenterPosition(markerPositions.first(), 12)
            )
            else -> {
                val avgLat = markerPositions.map { it.latitude }.average()
                val avgLng = markerPositions.map { it.longitude }.average()
                map.moveCamera(
                    CameraUpdateFactory.newCenterPosition(LatLng.from(avgLat, avgLng), 9)
                )
            }
        }
    }

    fun setMapTypeNormal() {
        kakaoMap?.changeMapType(MapType.NORMAL)
    }

    fun setMapTypeSatellite() {
        kakaoMap?.changeMapType(MapType.SKYVIEW)
    }

    fun fitAllMarkers() {
        val map = kakaoMap ?: return
        if (markerPositions.isEmpty()) {
            loadMarkers(showEmptyHints = true)
            return
        }
        moveCameraToMarkers(map)
    }

    override fun onDestroyView() {
        try {
            mapView?.finish()
        } catch (e: Exception) {
            Log.w(TAG, "MapView.finish skipped", e)
        }
        mapView?.pause()
        mapStartRequested = false
        kakaoMap = null
        labelLayer = null
        textLabelStyles = null
        markerPositions.clear()
        activeLabels.clear()
        mapView = null
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val TAG = "MapFragment"

        @Volatile
        var pendingMarkerRefresh = false

        fun markMarkersStale() {
            pendingMarkerRefresh = true
        }
    }
}
