package io.kitsuri.m1rage.globals

import android.app.Application
import io.kitsuri.m1rage.model.SettingsManager


class AppContext : Application() {

    companion object {
        lateinit var instance: AppContext
            private set

        lateinit var settingsManager: SettingsManager
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        settingsManager = SettingsManager(this)

    }

    override fun onTerminate() {
        super.onTerminate()
    }
}