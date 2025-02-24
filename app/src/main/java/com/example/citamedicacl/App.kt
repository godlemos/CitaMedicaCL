package com.example.citamedicacl

import android.app.Application
import android.content.SharedPreferences

class App : Application() {
    companion object {
        lateinit var prefs: SharedPreferences
            private set
    }

    override fun onCreate() {
        super.onCreate()
        prefs = getSharedPreferences("CitaMedicaCL", MODE_PRIVATE)
    }
} 