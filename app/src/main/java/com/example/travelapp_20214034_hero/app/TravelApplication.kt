package com.example.travelapp_20214034_hero.app

import android.app.Application
import android.util.Log
import com.example.travelapp_20214034_hero.BuildConfig
import com.kakao.vectormap.KakaoMapSdk

class TravelApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        val key = BuildConfig.KAKAO_NATIVE_APP_KEY
        if (key.isBlank()) return
        try {
            KakaoMapSdk.init(this, key)
        } catch (e: Exception) {
            Log.e(TAG, "KakaoMapSdk.init failed", e)
        }
    }

    companion object {
        private const val TAG = "TravelApplication"
    }
}
