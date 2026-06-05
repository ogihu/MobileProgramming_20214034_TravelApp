package com.example.travelapp_20214034_hero.ui.detail

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.travelapp_20214034_hero.R
import com.example.travelapp_20214034_hero.common.ImageFileHelper
import com.example.travelapp_20214034_hero.common.TravelExtras
import com.example.travelapp_20214034_hero.data.TravelDbHelper
import com.example.travelapp_20214034_hero.databinding.ActivityDetailBinding
import com.example.travelapp_20214034_hero.ui.addedit.AddEditActivity
import com.example.travelapp_20214034_hero.ui.map.MapFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDetailBinding
    private var travelId: Long = -1L

    private val editLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK) {
                MapFragment.markMarkersStale()
                loadTravel(travelId)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = getString(R.string.detail_travel)

        travelId = intent.getLongExtra(TravelExtras.EXTRA_TRAVEL_ID, -1L)
        if (travelId <= 0) {
            Toast.makeText(this, R.string.error_not_found, Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        binding.fabEdit.setOnClickListener { openEdit() }
        loadTravel(travelId)
    }

    private fun loadTravel(id: Long) {
        binding.progressBar.visibility = View.VISIBLE
        binding.fabEdit.visibility = View.GONE
        lifecycleScope.launch {
            val item = withContext(Dispatchers.IO) {
                TravelDbHelper.getInstance(this@DetailActivity).getTravelById(id)
            }
            if (isFinishing || isDestroyed) return@launch
            binding.progressBar.visibility = View.GONE

            if (item == null) {
                Toast.makeText(this@DetailActivity, R.string.error_not_found, Toast.LENGTH_SHORT)
                    .show()
                finish()
                return@launch
            }

            binding.textPlace.text = item.place
            binding.textDate.text = item.visitDate
            binding.textMemo.text = if (item.memo.isBlank()) "-" else item.memo
            binding.textLocation.text =
                if (item.latitude != null && item.longitude != null) {
                    "${item.latitude}, ${item.longitude}"
                } else {
                    getString(R.string.no_location)
                }

            val photoTarget = ImageFileHelper.resolveForGlide(item.photoUri)
            if (photoTarget != null) {
                Glide.with(this@DetailActivity)
                    .load(photoTarget)
                    .override(1080, 1080)
                    .centerCrop()
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .dontAnimate()
                    .placeholder(R.drawable.bg_photo_placeholder)
                    .into(binding.imagePhoto)
            } else {
                binding.imagePhoto.setImageResource(R.drawable.bg_photo_placeholder)
            }

            binding.fabEdit.visibility = View.VISIBLE
        }
    }

    private fun openEdit() {
        editLauncher.launch(
            Intent(this, AddEditActivity::class.java).apply {
                putExtra(TravelExtras.EXTRA_TRAVEL_ID, travelId)
            }
        )
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_detail, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_edit_travel -> {
                openEdit()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
