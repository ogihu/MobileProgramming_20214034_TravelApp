package com.example.travelapp_20214034_hero.ui.list

import android.app.Activity
import android.content.Intent
import android.location.Geocoder
import android.os.Build
import android.os.Bundle
import android.view.ContextMenu
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.travelapp_20214034_hero.R
import com.example.travelapp_20214034_hero.common.TravelExtras
import com.example.travelapp_20214034_hero.data.TravelDbHelper
import com.example.travelapp_20214034_hero.data.TravelItem
import com.example.travelapp_20214034_hero.databinding.FragmentHomeBinding
import com.example.travelapp_20214034_hero.databinding.ItemRecommendTravelBinding
import com.example.travelapp_20214034_hero.ui.addedit.AddEditActivity
import com.example.travelapp_20214034_hero.ui.detail.DetailActivity
import com.example.travelapp_20214034_hero.ui.map.MapFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var dbHelper: TravelDbHelper
    private lateinit var adapter: TravelAdapter
    private lateinit var deleteUndoHandler: TravelDeleteUndoHandler

    private var sortDescending = true
    private var contextMenuTravel: TravelItem? = null
    private var allTravels: List<TravelItem> = emptyList()
    private var searchQuery: String = ""

    private val addEditLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                refreshList()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dbHelper = TravelDbHelper.getInstance(requireContext())
        deleteUndoHandler = TravelDeleteUndoHandler(
            fragment = this,
            rootView = binding.root,
            dbHelper = dbHelper,
            onChanged = ::refreshList
        )

        adapter = TravelAdapter(
            onItemClick = { item ->
                startActivity(
                    Intent(requireContext(), DetailActivity::class.java).apply {
                        putExtra(TravelExtras.EXTRA_TRAVEL_ID, item.id)
                    }
                )
            },
            onRegisterContextMenu = { itemView ->
                registerForContextMenu(itemView)
            }
        )

        binding.recyclerTravels.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerTravels.setHasFixedSize(true)
        binding.recyclerTravels.itemAnimator = null
        binding.recyclerTravels.adapter = adapter

        binding.fabAdd.setOnClickListener {
            addEditLauncher.launch(Intent(requireContext(), AddEditActivity::class.java))
        }
        binding.editSearch.doOnTextChanged { text, _, _, _ ->
            searchQuery = text?.toString()?.trim().orEmpty()
            submitFilteredList()
        }
        binding.editSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                searchFromHome()
                true
            } else {
                false
            }
        }
        binding.textHomeMore.setOnClickListener {
            binding.editSearch.setText("")
            scrollToMyTrips()
        }
        setupTravelHomeUi()

        refreshList()
    }

    private fun searchFromHome() {
        val query = binding.editSearch.text?.toString()?.trim().orEmpty()
        if (query.isBlank()) {
            scrollToMyTrips()
            return
        }
        if (filteredTravels().isNotEmpty()) {
            scrollToMyTrips()
            return
        }
        viewLifecycleOwner.lifecycleScope.launch {
            binding.progressBar.visibility = View.VISIBLE
            val result = withContext(Dispatchers.IO) {
                geocodePlaceName(query)
            }
            if (_binding == null || !isAdded) return@launch
            binding.progressBar.visibility = View.GONE
            if (result == null) {
                Toast.makeText(requireContext(), R.string.search_place_failed, Toast.LENGTH_SHORT)
                    .show()
                scrollToMyTrips()
                return@launch
            }
            addEditLauncher.launch(
                Intent(requireContext(), AddEditActivity::class.java).apply {
                    putExtra(TravelExtras.EXTRA_PLACE, query)
                    putExtra(TravelExtras.EXTRA_LATITUDE, result.latitude)
                    putExtra(TravelExtras.EXTRA_LONGITUDE, result.longitude)
                }
            )
        }
    }

    private suspend fun geocodePlaceName(keyword: String): android.location.Address? {
        return try {
            if (!Geocoder.isPresent()) return null
            val geocoder = Geocoder(requireContext(), Locale.KOREA)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                suspendCancellableCoroutine { cont ->
                    geocoder.getFromLocationName(keyword, 1) { addresses ->
                        cont.resume(addresses?.firstOrNull())
                    }
                }
            } else {
                @Suppress("DEPRECATION")
                geocoder.getFromLocationName(keyword, 1)?.firstOrNull()
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun setupTravelHomeUi() {
        recommendations().forEach { item ->
            setRecommendation(item)
        }
    }

    private fun recommendations(): List<RecommendationItem> {
        return listOf(
            RecommendationItem(
                binding.cardRecommendSea,
                R.drawable.travel_recommend_1,
                R.string.recommend_sea_title,
                R.string.recommend_sea_subtitle,
                "신창"
            ),
            RecommendationItem(
                binding.cardRecommendCity,
                R.drawable.travel_recommend_3,
                R.string.recommend_city_title,
                R.string.recommend_city_subtitle,
                "인천"
            ),
            RecommendationItem(
                binding.cardRecommendPhoto,
                R.drawable.travel_recommend_2,
                R.string.recommend_photo_title,
                R.string.recommend_photo_subtitle,
                "용산"
            )
        )
    }

    private fun setRecommendation(item: RecommendationItem) {
        item.card.imageRecommend.setImageResource(item.imageRes)
        item.card.textRecommendTitle.setText(item.titleRes)
        item.card.textRecommendSubtitle.setText(item.subtitleRes)
        item.card.root.setOnClickListener {
            binding.editSearch.setText(item.keyword)
            binding.editSearch.setSelection(binding.editSearch.text?.length ?: 0)
            scrollToMyTrips()
        }
    }

    override fun onCreateContextMenu(
        menu: ContextMenu,
        v: View,
        menuInfo: ContextMenu.ContextMenuInfo?
    ) {
        super.onCreateContextMenu(menu, v, menuInfo)
        val position = v.getTag(R.id.tag_list_position) as? Int ?: return
        contextMenuTravel = adapter.getItemAt(position) ?: return
        requireActivity().menuInflater.inflate(R.menu.menu_travel_context, menu)
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        val travel = contextMenuTravel ?: return false
        return when (item.itemId) {
            R.id.context_edit -> {
                addEditLauncher.launch(
                    Intent(requireContext(), AddEditActivity::class.java).apply {
                        putExtra(TravelExtras.EXTRA_TRAVEL_ID, travel.id)
                    }
                )
                true
            }
            R.id.context_delete -> {
                confirmDelete(travel)
                true
            }
            else -> super.onContextItemSelected(item)
        }
    }

    fun refreshList(sortDesc: Boolean? = null) {
        if (_binding == null || !isAdded) return
        if (sortDesc != null) {
            sortDescending = sortDesc
        }
        viewLifecycleOwner.lifecycleScope.launch {
            val binding = _binding ?: return@launch
            val showProgress = adapter.itemCount == 0
            if (showProgress) {
                binding.progressBar.visibility = View.VISIBLE
            }
            allTravels = withContext(Dispatchers.IO) {
                dbHelper.getAllTravels(sortDescending)
            }
            if (_binding == null || !isAdded) return@launch
            updateStats(allTravels)
            submitFilteredList()
            MapFragment.markMarkersStale()
            binding.progressBar.visibility = View.GONE
            val isEmpty = filteredTravels().isEmpty()
            binding.textEmpty.visibility = if (isEmpty) View.VISIBLE else View.GONE
            binding.recyclerTravels.visibility = if (isEmpty) View.GONE else View.VISIBLE
        }
    }

    private fun submitFilteredList() {
        if (_binding == null) return
        val filtered = filteredTravels()
        adapter.submitList(filtered)
        binding.textEmpty.text = if (allTravels.isEmpty() || searchQuery.isBlank()) {
            getString(R.string.empty_travel_list)
        } else {
            getString(R.string.empty_search_result)
        }
        binding.textEmpty.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
        binding.recyclerTravels.visibility = if (filtered.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun filteredTravels(): List<TravelItem> {
        if (searchQuery.isBlank()) return allTravels
        val keywords = searchKeywords(searchQuery)
        return allTravels.filter { item ->
            keywords.any { keyword ->
                item.place.contains(keyword, ignoreCase = true) ||
                    item.memo.contains(keyword, ignoreCase = true) ||
                    item.visitDate.contains(keyword, ignoreCase = true)
            }
        }
    }

    private fun searchKeywords(query: String): List<String> {
        val normalized = query.lowercase().replace(" ", "")
        val aliases = when (normalized) {
            "yongsan", "youngsan" -> listOf("용산", "용리단길")
            "sinchang", "shinchang" -> listOf("신창", "신창풍차")
            "incheon", "inchon" -> listOf("인천", "월미도")
            "jeju" -> listOf("제주")
            else -> emptyList()
        }
        return listOf(query) + aliases
    }

    private fun scrollToMyTrips() {
        binding.homeScrollView.post {
            binding.homeScrollView.smoothScrollTo(0, binding.textMyTripTitle.top)
        }
    }

    private fun updateStats(list: List<TravelItem>) {
        binding.textStatsTotal.text = getString(R.string.stats_total_format, list.size)
        binding.textStatsLocation.text = getString(
            R.string.stats_location_format,
            list.count { it.latitude != null && it.longitude != null }
        )
        binding.textStatsLatest.text = if (list.isEmpty()) {
            getString(R.string.stats_latest_empty)
        } else {
            getString(R.string.stats_latest_format, list.first().visitDate.takeLast(5))
        }
    }

    fun toggleSort() {
        sortDescending = !sortDescending
        refreshList()
        val msg = if (sortDescending) "최신 날짜순" else "오래된 날짜순"
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
    }

    fun confirmDeleteAll() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.dialog_delete_title)
            .setMessage(R.string.dialog_delete_all_message)
            .setPositiveButton(R.string.yes) { _, _ ->
                deleteUndoHandler.deleteAll(sortDescending)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun confirmDelete(item: TravelItem) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.dialog_delete_title)
            .setMessage(R.string.dialog_delete_message)
            .setPositiveButton(R.string.yes) { _, _ ->
                deleteUndoHandler.delete(item)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private data class RecommendationItem(
        val card: ItemRecommendTravelBinding,
        val imageRes: Int,
        val titleRes: Int,
        val subtitleRes: Int,
        val keyword: String
    )
}
