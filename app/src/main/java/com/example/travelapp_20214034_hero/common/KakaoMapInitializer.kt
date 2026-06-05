package com.example.travelapp_20214034_hero.common

import android.content.Context
import android.util.Log
import com.example.travelapp_20214034_hero.BuildConfig
import com.kakao.vectormap.KakaoMapSdk

object KakaoMapInitializer {

    private const val TAG = "KakaoMapInitializer"

    @Volatile
    private var initialized = false

    data class InitResult(
        val success: Boolean,
        val errorMessage: String? = null
    )

    fun ensureInitialized(context: Context): InitResult {
        if (initialized) return InitResult(success = true)
        synchronized(this) {
            if (initialized) return InitResult(success = true)

            val key = BuildConfig.KAKAO_NATIVE_APP_KEY.trim()
            if (key.isBlank()) {
                return InitResult(
                    success = false,
                    errorMessage = "BuildConfig에 키가 비어 있습니다. Sync 후 Rebuild 하세요."
                )
            }

            return try {
                KakaoMapSdk.init(context.applicationContext, key)
                initialized = true
                Log.i(TAG, "KakaoMapSdk initialized")
                InitResult(success = true)
            } catch (e: Exception) {
                Log.e(TAG, "KakaoMapSdk.init failed", e)
                InitResult(
                    success = false,
                    errorMessage = e.message ?: e.javaClass.simpleName
                )
            }
        }
    }

    fun buildSetupGuide(context: Context, detail: String? = null): String {
        val keyHash = KeyHashHelper.getKeyHash(context) ?: "(앱에서 확인 불가)"
        return buildString {
            if (!detail.isNullOrBlank()) {
                append(detail.trim())
                append("\n\n")
            }
            append("카카오 개발자 콘솔에 아래를 등록하세요.\n")
            append("1. 앱 설정 → 플랫폼 → Android 추가\n")
            append("2. 패키지명: ${context.packageName}\n")
            append("3. 키 해시: $keyHash\n")
            append("4. 제품 설정 → 카카오맵 → 활성화\n")
            append("5. Android Studio → Sync → Rebuild → 앱 재실행")
        }
    }
}
