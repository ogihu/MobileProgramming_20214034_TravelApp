package com.example.travelapp_20214034_hero.util

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

/**
 * CH06 파일 처리: 갤러리/카메라 URI를 앱 내부 저장소(files/photos)에 복사해
 * 앱 재실행 후에도 사진이 유지되도록 함.
 */
object ImageFileHelper {

    private const val PHOTO_DIR = "photos"

    fun copyToInternalStorage(context: Context, sourceUri: Uri): String? {
        return try {
            val dir = File(context.filesDir, PHOTO_DIR).apply { mkdirs() }
            val outFile = File(dir, "travel_${System.currentTimeMillis()}.jpg")
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(outFile).use { output -> input.copyTo(output) }
            }
            outFile.absolutePath
        } catch (_: Exception) {
            null
        }
    }

    fun copyFileToInternalStorage(context: Context, sourceFile: File): String? {
        if (!sourceFile.exists()) return null
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
}
