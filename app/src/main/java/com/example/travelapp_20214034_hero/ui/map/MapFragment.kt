package com.example.travelapp_20214034_hero.ui.map

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.webkit.WebViewAssetLoader
import com.example.travelapp_20214034_hero.BuildConfig
import com.example.travelapp_20214034_hero.R
import com.example.travelapp_20214034_hero.common.TravelExtras
import com.example.travelapp_20214034_hero.data.TravelDbHelper
import com.example.travelapp_20214034_hero.databinding.FragmentMapBinding
import com.example.travelapp_20214034_hero.ui.detail.DetailActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * x86 에뮬레이터 호환을 위해 카카오맵 JavaScript API를 WebView로 표시합니다.
 */
class MapFragment : Fragment() {

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!

    private var mapReady = false
    private lateinit var assetLoader: WebViewAssetLoader

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val jsKey = BuildConfig.KAKAO_JAVASCRIPT_KEY.trim()
        if (jsKey.isBlank()) {
            showSetupHint(getString(R.string.map_js_key_hint))
            binding.mapWebView.visibility = View.GONE
            return
        }

        binding.scrollMapHint.visibility = View.GONE
        binding.mapWebView.visibility = View.VISIBLE

        binding.mapWebView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
        }
        binding.mapWebView.addJavascriptInterface(MapBridge(), "AndroidBridge")
        binding.mapWebView.webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                Log.d(TAG, "WebView console: ${consoleMessage?.message()}")
                return true
            }
        }
        assetLoader = WebViewAssetLoader.Builder()
            .setDomain(MAP_ASSET_DOMAIN)
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(requireContext()))
            .build()
        binding.mapWebView.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest?
            ): WebResourceResponse? {
                return request?.url?.let { assetLoader.shouldInterceptRequest(it) }
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                if (pendingMarkerRefresh && mapReady) {
                    pendingMarkerRefresh = false
                    loadMarkers(showEmptyHints = false)
                }
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                super.onReceivedError(view, request, error)
                if (request?.isForMainFrame == true) {
                    showSetupHint(error?.description?.toString())
                }
            }
        }

        try {
            val url = Uri.Builder()
                .scheme("https")
                .authority(MAP_ASSET_DOMAIN)
                .path("/assets/kakao_map.html")
                .appendQueryParameter("appkey", jsKey)
                .build()
                .toString()
            binding.mapWebView.loadUrl(url)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load map html", e)
            showSetupHint(e.message)
        }
    }

    override fun onResume() {
        super.onResume()
        if (mapReady && pendingMarkerRefresh) {
            pendingMarkerRefresh = false
            loadMarkers(showEmptyHints = false)
        }
    }

    private fun showSetupHint(message: String?) {
        if (_binding == null) return
        binding.scrollMapHint.visibility = View.VISIBLE
        binding.textMapHint.text = buildString {
            if (!message.isNullOrBlank()) {
                append(message.trim())
                append("\n\n")
            }
            append(getString(R.string.map_js_setup_guide))
        }
        binding.mapWebView.visibility = View.GONE
    }

    fun loadMarkers(showEmptyHints: Boolean = true) {
        if (!mapReady || _binding == null || !isAdded) return

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val travels = withContext(Dispatchers.IO) {
                    TravelDbHelper.getInstance(requireContext()).getTravelsWithLocation()
                }
                if (_binding == null || !isAdded || !mapReady) return@launch

                val json = JSONArray()
                travels.forEach { item ->
                    val lat = item.latitude ?: return@forEach
                    val lng = item.longitude ?: return@forEach
                    json.put(
                        JSONObject()
                            .put("id", item.id)
                            .put("place", item.place)
                            .put("lat", lat)
                            .put("lng", lng)
                    )
                }

                binding.mapWebView.evaluateJavascript("setMarkers(${json})") { result ->
                    if (!isAdded) return@evaluateJavascript
                    val count = result?.trim('"')?.toDoubleOrNull()?.toInt() ?: 0
                    if (count == 0 && showEmptyHints) {
                        val msg = when {
                            travels.isEmpty() -> R.string.map_no_markers
                            travels.none { it.latitude != null && it.longitude != null } ->
                                R.string.map_no_location_data
                            else -> R.string.map_no_markers
                        }
                        Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "loadMarkers failed", e)
                Toast.makeText(requireContext(), R.string.map_load_error, Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun setMapTypeNormal() {
        binding.mapWebView.evaluateJavascript("setMapType('roadmap')", null)
    }

    fun setMapTypeSatellite() {
        binding.mapWebView.evaluateJavascript("setMapType('satellite')", null)
    }

    fun fitAllMarkers() {
        if (!mapReady) return
        binding.mapWebView.evaluateJavascript("fitMarkers()") { result ->
            if (!isAdded) return@evaluateJavascript
            val count = result?.toIntOrNull() ?: 0
            if (count == 0) {
                loadMarkers(showEmptyHints = true)
            }
        }
    }

    override fun onDestroyView() {
        mapReady = false
        binding.mapWebView.apply {
            loadUrl("about:blank")
            removeJavascriptInterface("AndroidBridge")
            destroy()
        }
        super.onDestroyView()
        _binding = null
    }

    private inner class MapBridge {
        @JavascriptInterface
        @Suppress("unused")
        fun onMapReady() {
            requireActivity().runOnUiThread {
                if (_binding == null || !isAdded) return@runOnUiThread
                mapReady = true
                binding.scrollMapHint.visibility = View.GONE
                loadMarkers()
            }
        }

        @JavascriptInterface
        @Suppress("unused")
        fun onMapError(message: String?) {
            requireActivity().runOnUiThread {
                if (_binding == null || !isAdded) return@runOnUiThread
                showSetupHint(message)
            }
        }

        @JavascriptInterface
        @Suppress("unused")
        fun onMarkerClick(travelId: Long) {
            requireActivity().runOnUiThread {
                if (!isAdded) return@runOnUiThread
                startActivity(
                    Intent(requireContext(), DetailActivity::class.java).apply {
                        putExtra(TravelExtras.EXTRA_TRAVEL_ID, travelId)
                    }
                )
            }
        }
    }

    companion object {
        private const val TAG = "MapFragment"
        private const val MAP_ASSET_DOMAIN = "appassets.androidplatform.net"

        @Volatile
        var pendingMarkerRefresh = false

        fun markMarkersStale() {
            pendingMarkerRefresh = true
        }
    }
}
