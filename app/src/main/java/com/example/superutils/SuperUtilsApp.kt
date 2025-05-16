package com.example.superutils

import android.app.Application
import android.content.Context

class SuperUtilsApp : Application() {

    override fun onCreate() {
        super.onCreate()
        appContext = applicationContext
    }

    companion object {
        lateinit var appContext: Context
            private set
    }
}