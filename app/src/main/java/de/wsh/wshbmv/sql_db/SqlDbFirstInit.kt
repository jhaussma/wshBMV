package de.wsh.wshbmv.sql_db

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.provider.ContactsContract
import android.util.Log
import de.wsh.wshbmv.db.Converters
import de.wsh.wshbmv.db.entities.*
import de.wsh.wshbmv.other.Constants.TAG
import de.wsh.wshbmv.other.GlobalVars.sqlServerConnected
import de.wsh.wshbmv.other.GlobalVars.sqlStatus
import de.wsh.wshbmv.other.GlobalVars.sqlUserLoaded
import de.wsh.wshbmv.other.enSqlStatus
import de.wsh.wshbmv.repositories.MainRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.lang.Exception
import java.sql.Connection
import java.util.*
import javax.inject.Inject


/**
 * Erst-Installation / Synchronisierung mit SQL-DB auf WSH-Server
 */
class SqlDbFirstInit @Inject constructor(
    val mainRepository: MainRepository, val doFirstSync : Boolean = false
) {

    lateinit var connectionClass: SqlConnection
    var myConn: Connection? = null

    init {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                sqlStatus = enSqlStatus.IN_PROCESS
                myConn = connectionClass.dbConn()
                if (myConn == null) {
                    Timber.e( "Fehler bei Kommunikation mit SQL-Server!")
                    sqlStatus = enSqlStatus.IN_ERROR
                } else {
                    sqlServerConnected = true
                    Timber.d( "Verbindung zum SQL-Server und TbmvMat steht!")
                    if (doFirstSync) {
                        if (firstSyncDatabase()) {
                            Timber.d("Erst-Synchronisierung hat geklappt")
                        } else {
                            Timber.d("Erst-Synchronisieriung war nicht erfolgreich!")
                        }
                    } else {
                        sqlStatus = enSqlStatus.PROCESS_ENDED
                    }
                }
            } catch (ex: Exception) {
                sqlStatus = enSqlStatus.IN_ERROR // Ende ohne Erfolg!
                Timber.tag(TAG).e( "Fehler ist aufgetreten: ${ex.message ?: ""}")
            }
        }
    }


    /**
     *  Erst-Ladung aller Tabellen aus der SQL-DB auf WSH-Server startet hier
     */
    private suspend fun firstSyncDatabase(): Boolean {
        sqlStatus = enSqlStatus.IN_PROCESS
        syncFirstPrioTabs()
        sqlUserLoaded = true
        syncAllTabs()
        sqlStatus = enSqlStatus.PROCESS_ENDED
        return true
    }


    /**
     *  initialisiert alle User-Tabellen + die Lager-Tabelle
     */
    private suspend fun syncFirstPrioTabs(): Boolean {
        lateinit var user: TsysUser
        lateinit var userGruppe: TsysUserGruppe
        lateinit var userInGruppe: TsysUserInGruppe
        lateinit var lager: TbmvLager

        // die User:
        val statement = myConn!!.createStatement()
        var resultSet = statement.executeQuery("SELECT * FROM TsysUser")
        if (resultSet != null) {
            Timber.d("Wir schreiben TsysUser...")
            while (resultSet.next()) {
                user = TsysUser()
                user.id = resultSet.getString("ID")
                user.vorname = resultSet.getString("Vorname") ?: ""
                user.nachname = resultSet.getString("NachName") ?: ""
                user.anrede = resultSet.getString("Anrede") ?: ""
                user.benutzerStatus = resultSet.getString("BenutzerStatus")
                user.email = resultSet.getString("Email") ?: ""
                user.telefon = resultSet.getString("Telefon") ?: ""
                user.kurzZeichen = resultSet.getString("KurzZeichen") ?: ""
                user.userKennung = resultSet.getString("UserKennung") ?: ""
                user.passHash = resultSet.getString("PassHash")
                user.titel = resultSet.getString("Titel") ?: ""
                user.dw = resultSet.getString("DW") ?: ""
                user.admin = resultSet.getInt("Admin")
                user.terminW = resultSet.getInt("TerminW")
                user.stammR = resultSet.getInt("StammR")
                user.stammW = resultSet.getInt("StammW")
                user.kundenR = resultSet.getInt("KundenR")
                user.kundenW = resultSet.getInt("KundenW")
                user.vorlagenR = resultSet.getInt("VorlagenR")
                user.vorlagenW = resultSet.getInt("VorlagenW")
                user.monteur = resultSet.getInt("Monteur")
                user.vertragR = resultSet.getInt("VertragR")
                user.vertragW = resultSet.getInt("VertragW")
                user.rechnungR = resultSet.getInt("RechnungR")
                user.rechnungW = resultSet.getInt("RechnungW")
                user.bmvR = resultSet.getInt("BmvR")
                user.bmvW = resultSet.getInt("BmvW")
                user.bmvAdmin = resultSet.getInt("BmvAdmin")
                // füge den Datensatz in die SQLite ein
                mainRepository.insertUser(user)
            }
        } else {
            Timber.d("Es wurden keine Userdaten gefunden!!")
            return false
        }

        // die User-Gruppen
        resultSet = statement.executeQuery("SELECT * FROM TsysUserGruppe")
        if (resultSet != null) {
            Timber.d("Wir schreiben TsysUserGruppe...")
            while (resultSet.next()) {
                userGruppe = TsysUserGruppe()
                userGruppe.id = resultSet.getString("ID")
                userGruppe.nameGruppe = resultSet.getString("NameGruppe")
                // füge den Datensatz in die SQLite ein
                mainRepository.insertUserGruppe(userGruppe)
            }
        } else {
            Timber.d("Es wurden keine Usergruppen gefunden!!")
        }

        // die Relationstabelle User in Gruppen
        resultSet = statement.executeQuery("SELECT * FROM TsysUser_Gruppe")
        if (resultSet != null) {
            Timber.d("Wir schreiben TsysUser_Gruppe...")
            while (resultSet.next()) {
                userInGruppe = TsysUserInGruppe()
                userInGruppe.id = resultSet.getString("ID")
                userInGruppe.gruppeId = resultSet.getString("GruppeID")
                userInGruppe.userId = resultSet.getString("UserID")
                // füge den Datensatz in die SQLite ein
                mainRepository.insertUserInGruppe(userInGruppe)
            }
        } else {
            Timber.d("Es wurden keine Relationen User in Gruppen gefunden!!")
        }

        // die Lagertabelle
        resultSet = statement.executeQuery("SELECT * FROM TbmvLager")
        if (resultSet != null) {
            Timber.d("Wir schreiben TbmvLager...")
            while (resultSet.next()) {
                lager = TbmvLager()
                lager.id = resultSet.getString("ID")
                lager.scancode = resultSet.getString("Scancode")
                lager.typ = resultSet.getString("Typ")
                lager.matchcode = resultSet.getString("Matchcode")
                lager.userGuid = resultSet.getString("UserGUID")
                lager.beschreibung = resultSet.getString("Beschreibung")
                lager.status = resultSet.getString("Status")
                lager.bmLager = resultSet.getInt("BMLager")
                // füge den Datensatz in die SQLite ein
                mainRepository.insertLager(lager)
            }
        } else {
            Timber.d("Es wurden keine Lager gefunden!!")
        }
        return true
    }

    /**
     *  führt die Voll-Synchronisierung aller Tabellen mit Ausnahme der User-Tabs/Lager-Tab durch
     */
    private suspend fun syncAllTabs(): Boolean {
        lateinit var material: TbmvMat

        val statement = myConn!!.createStatement()

        // Material
        var resultSet = statement.executeQuery("SELECT * FROM TbmvMat")
        if (resultSet != null) {
            Timber.d("Wir schreiben TbmvMat...")
            while (resultSet.next()) {
                material = TbmvMat()
                material.id = resultSet.getString("ID")
                material.scancode = resultSet.getString("Scancode")
                material.typ = resultSet.getString("Typ")
                material.matchcode = resultSet.getString("Matchcode")
                material.matGruppeGuid = resultSet.getString("MatGruppeGUID")
                material.beschreibung = resultSet.getString("Beschreibung")
                material.hersteller = resultSet.getString("Hersteller")
                material.modell = resultSet.getString("Modell")
                material.seriennummer = resultSet.getString("Seriennummer")
                material.userGuid = resultSet.getString("UserGUID")
                material.matStatus = resultSet.getString("MatStatus")
                material.bildBmp = toBitmap(resultSet.getBytes("BildBmp"))
                // füge den Datensatz in die SQLite ein
                mainRepository.insertMat(material)
            }
        }




        return true
    }

    fun toBitmap(bytes: ByteArray?): Bitmap? {
        if (bytes != null) {
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } else {
            return null
        }
    }

    private fun fromBitmap(bmp: Bitmap): ByteArray {
        val outputStream = ByteArrayOutputStream()
        bmp.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        return  outputStream.toByteArray()
    }
}