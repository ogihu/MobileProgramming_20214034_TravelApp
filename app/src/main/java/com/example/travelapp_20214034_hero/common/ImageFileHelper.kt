package com.example.travelapp_20214034_hero.common

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

/**
 * CH06 파일 처리: 갤러리/카메라 URI를 앱 내부 저장소(files/photos)에 복사.
 */
object ImageFileHelper {

    private const val PHOTO_DIR = "photos"

    fun copyToInternalStorage(context: Context, sourceUri: Uri): String? {
        return try {
            val dir = File(context.filesDir, PHOTO_DIR).apply { mkdirs() }
            val outFile = File(dir, "travel_${System.currentTimeMillis()}.jpg")
            val input = context.contentResolver.openInputStream(sourceUri) ?: return null
            input.use { stream ->
                FileOutputStream(outFile).use { output -> stream.copyTo(output) }
            }
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
}
