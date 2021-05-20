package de.wsh.wshbmv

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import javax.inject.Named

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var app: MyApplication

    @Inject
    @Named("String1")
    lateinit var testString: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Log.d("MainActivity", "Test String from MainAcitivity: $testString")
        Log.d("MainActivity", "Testnachricht ${app.getString(R.string.app_name)}")
    }



}