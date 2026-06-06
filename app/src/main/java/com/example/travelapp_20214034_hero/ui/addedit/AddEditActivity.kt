package com.example.travelapp_20214034_hero.ui.addedit

import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.travelapp_20214034_hero.R
import com.example.travelapp_20214034_hero.common.ImageFileHelper
import com.example.travelapp_20214034_hero.common.PhotoExifHelper
import com.example.travelapp_20214034_hero.common.TravelExtras
import com.example.travelapp_20214034_hero.data.TravelDbHelper
import com.example.travelapp_20214034_hero.data.TravelItem
import com.example.travelapp_20214034_hero.databinding.ActivityAddEditBinding
import com.google.android.material.datepicker.MaterialDatePicker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class AddEditActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddEditBinding
    private lateinit var dbHelper: TravelDbHelper

    private var editId: Long = -1L
    private var photoUriString: String? = null
    private var previousPhotoPath: String? = null
    private var cameraOutputUri: Uri? = null
    private var cameraOutputFile: File? = null

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    private val pickGalleryLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                onPhotoPicked(uri)
            }
        }

    private val takePictureLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success && cameraOutputUri != null) {
                onPhotoPicked(cameraOutputUri!!)
            }
        }

    private val requestCameraPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                launchCamera()
            } else {
                Toast.makeText(this, R.string.camera_permission_denied, Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        @Suppress("DEPRECATION")
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        binding = ActivityAddEditBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dbHelper = TravelDbHelper.getInstance(this)
        editId = intent.getLongExtra(TravelExtras.EXTRA_TRAVEL_ID, -1L)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = if (editId > 0) getString(R.string.edit_travel) else getString(R.string.add_travel)

        if (editId > 0) {
            loadExisting(editId)
        } else {
            binding.editDate.setText(dateFormat.format(Date()))
            applyPrefillFromIntent()
        }

        setupListeners()
    }

    private fun applyPrefillFromIntent() {
        val place = intent.getStringExtra(TravelExtras.EXTRA_PLACE).orEmpty()
        val lat = intent.getDoubleExtra(TravelExtras.EXTRA_LATITUDE, Double.NaN)
        val lng = intent.getDoubleExtra(TravelExtras.EXTRA_LONGITUDE, Double.NaN)

        if (place.isNotBlank()) {
            binding.editPlace.setText(place)
        }
        if (!lat.isNaN() && !lng.isNaN()) {
            binding.editLatitude.setText(lat.toString())
            binding.editLongitude.setText(lng.toString())
        }
    }

    private fun setupListeners() {
        binding.editDate.setOnClickListener { showDatePicker() }
        binding.buttonGallery.setOnClickListener {
            pickGalleryLauncher.launch("image/*")
        }
        binding.buttonCamera.setOnClickListener { checkCameraAndCapture() }
        binding.buttonSearchPlace.setOnClickListener { showPlaceSearchDialog() }
        binding.buttonSave.setOnClickListener { saveTravel() }
    }

    private fun loadExisting(id: Long) {
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            val item = withContext(Dispatchers.IO) { dbHelper.getTravelById(id) }
            if (isFinishing || isDestroyed) return@launch
            binding.progressBar.visibility = View.GONE
            if (item == null) {
                Toast.makeText(this@AddEditActivity, R.string.error_not_found, Toast.LENGTH_SHORT)
                    .show()
                finish()
                return@launch
            }
            binding.editPlace.setText(item.place)
            binding.editDate.setText(item.visitDate)
            binding.editMemo.setText(item.memo)
            item.latitude?.let { binding.editLatitude.setText(it.toString()) }
            item.longitude?.let { binding.editLongitude.setText(it.toString()) }
            photoUriString = item.photoUri
            previousPhotoPath = item.photoUri
            ImageFileHelper.resolveForGlide(item.photoUri)?.let { loadPhotoPreview(it) }
        }
    }

    private fun onPhotoPicked(uri: Uri) {
        val cameraFile = cameraOutputFile
        cameraOutputFile = null
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            val savedPath = withContext(Dispatchers.IO) {
                ImageFileHelper.persistPhotoPath(this@AddEditActivity, uri.toString())
            }
            if (isFinishing || isDestroyed) return@launch
            binding.progressBar.visibility = View.GONE
            photoUriString = savedPath ?: uri.toString()
            val preview = ImageFileHelper.resolveForGlide(photoUriString) ?: uri
            loadPhotoPreview(preview)
            applyGpsFromPhoto(uri, cameraFile)
        }
    }

    private fun applyGpsFromPhoto(uri: Uri, cameraFile: File? = null) {
        lifecycleScope.launch {
            val gps = withContext(Dispatchers.IO) {
                if (cameraFile != null && cameraFile.exists()) {
                    PhotoExifHelper.readGpsFromFile(cameraFile)
                } else {
                    PhotoExifHelper.readGps(this@AddEditActivity, uri)
                        ?: uri.path?.let { PhotoExifHelper.readGpsFromPath(it) }
                }
            }
            if (isFinishing || isDestroyed) return@launch
            if (gps == null) {
                Toast.makeText(
                    this@AddEditActivity,
                    R.string.exif_gps_not_found,
                    Toast.LENGTH_SHORT
                ).show()
                return@launch
            }
            binding.editLatitude.setText(gps.latitude.toString())
            binding.editLongitude.setText(gps.longitude.toString())
            Toast.makeText(
                this@AddEditActivity,
                R.string.exif_gps_applied,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun showDatePicker() {
        val picker = MaterialDatePicker.Builder.datePicker().build()
        picker.addOnPositiveButtonClickListener { millis ->
            val utc = TimeZone.getTimeZone("UTC")
            dateFormat.timeZone = utc
            binding.editDate.setText(dateFormat.format(Date(millis)))
            dateFormat.timeZone = TimeZone.getDefault()
        }
        picker.show(supportFragmentManager, "date_picker")
    }

    private fun showPlaceSearchDialog() {
        val editText = EditText(this).apply {
            hint = getString(R.string.hint_place)
            setText(binding.editPlace.text)
            setPadding(48, 32, 48, 16)
        }
        AlertDialog.Builder(this)
            .setTitle(R.string.search_place_title)
            .setView(editText)
            .setPositiveButton(R.string.search) { _, _ ->
                searchPlace(editText.text?.toString()?.trim().orEmpty())
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun searchPlace(keyword: String) {
        if (keyword.isEmpty()) {
            Toast.makeText(this, R.string.error_place_required, Toast.LENGTH_SHORT).show()
            return
        }
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                geocodePlaceName(keyword)
            }
            if (isFinishing || isDestroyed) return@launch
            binding.progressBar.visibility = View.GONE
            if (result == null) {
                Toast.makeText(this@AddEditActivity, R.string.search_place_failed, Toast.LENGTH_SHORT)
                    .show()
                return@launch
            }
            binding.editPlace.setText(result.getAddressLine(0) ?: keyword)
            binding.editLatitude.setText(result.latitude.toString())
            binding.editLongitude.setText(result.longitude.toString())
            Toast.makeText(this@AddEditActivity, R.string.search_place_ok, Toast.LENGTH_SHORT).show()
        }
    }

    private suspend fun geocodePlaceName(keyword: String): android.location.Address? {
        return try {
            if (!Geocoder.isPresent()) return null
            val geocoder = Geocoder(this, Locale.KOREA)
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

    private fun checkCameraAndCapture() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED -> launchCamera()
            else -> requestCameraPermission.launch(Manifest.permission.CAMERA)
        }
    }

    private fun launchCamera() {
        val photoFile = File(cacheDir, "camera/photo_${System.currentTimeMillis()}.jpg")
        photoFile.parentFile?.mkdirs()
        cameraOutputFile = photoFile
        val outputUri = FileProvider.getUriForFile(
            this,
            "${packageName}.fileprovider",
            photoFile
        )
        cameraOutputUri = outputUri
        takePictureLauncher.launch(outputUri)
    }

    private fun loadPhotoPreview(source: Any) {
        Glide.with(this)
            .load(source)
            .override(PREVIEW_WIDTH, PREVIEW_HEIGHT)
            .centerCrop()
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .dontAnimate()
            .placeholder(R.drawable.bg_photo_placeholder)
            .into(binding.imagePhoto)
    }

    private fun saveTravel() {
        val place = binding.editPlace.text?.toString()?.trim().orEmpty()
        val date = binding.editDate.text?.toString()?.trim().orEmpty()
        val memo = binding.editMemo.text?.toString()?.trim().orEmpty()

        if (place.isEmpty()) {
            Toast.makeText(this, R.string.error_place_required, Toast.LENGTH_SHORT).show()
            return
        }
        if (date.isEmpty()) {
            Toast.makeText(this, R.string.error_date_required, Toast.LENGTH_SHORT).show()
            return
        }

        val locationInput = parseLocationInput() ?: return

        binding.buttonSave.isEnabled = false
        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            val savedPhotoPath = withContext(Dispatchers.IO) {
                ImageFileHelper.persistPhotoPath(this@AddEditActivity, photoUriString)
            }

            if (isFinishing || isDestroyed) return@launch

            if (!photoUriString.isNullOrBlank() && savedPhotoPath == null) {
                binding.progressBar.visibility = View.GONE
                binding.buttonSave.isEnabled = true
                Toast.makeText(
                    this@AddEditActivity,
                    R.string.error_photo_copy_failed,
                    Toast.LENGTH_SHORT
                ).show()
                return@launch
            }

            val item = TravelItem(
                id = if (editId > 0) editId else 0,
                place = place,
                visitDate = date,
                memo = memo,
                photoUri = savedPhotoPath,
                latitude = locationInput.latitude,
                longitude = locationInput.longitude
            )

            val success = withContext(Dispatchers.IO) {
                if (editId > 0) {
                    dbHelper.updateTravel(item) > 0
                } else {
                    dbHelper.insertTravel(item) != -1L
                }
            }

            withContext(Dispatchers.IO) {
                if (success) {
                    val oldPath = previousPhotoPath
                    if (!oldPath.isNullOrBlank() && oldPath != savedPhotoPath) {
                        ImageFileHelper.deletePhotoFile(oldPath)
                    }
                } else if (
                    !savedPhotoPath.isNullOrBlank() &&
                    savedPhotoPath != previousPhotoPath
                ) {
                    ImageFileHelper.deletePhotoFile(savedPhotoPath)
                }
            }

            if (isFinishing || isDestroyed) return@launch
            binding.progressBar.visibility = View.GONE
            binding.buttonSave.isEnabled = true

            if (success) {
                Toast.makeText(this@AddEditActivity, R.string.saved, Toast.LENGTH_SHORT).show()
                setResult(RESULT_OK)
                finish()
            } else {
                Toast.makeText(this@AddEditActivity, R.string.error_save_failed, Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    private fun parseLocationInput(): LocationInput? {
        val latText = binding.editLatitude.text?.toString()?.trim().orEmpty()
        val lngText = binding.editLongitude.text?.toString()?.trim().orEmpty()
        val lat = latText.toDoubleOrNull()
        val lng = lngText.toDoubleOrNull()

        if (latText.isNotEmpty() && lat == null || lat != null && lat !in LATITUDE_RANGE) {
            Toast.makeText(this, R.string.error_latitude_range, Toast.LENGTH_SHORT).show()
            return null
        }
        if (lngText.isNotEmpty() && lng == null || lng != null && lng !in LONGITUDE_RANGE) {
            Toast.makeText(this, R.string.error_longitude_range, Toast.LENGTH_SHORT).show()
            return null
        }
        if ((lat == null) != (lng == null)) {
            Toast.makeText(this, R.string.error_location_pair_required, Toast.LENGTH_SHORT).show()
            return null
        }
        return LocationInput(lat, lng)
    }

    override fun finish() {
        super.finish()
        @Suppress("DEPRECATION")
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    companion object {
        private const val PREVIEW_WIDTH = 800
        private const val PREVIEW_HEIGHT = 600
        private val LATITUDE_RANGE = -90.0..90.0
        private val LONGITUDE_RANGE = -180.0..180.0
    }

    private data class LocationInput(
        val latitude: Double?,
        val longitude: Double?
    )
}
