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
import de.wsh.wshbmv.other.GlobalVars.firstSyncCompleted
import de.wsh.wshbmv.other.GlobalVars.sqlServerConnected
import de.wsh.wshbmv.other.GlobalVars.myLager
import de.wsh.wshbmv.other.GlobalVars.myUser
import de.wsh.wshbmv.other.GlobalVars.sqlUserLoaded
import de.wsh.wshbmv.other.GlobalVars.sqlUserNewPassHash
import de.wsh.wshbmv.other.HashUtils
import kotlinx.coroutines.*
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named

@AndroidEntryPoint
class SetupFragment : Fragment(R.layout.fragment_setup) {

    // Binding zu den Objekten des Fragement-Layouts
    private lateinit var bind: FragmentSetupBinding

    @Inject
    lateinit var tbmvDAO: TbmvDAO

    @Inject
    lateinit var sharedPref: SharedPreferences

    @JvmField
    @field:[Inject Named("UserName")]
    var userName: String = ""

    @JvmField
    @field:[Inject Named("LagerOrt")]
    var lagerId: String = ""


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bind = FragmentSetupBinding.bind(view) // initialisiert die Binding zu den Layout-Objekten
        bind.tvLager.visibility = View.INVISIBLE

        if (firstSyncCompleted) {
            if (myUser == null || myLager == null) {
                val job = GlobalScope.launch(Dispatchers.IO) {
                    myUser = tbmvDAO.getUserByLogName(userName)
                    myLager = tbmvDAO.getLagerByID(lagerId)
                }
                runBlocking {
                    job.join()
                }
                if (myUser == null) {
                    Snackbar.make(
                        requireView(),
                        "Der User-Datensatz wurde nicht gefunden...",
                        Snackbar.LENGTH_LONG
                    ).show()
                    // ggf. muss hier mal eine Rettungs-Reaktion eingeführt werden
                }
                if (myLager == null) {
                    Snackbar.make(
                        requireView(),
                        "Der Lager-Datensatz wurde nicht gefunden...",
                        Snackbar.LENGTH_LONG
                    ).show()
                    // ggf. muss hier mal eine Rettungs-Reaktion eingeführt werden
                }
            }
            val navOptions = NavOptions.Builder()
                .setPopUpTo(R.id.setupFragment, true)
                .build()
            // direkt in die Übersichts-Ansicht
            findNavController().navigate(
                R.id.action_setupFragment_to_overviewFragment,
                savedInstanceState,
                navOptions
            )
        }

        // wir haben Userdaten abzufragen und warten auf die Synchronisierung...
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
                // nun erfolgt die User-Überprüfung in einem IO-Thread
                var message = ""
                val job = GlobalScope.launch(Dispatchers.IO) {
                    message = checkUserInfo()
                }
                // wir warten auf das Ergebnis...
                runBlocking {
                    job.join()
                }
                // die Auswertung...
                if (message != "Okay") {
                    Snackbar.make(
                        requireView(),
                        message,
                        Snackbar.LENGTH_LONG
                    ).show()
                } else {
                    // die Anmeldung hat geklappt, nun müssen wir nur noch das Ende der Installation abwarten...
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

                    // und warten nun, bis die Synchronisierung fertig ist...
                    val job = GlobalScope.launch(Dispatchers.Default) {
                        while (!firstSyncCompleted) {
                            delay(500)
                        }
                    }
                    runBlocking {
                        job.join()
                    }
                    // nun schreiben wir die Info ins Preference-Log des Users
                    writeSyncDoneToSharedPref()

                    // und wechseln in die Haupt-Übersicht...
                    val navOptions = NavOptions.Builder()
                        .setPopUpTo(R.id.setupFragment, true)
                        .build()
                    findNavController().navigate(
                        R.id.action_setupFragment_to_overviewFragment,
                        savedInstanceState,
                        navOptions
                    )
                }
            }
        }
    }

    // prüft die Userdaten auf korrekten Inhalt und Berechtigungen
    private suspend fun checkUserInfo(): String {
        // wurden Anmeldename und Passwort eingetragen?
        val userName = bind.etUserName.text.toString()
        val userPwd = bind.etUserPwd.text.toString()

        if ((userName.length < 4) || (userPwd.length < 4)) {
            return "Anmeldename/Passwort müssen mind. 4 Zeichen beinhalten!"
        }

        // nun werden die Daten abgefragt (in einem IO-Thread)..
        myUser = tbmvDAO.getUserByLogName(userName)
        if (myUser == null) {
            // dieser User ist uns nicht bekannt!
            return   "Benutzer $userName ist unbekannt!"
        }
        if (myUser?.passHash != null) {
            // in diesem Falle muss das Passwort überprüft werden
            val myHash = HashUtils.sha256(bind.etUserPwd.toString())
            if (myHash != myUser?.passHash) {
                return  "Falsches Passwort bitte korrigieren!"
            }
        } else {
            // neues Passwort eintragen...
            myUser!!.passHash = HashUtils.sha256(bind.etUserPwd.toString())
            tbmvDAO.updateUser(myUser!!)
            sqlUserNewPassHash = true
        }
        // nun klären wir Berechtigungen und ggf. die Lager-Zuordnung
        if (myUser!!.bmvR + myUser!!.bmvW + myUser!!.bmvAdmin == 0) {
            // der Benutzer darf keine BMV-Daten sehen
            return "Sie sind für Betriebsmittel nicht freigeschaltet!"
        }
        val myLagerList = tbmvDAO.getLagerByUserID(myUser!!.id)
        if (myLagerList.isEmpty()) {
            if (myUser!!.bmvAdmin == 0) {
                // der Benutzer ist keinem Lager zugeordnet und hat keine Admin-Berechtigung
                return "Sie sind keinem Lager zugeordnet, fehlende Berechtigung!"
            } else {
                // als Admin ohne Lagerzuordnung ordnen wir das Hauptlager zu
                myLager = tbmvDAO.getLagerByName("Lager")
            }
        } else {
            // wir ordnen das (erste der gefundenen) Lager zu
            myLager = myLagerList.first()
        }
        return "Okay"
    }


    private fun writeUserInfoToSharedPref() {
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

    private fun writeSyncDoneToSharedPref() {
        sharedPref.edit()
            .putBoolean(KEY_FIRST_SYNC_DONE, true)
            .apply()
    }


}