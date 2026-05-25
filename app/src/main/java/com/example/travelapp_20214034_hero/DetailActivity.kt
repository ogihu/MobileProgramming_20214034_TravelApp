package com.example.travelapp_20214034_hero

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.travelapp_20214034_hero.data.TravelDbHelper
import com.example.travelapp_20214034_hero.databinding.ActivityDetailBinding

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

        val item = TravelDbHelper(this).getTravelById(id)
        if (item == null) {
            Toast.makeText(this, R.string.error_not_found, Toast.LENGTH_SHORT).show()
            finish()
            return
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

        if (!item.photoUri.isNullOrBlank()) {
            Glide.with(this)
                .load(Uri.parse(item.photoUri))
                .centerCrop()
                .into(binding.imagePhoto)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
