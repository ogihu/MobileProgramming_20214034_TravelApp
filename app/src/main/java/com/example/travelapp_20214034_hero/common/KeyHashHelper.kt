package com.example.travelapp_20214034_hero.common

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Base64
import java.security.MessageDigest

object KeyHashHelper {

    fun getKeyHash(context: Context): String? {
        return try {
            val packageName = context.packageName
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                context.packageManager.getPackageInfo(
                    packageName,
                    PackageManager.GET_SIGNING_CERTIFICATES
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(
                    packageName,
                    PackageManager.GET_SIGNATURES
                )
            }

            val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.signingInfo?.apkContentsSigners
            } else {
                @Suppress("DEPRECATION")
                packageInfo.signatures
            } ?: return null

            val signature = signatures.firstOrNull() ?: return null
            val md = MessageDigest.getInstance("SHA")
            md.update(signature.toByteArray())
            Base64.encodeToString(md.digest(), Base64.NO_WRAP)
        } catch (_: Exception) {
            null
        }
    }
}
