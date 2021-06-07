package de.wsh.wshbmv.ui

import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.codecorp.decoder.CortexDecoderLibrary
import dagger.hilt.android.AndroidEntryPoint
import de.wsh.wshbmv.MyApplication
import de.wsh.wshbmv.databinding.ActivityMainBinding
import de.wsh.wshbmv.db.TbmvDAO
import de.wsh.wshbmv.other.Constants.TAG
import de.wsh.wshbmv.other.GlobalVars.firstSyncCompleted
import de.wsh.wshbmv.other.GlobalVars.isFirstAppStart
import de.wsh.wshbmv.repositories.MainRepository
import de.wsh.wshbmv.sql_db.SqlConnection
import de.wsh.wshbmv.sql_db.SqlDbFirstInit
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private  lateinit var binding: ActivityMainBinding

//    @Inject
//    lateinit var cortexScan: CortexDecoderLibrary

    @Inject
    lateinit var app: MyApplication

    @Inject
    lateinit var sharedPref: SharedPreferences

    @JvmField
    @field:[Inject Named("FirstTimeAppOpend")]
    var _isFirstAppStart: Boolean = true

    @JvmField
    @field:[Inject Named("FirstSyncDone")]
    var hasFirstSyncDone: Boolean = false

    @Inject
    lateinit var tbmvDAO: TbmvDAO
    lateinit var db: SqlDbFirstInit


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.tag(TAG).d("Start MainActivity mit onCreate")
        // erste Statusabklärung...
        isFirstAppStart = _isFirstAppStart
        firstSyncCompleted = hasFirstSyncDone
        // wir starten die MS-SQL-Serververbindung
        db = SqlDbFirstInit(MainRepository(tbmvDAO), isFirstAppStart)
        db.connectionClass = SqlConnection()

        Timber.tag(TAG).d("isFirstAppStart = ${isFirstAppStart.toString()}")
        Timber.tag(TAG).d("firstSyncCompleted = ${firstSyncCompleted.toString()}")

        // wir starten den Layout-Inflater
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // starte den CortexDecoder
//        cortexScan = CortexDecoderLibrary.sharedObject(this,"noCamera")

    }

}