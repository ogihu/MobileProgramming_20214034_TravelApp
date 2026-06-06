package com.example.travelapp_20214034_hero.ui.detail

import android.content.Intent
import android.net.Uri
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
import com.example.travelapp_20214034_hero.data.TravelItem
import com.example.travelapp_20214034_hero.databinding.ActivityDetailBinding
import com.example.travelapp_20214034_hero.ui.addedit.AddEditActivity
import com.example.travelapp_20214034_hero.ui.map.MapFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDetailBinding
    private var travelId: Long = -1L
    private var currentTravel: TravelItem? = null

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
        binding.buttonOpenMap.setOnClickListener { openMap() }
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

            bindTravel(item)
            loadPhoto(item)
            binding.fabEdit.visibility = View.VISIBLE
        }
    }

    private fun bindTravel(item: TravelItem) {
        currentTravel = item
        binding.textPlace.text = item.place
        binding.textDate.text = item.visitDate
        binding.textMemo.text = if (item.memo.isBlank()) "-" else item.memo
        val hasPhoto = !item.photoUri.isNullOrBlank()
        val hasLocation = item.latitude != null && item.longitude != null
        binding.textLocation.text =
            if (hasLocation) {
                "${item.latitude}, ${item.longitude}"
            } else {
                getString(R.string.no_location)
            }
        binding.textDetailStatus.text = getString(
            if (hasLocation) R.string.detail_status_map_ready else R.string.detail_status_map_empty
        )
        binding.textVisitDateInfo.text = getString(R.string.detail_visit_date_format, item.visitDate)
        binding.textPhotoInfo.text = getString(
            if (hasPhoto) R.string.detail_photo_ready else R.string.detail_photo_empty
        )
        binding.textMapInfo.text = getString(
            if (hasLocation) R.string.detail_map_ready else R.string.detail_map_empty
        )
        binding.buttonOpenMap.isEnabled = hasLocation
    }

    private fun loadPhoto(item: TravelItem) {
        val photoTarget = ImageFileHelper.resolveForGlide(item.photoUri)
        if (photoTarget == null) {
            binding.imagePhoto.setImageResource(R.drawable.bg_photo_placeholder)
            return
        }
        Glide.with(this)
            .load(photoTarget)
            .override(PHOTO_SIZE, PHOTO_SIZE)
            .centerCrop()
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .dontAnimate()
            .placeholder(R.drawable.bg_photo_placeholder)
            .into(binding.imagePhoto)
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
            R.id.action_share_travel -> {
                shareTravel()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun shareTravel() {
        val travel = currentTravel ?: return
        val location = if (travel.latitude != null && travel.longitude != null) {
            "\n위치: ${travel.latitude}, ${travel.longitude}"
        } else {
            ""
        }
        val shareText = """
            ${travel.place}
            날짜: ${travel.visitDate}$location
            
            ${travel.memo.ifBlank { "메모 없음" }}
        """.trimIndent()
        startActivity(
            Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, shareText)
                },
                getString(R.string.share_travel)
            )
        )
    }

    private fun openMap() {
        val travel = currentTravel ?: return
        val lat = travel.latitude
        val lng = travel.longitude
        if (lat == null || lng == null) {
            Toast.makeText(this, R.string.no_location, Toast.LENGTH_SHORT).show()
            return
        }
        val encodedPlace = Uri.encode(travel.place)
        val uri = Uri.parse("geo:$lat,$lng?q=$lat,$lng($encodedPlace)")
        try {
            startActivity(Intent(Intent.ACTION_VIEW, uri))
        } catch (_: Exception) {
            Toast.makeText(this, R.string.search_place_failed, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    companion object {
        private const val PHOTO_SIZE = 1080
    }
}
