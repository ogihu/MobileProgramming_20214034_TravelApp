package com.example.travelapp_20214034_hero.ui.list

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.ContextMenu
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.travelapp_20214034_hero.R
import com.example.travelapp_20214034_hero.common.ImageFileHelper
import com.example.travelapp_20214034_hero.common.TravelExtras
import com.example.travelapp_20214034_hero.data.TravelDbHelper
import com.example.travelapp_20214034_hero.data.TravelItem
import com.example.travelapp_20214034_hero.databinding.FragmentHomeBinding
import com.example.travelapp_20214034_hero.ui.addedit.AddEditActivity
import com.example.travelapp_20214034_hero.ui.detail.DetailActivity
import com.example.travelapp_20214034_hero.ui.map.MapFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var dbHelper: TravelDbHelper
    private lateinit var adapter: TravelAdapter

    private var sortDescending = true
    private var contextMenuTravel: TravelItem? = null

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
        dbHelper = TravelDbHelper(requireContext())

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

        refreshList()
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
            val list = withContext(Dispatchers.IO) {
                dbHelper.getAllTravels(sortDescending)
            }
            if (_binding == null || !isAdded) return@launch
            adapter.submitList(list)
            MapFragment.markMarkersStale()
            binding.progressBar.visibility = View.GONE
            val isEmpty = list.isEmpty()
            binding.textEmpty.visibility = if (isEmpty) View.VISIBLE else View.GONE
            binding.recyclerTravels.visibility = if (isEmpty) View.GONE else View.VISIBLE
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
                viewLifecycleOwner.lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        dbHelper.deleteAllTravels()
                        ImageFileHelper.deleteAllInternalPhotos(requireContext())
                    }
                    if (!isAdded) return@launch
                    Toast.makeText(requireContext(), R.string.deleted, Toast.LENGTH_SHORT).show()
                    refreshList()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun confirmDelete(item: TravelItem) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.dialog_delete_title)
            .setMessage(R.string.dialog_delete_message)
            .setPositiveButton(R.string.yes) { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        val existing = dbHelper.getTravelById(item.id)
                        dbHelper.deleteTravel(item.id)
                        ImageFileHelper.deletePhotoFile(existing?.photoUri)
                    }
                    if (!isAdded) return@launch
                    Toast.makeText(requireContext(), R.string.deleted, Toast.LENGTH_SHORT).show()
                    refreshList()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
