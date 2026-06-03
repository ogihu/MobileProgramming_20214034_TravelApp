package com.example.travelapp_20214034_hero.ui.detail

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.travelapp_20214034_hero.R
import com.example.travelapp_20214034_hero.common.ImageFileHelper
import com.example.travelapp_20214034_hero.common.TravelExtras
import com.example.travelapp_20214034_hero.data.TravelDbHelper
import com.example.travelapp_20214034_hero.databinding.ActivityDetailBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = getString(R.string.detail_travel)

        val id = intent.getLongExtra(TravelExtras.EXTRA_TRAVEL_ID, -1L)
        if (id <= 0) {
            Toast.makeText(this, R.string.error_not_found, Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            val item = withContext(Dispatchers.IO) {
                TravelDbHelper(this@DetailActivity).getTravelById(id)
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
                    .centerCrop()
                    .placeholder(R.drawable.bg_photo_placeholder)
                    .into(binding.imagePhoto)
            } else {
                binding.imagePhoto.setImageResource(R.drawable.bg_photo_placeholder)
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
