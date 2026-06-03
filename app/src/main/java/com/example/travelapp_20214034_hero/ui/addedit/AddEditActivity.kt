package com.example.travelapp_20214034_hero.ui.addedit

import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import android.net.Uri
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
import kotlinx.coroutines.withContext
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
        binding = ActivityAddEditBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dbHelper = TravelDbHelper(this)
        editId = intent.getLongExtra(TravelExtras.EXTRA_TRAVEL_ID, -1L)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = if (editId > 0) getString(R.string.edit_travel) else getString(R.string.add_travel)

        if (editId > 0) {
            loadExisting(editId)
        }

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
        photoUriString = uri.toString()
        loadPhotoPreview(uri)
        applyGpsFromPhoto(uri)
    }

    private fun applyGpsFromPhoto(uri: Uri) {
        lifecycleScope.launch {
            val gps = withContext(Dispatchers.IO) {
                PhotoExifHelper.readGps(this@AddEditActivity, uri)
                    ?: uri.path?.let { PhotoExifHelper.readGpsFromPath(it) }
            } ?: return@launch

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
                try {
                    if (!Geocoder.isPresent()) return@withContext null
                    val geocoder = Geocoder(this@AddEditActivity, Locale.KOREA)
                    @Suppress("DEPRECATION")
                    val addresses = geocoder.getFromLocationName(keyword, 1)
                    addresses?.firstOrNull()
                } catch (_: Exception) {
                    null
                }
            }
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
            .centerCrop()
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

        val lat = binding.editLatitude.text?.toString()?.trim()?.toDoubleOrNull()
        val lng = binding.editLongitude.text?.toString()?.trim()?.toDoubleOrNull()

        binding.buttonSave.isEnabled = false
        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            val savedPhotoPath = withContext(Dispatchers.IO) {
                ImageFileHelper.persistPhotoPath(this@AddEditActivity, photoUriString)
            }

            val item = TravelItem(
                id = if (editId > 0) editId else 0,
                place = place,
                visitDate = date,
                memo = memo,
                photoUri = savedPhotoPath,
                latitude = lat,
                longitude = lng
            )

            val success = withContext(Dispatchers.IO) {
                if (editId > 0) {
                    dbHelper.updateTravel(item) > 0
                } else {
                    dbHelper.insertTravel(item) != -1L
                }
            }

            if (success) {
                withContext(Dispatchers.IO) {
                    val oldPath = previousPhotoPath
                    if (!oldPath.isNullOrBlank() && oldPath != savedPhotoPath) {
                        ImageFileHelper.deletePhotoFile(oldPath)
                    }
                }
            }

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

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
