package de.wsh.wshbmv.sql_db

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import de.wsh.wshbmv.db.entities.*
import de.wsh.wshbmv.db.entities.relations.*
import de.wsh.wshbmv.other.Constants.TAG
import de.wsh.wshbmv.other.GlobalVars.firstSyncCompleted
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
import javax.inject.Inject


/**
 * Erst-Installation / Synchronisierung mit SQL-DB auf WSH-Server
 */
class SqlDbFirstInit @Inject constructor(
    val mainRepository: MainRepository, private val doFirstSync: Boolean = false
) {

    lateinit var connectionClass: SqlConnection
    private var myConn: Connection? = null

    init {
        Timber.tag(TAG).d("Init SqlDbFirstInit...")
        GlobalScope.launch(Dispatchers.IO) {
            try {
                sqlStatus = enSqlStatus.IN_PROCESS
                myConn = connectionClass.dbConn()
                if (myConn == null) {
                    Timber.tag(TAG).e("Fehler bei Kommunikation mit SQL-Server!")
                    sqlStatus = enSqlStatus.IN_ERROR
                } else {
                    sqlServerConnected = true
                    Timber.tag(TAG).d("Verbindung zum SQL-Server und TbmvMat steht!")
                    if (doFirstSync) {
                        if (firstSyncDatabase()) {
                            Timber.tag(TAG).d("Erst-Synchronisierung ist durchgelaufen")
                        } else {
                            Timber.tag(TAG).d("Erst-Synchronisieriung war nicht erfolgreich!")
                        }
                    } else {
                        sqlStatus = enSqlStatus.PROCESS_ENDED
                    }
                }
            } catch (ex: Exception) {
                sqlStatus = enSqlStatus.IN_ERROR // Ende ohne Erfolg!
                Timber.tag(TAG).e("Fehler ist aufgetreten: ${ex.message ?: ""}")
            }
        }
    }


    /**
     *  Erst-Ladung aller Tabellen aus der SQL-DB auf WSH-Server startet hier
     */
    private suspend fun firstSyncDatabase(): Boolean {
        sqlStatus = enSqlStatus.IN_PROCESS
        val nowInMillis = System.currentTimeMillis()

        syncFirstPrioTabs()
        sqlUserLoaded = true
        syncAllTabs()

        // nach erfolgreichem First-Sync-Vorgang...
        // ...schreib einen Sync-Report-Eintrag
        val syncReport = TappSyncReport()
        syncReport.timeStamp = nowInMillis
        syncReport.lastFromServerTime = nowInMillis
        syncReport.lastToServerTime = nowInMillis
        mainRepository.insertSyncReport(syncReport)

        sqlStatus = enSqlStatus.PROCESS_ENDED
        firstSyncCompleted = true
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
            Timber.tag(TAG).d("Wir schreiben TsysUser...")
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
            Timber.tag(TAG).d("Es wurden keine Userdaten gefunden!!")
            return false
        }

        // die User-Gruppen
        resultSet = statement.executeQuery("SELECT * FROM TsysUserGruppe")
        if (resultSet != null) {
            Timber.tag(TAG).d("Wir schreiben TsysUserGruppe...")
            while (resultSet.next()) {
                userGruppe = TsysUserGruppe()
                userGruppe.id = resultSet.getString("ID")
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
                userInGruppe = TsysUserInGruppe()
                userInGruppe.id = resultSet.getString("ID")
                userInGruppe.gruppeId = resultSet.getString("GruppeID")
                userInGruppe.userId = resultSet.getString("UserID")
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
            Timber.tag(TAG).d("Es wurden keine Lager gefunden!!")
        }
        return true
    }

    /**
     *  führt die Voll-Synchronisierung aller Tabellen mit Ausnahme der User-Tabs/Lager-Tab durch
     */
    private suspend fun syncAllTabs(): Boolean {
        lateinit var material: TbmvMat
        lateinit var matGruppe: TbmvMatGruppe
        lateinit var service: TbmvService
        lateinit var beleg: TbmvBeleg
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
                mainRepository.updateMat(material,true)
            }
        }

        // Materialgruppen
        resultSet = statement.executeQuery("SELECT * FROM TbmvMatGruppen")
        if (resultSet != null) {
            Timber.tag(TAG).d("Wir schreiben TbmvMatGruppen...")
            while (resultSet.next()) {
                matGruppe = TbmvMatGruppe()
                matGruppe.id = resultSet.getString("ID")
                matGruppe.matGruppe = resultSet.getString("MatGruppe")
                matGruppe.aktiv = resultSet.getInt("Aktiv")
                // füge den Datensatz in die SQLite ein
                mainRepository.insertMatGruppe(matGruppe)
            }
        }

        // Service
        resultSet = statement.executeQuery("SELECT * FROM TbmvServices")
        if (resultSet != null) {
            Timber.tag(TAG).d("Wir schreiben TbmvService...")
            while (resultSet.next()) {
                service = TbmvService()
                service.id = resultSet.getString("ID")
                service.name = resultSet.getString("Name")
                service.beschreibung = resultSet.getString("Beschreibung")
                service.intervalNum = resultSet.getInt("IntervalNum")
                service.intervalUnit = resultSet.getString("IntervalUnit")
                service.doInfo = resultSet.getInt("DoInfo")
                service.infoNum = resultSet.getInt("InfoNum")
                service.infoUnit = resultSet.getString("InfoUnit")
                // füge den Datensatz in die SQLite ein
                mainRepository.insertService(service)
            }
        }

        // Belege (Transfer)
        resultSet = statement.executeQuery("SELECT * FROM TbmvBelege")
        if (resultSet != null) {
            Timber.tag(TAG).d("Wir schreiben TbmvBelege...")
            while (resultSet.next()) {
                beleg = TbmvBeleg()
                beleg.id = resultSet.getString("ID")
                beleg.belegTyp = resultSet.getString("BelegTyp")
                beleg.belegDatum = resultSet.getTimestamp("BelegDatum")
                beleg.belegUserGuid = resultSet.getString("BelegUserGUID")
                beleg.zielLagerGuid = resultSet.getString("ZielLagerGUID")
                beleg.zielUserGuid = resultSet.getString("ZielUserGUID")
                beleg.belegStatus = resultSet.getString("BelegStatus")
                beleg.toAck = resultSet.getInt("ToAck")
                beleg.notiz = resultSet.getString("Notiz")
                // füge den Datensatz in die SQLite ein
                mainRepository.insertBeleg(beleg)
            }
        }
        // .. und Positionen
        resultSet = statement.executeQuery("SELECT * FROM TbmvBelegPos")
        if (resultSet != null) {
            Timber.tag(TAG).d("Wir schreiben TbmvBelegPos...")
            while (resultSet.next()) {
                belegPos = TbmvBelegPos()
                belegPos.id = resultSet.getString("ID")
                belegPos.belegId = resultSet.getString("BelegID")
                belegPos.pos = resultSet.getInt("Pos")
                belegPos.matGuid = resultSet.getString("MatGUID")
                belegPos.menge = resultSet.getFloat("Menge")
                belegPos.vonLagerGuid = resultSet.getString("VonLagerGUID")
                belegPos.ackDatum = resultSet.getTimestamp("AckDatum")
                // füge den Datensatz in die SQLite ein
                mainRepository.insertBelegPos(belegPos, false)
            }
        }

        // Dokumente
        resultSet = statement.executeQuery("SELECT * FROM TbmvDokumente")
        if (resultSet != null) {
            Timber.tag(TAG).d("Wir schreiben TbmvDokumente...")
            while (resultSet.next()) {
                dokument = TbmvDokument()
                dokument.id = resultSet.getString("ID")
                dokument.version = resultSet.getString("Version")
                dokument.matID = resultSet.getString("MatID")
                dokument.serviceID = resultSet.getString("ServiceID")
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
                matInLager.id = resultSet.getString("ID")
                matInLager.matId = resultSet.getString("MatGUID")
                matInLager.lagerId = resultSet.getString("LagerGUID")
                matInLager.isDefault = resultSet.getInt("Default")
                matInLager.bestand = resultSet.getFloat("Bestand")
                // füge den Datensatz in die SQLite ein
                mainRepository.insertMat_Lager(matInLager)
            }
        }

        // Material zu Service
        resultSet = statement.executeQuery("SELECT * FROM TbmvMat_Service")
        if (resultSet != null) {
            Timber.tag(TAG).d("Wir schreiben TbmvMat_Service...")
            while (resultSet.next()) {
                matZuService = TbmvMat_Service()
                matZuService.id = resultSet.getString("ID")
                matZuService.matId = resultSet.getString("MatID")
                matZuService.serviceId = resultSet.getString("ServiceID")
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
                matServiceDok.id = resultSet.getString("ID")
                matServiceDok.matServiceId = resultSet.getString("MatServiceID")
                matServiceDok.dokId = resultSet.getString("DokID")
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
                matServiceHistory.id = resultSet.getString("ID")
                matServiceHistory.matId = resultSet.getString("MatID")
                matServiceHistory.serviceId = resultSet.getString("ServiceID")
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
                serviceDok.id = resultSet.getString("ID")
                serviceDok.serviceId = resultSet.getString("ServiceID")
                serviceDok.dokId = resultSet.getString("DokID")
                // füge den Datensatz in die SQLite ein
                mainRepository.insertService_Dok(serviceDok)
            }
        }

        return true
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