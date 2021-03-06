package de.wsh.wshbmv.ui.fragments

import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import de.wsh.wshbmv.R
import de.wsh.wshbmv.databinding.FragmentSetupBinding
import de.wsh.wshbmv.db.TbmvDAO
import de.wsh.wshbmv.db.entities.TbmvLager
import de.wsh.wshbmv.other.Constants.KEY_FIRST_SYNC_DONE
import de.wsh.wshbmv.other.Constants.KEY_FIRST_TIME
import de.wsh.wshbmv.other.Constants.KEY_LAGER_ID
import de.wsh.wshbmv.other.Constants.KEY_LAGER_NAME
import de.wsh.wshbmv.other.Constants.KEY_USER_NAME
import de.wsh.wshbmv.other.Constants.KEY_USER_HASH
import de.wsh.wshbmv.other.Constants.TAG
import de.wsh.wshbmv.other.GlobalVars.firstSyncCompleted
import de.wsh.wshbmv.other.GlobalVars.isFirstAppStart
import de.wsh.wshbmv.other.GlobalVars.sqlServerConnected
import de.wsh.wshbmv.other.GlobalVars.myLager
import de.wsh.wshbmv.other.GlobalVars.myLagers
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
    @field:[Inject Named("UserHash")]
    var userHash: String = ""

    @JvmField
    @field:[Inject Named("LagerId")]
    var lagerId: String = ""

    @JvmField
    @field:[Inject Named("LagerName")]
    var lagerName: String = ""


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bind = FragmentSetupBinding.bind(view) // initialisiert die Binding zu den Layout-Objekten
        bind.tvLager.visibility = View.INVISIBLE


        // wir laden zuerst die bekannten User-Daten in die Anmeldemaske...
        if (!isFirstAppStart) loadUserInfo()

        if (firstSyncCompleted) {
            var message = verifyUserInfo()
            if (message == "Okay") {
                Timber.tag(TAG).d("verifyUserInfo meldet 'Okay'")
                // schreibe die Anmeldedaten in die SharedPreferences zur??ck
                writeUserInfoToSharedPref()

                // l??sche das Fragement vom BackStack
                val navOptions = NavOptions.Builder()
                    .setPopUpTo(R.id.setupFragment, true)
                    .build()
                // und wechsle direkt in die Betriebsmittel-??bersichtsansicht
                findNavController().navigate(
                    R.id.action_setupFragment_to_overviewFragment,
                    savedInstanceState,
                    navOptions
                )

            } else {
                Snackbar.make(
                    requireView(),
                    message,
                    Snackbar.LENGTH_LONG
                ).show()

            }
        }

        /**
         *  User-Anmeldung verarbeiten
         */
        // wir haben Userdaten abzufragen, bei Erststart warten wir u.U. noch auf die Synchronisierung...
        bind.tvContinue.setOnClickListener {
            if (!firstSyncCompleted && !sqlServerConnected) {
                // wir haben noch keine Serververbindung...
                Snackbar.make(
                    requireView(),
                    "Keine Serververbindung, bitte kurz warten...",
                    Snackbar.LENGTH_LONG
                ).show()
            } else if (!firstSyncCompleted && !sqlUserLoaded) {
                // die User-Tabellen wurden noch nicht vollst??ndig geladen...
                Snackbar.make(
                    requireView(),
                    "Die Benutzerdaten fehlen noch, bitte kurz warten...",
                    Snackbar.LENGTH_LONG
                ).show()
            } else {
                Timber.tag(TAG).d("Die User-??berpr??fung startet nun...")
                // nun erfolgt die User-??berpr??fung
                var message = verifyUserInfo()
                // die Auswertung...
                Timber.tag(TAG).d("User-??berpr??fung ergab: $message")
                if (message != "Okay") {
                    Snackbar.make(
                        requireView(),
                        message,
                        Snackbar.LENGTH_LONG
                    ).show()
                } else {
                    // die Anmeldung hat geklappt, ..
                    //.. und schreiben die Preferenzen ins User-Log
                    Timber.tag(TAG)
                        .d("Anmeldung war okay, firstSyncCompleted = $firstSyncCompleted")
                    if (!isFirstAppStart) {
                        writeUserInfoToSharedPref()
                    } else {
                        writeUserInfoToSharedPrefFirsttime()
                        // wir machen die Lagerzuordnung sichtbar
                        bind.tvLager.text = myLager!!.matchcode
                        bind.tvLager.visibility = View.VISIBLE
                        // .. blockieren die Eingabem??glichkeiten...
                        bind.etUserName.isEnabled = false
                        bind.etUserPwd.isEnabled = false
                        bind.tvContinue.isEnabled = false
                        if (!firstSyncCompleted) {
                            // und melden weitere Wartezeit an...
                            Snackbar.make(
                                requireView(),
                                "Bitte warten, die Synchronisierung geht weiter...",
                                Snackbar.LENGTH_LONG
                            ).show()

                            // ..und warten nun, bis die Synchronisierung fertig ist...
                            val myJob = GlobalScope.launch(Dispatchers.Default) {
                                while (!firstSyncCompleted) {
                                    delay(500)
                                }
                            }
                            runBlocking {
                                myJob.join()
                            }
                        }
                        // nun schreiben wir die First-Sync-Info ins Preference-Log des Users
                        writeSyncDoneToSharedPref()
                    }

                    // zum Abschluss wechseln wir in die Haupt-??bersicht...
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


    // wir laden die bekannten? Userdaten, falls eine Eingabe erforderlich werden sollte
    private fun loadUserInfo() {
        bind.etUserName.setText(userName)
        bind.tvLager.text = lagerName
        bind.tvInstInfo.text = "Bitte Userdaten eingeben..."
    }

    // ??berpr??ft die bekannten User-Daten mit den gespeicherten...
    private fun verifyUserInfo(): String {
        val myUserName = bind.etUserName.text.toString()
        Timber.tag(TAG).d("Username-Eingabe: $myUserName")
        if (myUserName.length < 4) return "Anmeldename muss mind. 4 Zeichen beinhalten!"
        val userPwd = bind.etUserPwd.text.toString()
        Timber.tag(TAG).d("Passwort-Eingabe: $userPwd")
        val myHash: String = if (userPwd != "") {
            // wir pr??fen gegen eine Passworteingabe
            if (userPwd.length < 4) return "Bitte Passwort mit mind. 4 Zeichen verwenden!"
            Timber.tag(TAG).d("PW-Hash wird berechnet...")
            HashUtils.sha256(userPwd)
        } else {
            // stille Anmeldung wird erwartet (Hash aus Shared Preferences)
            userHash
        }
        Timber.tag(TAG).d("Hash: $myHash")
        if (myHash.isEmpty()) return "Bitte das Passwort eingeben!"

        // wir laden die Userdaten
        var message = "Okay"
        var job = GlobalScope.launch(Dispatchers.IO) {
            myUser = tbmvDAO.getUserByLogName(myUserName)
            if (myUser == null) {
                // dieser User ist uns nicht bekannt!
                message = "Benutzer $userName ist unbekannt!"
            } else {
                // wir ??berpr??fen die Useranmeldung
                if (myUser!!.passHash != null) {
                    // in diesem Falle muss das Passwort ??berpr??ft werden
                    if (myHash != myUser?.passHash) {
                        Timber.tag(TAG).d("myHash: $myHash")
                        Timber.tag(TAG).d("myUser.passHash: ${myUser!!.passHash}")
                        message = "Falsches Passwort bitte korrigieren!"
                    } else {
                        // Anmeldung war hier erfolgreich
                        userName = myUserName
                        userHash = myHash
                        message = "Okay"
                    }
                } else {
                    // neues Passwort wird eintragen, ansonsten ist die Anmeldung okay...
                    myUser!!.passHash = myHash
                    tbmvDAO.updateUser(myUser!!)
                    sqlUserNewPassHash = true
                    userName = myUserName
                    userHash = myHash
                    message = "Okay"
                }
            }
        }
        runBlocking {
            job.join()
        }
        // Auswertung der Userdaten und Berechtigungen
        if (message != "Okay") {
            // wir steigen hier mit Fehlerinformation aus
            return message
        }

        // nun kl??ren wir Berechtigungen
        if (myUser!!.bmvR + myUser!!.bmvW + myUser!!.bmvAdmin == 0) {
            // der Benutzer darf keine BMV-Daten sehen
            return "Sie sind f??r Betriebsmittel nicht freigeschaltet!"
        }
        // und nun die Lager-Zuodnung
        job = GlobalScope.launch(Dispatchers.IO) {
            val locLagerList = tbmvDAO.getLagerListeByUserID(myUser!!.id)
            if (locLagerList.isEmpty() == true) {
                if (myUser!!.bmvAdmin == 0) {
                    // der Benutzer ist keinem Lager zugeordnet und hat keine Admin-Berechtigung
                    message = "Sie sind keinem Lager zugeordnet, fehlende Berechtigung!"
                } else {
                    // als Admin ohne Lagerzuordnung ordnen wir das Hauptlager zu
                    myLager = tbmvDAO.getLagerByName("Lager")?.value
                }
            } else {
                // ist die bisher eingetragene ID noch zugeordnet?
                val locLagerListFiltered = locLagerList.filter { it.id == lagerId }
                myLager = if (locLagerListFiltered.isEmpty()) {
                    // wir stellen auf den ersten gefundenen Lagereintrag um
                    locLagerList.first()
                } else {
                    // wir verwenden den alten Eintrag
                    locLagerListFiltered.first()
                }
                lagerId = myLager!!.id
                lagerName = myLager!!.matchcode
            }
        }
        runBlocking {
            job.join()
        }
        if (message != "Okay") {
            // wir steigen hier mit Fehlerinformation aus
            return message
        }

        // wir sammeln alle Lager,die der User sehen darf in einer Liste
        job = GlobalScope.launch(Dispatchers.IO) {
            myLagers = tbmvDAO.getLagerListSorted()
            if (myUser!!.bmvAdmin == 0) {
                // als NICHT-Admin darf ich nur die Lager ausw??hlen, die mir geh??ren -> alle anderen Datens??tze rausl??schen
                (myLagers as MutableList<TbmvLager>).removeAll {
                    it.userGuid != myUser!!.id
                }
            }
        }
        runBlocking {
            job.join()
        }
        return message
    }


    private fun writeUserInfoToSharedPrefFirsttime() {
        Timber.tag(TAG).d("WriteFirstPref, username: $userName")
        Timber.tag(TAG).d("userHash: $userHash")
        Timber.tag(TAG).d("LagerID: $lagerId")
        Timber.tag(TAG).d("Lagername: $lagerName")
        sharedPref.edit()
            .putString(KEY_USER_NAME, userName)
            .putString(KEY_USER_HASH, userHash)
            .putBoolean(KEY_FIRST_TIME, false)
            .putBoolean(KEY_FIRST_SYNC_DONE, false)
            .putString(KEY_LAGER_ID, lagerId)
            .putString(KEY_LAGER_NAME, lagerName)
            .apply()
        return
    }

    private fun writeUserInfoToSharedPref() {
        Timber.tag(TAG).d("WritePref, username: $userName")
        Timber.tag(TAG).d("userHash: $userHash")
        Timber.tag(TAG).d("LagerID: $lagerId")
        Timber.tag(TAG).d("Lagername: $lagerName")
        sharedPref.edit()
            .putString(KEY_USER_NAME, userName)
            .putString(KEY_USER_HASH, userHash)
            .putString(KEY_LAGER_ID, lagerId)
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