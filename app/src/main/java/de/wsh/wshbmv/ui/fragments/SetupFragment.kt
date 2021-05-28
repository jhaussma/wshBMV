package de.wsh.wshbmv.ui.fragments

import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import de.wsh.wshbmv.R
import de.wsh.wshbmv.databinding.FragmentSetupBinding
import de.wsh.wshbmv.db.TbmvDAO
import de.wsh.wshbmv.other.Constants.KEY_FIRST_SYNC_DONE
import de.wsh.wshbmv.other.Constants.KEY_FIRST_TIME
import de.wsh.wshbmv.other.Constants.KEY_LAGER_ID
import de.wsh.wshbmv.other.Constants.KEY_USER_NAME
import de.wsh.wshbmv.other.Constants.KEY_USER_HASH
import de.wsh.wshbmv.other.Constants.TAG
import de.wsh.wshbmv.other.GlobalVars.sqlServerConnected
import de.wsh.wshbmv.other.GlobalVars.isFirstAppStart
import de.wsh.wshbmv.other.HashUtils
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named

@AndroidEntryPoint
class SetupFragment : Fragment(R.layout.fragment_setup) {

    // Binding zu den Objekten des Fragement-Layouts
    private lateinit var bind: FragmentSetupBinding

    @Inject
    lateinit var sharedPref: SharedPreferences

    @JvmField
    @field:[Inject Named("FirstTimeAppOpend")]
    var isFirstAppOpen: Boolean = true

    @JvmField
    @field:[Inject Named("FirstSyncDone")]
    var hasFirstSyncDone: Boolean = false

    @Inject
    lateinit var tbmvDAO: TbmvDAO


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bind = FragmentSetupBinding.bind(view) // initialisiert die Binding zu den Layout-Objekten
        Timber.tag(TAG).d("OnViewCreated in SetupFragment...")
        Timber.tag(TAG).d("hasServerConnection = ${sqlServerConnected.toString()}")

//        lateinit var db: SqlDbFirstInit
//        if (hasFirstSyncDone) {
//            // wir bauen die Verbindung auf ohne weitere Aktion...
//            Timber.d("Start mit doFirstSync = false")
//            db = SqlDbFirstInit(MainRepository(tbmvDAO), false)
//            db.connectionClass = SqlConnection()
//        } else {
//            // wir bauen die Verbindung zur Erst-Synchronisierung (Userdaten) mit SQL auf...
//            Timber.d("Start mit doFirstSync = true")
//            db = SqlDbFirstInit(MainRepository(tbmvDAO), true)
//            db.connectionClass = SqlConnection()
//        }

        if (!isFirstAppStart) {
            Log.d("wshBMV", "not isFirstAppOpen")
            val navOptions = NavOptions.Builder()
                .setPopUpTo(R.id.setupFragment, true)
                .build()

            var myLager = sharedPref.getString(KEY_LAGER_ID, "") ?: ""

            if (myLager == "") {
                // wir müssen noch die Lager-Bestimmung angehen
                    //TODO hier gibts noch etwas zu klären...

                findNavController().navigate(
                    R.id.action_setupFragment_to_overviewFragment,
                    savedInstanceState,
                    navOptions
                )
            } else {
                // direkt in die Übersichts-Ansicht
                findNavController().navigate(
                    R.id.action_setupFragment_to_overviewFragment,
                    savedInstanceState,
                    navOptions
                )
            }
        } else {
            Log.d("wshBMV", "isFirstAppOpen")
        }

        bind.tvContinue.setOnClickListener {
            if (sqlServerConnected) {
                Snackbar.make(
                    requireView(),
                    "und weiter gehts...",
                    Snackbar.LENGTH_SHORT
                ).show()



            } else {
                // wir haben noch keine Serververbindung...
                Snackbar.make(
                    requireView(),
                    "Keine Serververbindung, bitte kurz warten...",
                    Snackbar.LENGTH_SHORT
                ).show()
            }
//            if (db.isError || !db.isConnected) {
//                Snackbar.make(
//                    requireView(),
//                    "Fehlende Serververbindung, ...",
//                    Snackbar.LENGTH_SHORT
//                ).show()
//            } else if (db.inProgress) {
//                Snackbar.make(
//                    requireView(),
//                    "Synchronisiere Userdaten, bitte warten...",
//                    Snackbar.LENGTH_SHORT
//                ).show()
//            } else {
//                // prüfe zuerst auf HashCode-Username
//
//                val success = writeUserInfoToSharedPref()
//                if (success) {
//                    findNavController().navigate(R.id.action_setupFragment_to_synchronizeFragment)
//                } else {
//                    Snackbar.make(
//                        requireView(),
//                        "Bitte zuerst Username und Passwort eingeben!",
//                        Snackbar.LENGTH_SHORT
//                    ).show()
//                }
//            }
        }
    }

    // prüft die Userdaten auf korrekten Inhalt
    private fun checkUserInfo() : Boolean {



        return true
    }

    private fun writeUserInfoToSharedPref(): Boolean {
        val username = bind.etUserName.text.toString()
        val userHash = HashUtils.sha256(bind.etUserPwd.text.toString())
        // überprüfe die Plausibilität der Eingaben
        if (username.isEmpty() || userHash.isEmpty() || userHash.length < 4) {
            return false
        }
        sharedPref.edit()
            .putString(KEY_USER_NAME, username)
            .putString(KEY_USER_HASH, userHash)
            .putBoolean(KEY_FIRST_TIME, false)
            .putBoolean(KEY_FIRST_SYNC_DONE, false)
            .apply()
        return true
    }


}