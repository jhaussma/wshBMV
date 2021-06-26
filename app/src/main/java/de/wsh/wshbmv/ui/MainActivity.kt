package de.wsh.wshbmv.ui

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.core.content.FileProvider
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import com.codecorp.cortexdecoderlibrary.BuildConfig
import com.google.android.material.navigation.NavigationView
import dagger.hilt.android.AndroidEntryPoint
import de.wsh.wshbmv.MyApplication
import de.wsh.wshbmv.R
import de.wsh.wshbmv.cortex_decoder.ScanActivity
import de.wsh.wshbmv.databinding.ActivityMainBinding
import de.wsh.wshbmv.db.TbmvDAO
import de.wsh.wshbmv.other.Constants.PIC_SCALE_FILTERING
import de.wsh.wshbmv.other.Constants.PIC_SCALE_HEIGHT
import de.wsh.wshbmv.other.Constants.TAG
import de.wsh.wshbmv.other.GlobalVars.firstSyncCompleted
import de.wsh.wshbmv.other.GlobalVars.hasNewBarcode
import de.wsh.wshbmv.other.GlobalVars.isFirstAppStart
import de.wsh.wshbmv.other.GlobalVars.newBarcode
import de.wsh.wshbmv.repositories.MainRepository
import de.wsh.wshbmv.sql_db.SqlConnection
import de.wsh.wshbmv.sql_db.SqlDbFirstInit
import de.wsh.wshbmv.ui.fragments.*
import pub.devrel.easypermissions.EasyPermissions
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Named
import kotlin.math.roundToInt
import kotlin.system.exitProcess

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

    private val PERMISSION_LIST: Array<String> = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.INTERNET
    )
    private val PERMISSION_REQUEST = 100

    // für den Photo-Import in Fragments...
    private var photoFile: File? = null
    private val CAPTURE_IMAGE_REQUEST = 1
    private var mCurrentPhotoPath: String? = null

    private var importFragment: Fragment? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.tag(TAG).d("MainActivity, onCreate")
        mainRepo = MainRepository(tbmvDAO)

        // erste Statusabklärung...
        isFirstAppStart = _isFirstAppStart
        firstSyncCompleted = hasFirstSyncDone
        // wir starten die MS-SQL-Serververbindung
        db = SqlDbFirstInit(MainRepository(tbmvDAO), isFirstAppStart)
        db.connectionClass = SqlConnection()

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
                    // ermittle das aktive Fragment und speichere es in barcodeImportFragment
                    importFragment = getVisibleFragment()
                    startBarcodeScanner()
                    true
                }

                R.id.miSync -> {
                    Toast.makeText(this, "Sync geklickt", Toast.LENGTH_SHORT).show()
                    true
                }

                R.id.miMatAddPhoto -> {
                    // ermittle das aktive Fragment und speichere es in photoImpoortFragment...
                    importFragment = getVisibleFragment()
                    captureImage()
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
//                binding.navHostFragment.findNavController()
//                    .navigate(R.id.action_global_materialFragment)
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
                exitProcess(0)
            }

            R.id.miScanner -> {
                // hier startet der Barcodescanner
                importFragment = getVisibleFragment()
                startBarcodeScanner()
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

    // hier verarbeiten wir ggf. einen empfangenen Barcode...
    override fun onResume() {
        super.onResume()
        Timber.tag(TAG).d("MainActivity, onResume ...")
        if (hasNewBarcode) {
            newBarcode?.let { sendBarcodeToFragment(it) }
        }
    }


    /** xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
     *   Aufnahme eines Photos
     */
    private fun captureImage() {
        if (EasyPermissions.hasPermissions(this, *PERMISSION_LIST)) {
            val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            if (takePictureIntent.resolveActivity(packageManager) != null) {
                // Create the File where the photo should go
                try {
                    photoFile = createImageFile()
                    // Continue only if the File was successfully created
                    if (photoFile != null) {
                        val photoURI = FileProvider.getUriForFile(
                            this,
                            "de.wsh.wshbmv.fileprovider",
                            photoFile!!
                        )
                        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                        startActivityForResult(takePictureIntent, CAPTURE_IMAGE_REQUEST)
                    }
                } catch (ex: Exception) {
                    // Error occurred while creating the File
                    Toast.makeText(
                        applicationContext,
                        ex.message.toString(),
                        Toast.LENGTH_LONG
                    ).show()
                }

            } else {
                Toast.makeText(applicationContext, "kein Bild", Toast.LENGTH_LONG).show()
            }
        } else {
            EasyPermissions.requestPermissions(
                this,
                "Bitte alle Berechtigungen zulassen!",
                PERMISSION_REQUEST,
                *PERMISSION_LIST
            )
        }

    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        // Create an image file name
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.GERMANY).format(Date())
        val imageFileName = "JPEG_" + timeStamp + "_"
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val image = File.createTempFile(
            imageFileName, /* prefix */
            ".jpg", /* suffix */
            storageDir      /* directory */
        )

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.absolutePath
        return image
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == CAPTURE_IMAGE_REQUEST && resultCode == Activity.RESULT_OK) {
            // hier wird das Bild nun in ein Bitmap aufgenommen, scaliert und ans Fragment weitergereicht ...
            val myBitmap = BitmapFactory.decodeFile(photoFile!!.absolutePath)
            val aspectRatio = myBitmap.height.toDouble() / myBitmap.width
            val newHeight = PIC_SCALE_HEIGHT // neue Zielhöhe ist festgelegt in den Konstanten
            val newWidth = (newHeight.toDouble() / aspectRatio).roundToInt() // das Seiten-/Höhenverhältnis bleibt erhalten
            val myScaledBitmap =
                Bitmap.createScaledBitmap(myBitmap, newWidth, newHeight, PIC_SCALE_FILTERING)
            sendPhotoToFragment(myScaledBitmap)
        } else {
            Toast.makeText(applicationContext, "Photo-Import wurde abgebrochen.", Toast.LENGTH_LONG)
                .show()
        }
    }

    // sendet das Photo-Bitmap zur Weiterverarbeitung an das jeweilige Fragment
    private fun sendPhotoToFragment(bitmap: Bitmap) {
        when (importFragment!!::class.java.name) {
            "de.wsh.wshbmv.ui.fragments.MaterialFragment" -> {
                val materialFragment: MaterialFragment = importFragment as MaterialFragment
                materialFragment.importNewPhoto(bitmap)
            }
        }
    }

    /** xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
     *   Import eines Barcodes...
     */
    // startet den Barcodescanner und merkt sich das aktive Fragment für die Rückkehr
    private fun startBarcodeScanner() {
        // lösche die Ergebnisfelder für den Barcode
        hasNewBarcode = false
        newBarcode = null
        // starte den Barcode-Scanner
        Intent(this, ScanActivity::class.java).also {
            startActivity(it)
        }
    }

    // sendet einen Barcode an das zuletzt aktive Fragment der Anwendung
    private fun sendBarcodeToFragment(barcode: String) {
        when (importFragment!!::class.java.name) {
            "de.wsh.wshbmv.ui.fragments.MaterialFragment" -> {
                val materialFragment: MaterialFragment = importFragment as MaterialFragment
                materialFragment.importNewBarcode(barcode)
            }
            "de.wsh.wshbmv.ui.fragments.OverviewFragment" -> {
                val overviewFragment: OverviewFragment = importFragment as OverviewFragment
                overviewFragment.importNewBarcode(barcode)
            }
        }
    }


    /** xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
     *   Hilfsfunktionen für Fragment-Wechsel und Einstellung
     */
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

    private fun getVisibleFragment(): Fragment? {
        var fragments = supportFragmentManager.fragments
        var fragment: Fragment? = null
        if (fragments.isNotEmpty()) {
            run loop@{
                fragments.forEach {
                    if (it.isVisible) {
                        fragment = it
                        return@loop
                    }
                }
            }
        }
        if (fragment != null) {
            if (fragment!!::class.java.name == "androidx.navigation.fragment.NavHostFragment") {
                // wir suchen im ChildFragmentManager...
                fragments = fragment!!.childFragmentManager.fragments
                fragment = null
                if (fragments.isNotEmpty()) {
                    run loop@{
                        fragments.forEach {
                            if (it.isVisible) {
                                fragment = it
                                return@loop
                            }
                        }
                    }
                }
                return fragment
            } else {
                return fragment
            }
        }
        return null
    }


    /** xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
     *   Übergaben über den FragCommunicator (aus Fragmenten oder anderen Activities)
     */

    // Start einer Material-Detailsicht
    override fun passBmDataID(materialId: String) {
        Timber.tag(TAG).d("MainActivity, passBmDataID mit materialID $materialId gestartet")
        val bundle = Bundle()
        bundle.putString("materialId", materialId)

        val fragmentMaterial = MaterialFragment()
        fragmentMaterial.arguments = bundle
        supportFragmentManager.beginTransaction().apply {
            replace(R.id.navHostFragment, fragmentMaterial)
            addToBackStack(fragmentMaterial::class.java.name)
            commit()
        }
    }

    // Start einer Beleg-Detailsicht
    override fun passBelegID(belegId: String) {
        Timber.tag(TAG).d("MainActivity, passBelegID mit belegID $belegId gestartet")
        val bundle = Bundle()
        bundle.putString("belegId", belegId)

        val fragmentBeleg = BelegFragment()
        fragmentBeleg.arguments = bundle
        supportFragmentManager.beginTransaction().apply {
            replace(R.id.navHostFragment, fragmentBeleg)
            addToBackStack(fragmentBeleg::class.java.name)
            commit()
        }
    }


    /** xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
     *   wir prüfen alle notwendigen Permissions für die APP
     */
    // die Permissions....
    private fun checkSDKLevel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                val uri = Uri.parse("package:" + BuildConfig.APPLICATION_ID)
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, uri)
                startActivity(intent)
            }
            checkPermission()
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