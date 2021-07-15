package de.wsh.wshbmv.sql_db

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import de.wsh.wshbmv.db.entities.*
import de.wsh.wshbmv.db.entities.relations.*
import de.wsh.wshbmv.other.Constants.TAG
import de.wsh.wshbmv.other.GlobalVars.firstSyncCompleted
import de.wsh.wshbmv.other.GlobalVars.sqlErrorMessage
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
    val mainRepository: MainRepository
) {

    //    lateinit var connectionClass: SqlConnection
    private var myConn: Connection? = null
    private val connectionClass = SqlConnection()

    init {
        Timber.tag(TAG).d("Init SqlDbFirstInit...")
        GlobalScope.launch(Dispatchers.IO) {
            sqlErrorMessage.postValue("")
            try {
                sqlStatus.postValue(enSqlStatus.INIT)
                myConn = connectionClass.dbConn()
                if (myConn == null) {
                    Timber.tag(TAG).e("Keine Verbindung zum SQL-Server!")
                    sqlStatus.postValue(enSqlStatus.NO_CONTACT)
                } else {
                    sqlServerConnected = true
                    if (firstSyncDatabase()) {
                        Timber.tag(TAG).d("Erst-Synchronisierung ist durchgelaufen")
                        sqlStatus.postValue(enSqlStatus.PROCESS_ENDED)
                    } else {
                        Timber.tag(TAG).d("Erst-Synchronisieriung war nicht erfolgreich!")
                        sqlStatus.postValue(enSqlStatus.PROCESS_ABORTED)
                    }
                }
            } catch (ex: Exception) {
                sqlStatus.postValue(enSqlStatus.IN_ERROR)  // Ende ohne Erfolg!
                sqlErrorMessage.postValue("SQL-Verbindungsfehler: ${ex.message ?: ""}")
                Timber.tag(TAG).e("Fehler ist aufgetreten: ${ex.message ?: ""}")
            }
            connectionClass.disConnect(myConn)
        }
    }


    /**
     *  Erst-Ladung aller Tabellen aus der SQL-DB auf WSH-Server startet hier
     */
    private suspend fun firstSyncDatabase(): Boolean {
        sqlStatus.postValue(enSqlStatus.IN_PROCESS)
        val nowInMillis = System.currentTimeMillis()

        if (!syncFirstPrioTabs()) return false
        sqlUserLoaded = true
        syncAllTabs()

        // nach erfolgreichem First-Sync-Vorgang...
        // ...schreib einen Sync-Report-Eintrag
        val syncReport = TappSyncReport()
        syncReport.timeStamp = nowInMillis
        syncReport.lastFromServerTime = nowInMillis
        syncReport.lastToServerTime = nowInMillis
        mainRepository.insertSyncReport(syncReport)

        sqlStatus.postValue(enSqlStatus.PROCESS_ENDED)
        firstSyncCompleted = true
        return true
    }


    /**
     *  initialisiert alle User-Tabellen + die Lager-Tabelle
     */
    private suspend fun syncFirstPrioTabs(): Boolean {
        lateinit var user: TsysUser
        lateinit var userGruppe: TsysUserGruppe
        lateinit var userInGruppe: TsysUserToGruppe
        lateinit var lager: TbmvLager

        // die User:
        val statement = myConn!!.createStatement()
        var resultSet = statement.executeQuery("SELECT * FROM TsysUser")
        if (resultSet != null) {
            Timber.tag(TAG).d("Wir schreiben TsysUser...")
            while (resultSet.next()) {
                user = TsysUser()
                user.id = resultSet.getString("ID").lowercase(Locale.getDefault())
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
            Timber.tag(TAG).d("Es wurden keine Userdaten gefunden!!")
            return false
        }

        // die User-Gruppen
        resultSet = statement.executeQuery("SELECT * FROM TsysUserGruppe")
        if (resultSet != null) {
            Timber.tag(TAG).d("Wir schreiben TsysUserGruppe...")
            while (resultSet.next()) {
                userGruppe = TsysUserGruppe()
                userGruppe.id = resultSet.getString("ID").lowercase(Locale.getDefault())
                userGruppe.nameGruppe = resultSet.getString("NameGruppe")
                // füge den Datensatz in die SQLite ein
                mainRepository.insertUserGruppe(userGruppe)
            }
        } else {
            Timber.tag(TAG).d("Es wurden keine Usergruppen gefunden!!")
        }

        // die Relationstabelle User in Gruppen
        resultSet = statement.executeQuery("SELECT * FROM TsysUser_Gruppe")
        if (resultSet != null) {
            Timber.tag(TAG).d("Wir schreiben TsysUser_Gruppe...")
            while (resultSet.next()) {
                userInGruppe = TsysUserToGruppe()
                userInGruppe.id = resultSet.getString("ID").lowercase(Locale.getDefault())
                userInGruppe.gruppeId =
                    resultSet.getString("GruppeID").lowercase(Locale.getDefault())
                userInGruppe.userId = resultSet.getString("UserID").lowercase(Locale.getDefault())
                // füge den Datensatz in die SQLite ein
                mainRepository.insertUserInGruppe(userInGruppe)
            }
        } else {
            Timber.tag(TAG).d("Es wurden keine Relationen User in Gruppen gefunden!!")
        }

        // die Lagertabelle
        resultSet = statement.executeQuery("SELECT * FROM TbmvLager")
        if (resultSet != null) {
            Timber.tag(TAG).d("Wir schreiben TbmvLager...")
            while (resultSet.next()) {
                lager = TbmvLager()
                lager.id = resultSet.getString("ID").lowercase(Locale.getDefault())
                lager.scancode = resultSet.getString("Scancode")
                lager.typ = resultSet.getString("Typ")
                lager.matchcode = resultSet.getString("Matchcode")
                lager.userGuid = resultSet.getString("UserGUID").lowercase(Locale.getDefault())
                lager.beschreibung = resultSet.getString("Beschreibung")
                lager.status = resultSet.getString("Status")
                lager.bmLager = resultSet.getInt("BMLager")
                // füge den Datensatz in die SQLite ein
                mainRepository.insertLager(lager)
            }
        } else {
            Timber.tag(TAG).d("Es wurden keine Lager gefunden!!")
        }
        return true
    }

    /**
     *  führt die Voll-Synchronisierung aller Tabellen mit Ausnahme der User-Tabs/Lager-Tab durch
     */
    private suspend fun syncAllTabs() {
        lateinit var material: TbmvMat
        lateinit var matGruppe: TbmvMatGruppe
        lateinit var services: TbmvServices
        lateinit var belege: TbmvBelege
        lateinit var belegPos: TbmvBelegPos
        lateinit var dokument: TbmvDokument
        lateinit var matInLager: TbmvMat_Lager
        lateinit var matZuService: TbmvMat_Service
        lateinit var matServiceDok: TbmvMatService_Dok
        lateinit var matServiceHistory: TbmvMatService_Historie
        lateinit var serviceDok: TbmvService_Dok

        val statement = myConn!!.createStatement()

        // Material
        var resultSet = statement.executeQuery("SELECT * FROM TbmvMat")
        if (resultSet != null) {
            Timber.tag(TAG).d("Wir schreiben TbmvMat...")
            while (resultSet.next()) {
                material = TbmvMat()
                material.id = resultSet.getString("ID").lowercase(Locale.getDefault())
                material.scancode = resultSet.getString("Scancode")
                material.typ = resultSet.getString("Typ")
                material.matchcode = resultSet.getString("Matchcode")
                material.matGruppeGuid =
                    resultSet.getString("MatGruppeGUID").lowercase(Locale.getDefault())
                material.beschreibung = resultSet.getString("Beschreibung")
                material.hersteller = resultSet.getString("Hersteller")
                material.modell = resultSet.getString("Modell")
                material.seriennummer = resultSet.getString("Seriennummer")
                material.userGuid = resultSet.getString("UserGUID").lowercase(Locale.getDefault())
                material.matStatus = resultSet.getString("MatStatus")
                material.bildBmp = toBitmap(resultSet.getBytes("BildBmp"))
                // füge den Datensatz in die SQLite ein
                mainRepository.updateMat(material, noProtokoll = true)
            }
        }

        // Materialgruppen
        resultSet = statement.executeQuery("SELECT * FROM TbmvMatGruppen")
        if (resultSet != null) {
            Timber.tag(TAG).d("Wir schreiben TbmvMatGruppen...")
            while (resultSet.next()) {
                matGruppe = TbmvMatGruppe()
                matGruppe.id = resultSet.getString("ID").lowercase(Locale.getDefault())
                matGruppe.matGruppe = resultSet.getString("MatGruppe")
                matGruppe.aktiv = resultSet.getInt("Aktiv")
                // füge den Datensatz in die SQLite ein
                mainRepository.insertMatGruppe(matGruppe)
            }
        }

        // Service
        resultSet = statement.executeQuery("SELECT * FROM TbmvServices")
        if (resultSet != null) {
            Timber.tag(TAG).d("Wir schreiben TbmvServices...")
            while (resultSet.next()) {
                services = TbmvServices()
                services.id = resultSet.getString("ID").lowercase(Locale.getDefault())
                services.name = resultSet.getString("Name")
                services.beschreibung = resultSet.getString("Beschreibung")
                services.intervalNum = resultSet.getInt("IntervalNum")
                services.intervalUnit = resultSet.getString("IntervalUnit")
                services.doInfo = resultSet.getInt("DoInfo")
                services.infoNum = resultSet.getInt("InfoNum")
                services.infoUnit = resultSet.getString("InfoUnit")
                // füge den Datensatz in die SQLite ein
                mainRepository.insertService(services)
            }
        }

        // Belege (Transfer)
        resultSet = statement.executeQuery("SELECT * FROM TbmvBelege")
        if (resultSet != null) {
            Timber.tag(TAG).d("Wir schreiben TbmvBelege...")
            while (resultSet.next()) {
                belege = TbmvBelege()
                belege.id = resultSet.getString("ID").lowercase(Locale.getDefault())
                belege.belegTyp = resultSet.getString("BelegTyp")
                belege.belegDatum = resultSet.getTimestamp("BelegDatum")
                belege.belegUserGuid =
                    resultSet.getString("BelegUserGUID").lowercase(Locale.getDefault())
                belege.zielLagerGuid =
                    resultSet.getString("ZielLagerGUID").lowercase(Locale.getDefault())
                belege.zielUserGuid =
                    resultSet.getString("ZielUserGUID").lowercase(Locale.getDefault())
                belege.belegStatus = resultSet.getString("BelegStatus")
                belege.toAck = resultSet.getInt("ToAck")
                belege.notiz = resultSet.getString("Notiz")
                // füge den Datensatz in die SQLite ein
                mainRepository.insertBeleg(belege)
            }
        }
        // .. und Positionen
        resultSet = statement.executeQuery("SELECT * FROM TbmvBelegPos")
        if (resultSet != null) {
            Timber.tag(TAG).d("Wir schreiben TbmvBelegPos...")
            while (resultSet.next()) {
                belegPos = TbmvBelegPos()
                belegPos.id = resultSet.getString("ID").lowercase(Locale.getDefault())
                belegPos.belegId = resultSet.getString("BelegID").lowercase(Locale.getDefault())
                belegPos.pos = resultSet.getInt("Pos")
                belegPos.matGuid = resultSet.getString("MatGUID").lowercase(Locale.getDefault())
                belegPos.menge = resultSet.getFloat("Menge")
                belegPos.vonLagerGuid =
                    resultSet.getString("VonLagerGUID")?.let { it.lowercase(Locale.getDefault()) }
                belegPos.ackDatum = resultSet.getTimestamp("AckDatum")
                // füge den Datensatz in die SQLite ein
                mainRepository.insertBelegPos(belegPos, true)
            }
        }

        // Dokumente
        resultSet = statement.executeQuery("SELECT * FROM TbmvDokumente")
        if (resultSet != null) {
            Timber.tag(TAG).d("Wir schreiben TbmvDokumente...")
            while (resultSet.next()) {
                dokument = TbmvDokument()
                dokument.id = resultSet.getString("ID").lowercase(Locale.getDefault())
                dokument.version = resultSet.getString("Version")
                dokument.matID = resultSet.getString("MatID")?.let { it.lowercase(Locale.getDefault()) }
                dokument.serviceID = resultSet.getString("ServiceID")?.let { it.lowercase(Locale.getDefault()) }
                dokument.dateiName = resultSet.getString("DateiName")
                dokument.dateiVerzeichnis = resultSet.getString("DateiVerzeichnis")
                dokument.status = resultSet.getString("Status")
                dokument.erstellDtm = resultSet.getTimestamp("ErstellDtm")
                dokument.erstellName = resultSet.getString("ErstellName")
                dokument.grobklasse = resultSet.getString("Grobklasse")
                dokument.stichwort = resultSet.getString("Stichwort")
                dokument.extern = resultSet.getInt("Extern")
                dokument.inBearbeitung = resultSet.getInt("inBearbeitung")
                dokument.bearbeiter = resultSet.getString("Bearbeiter")
                dokument.reservierungTxt = resultSet.getString("Reservierungtxt")
                // füge den Datensatz in die SQLite ein
                mainRepository.insertDokument(dokument)
            }
        }

        // Material zu Lager
        resultSet = statement.executeQuery("SELECT * FROM TbmvMat_Lager")
        if (resultSet != null) {
            Timber.tag(TAG).d("Wir schreiben TbmvMat_Lager...")
            while (resultSet.next()) {
                matInLager = TbmvMat_Lager()
                matInLager.id = resultSet.getString("ID").lowercase(Locale.getDefault())
                matInLager.matId = resultSet.getString("MatGUID").lowercase(Locale.getDefault())
                matInLager.lagerId = resultSet.getString("LagerGUID").lowercase(Locale.getDefault())
                matInLager.isDefault = resultSet.getInt("Default")
                matInLager.bestand = resultSet.getFloat("Bestand")
                // füge den Datensatz in die SQLite ein
                mainRepository.insertMat_Lager(matInLager, true)
            }
        }

        // Material zu Service
        resultSet = statement.executeQuery("SELECT * FROM TbmvMat_Service")
        if (resultSet != null) {
            Timber.tag(TAG).d("Wir schreiben TbmvMat_Service...")
            while (resultSet.next()) {
                matZuService = TbmvMat_Service()
                matZuService.id = resultSet.getString("ID").lowercase(Locale.getDefault())
                matZuService.matId = resultSet.getString("MatID").lowercase(Locale.getDefault())
                matZuService.serviceId =
                    resultSet.getString("ServiceID").lowercase(Locale.getDefault())
                matZuService.nextServiceDatum = resultSet.getTimestamp("NextServiceDatum")
                matZuService.nextInfoDatum = resultSet.getTimestamp("NextInfoDatum")
                // füge den Datensatz in die SQLite ein
                mainRepository.insertMat_Service(matZuService)
            }
        }

        // Dokumente zu Material-Service
        resultSet = statement.executeQuery("SELECT * FROM TbmvMatService_Dok")
        if (resultSet != null) {
            Timber.tag(TAG).d("Wir schreiben TbmvMatService_Dok...")
            while (resultSet.next()) {
                matServiceDok = TbmvMatService_Dok()
                matServiceDok.id = resultSet.getString("ID").lowercase(Locale.getDefault())
                matServiceDok.matServiceId =
                    resultSet.getString("MatServiceID").lowercase(Locale.getDefault())
                matServiceDok.dokId = resultSet.getString("DokID").lowercase(Locale.getDefault())
                // füge den Datensatz in die SQLite ein
                mainRepository.insertMatService_Dok(matServiceDok)
            }
        }

        // Historie zu Material-Service
        resultSet = statement.executeQuery("SELECT * FROM TbmvMatService_Historie")
        if (resultSet != null) {
            Timber.tag(TAG).d("Wir schreiben TbmvMatService_Historie...")
            while (resultSet.next()) {
                matServiceHistory = TbmvMatService_Historie()
                matServiceHistory.id = resultSet.getString("ID").lowercase(Locale.getDefault())
                matServiceHistory.matId =
                    resultSet.getString("MatID").lowercase(Locale.getDefault())
                matServiceHistory.serviceId =
                    resultSet.getString("ServiceID").lowercase(Locale.getDefault())
                matServiceHistory.serviceDatum = resultSet.getTimestamp("Servicedatum")
                matServiceHistory.abschlussDatum = resultSet.getTimestamp("Abschlussdatum")
                matServiceHistory.userGuid = resultSet.getString("UserGUID")
                // füge den Datensatz in die SQLite ein
                mainRepository.insertMatService_Historie(matServiceHistory)
            }
        }

        // Dokumente zu Service - Vorlagen
        resultSet = statement.executeQuery("SELECT * FROM TbmvService_Dok")
        if (resultSet != null) {
            Timber.tag(TAG).d("Wir schreiben TbmvService_Dok...")
            while (resultSet.next()) {
                serviceDok = TbmvService_Dok()
                serviceDok.id = resultSet.getString("ID").lowercase(Locale.getDefault())
                serviceDok.serviceId =
                    resultSet.getString("ServiceID").lowercase(Locale.getDefault())
                serviceDok.dokId = resultSet.getString("DokID").lowercase(Locale.getDefault())
                // füge den Datensatz in die SQLite ein
                mainRepository.insertService_Dok(serviceDok)
            }
        }
    }

    private fun toBitmap(bytes: ByteArray?): Bitmap? {
        return if (bytes != null) {
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } else {
            null
        }
    }

    private fun fromBitmap(bmp: Bitmap): ByteArray {
        val outputStream = ByteArrayOutputStream()
        bmp.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        return outputStream.toByteArray()
    }
}