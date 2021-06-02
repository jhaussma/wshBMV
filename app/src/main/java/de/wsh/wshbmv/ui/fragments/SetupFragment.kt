package de.wsh.wshbmv.ui.fragments

import android.content.SharedPreferences
import android.os.Bundle
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
import de.wsh.wshbmv.other.Constants.KEY_LAGER_NAME
import de.wsh.wshbmv.other.Constants.KEY_USER_NAME
import de.wsh.wshbmv.other.Constants.KEY_USER_HASH
import de.wsh.wshbmv.other.Constants.TAG
import de.wsh.wshbmv.other.GlobalVars.sqlServerConnected
import de.wsh.wshbmv.other.GlobalVars.isFirstAppStart
import de.wsh.wshbmv.other.GlobalVars.myLager
import de.wsh.wshbmv.other.GlobalVars.myUser
import de.wsh.wshbmv.other.GlobalVars.sqlUserLoaded
import de.wsh.wshbmv.other.HashUtils
import de.wsh.wshbmv.repositories.MainRepository
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named

@AndroidEntryPoint
class SetupFragment @Inject constructor(
    private val mainRepository: MainRepository
) : Fragment(R.layout.fragment_setup) {

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
        bind.tvLager.visibility = View.INVISIBLE

        if (!isFirstAppStart) {
            Timber.tag(TAG).d( "not isFirstAppOpen")
            val navOptions = NavOptions.Builder()
                .setPopUpTo(R.id.setupFragment, true)
                .build()

            val myLager = sharedPref.getString(KEY_LAGER_ID, "") ?: ""

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
            Timber.tag(TAG).d( "isFirstAppOpen")
        }

        bind.tvContinue.setOnClickListener {
            if (!sqlServerConnected) {
                // wir haben noch keine Serververbindung...
                Snackbar.make(
                    requireView(),
                    "Keine Serververbindung, bitte kurz warten...",
                    Snackbar.LENGTH_LONG
                ).show()
            } else if (!sqlUserLoaded) {
                // die User-Tabellen wurden noch nicht vollständig geladen...
                Snackbar.make(
                    requireView(),
                    "Die Benutzerdaten fehlen noch, bitte kurz warten...",
                    Snackbar.LENGTH_LONG
                ).show()
            } else {
                // nun erfolgt die User-Überprüfung
                val message = checkUserInfo()
                if (message != "Okay") {
                    Snackbar.make(
                        requireView(),
                        message,
                        Snackbar.LENGTH_LONG
                    ).show()
                } else {
                    // die Anmeldung hat auch geklappt, nun müssen wir nur noch das Ende der Installation abwarten...
                    //.. und schreiben die Preferenzen ins User-Log
                    writeUserInfoToSharedPref()
                    // wir machen die Lagerzuordnung sichtbar
                    bind.tvLager.text = myLager!!.matchcode
                    bind.tvLager.visibility = View.VISIBLE
                    // .. blockieren die Eingabemöglichkeiten...
                    bind.etUserName.isEnabled = false
                    bind.etUserPwd.isEnabled = false
                    bind.tvContinue.isEnabled = false
                    // und melden weitere Wartezeit an...
                    Snackbar.make(
                        requireView(),
                        "Bitte warten, die Synchronisierung geht weiter...",
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    // prüft die Userdaten auf korrekten Inhalt und Berechtigungen
    private fun checkUserInfo(): String {
        // wurden Anmeldename und Passwort eingetragen?
        val userName = bind.etUserName.text.toString()
        val userPwd = bind.etUserPwd.text.toString()

        if ((userName.length < 4) || (userPwd.length < 4)) {
            return "Anmeldename/Passwort müssen mind. 4 Zeichen beinhalten!"
        }
        myUser = mainRepository.getUserByLogName(userName)
        if (myUser == null) {
            // dieser User ist uns nicht bekannt!
            return "Benutzer $userName ist unbekannt!"
        }
        if (myUser?.passHash != null) {
            // in diesem Falle muss das Passwort überprüft werden
            val myHash = HashUtils.sha256(bind.etUserPwd.toString())
            if (myHash != myUser?.passHash) {
                return "Falsches Passwort bitte korrigieren!"
            }
        } else {
            // neues Passwort eintragen...
            myUser!!.passHash = HashUtils.sha256(bind.etUserPwd.toString())
            mainRepository.updateUser(myUser!!)
        }
        // nun klären wir Berechtigungen und ggf. die Lager-Zuordnung
        if (myUser!!.bmvR + myUser!!.bmvW + myUser!!.bmvAdmin == 0) {
            // der Benutzer darf keine BMV-Daten sehen
            return "Sie sind für Betriebsmittel nicht freigeschaltet!"
        }
        val lager = mainRepository.getLagerByUserID(myUser!!.id)
        myLager = if (lager.isEmpty()) {
            if (myUser!!.bmvAdmin == 0) {
                // der Benutzer ist keinem Lager zugeordnet und hat keine Admin-Berechtigung
                return "Sie sind keinem Lager zugeordnet, fehlende Berechtigung!"
            } else {
                // als Admin ohne Lagerzuordnung ordnen wir das Hauptlager zu
                mainRepository.getLagerByName("Lager")
            }
        } else {
            // wir ordnen das (erste der gefundenen) Lager zu
            lager.first()
        }
        return "Okay"
    }


    private fun writeUserInfoToSharedPref()  {
        val username = bind.etUserName.text.toString()
        val userHash = HashUtils.sha256(bind.etUserPwd.text.toString())
        val lagerID = myLager!!.id
        val lagerName = myLager!!.matchcode
        sharedPref.edit()
            .putString(KEY_USER_NAME, username)
            .putString(KEY_USER_HASH, userHash)
            .putBoolean(KEY_FIRST_TIME, false)
            .putBoolean(KEY_FIRST_SYNC_DONE, false)
            .putString(KEY_LAGER_ID, lagerID)
            .putString(KEY_LAGER_NAME, lagerName)
            .apply()
        return
    }

    private fun writeSyncDoneToSharedPref()  {
        sharedPref.edit()
            .putBoolean(KEY_FIRST_SYNC_DONE, true)
            .apply()
    }


}