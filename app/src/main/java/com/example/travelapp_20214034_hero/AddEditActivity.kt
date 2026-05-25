package com.example.travelapp_20214034_hero

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.bumptech.glide.Glide
import com.example.travelapp_20214034_hero.data.TravelDbHelper
import com.example.travelapp_20214034_hero.data.TravelItem
import com.example.travelapp_20214034_hero.databinding.ActivityAddEditBinding
import com.google.android.material.datepicker.MaterialDatePicker
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
    private var cameraOutputUri: Uri? = null

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    private val pickGalleryLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                photoUriString = uri.toString()
                loadPhotoPreview(uri)
            }
        }

    private val takePictureLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success && cameraOutputUri != null) {
                photoUriString = cameraOutputUri.toString()
                loadPhotoPreview(cameraOutputUri!!)
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
        binding.buttonSave.setOnClickListener { saveTravel() }
    }

    private fun loadExisting(id: Long) {
        val item = dbHelper.getTravelById(id)
        if (item == null) {
            Toast.makeText(this, R.string.error_not_found, Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        binding.editPlace.setText(item.place)
        binding.editDate.setText(item.visitDate)
        binding.editMemo.setText(item.memo)
        item.latitude?.let { binding.editLatitude.setText(it.toString()) }
        item.longitude?.let { binding.editLongitude.setText(it.toString()) }
        photoUriString = item.photoUri
        item.photoUri?.let { loadPhotoPreview(Uri.parse(it)) }
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
        cameraOutputUri = FileProvider.getUriForFile(
            this,
            "${packageName}.fileprovider",
            photoFile
        )
        takePictureLauncher.launch(cameraOutputUri)
    }

    private fun loadPhotoPreview(uri: Uri) {
        Glide.with(this)
            .load(uri)
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

        val item = TravelItem(
            id = if (editId > 0) editId else 0,
            place = place,
            visitDate = date,
            memo = memo,
            photoUri = photoUriString,
            latitude = lat,
            longitude = lng
        )

        if (editId > 0) {
            dbHelper.updateTravel(item)
        } else {
            dbHelper.insertTravel(item)
        }

        Toast.makeText(this, R.string.saved, Toast.LENGTH_SHORT).show()
        setResult(RESULT_OK)
        finish()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
