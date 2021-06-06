package de.wsh.wshbmv

import android.app.Application
import com.codecorp.decoder.CortexDecoderLibrary
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())
    }
}