package com.netify.app

import android.app.Application
import com.netify.app.di.AppContainer

class NetifyApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
