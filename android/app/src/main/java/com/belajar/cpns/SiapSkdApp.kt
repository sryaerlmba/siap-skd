package com.belajar.cpns

import android.app.Application
import com.belajar.cpns.di.AppContainer
import com.belajar.cpns.di.DefaultAppContainer

class SiapSkdApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)
    }
}
