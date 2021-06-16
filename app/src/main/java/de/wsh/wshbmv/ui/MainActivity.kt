package de.wsh.wshbmv.ui

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import com.codecorp.cortexdecoderlibrary.BuildConfig
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
import pub.devrel.easypermissions.EasyPermissions
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named

@AndroidEntryPoint
class MainActivity : AppCompatActivity(), FragCommunicator, EasyPermissions.PermissionCallbacks,
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

    val PERMISSION_LIST: Array<String> = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.INTERNET
    )
    val PERMISSION_REQUEST = 5679


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
        checkSDKLevel()

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
                R.id.miBarcode -> {
                    Toast.makeText(this, "Barcode geklickt", Toast.LENGTH_SHORT).show()
                    false
                }
                R.id.miSync -> {
                    Toast.makeText(this, "Sync geklickt", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.miMatAddPhoto -> {
                    Toast.makeText(this, "Bild wird aufgenommen", Toast.LENGTH_SHORT).show()
                    true
                }

                else -> true
            }
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
                Intent(this, ScanActivity::class.java).also {
                    startActivity(it)
                }
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

    override fun passNewPhoto(uri: Uri) {
        TODO("Not yet implemented")
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

    // die Permissions....
    private fun checkSDKLevel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                val uri = Uri.parse("package:" + BuildConfig.APPLICATION_ID)
                Timber.tag(TAG).d("Uri: ${uri.toString()}")
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, uri)
                startActivity(intent)
            }

            checkPermission()
//        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//            checkPermission()
        } else {
            checkPermission()
        }
    }

    private fun checkPermission() {
        if (EasyPermissions.hasPermissions(this, *PERMISSION_LIST)) {
            Timber.tag(TAG).d("Alle APP-Permissions sind freigegeben...")
        } else {
            EasyPermissions.requestPermissions(
                this,
                "Bitte alle Berechtigungen zulassen!",
                PERMISSION_REQUEST,
                *PERMISSION_LIST
            )
        }
    }

    // Easy Permission -Funktionen
    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {
        Toast.makeText(this, "Bitte alle Brechtigungen zulassen!", Toast.LENGTH_SHORT).show()
    }

    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {
        if (requestCode == PERMISSION_REQUEST && perms.size == PERMISSION_LIST.size) {
            Toast.makeText(this, "Berechtigungen wurden eingetragen", Toast.LENGTH_SHORT).show()
            Timber.tag(TAG).d("Alle APP-Permissions sind freigegeben...")
        } else {
            Toast.makeText(this, "Bitte alle Brechtigungen zulassen!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

}