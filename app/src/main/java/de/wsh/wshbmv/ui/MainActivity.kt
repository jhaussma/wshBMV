package de.wsh.wshbmv.ui

import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import com.google.android.material.navigation.NavigationView
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
import de.wsh.wshbmv.ui.fragments.MaterialFragment
import de.wsh.wshbmv.ui.fragments.SettingsFragment
import de.wsh.wshbmv.ui.fragments.TransferlistFragment
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named

@AndroidEntryPoint
class MainActivity : AppCompatActivity(), FragCommunicator,
    NavigationView.OnNavigationItemSelectedListener {

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
    private lateinit var mainRepo: MainRepository

    private lateinit var toggle: ActionBarDrawerToggle

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.tag(TAG).d("Start MainActivity mit onCreate")

        mainRepo = MainRepository(tbmvDAO)

        // erste Statusabklärung...
        isFirstAppStart = _isFirstAppStart
        firstSyncCompleted = hasFirstSyncDone
        // wir starten die MS-SQL-Serververbindung
        db = SqlDbFirstInit(MainRepository(tbmvDAO), isFirstAppStart)
        db.connectionClass = SqlConnection()

        Timber.tag(TAG).d("isFirstAppStart = $isFirstAppStart")
        Timber.tag(TAG).d("firstSyncCompleted = $firstSyncCompleted")

        // wir starten den Layout-Inflater
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)


        toggle = ActionBarDrawerToggle(
            this,
            binding.drawerLayout,
            R.string.read_open,
            R.string.read_close
        )
        binding.drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        // Click-Reaktionen des Slide-In_Menü  einbinden
        binding.navView.setNavigationItemSelectedListener(this)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.app_bar_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (toggle.onOptionsItemSelected(item)) {
            true
        } else {
            when (item.itemId) {
                R.id.miBarcode -> Toast.makeText(this, "Barcode geklickt", Toast.LENGTH_SHORT)
                    .show()
                R.id.miSync -> Toast.makeText(this, "Sync geklickt", Toast.LENGTH_SHORT).show()
            }
            true
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        binding.drawerLayout.closeDrawer(GravityCompat.START)
        when (item.itemId) {
            R.id.miBMListe -> {
                Toast.makeText(
                    applicationContext,
                    "BM-Liste geklickt",
                    Toast.LENGTH_LONG
                ).show()
                // nur für Testzwecke!!
                binding.navHostFragment.findNavController()
                    .navigate(R.id.action_global_materialFragment)
            }

            R.id.miTransfer -> {
                Toast.makeText(
                    applicationContext,
                    "Transferlisten geklickt",
                    Toast.LENGTH_LONG
                ).show()
                setToolbarTitel("Transfer")
                changeFragment(TransferlistFragment())
            }

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
                // hier startet der Scanner
                // entweder:
                Intent(this,ScanActivity::class.java).also {
                    startActivity(it)
                }
                // oder alternativ:
//                startActivity(Intent(this, ScanActivity::class.java))

//                // nur für Testzwecke!!
//                val job = GlobalScope.launch(Dispatchers.IO) {
//                    Timber.tag(TAG).d("Datenausgabeversuch für mein Material:")
//                    var bmDaten = mainRepo.getBMDatenZuMatID("5F23C813-ED3F-4C76-BD4E-7D86f3206A18")
//                    Timber.tag(TAG).d(bmDaten.toString())
//                }
//                runBlocking {
//                    job.join()
//                }

//                setToolbarTitel("Transfer")
//                changeFragment(MaterialFragment())
            }

            R.id.miSync -> {
                Toast.makeText(
                    applicationContext,
                    "Synchronisieren geklickt",
                    Toast.LENGTH_LONG
                ).show()
            }

            R.id.miSettings -> {
                setToolbarTitel("Einstellungen")
                changeFragment(SettingsFragment())
            }
        }
        return true
    }

    private fun setToolbarTitel(title: String) {
        supportActionBar?.title = title
    }

    private fun changeFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction().apply {
            replace(R.id.navHostFragment, fragment)
            addToBackStack(fragment::class.java.name)
            commit()
        }
    }

    // Start einer Material-Detailsicht
    override fun passBmDataID(materialId: String) {
        val bundle = Bundle()
        bundle.putString("materialId", materialId)
        Timber.tag(TAG).d("override passBmDataID mit: $materialId")

//        val transaction = supportFragmentManager.beginTransaction()
        val fragmentMaterial = MaterialFragment()
        fragmentMaterial.arguments = bundle
        supportFragmentManager.beginTransaction().apply {
            replace(R.id.navHostFragment, fragmentMaterial)
            addToBackStack(fragmentMaterial::class.java.name)
            commit()
        }
    }

}