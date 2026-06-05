package com.example.travelapp_20214034_hero.common

import android.content.Context
import android.util.Log
import com.example.travelapp_20214034_hero.BuildConfig
import com.kakao.vectormap.KakaoMapSdk

/**
 * 지도 탭을 열 때만 카카오맵 SDK를 초기화해 앱 시작·입력 화면 부담을 줄입니다.
 */
object KakaoMapInitializer {

    private const val TAG = "KakaoMapInitializer"

    @Volatile
    private var initialized = false

    fun ensureInitialized(context: Context): Boolean {
        if (initialized) return true
        synchronized(this) {
            if (initialized) return true
            val key = BuildConfig.KAKAO_NATIVE_APP_KEY
            if (key.isBlank()) {
                Log.w(TAG, "KAKAO_NATIVE_APP_KEY is empty")
                return false
            }
            return try {
                KakaoMapSdk.init(context.applicationContext, key)
                initialized = true
                Log.i(TAG, "KakaoMapSdk initialized (lazy)")
                true
            } catch (e: Exception) {
                Log.e(TAG, "KakaoMapSdk.init failed", e)
                false
            }
        }
    }
}
