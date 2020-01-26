package com.pickle.maplocationsample

import android.app.Application
import timber.log.Timber

class MapLocationSampleApp : Application(){

    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())
    }

}