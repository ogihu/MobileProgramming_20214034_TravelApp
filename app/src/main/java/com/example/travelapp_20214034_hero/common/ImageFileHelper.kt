package com.example.travelapp_20214034_hero.common

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

/**
 * CH06 파일 처리: 갤러리/카메라 URI를 앱 내부 저장소(files/photos)에 복사.
 * 저장 시 이미지를 리사이즈·압축해 메모리·로딩 부담을 줄입니다.
 */
object ImageFileHelper {

    private const val PHOTO_DIR = "photos"
    private const val MAX_IMAGE_DIMENSION = 1280
    private const val JPEG_QUALITY = 85

    fun copyToInternalStorage(context: Context, sourceUri: Uri): String? {
        return try {
            val dir = File(context.filesDir, PHOTO_DIR).apply { mkdirs() }
            val outFile = File(dir, "travel_${System.currentTimeMillis()}.jpg")
            val bitmap = decodeSampledBitmap(context, sourceUri, MAX_IMAGE_DIMENSION) ?: return null
            FileOutputStream(outFile).use { output ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output)
            }
            bitmap.recycle()
            if (!outFile.exists() || outFile.length() == 0L) {
                outFile.delete()
                return null
            }
            outFile.absolutePath
        } catch (_: Exception) {
            null
        }
    }

    fun copyFileToInternalStorage(context: Context, sourceFile: File): String? {
        if (!sourceFile.exists() || sourceFile.length() == 0L) return null
        val photosDir = File(context.filesDir, PHOTO_DIR).absolutePath
        if (sourceFile.absolutePath.startsWith(photosDir)) {
            return sourceFile.absolutePath
        }
        return copyToInternalStorage(context, Uri.fromFile(sourceFile))
    }

    fun resolveForGlide(pathOrUri: String?): Any? {
        if (pathOrUri.isNullOrBlank()) return null
        return when {
            pathOrUri.startsWith("/") -> File(pathOrUri)
            pathOrUri.startsWith("file://") -> File(Uri.parse(pathOrUri).path ?: return null)
            else -> Uri.parse(pathOrUri)
        }
    }

    fun persistPhotoPath(context: Context, pathOrUriString: String?): String? {
        if (pathOrUriString.isNullOrBlank()) return null
        if (pathOrUriString.startsWith("/")) {
            val file = File(pathOrUriString)
            if (file.exists() && file.absolutePath.startsWith(context.filesDir.absolutePath)) {
                return pathOrUriString
            }
        }
        val uri = Uri.parse(pathOrUriString)
        if (uri.scheme == "file") {
            val path = uri.path ?: return null
            val file = File(path)
            if (file.absolutePath.startsWith(context.filesDir.absolutePath)) {
                return file.absolutePath
            }
            return copyFileToInternalStorage(context, file)
        }
        return copyToInternalStorage(context, uri)
    }

    fun deletePhotoFile(pathOrUri: String?) {
        if (pathOrUri.isNullOrBlank()) return
        val path = when {
            pathOrUri.startsWith("/") -> pathOrUri
            pathOrUri.startsWith("file://") -> Uri.parse(pathOrUri).path
            else -> null
        } ?: return
        try {
            File(path).takeIf { it.exists() }?.delete()
        } catch (_: Exception) {
        }
    }

    fun deleteAllInternalPhotos(context: Context) {
        try {
            File(context.filesDir, PHOTO_DIR).listFiles()?.forEach { it.delete() }
        } catch (_: Exception) {
        }
    }

    private fun decodeSampledBitmap(context: Context, uri: Uri, maxSize: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, bounds)
        }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        val sampleSize = calculateInSampleSize(bounds, maxSize, maxSize)
        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.RGB_565
        }
        return context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, decodeOptions)
        }
    }

    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        val (height, width) = options.outHeight to options.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            var halfHeight = height / 2
            var halfWidth = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }
}
