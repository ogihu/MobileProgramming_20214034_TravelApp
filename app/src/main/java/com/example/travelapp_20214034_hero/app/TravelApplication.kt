package com.example.travelapp_20214034_hero.app

import android.app.Application
import com.bumptech.glide.Glide
import com.example.travelapp_20214034_hero.common.KakaoMapInitializer

class TravelApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        KakaoMapInitializer.ensureInitialized(this)
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= TRIM_MEMORY_RUNNING_LOW) {
            Glide.get(this).clearMemory()
        }
    }

    override fun onLowMemory() {
        super.onLowMemory()
        Glide.get(this).clearMemory()
    }
}
