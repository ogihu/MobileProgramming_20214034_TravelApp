package com.example.travelapp_20214034_hero.common

import android.content.Context
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.File

object PhotoExifHelper {

    data class GpsCoordinates(val latitude: Double, val longitude: Double)

    fun readGps(context: Context, uri: Uri): GpsCoordinates? {
        return try {
            when (uri.scheme) {
                "file" -> uri.path?.let { readGpsFromPath(it) }
                else -> context.contentResolver.openInputStream(uri)?.use { input ->
                    readGpsFromExif(ExifInterface(input))
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    fun readGpsFromPath(path: String): GpsCoordinates? {
        return try {
            readGpsFromExif(ExifInterface(path))
        } catch (_: Exception) {
            null
        }
    }

    fun readGpsFromFile(file: File): GpsCoordinates? {
        if (!file.exists()) return null
        return readGpsFromPath(file.absolutePath)
    }

    private fun readGpsFromExif(exif: ExifInterface): GpsCoordinates? {
        val latLong = exif.latLong ?: return null
        val lat = latLong[0]
        val lng = latLong[1]
        if (lat == 0.0 && lng == 0.0) return null
        return GpsCoordinates(lat, lng)
    }
}
