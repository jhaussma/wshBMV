package de.wsh.wshbmv.ui

import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.navigation.findNavController
import dagger.hilt.android.AndroidEntryPoint
import de.wsh.wshbmv.MyApplication
import de.wsh.wshbmv.R
import de.wsh.wshbmv.cortex_decoder.ScanActivity
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

    private lateinit var binding: ActivityMainBinding

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

    lateinit var toggle: ActionBarDrawerToggle

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


        toggle = ActionBarDrawerToggle(this, binding.drawerLayout, R.string.read_open, R.string.read_close)
        binding.drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        // für die Click-Reaktionen des Slide-In_Menü :
        binding.navView.setNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.miBMListe -> {
                    Toast.makeText(
                        applicationContext,
                        "BM-Liste geklickt",
                        Toast.LENGTH_LONG
                    ).show()
                    binding.navHostFragment.findNavController().navigate(R.id.action_global_materialFragment)
                }

                R.id.miTransfer -> Toast.makeText(
                    applicationContext,
                    "Transferlisten geklickt",
                    Toast.LENGTH_LONG
                ).show()

                R.id.miInventur -> Toast.makeText(
                    applicationContext,
                    "Inventur geklickt",
                    Toast.LENGTH_LONG
                ).show()

                R.id.miEndApp -> {
                    Toast.makeText(
                        applicationContext,
                        "Anwendung wird beendet",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                }

                R.id.miScanner -> {
                    Toast.makeText(
                        applicationContext,
                        "Scanner geklickt",
                        Toast.LENGTH_LONG
                    ).show()
                    startActivity(Intent(this, ScanActivity::class.java))
                }

                R.id.miSync -> {
                    Toast.makeText(
                        applicationContext,
                        "Synchronisieren geklickt",
                        Toast.LENGTH_LONG
                    ).show()
                }

                R.id.miSettings -> {
                    Toast.makeText(
                        applicationContext,
                        "Einstellungen geklickt",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
            true
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.app_bar_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if(toggle.onOptionsItemSelected(item)) {
            return true
        } else {
            when (item.itemId) {
                R.id.miBarcode -> Toast.makeText(this, "Barcode geklickt", Toast.LENGTH_SHORT)
                    .show()
                R.id.miSync -> Toast.makeText(this, "Sync geklickt", Toast.LENGTH_SHORT).show()
            }
            return true
        }
    }



}