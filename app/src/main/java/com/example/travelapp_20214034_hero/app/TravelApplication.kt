package com.example.travelapp_20214034_hero.app

import android.app.Application
import com.example.travelapp_20214034_hero.BuildConfig
import com.kakao.vectormap.KakaoMapSdk

class TravelApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        val key = BuildConfig.KAKAO_NATIVE_APP_KEY
        if (key.isNotBlank()) {
            KakaoMapSdk.init(this, key)
        }
    }
}
