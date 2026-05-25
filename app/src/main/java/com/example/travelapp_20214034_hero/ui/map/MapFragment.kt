package com.example.travelapp_20214034_hero.ui.map

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
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
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MapFragment : Fragment(), OnMapReadyCallback {

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!

    private var googleMap: GoogleMap? = null

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
        loadMarkers()
    }

    private fun loadMarkers() {
        val map = googleMap ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            val travels = withContext(Dispatchers.IO) {
                TravelDbHelper(requireContext()).getAllTravels()
            }
            map.clear()
            val points = travels.mapNotNull { item ->
                val lat = item.latitude ?: return@mapNotNull null
                val lng = item.longitude ?: return@mapNotNull null
                LatLng(lat, lng) to item.place
            }

            if (points.isEmpty()) {
                Toast.makeText(requireContext(), R.string.map_no_markers, Toast.LENGTH_LONG).show()
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(36.5, 127.5), 6f))
                return@launch
            }

            points.forEach { (latLng, title) ->
                map.addMarker(MarkerOptions().position(latLng).title(title))
            }
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(points.first().first, 8f))
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
