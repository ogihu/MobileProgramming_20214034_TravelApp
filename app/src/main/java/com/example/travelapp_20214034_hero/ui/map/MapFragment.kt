package com.example.travelapp_20214034_hero.ui.map

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.travelapp_20214034_hero.R
import com.example.travelapp_20214034_hero.data.TravelDbHelper
import com.example.travelapp_20214034_hero.databinding.FragmentMapBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MapFragment : Fragment(), OnMapReadyCallback {

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!

    private var googleMap: GoogleMap? = null
    private var lastMarkerPoints: List<LatLng> = emptyList()

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        if (grants.values.any { it }) {
            googleMap?.let { enableMyLocation(it) }
        }
    }

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
        val mapFragment =
            childFragmentManager.findFragmentById(R.id.mapContainer) as? SupportMapFragment
        mapFragment?.getMapAsync(this)
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        binding.textMapHint.visibility = View.GONE

        map.uiSettings.isZoomControlsEnabled = true
        map.uiSettings.isScrollGesturesEnabled = true
        map.uiSettings.isRotateGesturesEnabled = true

        map.setOnMarkerClickListener { marker ->
            Toast.makeText(requireContext(), marker.title ?: "", Toast.LENGTH_SHORT).show()
            false
        }

        requestLocationAndEnable(map)
        loadMarkers()
    }

    private fun requestLocationAndEnable(map: GoogleMap) {
        when {
            hasLocationPermission() -> enableMyLocation(map)
            else -> locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    private fun enableMyLocation(map: GoogleMap) {
        if (!hasLocationPermission()) return
        try {
            map.isMyLocationEnabled = true
        } catch (_: SecurityException) {
            // 권한 직후 타이밍 이슈 방지
        }
    }

    fun loadMarkers() {
        val map = googleMap ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val travels = withContext(Dispatchers.IO) {
                    TravelDbHelper(requireContext()).getTravelsWithLocation()
                }
                map.clear()
                val points = travels.mapNotNull { item ->
                    val lat = item.latitude ?: return@mapNotNull null
                    val lng = item.longitude ?: return@mapNotNull null
                    val latLng = LatLng(lat, lng)
                    map.addMarker(
                        MarkerOptions().position(latLng).title(item.place)
                    )
                    latLng
                }
                lastMarkerPoints = points

                if (points.isEmpty()) {
                    Toast.makeText(
                        requireContext(),
                        R.string.map_no_markers,
                        Toast.LENGTH_LONG
                    ).show()
                    map.moveCamera(
                        CameraUpdateFactory.newLatLngZoom(LatLng(36.5, 127.5), 6f)
                    )
                    return@launch
                }

                if (points.size == 1) {
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(points.first(), 12f))
                } else {
                    val bounds = LatLngBounds.builder()
                    points.forEach { bounds.include(it) }
                    map.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds.build(), 80))
                }
            } catch (_: Exception) {
                Toast.makeText(requireContext(), R.string.map_load_error, Toast.LENGTH_SHORT)
                    .show()
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(36.5, 127.5), 6f))
            }
        }
    }

    fun setMapTypeNormal() {
        googleMap?.mapType = GoogleMap.MAP_TYPE_NORMAL
    }

    fun setMapTypeSatellite() {
        googleMap?.mapType = GoogleMap.MAP_TYPE_SATELLITE
    }

    fun fitAllMarkers() {
        val map = googleMap ?: return
        if (lastMarkerPoints.isEmpty()) {
            loadMarkers()
            return
        }
        if (lastMarkerPoints.size == 1) {
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(lastMarkerPoints.first(), 12f))
        } else {
            val bounds = LatLngBounds.builder()
            lastMarkerPoints.forEach { bounds.include(it) }
            map.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds.build(), 80))
        }
    }

    override fun onResume() {
        super.onResume()
        googleMap?.let { loadMarkers() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        googleMap = null
        _binding = null
    }
}
