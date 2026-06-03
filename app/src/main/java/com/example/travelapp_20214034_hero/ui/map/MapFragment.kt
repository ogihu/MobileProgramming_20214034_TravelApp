package com.example.travelapp_20214034_hero.ui.map

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.travelapp_20214034_hero.BuildConfig
import com.example.travelapp_20214034_hero.R
import com.example.travelapp_20214034_hero.data.TravelDbHelper
import com.example.travelapp_20214034_hero.databinding.FragmentMapBinding
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.KakaoMapReadyCallback
import com.kakao.vectormap.LatLng
import com.kakao.vectormap.MapLifeCycleCallback
import com.kakao.vectormap.MapView
import com.kakao.vectormap.camera.CameraUpdateFactory
import com.kakao.vectormap.label.Label
import com.kakao.vectormap.label.LabelLayer
import com.kakao.vectormap.label.LabelOptions
import com.kakao.vectormap.label.LabelStyle
import com.kakao.vectormap.label.LabelStyles
import com.kakao.vectormap.label.LabelTextStyle
import com.kakao.vectormap.label.LabelManager
import com.kakao.vectormap.MapType
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

        if (BuildConfig.KAKAO_NATIVE_APP_KEY.isBlank()) {
            binding.textMapHint.visibility = View.VISIBLE
            binding.mapView.visibility = View.GONE
            return
        }

        binding.textMapHint.visibility = View.GONE
        mapView = binding.mapView
        startKakaoMap()
    }

    private fun startKakaoMap() {
        val view = mapView ?: return
        view.start(
            object : MapLifeCycleCallback() {
                override fun onMapDestroy() {}

                override fun onMapError(error: Exception) {
                    binding.textMapHint.visibility = View.VISIBLE
                    binding.textMapHint.text = getString(R.string.map_auth_error, error.message ?: "")
                    Toast.makeText(requireContext(), R.string.map_load_error, Toast.LENGTH_LONG).show()
                }
            },
            object : KakaoMapReadyCallback() {
                override fun onMapReady(map: KakaoMap) {
                    kakaoMap = map
                    val manager: LabelManager = map.labelManager
                    textLabelStyles = manager.addLabelStyles(
                        LabelStyles.from(
                            LabelStyle.from().setTextStyles(
                                LabelTextStyle.from(28, Color.BLACK)
                            )
                        )
                    )
                    labelLayer = manager.layer

                    map.setOnLabelClickListener { _, _, label ->
                        val title = label.texts?.firstOrNull().orEmpty()
                        if (title.isNotEmpty()) {
                            Toast.makeText(requireContext(), title, Toast.LENGTH_SHORT).show()
                        }
                        false
                    }

                    loadMarkers()
                }

                override fun getPosition(): LatLng {
                    return LatLng.from(36.5, 127.5)
                }

                override fun getZoomLevel(): Int = 7
            }
        )
    }

    fun loadMarkers() {
        val map = kakaoMap ?: return
        val layer = labelLayer ?: return
        val styles = textLabelStyles ?: return

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val travels = withContext(Dispatchers.IO) {
                    TravelDbHelper(requireContext()).getTravelsWithLocation()
                }

                markerPositions.clear()
                activeLabels.forEach { it.remove() }
                activeLabels.clear()

                travels.forEach { item ->
                    val lat = item.latitude ?: return@forEach
                    val lng = item.longitude ?: return@forEach
                    val position = LatLng.from(lat, lng)
                    markerPositions.add(position)
                    val label = layer.addLabel(
                        LabelOptions.from(position)
                            .setStyles(styles)
                            .setTexts(item.place)
                    )
                    activeLabels.add(label)
                }

                if (markerPositions.isEmpty()) {
                    Toast.makeText(requireContext(), R.string.map_no_markers, Toast.LENGTH_LONG).show()
                    map.moveCamera(CameraUpdateFactory.newCenterPosition(LatLng.from(36.5, 127.5), 7))
                    return@launch
                }

                moveCameraToMarkers(map)
            } catch (_: Exception) {
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
            loadMarkers()
            return
        }
        moveCameraToMarkers(map)
    }

    override fun onResume() {
        super.onResume()
        mapView?.resume()
        kakaoMap?.let { loadMarkers() }
    }

    override fun onPause() {
        mapView?.pause()
        super.onPause()
    }

    override fun onDestroyView() {
        mapView?.pause()
        kakaoMap = null
        labelLayer = null
        textLabelStyles = null
        markerPositions.clear()
        activeLabels.clear()
        mapView = null
        super.onDestroyView()
        _binding = null
    }
}
