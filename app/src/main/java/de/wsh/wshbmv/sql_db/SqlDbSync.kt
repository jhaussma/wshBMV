package de.wsh.wshbmv.sql_db

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import de.wsh.wshbmv.db.entities.*
import de.wsh.wshbmv.db.entities.relations.*
import de.wsh.wshbmv.other.Constants.DB_AKTION_ADD_DS
import de.wsh.wshbmv.other.Constants.DB_AKTION_DELETE_DS
import de.wsh.wshbmv.other.Constants.DB_AKTION_UPDATE_DS
import de.wsh.wshbmv.other.Constants.SQL_SYNC_TABLES
import de.wsh.wshbmv.other.Constants.TAG
import de.wsh.wshbmv.other.GlobalVars
import de.wsh.wshbmv.other.GlobalVars.sqlErrorMessage
import de.wsh.wshbmv.other.GlobalVars.sqlStatus
import de.wsh.wshbmv.other.GlobalVars.sqlSynchronized
import de.wsh.wshbmv.other.enSqlStatus
import de.wsh.wshbmv.repositories.MainRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.lang.Exception
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

/** ################################################################################################
 *  Synchronisierung aller Daten der mobilen App mit dem WSH-Server
 */
class SqlDbSync @Inject constructor(
    private val mainRepo: MainRepository
) {
    private val connectionClass = SqlConnection()
    private var myConn: Connection? = null
    private var endTimeInMillis = 0L
    private var lastChgToServer = 0L
    private var lastChgFromServer = 0L
    private var tappSyncReport: TappSyncReport? = null
    private var sqlChangeProtokoll = mutableListOf<ChangeProtokoll>()
    private var mobilChangeProtokoll = mutableListOf<ChangeProtokoll>()

    init {
        Timber.tag(TAG).d("SqlDbSyn gestartet...")
        GlobalScope.launch(Dispatchers.IO) {
            sqlErrorMessage.postValue("")
            try {
                sqlStatus.postValue(enSqlStatus.INIT)
                myConn = connectionClass.dbConn()
                if (myConn == null) {
                    Timber.tag(TAG).e("Keine Verbindung zum SQL-Server!")
                    sqlStatus.postValue(enSqlStatus.NO_CONTACT)
                } else {
                    GlobalVars.sqlServerConnected = true
                    Timber.tag(TAG).d("Verbindung zum SQL-Server und TbmvMat steht!")

                    // hier wird nun synchronisiert ....
                    if (syncDatabase()) {
                        sqlStatus.postValue(enSqlStatus.PROCESS_ENDED)

                    } else {
                        sqlStatus.postValue(enSqlStatus.PROCESS_ABORTED)
                    }

                }
            } catch (ex: Exception) {
                sqlStatus.postValue(enSqlStatus.IN_ERROR)   // Ende ohne Erfolg!
                Timber.tag(TAG).e("Fehler ist aufgetreten: ${ex.message ?: ""}")
            }
            connectionClass.disConnect(myConn)
        }
    }


    /**
     *  eigentliche Synchronisierungsfunktion der SQL-DB mit der mobilen sqLite-DB
     */
    private suspend fun syncDatabase(): Boolean {
        sqlStatus.postValue(enSqlStatus.IN_PROCESS)
        // wir nehmen die aktuelle Zeit für den Start der Synchronisierung
        endTimeInMillis = System.currentTimeMillis()
        if (syncIsNeeded()) {
            // wir lesen das Änderungsprotokoll vom Server ein, gruppiert und sortiert nach Datenbank, SatzID...
            val statement = myConn!!.createStatement()
            val dtChgFromServer = Date(lastChgFromServer + 1000)
            val dtSyncFromServer = Date(tappSyncReport!!.lastFromServerTime + 1000)
            val dtChgToServer = Date(lastChgToServer + 1000)
            val dtSyncToServer = Date(tappSyncReport!!.lastToServerTime + 1000)
            var sqlQuery =
                "SELECT Datenbank, SatzID, MAX(Zeitstempel) AS MaxZeitstempel, SUM(CASE Aktion WHEN 0 THEN 1 ELSE 0 END) AS AddDS, SUM(CASE Aktion WHEN 1 THEN 1 ELSE 0 END) AS EditDS, SUM(CASE Aktion WHEN 2 THEN 1 ELSE 0 END) AS DelDS "
            sqlQuery += "FROM TsysChgProtokoll "
            sqlQuery += "WHERE (Zeitstempel BETWEEN ${dtSyncFromServer.formatedDateToSQL()} AND ${dtChgFromServer.formatedDateToSQL()})"
            sqlQuery += " AND (Datenbank IN ($SQL_SYNC_TABLES)) "
            sqlQuery += "GROUP BY Datenbank, SatzID ORDER BY Datenbank, SatzID"

            Timber.tag(TAG).d("syncDatabase, sqlQuery: $sqlQuery")

            val resultSet = statement.executeQuery(sqlQuery)
            if (resultSet != null) {
                while (resultSet.next()) {
                    sqlChangeProtokoll.add(
                        ChangeProtokoll(
                            datenbank = resultSet.getString("Datenbank"),
                            satzId = resultSet.getString("SatzID"),
                            maxZeitstempel = resultSet.getTimestamp("MaxZeitstempel"),
                            addDS = resultSet.getInt("AddDS"),
                            editDS = resultSet.getInt("EditDS"),
                            delDS = resultSet.getInt("DelDS")
                        )
                    )
                    // nur für Kontrollzwecke in der Testphase...
                    Timber.tag(TAG).d(
                        "syncDatabase, gefunden: ${resultSet.getString("Datenbank")}, SatzID:${
                            resultSet.getString("SatzID")
                        }, AddDD:${resultSet.getInt("AddDS")}, EditDS:${resultSet.getInt("EditDS")}, DelDS:${
                            resultSet.getInt(
                                "DelDS"
                            )
                        }"
                    )
                }
            }

            // wir lesen das Änderungsprotokoll vom Mobil ein, gruppiert und sortiert nach Datenbank, SatzID...
            mobilChangeProtokoll =
                mainRepo.getChgProtokollGroupedList(dtSyncToServer, dtChgToServer)
            Timber.tag(TAG)
                .d("syncDatabase, gefunden: ${mobilChangeProtokoll.size} Datensätze im Mobil-Client")

            // ... und starten nun die Auswertung, zuerst von der Mobil-Seite aus:
            //      -> Prio 1 sind DEL-Befehle, Prio 2 sind ADD-Befehle, zum Schluss dann die UPDATES...
            mobilChangeProtokoll.forEach { mobilChangeDS ->
                Timber.tag(TAG).d("syncDatabase, DS: $mobilChangeDS")
                if (mobilChangeDS.delDS > 0) {
                    // Ein Löschbefehl ersetzt immer alle anderen Einträge zur SatzID
                    // ..im ServerChangeProtokoll
                    Timber.tag(TAG)
                        .d("syncDatabase, DEL to Server gefunden, ${mobilChangeDS.datenbank} mit ${mobilChangeDS.satzId}...")
                    val filteredProtokoll =
                        sqlChangeProtokoll.filter { sqlChangeDS: ChangeProtokoll ->
                            sqlChangeDS.datenbank == mobilChangeDS.datenbank && sqlChangeDS.satzId == mobilChangeDS.satzId
                        }
                    if (filteredProtokoll.isNotEmpty()) {
                        Timber.tag(TAG)
                            .d("syncDatabase, sqlChangeProtokoll-Satz wird gelöscht wegen DELETE-Auftrag")
                        sqlChangeProtokoll.remove(filteredProtokoll.first())
                    }
                    // Löschbefehl auf der Serverseite
                    if (!delDsOnSqlServer(
                            mobilChangeDS.datenbank!!,
                            mobilChangeDS.satzId!!
                        )
                    ) return false
                } else if (mobilChangeDS.addDS > 0) {
                    // ein Add-Befehl fügt einfach einen neuen Datensat auf Serverseite ein...
                    Timber.tag(TAG)
                        .d("syncDatabase, ADD to Server gefunden, ${mobilChangeDS.datenbank} mit ${mobilChangeDS.satzId}...")
                    if (!addDsOnSqlServer(
                            mobilChangeDS.datenbank!!,
                            mobilChangeDS.satzId!!
                        )
                    ) return false
                } else {
                    // dann bleibt nur eine Änderung: (mobilChangeDS.editDS > 0)
                    // Prüfung auf evtl. gegenstehender Löschbefehl auf Serverseite...
                    Timber.tag(TAG)
                        .d("syncDatabase, UPDATE to Server gefunden, ${mobilChangeDS.datenbank} mit ${mobilChangeDS.satzId}...")
                    val filteredProtokoll =
                        sqlChangeProtokoll.filter { sqlChangeDS: ChangeProtokoll ->
                            sqlChangeDS.datenbank == mobilChangeDS.datenbank && sqlChangeDS.satzId == mobilChangeDS.satzId && sqlChangeDS.delDS > 0
                        }
                    if (filteredProtokoll.isEmpty()) {
                        //..ermittle alle Feldnamen, deren Inhalte geändert wurden..
                        val fieldnameList = mainRepo.getChgProtokollFeldnames(
                            dtSyncToServer,
                            dtChgToServer,
                            mobilChangeDS.datenbank!!,
                            mobilChangeDS.satzId!!
                        )

                        // Übergabe des Änderungsbefehls an den SQL-Server
                        Timber.tag(TAG)
                            .d("syncDatabase, UPDATE to Server gefunden, ${mobilChangeDS.datenbank} mit ${mobilChangeDS.satzId}, ${fieldnameList}...")
                        if (!editDsOnSqlServer(
                                mobilChangeDS.datenbank,
                                mobilChangeDS.satzId,
                                fieldnameList
                            )
                        ) return false
                    }
                }
            }

            // im zweiten Durchgang wird Server zu Mobil synchronisiert
            //   ->  Prio 1 sind DEL-Befehle, Prio 2 sind ADD-Befehle, zum Schluss dann die UPDATES...
            sqlChangeProtokoll.forEach { sqlChangeDS ->
                // Löschvorgang ?
                Timber.tag(TAG).d("syncDatabase, SQL-DS: $sqlChangeDS")
                if (sqlChangeDS.delDS > 0) {
                    // wir löschen den Datensatz aus der Mobil-DB raus
                    if (!delDsOnMobil(
                            sqlChangeDS.datenbank!!,
                            sqlChangeDS.satzId!!
                        )
                    ) return false
                } else if (sqlChangeDS.addDS > 0) {
                    // wir hängen einen neuen Datensatz an die Mobil-DB dran
                    if (!addDsOnMobil(
                            sqlChangeDS.datenbank!!,
                            sqlChangeDS.satzId!!
                        )
                    ) return false
                } else {
                    // es handelt sich um Update-Datensätze vom SQL-Server zur Mobil-DB
                    // .. ermittle alle Feldnamen, für die eine Änderung ansteht
                    val feldnamen = getFieldnamesFromDsEdit(
                        startTimeInMillis = tappSyncReport!!.lastFromServerTime,
                        endTimeInMillis = lastChgFromServer,
                        sqlChangeDS.datenbank!!,
                        sqlChangeDS.satzId!!
                    )
                    // .. übertrage alle Daten der Feldnamen in den Mobil-DB-Datensatz
                    if (feldnamen.isNotEmpty()) {
                        if (!editDsOnMobil(
                                sqlChangeDS.datenbank,
                                sqlChangeDS.satzId,
                                feldnamen
                            )
                        ) return false
                    }
                }
            }


            /** ########################################################################
             *  war bis hierher alles okay, speichern wir den Report zur Synchronisierung
             */
            val newtappSyncReport = TappSyncReport(
                timeStamp = System.currentTimeMillis(),
                lastFromServerTime = lastChgFromServer,
                lastToServerTime = lastChgToServer,
                errorFlag = 0
            )
            mainRepo.insertSyncReport(newtappSyncReport)
        } else {
            Timber.tag(TAG).d("syncDatabase ergab keinen Bedarf zur Synchronisierung")
        }
        sqlSynchronized = true
        return true
    }


    /** ############################################################################################
     *  wir ermitteln, ob eine Synchronisierung erforderlich ist...
     */
    private suspend fun syncIsNeeded(): Boolean {
        // wir laden die Zeitzeiger des letzten Sync-Protokolls der APP
        tappSyncReport = mainRepo.getLastSyncReport() ?: return false


        // lade die Zeit des letzten Änderungsprotokolls der APP
        lastChgToServer = 0
        val tappChgProtokoll = mainRepo.getLastChgProtokoll()
        if (tappChgProtokoll != null) {
            lastChgToServer = tappChgProtokoll.timeStamp
        }

        // lade die Zeit des letzten Änderungsprotokolls des Servers
        lastChgFromServer = 0
        val statement = myConn!!.createStatement()
        val resultSet =
            statement.executeQuery("SELECT TOP 1 * FROM TsysChgProtokoll WHERE Datenbank IN ($SQL_SYNC_TABLES) ORDER BY ID DESC")
        if (resultSet != null) {
            if (resultSet.next()) {
                val chgDate = resultSet.getTimestamp("Zeitstempel")
                lastChgFromServer = chgDate.time
                Timber.tag(TAG).d("Zeitbestimmung aus SQL: $chgDate")
            }
        }
        return lastChgToServer > tappSyncReport!!.lastToServerTime || lastChgFromServer > tappSyncReport!!.lastFromServerTime
    }

    /** ############################################################################################
     *   Ermittlung aller Feldnamen einer Datensatzänderung im SQL-Server-Change-Protokoll
     */
    private fun getFieldnamesFromDsEdit(
        startTimeInMillis: Long,
        endTimeInMillis: Long,
        datenbank: String,
        satzId: String
    ): List<String> {
        val feldnamen = mutableListOf<String>()
        val statement = myConn!!.createStatement()
        val dtStartTime = Date(startTimeInMillis + 1000)
        val dtEndTime = Date(endTimeInMillis + 1000)
        var sqlQuery = "SELECT Feldname FROM TsysChgProtokoll "
        sqlQuery += "WHERE (Zeitstempel BETWEEN ${dtStartTime.formatedDateToSQL()} AND ${dtEndTime.formatedDateToSQL()})"
        sqlQuery += "GROUP BY Feldname, Aktion, Datenbank, SatzID "
        sqlQuery += "HAVING (Datenbank = '$datenbank') AND (SatzID = '$satzId') AND (Aktion = ${DB_AKTION_UPDATE_DS})"
        val resultSet = statement.executeQuery(sqlQuery)
        if (resultSet != null) {
            while (resultSet.next()) {
                feldnamen.add(resultSet.getString("Feldname"))
            }
        }
        Timber.tag(TAG).d("alle Feldnamen: $feldnamen")
        return feldnamen
    }

    /** ############################################################################################
     *   SQL-Funktionen für die Mobil-SQLite-DB (Delete und Add)
     */
    private suspend fun delDsOnMobil(datenbank: String, satzId: String): Boolean {
        when (datenbank) {
            "TbmvBelege" -> mainRepo.deleteBelegById(satzId)
            "TbmvBelegPos" -> mainRepo.deleteBelegPosById(satzId)
            "TbmvDokumente" -> mainRepo.deleteDokumentById(satzId)
            "TbmvLager" -> mainRepo.deleteLagerById(satzId)
            "TbmvMat" -> mainRepo.deleteMatById(satzId)
            "TbmvMat_Lager" -> mainRepo.deleteMatToLagerById(satzId)
            "TbmvMat_Service" -> mainRepo.deleteMatToServiceById(satzId)
            "TbmvMatGruppen" -> mainRepo.deleteMatGruppeById(satzId)
            "TbmvMatService_Dok" -> mainRepo.deleteMatServiceToDokById(satzId)
            "TbmvMatService_Historie" -> mainRepo.deleteMatServiceToHistorieById(satzId)
            "TbmvService_Dok" -> mainRepo.deleteServiceToDokById(satzId)
            "TbmvServices" -> mainRepo.deleteServiceById(satzId)
            "TsysUser" -> mainRepo.deleteUserById(satzId)
            "TsysUser_Gruppe" -> mainRepo.deleteUserToGruppeById(satzId)
            "TsysUserGruppe" -> mainRepo.deleteUserGruppeById(satzId)
        }
        return true
    }

    private suspend fun addDsOnMobil(datenbank: String, satzId: String): Boolean {
        when (datenbank) {
            "TbmvBelege" -> return getTbmvBelegFromSql(satzId)
            "TbmvBelegPos" -> return getTbmvBelegPosFromSql(satzId)
            "TbmvDokumente" -> return getTbmvDokumenteFromSql(satzId)
            "TbmvLager" -> return getTbmvLagerFromSql(satzId)
            "TbmvMat" -> return getTbmvMatFromSql(satzId)
            "TbmvMat_Lager" -> return getTbmvMatToLagerFromSql(satzId)
            "TbmvMat_Service" -> return getTbmvMatToServiceFromSql(satzId)
            "TbmvMatGruppen" -> return getTbmvMatGruppeFromSql(satzId)
            "TbmvMatService_Dok" -> return getTbmvMatServiceToDokFromSql(satzId)
            "TbmvMatService_Historie" -> return getTbmvMatServiceToHistorieFromSql(satzId)
            "TbmvService_Dok" -> return getTbmvServiceToDokFromSql(satzId)
            "TbmvServices" -> return getTbmvServiceFromSql(satzId)
            "TsysUser" -> return getTsysUserFromSql(satzId)
            "TsysUser_Gruppe" -> return getTsysUserToGruppeFromSql(satzId)
            "TsysUserGruppe" -> return getTsysUserGruppeFromSql(satzId)
        }
        return true
    }

    /** #####################################################################
     *  SQL-Abfragen der Tabellen für Add-Datensatz von SQL zu SQLite (Mobil)
     */
    private suspend fun getTbmvBelegFromSql(satzId: String): Boolean {
        try {
            val tbmvBelege = TbmvBelege()
            val statement = myConn!!.createStatement()
            val resultSet = statement.executeQuery(
                "SELECT * FROM TbmvBelege WHERE (ID LIKE '${
                    satzId.uppercase(Locale.getDefault())
                }')"
            )
            if (resultSet != null) {
                resultSet.next()
                tbmvBelege.id = resultSet.getString("ID").lowercase(Locale.getDefault())
                tbmvBelege.belegTyp = resultSet.getString("BelegTyp")
                tbmvBelege.belegDatum = resultSet.getTimestamp("BelegDatum")
                tbmvBelege.belegUserGuid =
                    resultSet.getString("BelegUserGUID").lowercase(Locale.getDefault())
                tbmvBelege.zielLagerGuid =
                    resultSet.getString("ZielLagerGUID").lowercase(Locale.getDefault())
                tbmvBelege.zielUserGuid =
                    resultSet.getString("ZielUserGUID").lowercase(Locale.getDefault())
                tbmvBelege.belegStatus = resultSet.getString("BelegStatus")
                tbmvBelege.toAck = resultSet.getInt("ToAck")
                tbmvBelege.notiz = resultSet.getString("Notiz")
                // füge den Datensatz in die SQLite ein
                mainRepo.insertBeleg(tbmvBelege)
            }
        } catch (ex: Exception) {
            //Fehlermeldung und -behandlung...
            sqlErrorMessage.postValue(ex.toString())
            sqlStatus.postValue(enSqlStatus.IN_ERROR)
            return false
        }
        return true
    }

    private suspend fun getTbmvBelegPosFromSql(satzId: String): Boolean {
        try {
            val tbmvBelegPos = TbmvBelegPos()
            val statement = myConn!!.createStatement()
            val resultSet = statement.executeQuery(
                "SELECT * FROM TbmvBelegPos WHERE (ID LIKE '${
                    satzId.uppercase(Locale.getDefault())
                }')"
            )
            if (resultSet != null) {
                resultSet.next()
                tbmvBelegPos.id = resultSet.getString("ID").lowercase(Locale.getDefault())
                tbmvBelegPos.belegId = resultSet.getString("BelegID").lowercase(Locale.getDefault())
                tbmvBelegPos.pos = resultSet.getInt("Pos")
                tbmvBelegPos.matGuid = resultSet.getString("MatGUID").lowercase(Locale.getDefault())
                tbmvBelegPos.menge = resultSet.getFloat("Menge")
                tbmvBelegPos.vonLagerGuid =
                    resultSet.getString("VonLagerGUID")?.lowercase(Locale.getDefault())
                tbmvBelegPos.ackDatum = resultSet.getTimestamp("AckDatum")
                // füge den Datensatz in die SQLite ein
                mainRepo.insertBelegPos(tbmvBelegPos, true)
            }
        } catch (ex: Exception) {
            //Fehlermeldung und -behandlung...
            sqlErrorMessage.postValue(ex.toString())
            sqlStatus.postValue(enSqlStatus.IN_ERROR)
            return false
        }
        return true
    }

    private suspend fun getTbmvDokumenteFromSql(satzId: String): Boolean {
        try {
            val tbmvDokument = TbmvDokument()
            val statement = myConn!!.createStatement()
            val resultSet = statement.executeQuery(
                "SELECT * FROM TbmvDokumente WHERE (ID LIKE '${
                    satzId.uppercase(Locale.getDefault())
                }')"
            )
            if (resultSet != null) {
                resultSet.next()
                tbmvDokument.id = resultSet.getString("ID").lowercase(Locale.getDefault())
                tbmvDokument.version = resultSet.getString("Version")
                tbmvDokument.matID =
                    resultSet.getString("MatID")?.lowercase(Locale.getDefault())
                tbmvDokument.serviceID =
                    resultSet.getString("ServiceID")?.lowercase(Locale.getDefault())
                tbmvDokument.dateiName = resultSet.getString("DateiName")
                tbmvDokument.dateiVerzeichnis = resultSet.getString("DateiVerzeichnis")
                tbmvDokument.status = resultSet.getString("Status")
                tbmvDokument.erstellDtm = resultSet.getTimestamp("ErstellDtm")
                tbmvDokument.erstellName = resultSet.getString("ErstellName")
                tbmvDokument.grobklasse = resultSet.getString("Grobklasse")
                tbmvDokument.stichwort = resultSet.getString("Stichwort")
                tbmvDokument.extern = resultSet.getInt("Extern")
                tbmvDokument.inBearbeitung = resultSet.getInt("inBearbeitung")
                tbmvDokument.bearbeiter = resultSet.getString("Bearbeiter")
                tbmvDokument.reservierungTxt = resultSet.getString("Reservierungtxt")
                // füge den Datensatz in die SQLite ein
                mainRepo.insertDokument(tbmvDokument)
            }
        } catch (ex: Exception) {
            //Fehlermeldung und -behandlung...
            sqlErrorMessage.postValue(ex.toString())
            sqlStatus.postValue(enSqlStatus.IN_ERROR)
            return false
        }
        return true
    }

    private suspend fun getTbmvLagerFromSql(satzId: String): Boolean {
        try {
            val tbmvLager = TbmvLager()
            val statement = myConn!!.createStatement()
            val resultSet = statement.executeQuery(
                "SELECT * FROM TbmvLager WHERE (ID LIKE '${
                    satzId.uppercase(Locale.getDefault())
                }')"
            )
            if (resultSet != null) {
                resultSet.next()
                tbmvLager.id = resultSet.getString("ID").lowercase(Locale.getDefault())
                tbmvLager.scancode = resultSet.getString("Scancode")
                tbmvLager.typ = resultSet.getString("Typ")
                tbmvLager.matchcode = resultSet.getString("Matchcode")
                tbmvLager.userGuid = resultSet.getString("UserGUID").lowercase(Locale.getDefault())
                tbmvLager.beschreibung = resultSet.getString("Beschreibung")
                tbmvLager.status = resultSet.getString("Status")
                tbmvLager.bmLager = resultSet.getInt("BMLager")
                // füge den Datensatz in die SQLite ein
                mainRepo.insertLager(tbmvLager)
            }
        } catch (ex: Exception) {
            //Fehlermeldung und -behandlung...
            sqlErrorMessage.postValue(ex.toString())
            sqlStatus.postValue(enSqlStatus.IN_ERROR)
            return false
        }
        return true
    }

    private suspend fun getTbmvMatFromSql(satzId: String): Boolean {
        try {
            val tbmvMat = TbmvMat()
            val statement = myConn!!.createStatement()
            val resultSet = statement.executeQuery(
                "SELECT * FROM TbmvMat WHERE (ID LIKE '${
                    satzId.uppercase(Locale.getDefault())
                }')"
            )
            if (resultSet != null) {
                resultSet.next()
                tbmvMat.id = resultSet.getString("ID").lowercase(Locale.getDefault())
                tbmvMat.scancode = resultSet.getString("Scancode")
                tbmvMat.typ = resultSet.getString("Typ")
                tbmvMat.matchcode = resultSet.getString("Matchcode")
                tbmvMat.matGruppeGuid =
                    resultSet.getString("MatGruppeGUID").lowercase(Locale.getDefault())
                tbmvMat.beschreibung = resultSet.getString("Beschreibung")
                tbmvMat.hersteller = resultSet.getString("Hersteller")
                tbmvMat.modell = resultSet.getString("Modell")
                tbmvMat.seriennummer = resultSet.getString("Seriennummer")
                tbmvMat.userGuid = resultSet.getString("UserGUID").lowercase(Locale.getDefault())
                tbmvMat.matStatus = resultSet.getString("MatStatus")
                tbmvMat.bildBmp = toBitmap(resultSet.getBytes("BildBmp"))
                // füge den Datensatz in die SQLite ein
                mainRepo.updateMat(tbmvMat, noProtokoll = true)
            }
        } catch (ex: Exception) {
            //Fehlermeldung und -behandlung...
            sqlErrorMessage.postValue(ex.toString())
            sqlStatus.postValue(enSqlStatus.IN_ERROR)
            return false
        }
        return true
    }

    private suspend fun getTbmvMatToLagerFromSql(satzId: String): Boolean {
        try {
            val tbmvMat_Lager = TbmvMat_Lager()
            val statement = myConn!!.createStatement()
            val resultSet = statement.executeQuery(
                "SELECT * FROM TbmvMat_Lager WHERE (ID LIKE '${
                    satzId.uppercase(Locale.getDefault())
                }')"
            )
            if (resultSet != null) {
                resultSet.next()
                tbmvMat_Lager.id = resultSet.getString("ID").lowercase(Locale.getDefault())
                tbmvMat_Lager.matId = resultSet.getString("MatGUID").lowercase(Locale.getDefault())
                tbmvMat_Lager.lagerId =
                    resultSet.getString("LagerGUID").lowercase(Locale.getDefault())
                tbmvMat_Lager.isDefault = resultSet.getInt("Default")
                tbmvMat_Lager.bestand = resultSet.getFloat("Bestand")
                // füge den Datensatz in die SQLite ein
                mainRepo.insertMatToLager(tbmvMat_Lager, true)
            }
        } catch (ex: Exception) {
            //Fehlermeldung und -behandlung...
            sqlErrorMessage.postValue(ex.toString())
            sqlStatus.postValue(enSqlStatus.IN_ERROR)
            return false
        }
        return true
    }

    private suspend fun getTbmvMatToServiceFromSql(satzId: String): Boolean {
        try {
            val tbmvMat_Service = TbmvMat_Service()
            val statement = myConn!!.createStatement()
            val resultSet = statement.executeQuery(
                "SELECT * FROM TbmvMat_Service WHERE (ID LIKE '${
                    satzId.uppercase(Locale.getDefault())
                }')"
            )
            if (resultSet != null) {
                resultSet.next()
                tbmvMat_Service.id = resultSet.getString("ID").lowercase(Locale.getDefault())
                tbmvMat_Service.matId = resultSet.getString("MatID").lowercase(Locale.getDefault())
                tbmvMat_Service.serviceId =
                    resultSet.getString("ServiceID").lowercase(Locale.getDefault())
                tbmvMat_Service.nextServiceDatum = resultSet.getTimestamp("NextServiceDatum")
                tbmvMat_Service.nextInfoDatum = resultSet.getTimestamp("NextInfoDatum")
                // füge den Datensatz in die SQLite ein
                mainRepo.insertMatToService(tbmvMat_Service)
            }
        } catch (ex: Exception) {
            //Fehlermeldung und -behandlung...
            sqlErrorMessage.postValue(ex.toString())
            sqlStatus.postValue(enSqlStatus.IN_ERROR)
            return false
        }
        return true
    }

    private suspend fun getTbmvMatGruppeFromSql(satzId: String): Boolean {
        try {
            val tbmvMatGruppe = TbmvMatGruppe()
            val statement = myConn!!.createStatement()
            val resultSet = statement.executeQuery(
                "SELECT * FROM TbmvMatGruppen WHERE (ID LIKE '${
                    satzId.uppercase(Locale.getDefault())
                }')"
            )
            if (resultSet != null) {
                resultSet.next()
                tbmvMatGruppe.id = resultSet.getString("ID").lowercase(Locale.getDefault())
                tbmvMatGruppe.matGruppe = resultSet.getString("MatGruppe")
                tbmvMatGruppe.aktiv = resultSet.getInt("Aktiv")
                // füge den Datensatz in die SQLite ein
                mainRepo.insertMatGruppe(tbmvMatGruppe)
            }
        } catch (ex: Exception) {
            //Fehlermeldung und -behandlung...
            sqlErrorMessage.postValue(ex.toString())
            sqlStatus.postValue(enSqlStatus.IN_ERROR)
            return false
        }
        return true
    }

    private suspend fun getTbmvMatServiceToDokFromSql(satzId: String): Boolean {
        try {
            val tbmvMatService_Dok = TbmvMatService_Dok()
            val statement = myConn!!.createStatement()
            val resultSet = statement.executeQuery(
                "SELECT * FROM TbmvMatService_Dok WHERE (ID LIKE '${
                    satzId.uppercase(Locale.getDefault())
                }')"
            )
            if (resultSet != null) {
                resultSet.next()
                tbmvMatService_Dok.id = resultSet.getString("ID").lowercase(Locale.getDefault())
                tbmvMatService_Dok.matServiceId =
                    resultSet.getString("MatServiceID").lowercase(Locale.getDefault())
                tbmvMatService_Dok.dokId =
                    resultSet.getString("DokID").lowercase(Locale.getDefault())
                // füge den Datensatz in die SQLite ein
                mainRepo.insertMatServiceToDok(tbmvMatService_Dok)
            }
        } catch (ex: Exception) {
            //Fehlermeldung und -behandlung...
            sqlErrorMessage.postValue(ex.toString())
            sqlStatus.postValue(enSqlStatus.IN_ERROR)
            return false
        }
        return true
    }

    private suspend fun getTbmvMatServiceToHistorieFromSql(satzId: String): Boolean {
        try {
            val tbmvMatService_Historie = TbmvMatService_Historie()
            val statement = myConn!!.createStatement()
            val resultSet = statement.executeQuery(
                "SELECT * FROM TbmvMatService_Historie WHERE (ID LIKE '${
                    satzId.uppercase(Locale.getDefault())
                }')"
            )
            if (resultSet != null) {
                resultSet.next()
                tbmvMatService_Historie.id =
                    resultSet.getString("ID").lowercase(Locale.getDefault())
                tbmvMatService_Historie.matId =
                    resultSet.getString("MatID").lowercase(Locale.getDefault())
                tbmvMatService_Historie.serviceId =
                    resultSet.getString("ServiceID").lowercase(Locale.getDefault())
                tbmvMatService_Historie.serviceDatum = resultSet.getTimestamp("Servicedatum")
                tbmvMatService_Historie.abschlussDatum = resultSet.getTimestamp("Abschlussdatum")
                tbmvMatService_Historie.userGuid = resultSet.getString("UserGUID")
                // füge den Datensatz in die SQLite ein
                mainRepo.insertMatServiceToHistorie(tbmvMatService_Historie)
            }
        } catch (ex: Exception) {
            //Fehlermeldung und -behandlung...
            sqlErrorMessage.postValue(ex.toString())
            sqlStatus.postValue(enSqlStatus.IN_ERROR)
            return false
        }
        return true
    }

    private suspend fun getTbmvServiceToDokFromSql(satzId: String): Boolean {
        try {
            val tbmvService_Dok = TbmvService_Dok()
            val statement = myConn!!.createStatement()
            val resultSet = statement.executeQuery(
                "SELECT * FROM TbmvService_Dok WHERE (ID LIKE '${
                    satzId.uppercase(Locale.getDefault())
                }')"
            )
            if (resultSet != null) {
                resultSet.next()
                tbmvService_Dok.id = resultSet.getString("ID").lowercase(Locale.getDefault())
                tbmvService_Dok.serviceId =
                    resultSet.getString("ServiceID").lowercase(Locale.getDefault())
                tbmvService_Dok.dokId = resultSet.getString("DokID").lowercase(Locale.getDefault())
                // füge den Datensatz in die SQLite ein
                mainRepo.insertServiceToDok(tbmvService_Dok)
            }
        } catch (ex: Exception) {
            //Fehlermeldung und -behandlung...
            sqlErrorMessage.postValue(ex.toString())
            sqlStatus.postValue(enSqlStatus.IN_ERROR)
            return false
        }
        return true
    }

    private suspend fun getTbmvServiceFromSql(satzId: String): Boolean {
        try {
            val tbmvServices = TbmvServices()
            val statement = myConn!!.createStatement()
            val resultSet = statement.executeQuery(
                "SELECT * FROM TbmvService_Dok WHERE (ID LIKE '${
                    satzId.uppercase(Locale.getDefault())
                }')"
            )
            if (resultSet != null) {
                resultSet.next()
                tbmvServices.id = resultSet.getString("ID").lowercase(Locale.getDefault())
                tbmvServices.name = resultSet.getString("Name")
                tbmvServices.beschreibung = resultSet.getString("Beschreibung")
                tbmvServices.intervalNum = resultSet.getInt("IntervalNum")
                tbmvServices.intervalUnit = resultSet.getString("IntervalUnit")
                tbmvServices.doInfo = resultSet.getInt("DoInfo")
                tbmvServices.infoNum = resultSet.getInt("InfoNum")
                tbmvServices.infoUnit = resultSet.getString("InfoUnit")
                // füge den Datensatz in die SQLite ein
                mainRepo.insertService(tbmvServices)
            }
        } catch (ex: Exception) {
            //Fehlermeldung und -behandlung...
            sqlErrorMessage.postValue(ex.toString())
            sqlStatus.postValue(enSqlStatus.IN_ERROR)
            return false
        }
        return true
    }

    private suspend fun getTsysUserFromSql(satzId: String): Boolean {
        try {
            val tsysUser = TsysUser()
            val statement = myConn!!.createStatement()
            val resultSet = statement.executeQuery(
                "SELECT * FROM TsysUser WHERE (ID LIKE '${
                    satzId.uppercase(Locale.getDefault())
                }')"
            )
            if (resultSet != null) {
                resultSet.next()
                tsysUser.id = resultSet.getString("ID").lowercase(Locale.getDefault())
                tsysUser.vorname = resultSet.getString("Vorname") ?: ""
                tsysUser.nachname = resultSet.getString("NachName") ?: ""
                tsysUser.anrede = resultSet.getString("Anrede") ?: ""
                tsysUser.benutzerStatus = resultSet.getString("BenutzerStatus")
                tsysUser.email = resultSet.getString("Email") ?: ""
                tsysUser.telefon = resultSet.getString("Telefon") ?: ""
                tsysUser.kurzZeichen = resultSet.getString("KurzZeichen") ?: ""
                tsysUser.userKennung = resultSet.getString("UserKennung") ?: ""
                tsysUser.passHash = resultSet.getString("PassHash")
                tsysUser.titel = resultSet.getString("Titel") ?: ""
                tsysUser.dw = resultSet.getString("DW") ?: ""
                tsysUser.admin = resultSet.getInt("Admin")
                tsysUser.terminW = resultSet.getInt("TerminW")
                tsysUser.stammR = resultSet.getInt("StammR")
                tsysUser.stammW = resultSet.getInt("StammW")
                tsysUser.kundenR = resultSet.getInt("KundenR")
                tsysUser.kundenW = resultSet.getInt("KundenW")
                tsysUser.vorlagenR = resultSet.getInt("VorlagenR")
                tsysUser.vorlagenW = resultSet.getInt("VorlagenW")
                tsysUser.monteur = resultSet.getInt("Monteur")
                tsysUser.vertragR = resultSet.getInt("VertragR")
                tsysUser.vertragW = resultSet.getInt("VertragW")
                tsysUser.rechnungR = resultSet.getInt("RechnungR")
                tsysUser.rechnungW = resultSet.getInt("RechnungW")
                tsysUser.bmvR = resultSet.getInt("BmvR")
                tsysUser.bmvW = resultSet.getInt("BmvW")
                tsysUser.bmvAdmin = resultSet.getInt("BmvAdmin")
                // füge den Datensatz in die SQLite ein
                mainRepo.insertUser(tsysUser)
            }
        } catch (ex: Exception) {
            //Fehlermeldung und -behandlung...
            sqlErrorMessage.postValue(ex.toString())
            sqlStatus.postValue(enSqlStatus.IN_ERROR)
            return false
        }
        return true
    }

    private suspend fun getTsysUserToGruppeFromSql(satzId: String): Boolean {
        try {
            val tsysUserToGruppe = TsysUserToGruppe()
            val statement = myConn!!.createStatement()
            val resultSet = statement.executeQuery(
                "SELECT * FROM TsysUser_Gruppe WHERE (ID LIKE '${
                    satzId.uppercase(Locale.getDefault())
                }')"
            )
            if (resultSet != null) {
                resultSet.next()
                tsysUserToGruppe.id = resultSet.getString("ID").lowercase(Locale.getDefault())
                tsysUserToGruppe.gruppeId =
                    resultSet.getString("GruppeID").lowercase(Locale.getDefault())
                tsysUserToGruppe.userId =
                    resultSet.getString("UserID").lowercase(Locale.getDefault())
                // füge den Datensatz in die SQLite ein
                mainRepo.insertUserToGruppe(tsysUserToGruppe)
            }
        } catch (ex: Exception) {
            //Fehlermeldung und -behandlung...
            sqlErrorMessage.postValue(ex.toString())
            sqlStatus.postValue(enSqlStatus.IN_ERROR)
            return false
        }
        return true
    }

    private suspend fun getTsysUserGruppeFromSql(satzId: String): Boolean {
        try {
            val tsysUserGruppe = TsysUserGruppe()
            val statement = myConn!!.createStatement()
            val resultSet = statement.executeQuery(
                "SELECT * FROM TsysUser_Gruppe WHERE (ID LIKE '${
                    satzId.uppercase(Locale.getDefault())
                }')"
            )
            if (resultSet != null) {
                resultSet.next()
                tsysUserGruppe.id = resultSet.getString("ID").lowercase(Locale.getDefault())
                tsysUserGruppe.nameGruppe = resultSet.getString("NameGruppe")
                // füge den Datensatz in die SQLite ein
                mainRepo.insertUserGruppe(tsysUserGruppe)
            }
        } catch (ex: Exception) {
            //Fehlermeldung und -behandlung...
            sqlErrorMessage.postValue(ex.toString())
            sqlStatus.postValue(enSqlStatus.IN_ERROR)
            return false
        }
        return true
    }

    /** ############################################################################################
     *   SQL-Funktionen UPDATE für die Mobil-SQLite-DB
     */
    private suspend fun editDsOnMobil(
        datenbank: String,
        satzId: String,
        feldnamen: List<String>
    ): Boolean {

        when (datenbank) {
            "TbmvBelege" -> if (!updateTbmvBelegFromServer(satzId, feldnamen)) return false
            "TbmvBelegPos" -> if (!updateTbmvBelegPosFromServer(satzId, feldnamen)) return false
            "TbmvDokumente" -> if (!updateTbmvDokumentFromServer(satzId, feldnamen)) return false
            "TbmvLager" -> if (!updateTbmvLagerFromServer(satzId, feldnamen)) return false
            "TbmvMat" -> if (!updateTbmvMatFromServer(satzId, feldnamen)) return false
            "TbmvMat_Lager" -> if (!updateTbmvMatToLagerFromServer(satzId, feldnamen)) return false
            "TbmvMat_Service" -> if (!updateTbmvMatToServiceFromServer(
                    satzId,
                    feldnamen
                )
            ) return false
            "TbmvMatGruppen" -> if (!updateTbmvMatGruppenFromServer(satzId, feldnamen)) return false
            "TbmvMatService_Dok" -> if (!updateTbmvMatServiceToDokFromServer(
                    satzId,
                    feldnamen
                )
            ) return false
            "TbmvMatService_Historie" -> if (!updateTbmvMatServiceToHistorieFromServer(
                    satzId,
                    feldnamen
                )
            ) return false
            "TbmvService_Dok" -> if (!updateTbmvServiceToDokFromServer(
                    satzId,
                    feldnamen
                )
            ) return false
            "TbmvServices" -> if (!updateTbmvServiceFromServer(satzId, feldnamen)) return false
            "TsysUser" -> if (!updateTsysUserFromServer(satzId, feldnamen)) return false
            "TsysUser_Gruppe" -> if (!updateTsysUserToGruppeFromServer(
                    satzId,
                    feldnamen
                )
            ) return false
            "TsysUserGruppe" -> if (!updateTsysUserGruppeFromServer(satzId, feldnamen)) return false
        }
        return true
    }

    /** ########################################################
     *   alle SQL-Hilfsfunktionen für ein UPDATE vom Server zur Mobil-SQLite-DB
     */
    private suspend fun updateTbmvBelegFromServer(
        satzId: String,
        feldnamen: List<String>
    ): Boolean {
        val statement = myConn!!.createStatement()
        val resultSet = statement.executeQuery(
            "SELECT * FROM TbmvBelege WHERE (ID LIKE '${
                satzId.uppercase(Locale.getDefault())
            }')"
        )
        val tbmvBelege = mainRepo.getBelegZuBelegId(satzId)
        if (tbmvBelege == null) {
            sqlErrorMessage.postValue("TbmvBelege, Datensatz [$satzId] fehlt in Mobil-DB für UPDATE vom Server")
            return false
        }
        if (resultSet != null) {
            resultSet.next()
            feldnamen.forEach {
                when (it) {
                    "BelegTyp" -> tbmvBelege.belegTyp = resultSet.getString(it)
                    "BelegDatum" -> tbmvBelege.belegDatum = resultSet.getTimestamp(it)
                    "BelegUserGUID" -> tbmvBelege.belegUserGuid =
                        resultSet.getString(it).lowercase(Locale.getDefault())
                    "ZielLagerGUID" -> tbmvBelege.zielLagerGuid =
                        resultSet.getString(it).lowercase(Locale.getDefault())
                    "ZielUserGUID" -> tbmvBelege.zielUserGuid =
                        resultSet.getString(it).lowercase(Locale.getDefault())
                    "BelegStatus" -> tbmvBelege.belegStatus = resultSet.getString(it)
                    "ToAck" -> tbmvBelege.toAck = resultSet.getInt(it)
                    "Notiz" -> tbmvBelege.notiz = resultSet.getString(it)
                }
            }
            mainRepo.updateBeleg(tbmvBelege, feldnamen, true)
        }
        return true
    }

    private suspend fun updateTbmvBelegPosFromServer(
        satzId: String,
        feldnamen: List<String>
    ): Boolean {
        val statement = myConn!!.createStatement()
        val resultSet = statement.executeQuery(
            "SELECT * FROM TbmvBelegPos WHERE (ID LIKE '${
                satzId.uppercase(Locale.getDefault())
            }')"
        )
        val tbmvBelegPos = mainRepo.getBelegPosZuBelegPosId(satzId)
        if (tbmvBelegPos == null) {
            sqlErrorMessage.postValue("TbmvBelegPos, Datensatz [$satzId] fehlt in Mobil-DB für UPDATE vom Server")
            return false
        }
        if (resultSet != null) {
            resultSet.next()
            feldnamen.forEach {
                when (it) {
                    "BelegID" -> tbmvBelegPos.belegId =
                        resultSet.getString(it).lowercase(Locale.getDefault())
                    "Pos" -> tbmvBelegPos.pos = resultSet.getInt(it)
                    "MatGUID" -> tbmvBelegPos.matGuid =
                        resultSet.getString(it).lowercase(Locale.getDefault())
                    "Menge" -> tbmvBelegPos.menge = resultSet.getFloat(it)
                    "VonLagerGUID" -> tbmvBelegPos.vonLagerGuid =
                        resultSet.getString(it)?.lowercase(Locale.getDefault())
                    "AckDatum" -> tbmvBelegPos.ackDatum = resultSet.getTimestamp(it)
                }
            }
            mainRepo.updateBelegPos(tbmvBelegPos, feldnamen, true)
        }
        return true
    }

    private suspend fun updateTbmvDokumentFromServer(
        satzId: String,
        feldnamen: List<String>
    ): Boolean {
        val statement = myConn!!.createStatement()
        val resultSet = statement.executeQuery(
            "SELECT * FROM TbmvDokumente WHERE (ID LIKE '${
                satzId.uppercase(Locale.getDefault())
            }')"
        )
        val tbmvDokument = mainRepo.getDokumentById(satzId)
        if (tbmvDokument == null) {
            sqlErrorMessage.postValue("TbmvDokumente, Datensatz [$satzId] fehlt in Mobil-DB für UPDATE vom Server")
            return false
        }
        if (resultSet != null) {
            resultSet.next()
            feldnamen.forEach {
                when (it) {
                    "Version" -> tbmvDokument.version = resultSet.getString(it)
                    "MatID" -> tbmvDokument.matID =
                        resultSet.getString(it)?.lowercase(Locale.getDefault())
                    "ServiceID" -> tbmvDokument.serviceID =
                        resultSet.getString(it)?.lowercase(Locale.getDefault())
                    "DateiName" -> tbmvDokument.dateiName = resultSet.getString(it)
                    "DateiVerzeichnis" -> tbmvDokument.dateiVerzeichnis = resultSet.getString(it)
                    "Status" -> tbmvDokument.status = resultSet.getString(it)
                    "ErstellDtm" -> tbmvDokument.erstellDtm = resultSet.getTimestamp(it)
                    "ErstellName" -> tbmvDokument.erstellName = resultSet.getString(it)
                    "Grobklasse" -> tbmvDokument.grobklasse = resultSet.getString(it)
                    "Stichwort" -> tbmvDokument.stichwort = resultSet.getString(it)
                    "Extern" -> tbmvDokument.extern = resultSet.getInt(it)
                    "inBearbeitung" -> tbmvDokument.inBearbeitung = resultSet.getInt(it)
                    "Bearbeiter" -> tbmvDokument.bearbeiter = resultSet.getString(it)
                    "Reservierungtxt" -> tbmvDokument.reservierungTxt = resultSet.getString(it)
                }
            }
            mainRepo.updateDokument(tbmvDokument)
        }
        return true
    }

    private suspend fun updateTbmvLagerFromServer(
        satzId: String,
        feldnamen: List<String>
    ): Boolean {
        val statement = myConn!!.createStatement()
        val resultSet = statement.executeQuery(
            "SELECT * FROM TbmvLager WHERE (ID LIKE '${
                satzId.uppercase(Locale.getDefault())
            }')"
        )
        val tbmvLager = mainRepo.getLagerById(satzId)
        if (tbmvLager == null) {
            sqlErrorMessage.postValue("TbmvLager, Datensatz [$satzId] fehlt in Mobil-DB für UPDATE vom Server")
            return false
        }
        if (resultSet != null) {
            resultSet.next()
            feldnamen.forEach {
                when (it) {
                    "Scancode" -> tbmvLager.scancode = resultSet.getString(it)
                    "Typ" -> tbmvLager.typ = resultSet.getString(it)
                    "Matchcode" -> tbmvLager.matchcode = resultSet.getString(it)
                    "UserGUID" -> tbmvLager.userGuid =
                        resultSet.getString(it).lowercase(Locale.getDefault())
                    "Beschreibung" -> tbmvLager.beschreibung = resultSet.getString(it)
                    "Status" -> tbmvLager.status = resultSet.getString(it)
                    "BMLager" -> tbmvLager.bmLager = resultSet.getInt(it)
                }
            }
            mainRepo.updateLager(tbmvLager)
        }
        return true
    }

    private suspend fun updateTbmvMatFromServer(satzId: String, feldnamen: List<String>): Boolean {
        val statement = myConn!!.createStatement()
        val resultSet = statement.executeQuery(
            "SELECT * FROM TbmvMat WHERE (ID LIKE '${
                satzId.uppercase(Locale.getDefault())
            }')"
        )
        val tbmvMat = mainRepo.getMaterialByMatID(satzId)
        if (tbmvMat == null) {
            sqlErrorMessage.postValue("TbmvMat, Datensatz [$satzId] fehlt in Mobil-DB für UPDATE vom Server")
            return false
        }
        if (resultSet != null) {
            resultSet.next()
            feldnamen.forEach {
                when (it) {
                    "Scancode" -> tbmvMat.scancode = resultSet.getString(it)
                    "Typ" -> tbmvMat.typ = resultSet.getString(it)
                    "Matchcode" -> tbmvMat.matchcode = resultSet.getString(it)
                    "MatGruppeGUID" -> tbmvMat.matGruppeGuid =
                        resultSet.getString(it).lowercase(Locale.getDefault())
                    "Beschreibung" -> tbmvMat.beschreibung = resultSet.getString(it)
                    "Hersteller" -> tbmvMat.hersteller = resultSet.getString(it)
                    "Modell" -> tbmvMat.modell = resultSet.getString(it)
                    "Seriennummer" -> tbmvMat.seriennummer = resultSet.getString(it)
                    "UserGUID" -> tbmvMat.userGuid =
                        resultSet.getString(it).lowercase(Locale.getDefault())
                    "MatStatus" -> tbmvMat.matStatus = resultSet.getString(it)
                    "BildBmp" -> tbmvMat.bildBmp = toBitmap(resultSet.getBytes(it))
                }
            }
            mainRepo.updateMat(tbmvMat, null, true)
        }
        return true
    }

    private suspend fun updateTbmvMatToLagerFromServer(
        satzId: String,
        feldnamen: List<String>
    ): Boolean {
        val statement = myConn!!.createStatement()
        val resultSet = statement.executeQuery(
            "SELECT * FROM TbmvMat_Lager WHERE (ID LIKE '${
                satzId.uppercase(Locale.getDefault())
            }')"
        )
        val tbmvMat_Lager = mainRepo.getMatToLagerByID(satzId)
        if (tbmvMat_Lager == null) {
            sqlErrorMessage.postValue("TbmvMat_Lager, Datensatz [$satzId] fehlt in Mobil-DB für UPDATE vom Server")
            return false
        }
        if (resultSet != null) {
            resultSet.next()
            feldnamen.forEach {
                when (it) {
                    "MatGUID" -> tbmvMat_Lager.matId =
                        resultSet.getString(it).lowercase(Locale.getDefault())
                    "LagerGUID" -> tbmvMat_Lager.lagerId =
                        resultSet.getString(it).lowercase(Locale.getDefault())
                    "Default" -> tbmvMat_Lager.isDefault = resultSet.getInt(it)
                    "Bestand" -> tbmvMat_Lager.bestand = resultSet.getFloat(it)
                }
            }
            mainRepo.updateMatToLager(tbmvMat_Lager, true)
        }
        return true
    }

    private suspend fun updateTbmvMatToServiceFromServer(
        satzId: String,
        feldnamen: List<String>
    ): Boolean {
        val statement = myConn!!.createStatement()
        val resultSet = statement.executeQuery(
            "SELECT * FROM TbmvMat_Service WHERE (ID LIKE '${
                satzId.uppercase(Locale.getDefault())
            }')"
        )
        val tbmvMat_Service = mainRepo.getMatToServiceById(satzId)
        if (tbmvMat_Service == null) {
            sqlErrorMessage.postValue("TbmvMat_Service, Datensatz [$satzId] fehlt in Mobil-DB für UPDATE vom Server")
            return false
        }
        if (resultSet != null) {
            resultSet.next()
            feldnamen.forEach {
                when (it) {
                    "MatID" -> tbmvMat_Service.matId =
                        resultSet.getString(it).lowercase(Locale.getDefault())
                    "ServiceID" -> tbmvMat_Service.serviceId =
                        resultSet.getString(it).lowercase(Locale.getDefault())
                    "NextServiceDatum" -> tbmvMat_Service.nextServiceDatum =
                        resultSet.getTimestamp(it)
                    "NextInfoDatum" -> tbmvMat_Service.nextInfoDatum = resultSet.getTimestamp(it)
                }
            }
            mainRepo.updateMatToService(tbmvMat_Service)
        }
        return true
    }

    private suspend fun updateTbmvMatGruppenFromServer(
        satzId: String,
        feldnamen: List<String>
    ): Boolean {
        val statement = myConn!!.createStatement()
        val resultSet = statement.executeQuery(
            "SELECT * FROM TbmvMatGruppen WHERE (ID LIKE '${
                satzId.uppercase(Locale.getDefault())
            }')"
        )
        val tbmvMatGruppe = mainRepo.getMatGruppeById(satzId)
        if (tbmvMatGruppe == null) {
            sqlErrorMessage.postValue("TbmvMatGruppen, Datensatz [$satzId] fehlt in Mobil-DB für UPDATE vom Server")
            return false
        }
        if (resultSet != null) {
            resultSet.next()
            feldnamen.forEach {
                when (it) {
                    "MatGruppe" -> tbmvMatGruppe.matGruppe = resultSet.getString(it)
                    "Aktiv" -> tbmvMatGruppe.aktiv = resultSet.getInt(it)
                }
            }
            mainRepo.updateMatGruppe(tbmvMatGruppe)
        }
        return true
    }

    private suspend fun updateTbmvMatServiceToDokFromServer(
        satzId: String,
        feldnamen: List<String>
    ): Boolean {
        val statement = myConn!!.createStatement()
        val resultSet = statement.executeQuery(
            "SELECT * FROM TbmvMatService_Dok WHERE (ID LIKE '${
                satzId.uppercase(Locale.getDefault())
            }')"
        )
        val tbmvMatService_Dok = mainRepo.getMatServiceToDokById(satzId)
        if (tbmvMatService_Dok == null) {
            sqlErrorMessage.postValue("TbmvMatService_Dok, Datensatz [$satzId] fehlt in Mobil-DB für UPDATE vom Server")
            return false
        }
        if (resultSet != null) {
            resultSet.next()
            feldnamen.forEach {
                when (it) {
                    "MatServiceID" -> tbmvMatService_Dok.matServiceId =
                        resultSet.getString(it).lowercase(Locale.getDefault())
                    "DokID" -> tbmvMatService_Dok.dokId =
                        resultSet.getString(it).lowercase(Locale.getDefault())
                }
            }
            mainRepo.updateMatServiceToDok(tbmvMatService_Dok)
        }
        return true
    }

    private suspend fun updateTbmvMatServiceToHistorieFromServer(
        satzId: String,
        feldnamen: List<String>
    ): Boolean {
        val statement = myConn!!.createStatement()
        val resultSet = statement.executeQuery(
            "SELECT * FROM TbmvMatService_Historie WHERE (ID LIKE '${
                satzId.uppercase(Locale.getDefault())
            }')"
        )
        val tbmvMatService_Historie = mainRepo.getMatServiceToHistorieById(satzId)
        if (tbmvMatService_Historie == null) {
            sqlErrorMessage.postValue("TbmvMatService_Historie, Datensatz [$satzId] fehlt in Mobil-DB für UPDATE vom Server")
            return false
        }
        if (resultSet != null) {
            resultSet.next()
            feldnamen.forEach {
                when (it) {
                    "MatID" -> tbmvMatService_Historie.matId =
                        resultSet.getString(it).lowercase(Locale.getDefault())
                    "ServiceID" -> tbmvMatService_Historie.serviceId =
                        resultSet.getString(it).lowercase(Locale.getDefault())
                    "Servicedatum" -> tbmvMatService_Historie.serviceDatum =
                        resultSet.getTimestamp(it)
                    "Abschlussdatum" -> tbmvMatService_Historie.abschlussDatum =
                        resultSet.getTimestamp(it)
                    "UserGUID" -> tbmvMatService_Historie.userGuid =
                        resultSet.getString(it).lowercase(Locale.getDefault())
                }
            }
            mainRepo.updateMatServiceToHistorie(tbmvMatService_Historie)
        }
        return true
    }

    private suspend fun updateTbmvServiceToDokFromServer(
        satzId: String,
        feldnamen: List<String>
    ): Boolean {
        val statement = myConn!!.createStatement()
        val resultSet = statement.executeQuery(
            "SELECT * FROM TbmvService_Dok WHERE (ID LIKE '${
                satzId.uppercase(Locale.getDefault())
            }')"
        )
        val tbmvService_Dok = mainRepo.getServiceToDokById(satzId)
        if (tbmvService_Dok == null) {
            sqlErrorMessage.postValue("TbmvService_Dok, Datensatz [$satzId] fehlt in Mobil-DB für UPDATE vom Server")
            return false
        }
        if (resultSet != null) {
            resultSet.next()
            feldnamen.forEach {
                when (it) {
                    "ServiceID" -> tbmvService_Dok.serviceId =
                        resultSet.getString(it).lowercase(Locale.getDefault())
                    "DokID" -> tbmvService_Dok.dokId =
                        resultSet.getString(it).lowercase(Locale.getDefault())
                }
            }
            mainRepo.updateServiceToDok(tbmvService_Dok)
        }
        return true
    }

    private suspend fun updateTbmvServiceFromServer(
        satzId: String,
        feldnamen: List<String>
    ): Boolean {
        val statement = myConn!!.createStatement()
        val resultSet = statement.executeQuery(
            "SELECT * FROM TbmvServices WHERE (ID LIKE '${
                satzId.uppercase(Locale.getDefault())
            }')"
        )
        val tbmvService = mainRepo.getServiceById(satzId)
        if (tbmvService == null) {
            sqlErrorMessage.postValue("TbmvServices, Datensatz [$satzId] fehlt in Mobil-DB für UPDATE vom Server")
            return false
        }
        if (resultSet != null) {
            resultSet.next()
            feldnamen.forEach {
                when (it) {
                    "Name" -> tbmvService.name = resultSet.getString(it)
                    "Beschreibung" -> tbmvService.beschreibung = resultSet.getString(it)
                    "IntervalNum" -> tbmvService.intervalNum = resultSet.getInt(it)
                    "IntervalUnit" -> tbmvService.intervalUnit = resultSet.getString(it)
                    "DoInfo" -> tbmvService.doInfo = resultSet.getInt(it)
                    "InfoNum" -> tbmvService.infoNum = resultSet.getInt(it)
                    "InfoUnit" -> tbmvService.infoUnit = resultSet.getString(it)
                }
            }
            mainRepo.updateService(tbmvService)
        }
        return true
    }

    private suspend fun updateTsysUserFromServer(satzId: String, feldnamen: List<String>): Boolean {
        val statement = myConn!!.createStatement()
        val resultSet = statement.executeQuery(
            "SELECT * FROM TsysUser WHERE (ID LIKE '${
                satzId.uppercase(Locale.getDefault())
            }')"
        )
        val tsysUser = mainRepo.getUserByID(satzId)
        if (tsysUser == null) {
            sqlErrorMessage.postValue("TsysUser, Datensatz [$satzId] fehlt in Mobil-DB für UPDATE vom Server")
            return false
        }
        if (resultSet != null) {
            resultSet.next()
            feldnamen.forEach {
                when (it) {
                    "Vorname" -> tsysUser.vorname = resultSet.getString(it) ?: ""
                    "NachName" -> tsysUser.nachname = resultSet.getString(it) ?: ""
                    "Anrede" -> tsysUser.anrede = resultSet.getString(it) ?: ""
                    "BenutzerStatus" -> tsysUser.benutzerStatus = resultSet.getString(it)
                    "Email" -> tsysUser.email = resultSet.getString(it) ?: ""
                    "Telefon" -> tsysUser.telefon = resultSet.getString(it) ?: ""
                    "KurzZeichen" -> tsysUser.kurzZeichen = resultSet.getString(it) ?: ""
                    "UserKennung" -> tsysUser.userKennung = resultSet.getString(it) ?: ""
                    "PassHash" -> tsysUser.passHash = resultSet.getString(it)
                    "Titel" -> tsysUser.titel = resultSet.getString(it) ?: ""
                    "DW" -> tsysUser.dw = resultSet.getString(it) ?: ""
                    "Admin" -> tsysUser.admin = resultSet.getInt(it)
                    "TerminW" -> tsysUser.terminW = resultSet.getInt(it)
                    "StammR" -> tsysUser.stammR = resultSet.getInt(it)
                    "StammW" -> tsysUser.stammW = resultSet.getInt(it)
                    "KundenR" -> tsysUser.kundenR = resultSet.getInt(it)
                    "KundenW" -> tsysUser.kundenW = resultSet.getInt(it)
                    "VorlagenR" -> tsysUser.vorlagenR = resultSet.getInt(it)
                    "VorlagenW" -> tsysUser.vorlagenW = resultSet.getInt(it)
                    "Monteur" -> tsysUser.monteur = resultSet.getInt(it)
                    "VertragR" -> tsysUser.vertragR = resultSet.getInt(it)
                    "VertragW" -> tsysUser.vertragW = resultSet.getInt(it)
                    "RechnungR" -> tsysUser.rechnungR = resultSet.getInt(it)
                    "RechnungW" -> tsysUser.rechnungW = resultSet.getInt(it)
                    "BmvR" -> tsysUser.bmvR = resultSet.getInt(it)
                    "BmvW" -> tsysUser.bmvW = resultSet.getInt(it)
                    "BmvAdmin" -> tsysUser.bmvAdmin = resultSet.getInt(it)
                }
            }
            mainRepo.updateUser(tsysUser)
        }
        return true
    }

    private suspend fun updateTsysUserToGruppeFromServer(
        satzId: String,
        feldnamen: List<String>
    ): Boolean {
        val statement = myConn!!.createStatement()
        val resultSet = statement.executeQuery(
            "SELECT * FROM TsysUser_Gruppe WHERE (ID LIKE '${
                satzId.uppercase(Locale.getDefault())
            }')"
        )
        val tsysUserToGruppe = mainRepo.getUserToGruppeById(satzId)
        if (tsysUserToGruppe == null) {
            sqlErrorMessage.postValue("TsysUser_Gruppe, Datensatz [$satzId] fehlt in Mobil-DB für UPDATE vom Server")
            return false
        }
        if (resultSet != null) {
            resultSet.next()
            feldnamen.forEach {
                when (it) {
                    "GruppeID" -> tsysUserToGruppe.gruppeId =
                        resultSet.getString(it).lowercase(Locale.getDefault())
                    "UserID" -> tsysUserToGruppe.userId =
                        resultSet.getString(it).lowercase(Locale.getDefault())
                }
            }
            mainRepo.updateUserToGruppe(tsysUserToGruppe)
        }
        return true
    }

    private suspend fun updateTsysUserGruppeFromServer(
        satzId: String,
        feldnamen: List<String>
    ): Boolean {
        val statement = myConn!!.createStatement()
        val resultSet = statement.executeQuery(
            "SELECT * FROM TsysUserGruppe WHERE (ID LIKE '${
                satzId.uppercase(Locale.getDefault())
            }')"
        )
        val tsysUserGruppe = mainRepo.getUserGruppeById(satzId)
        if (tsysUserGruppe == null) {
            sqlErrorMessage.postValue("TsysUserGruppe, Datensatz [$satzId] fehlt in Mobil-DB für UPDATE vom Server")
            return false
        }
        if (resultSet != null) {
            resultSet.next()
            feldnamen.forEach {
                when (it) {
                    "NameGruppe" -> tsysUserGruppe.nameGruppe = resultSet.getString(it)
                }
            }
            mainRepo.updateUserGruppe(tsysUserGruppe)
        }
        return true
    }


    /** ############################################################################################
     *   SQL-Funktionen für den SQL-Server,
     *    - Delete Datensatz
     */
    private fun delDsOnSqlServer(datenbank: String, satzId: String): Boolean {
        var preparedStatement =
            myConn!!.prepareStatement("DELETE FROM $datenbank WHERE (ID = '$satzId')")
        try {
            Timber.tag(TAG).d("delDsOnSqlServer: $preparedStatement")
            preparedStatement.execute()
        } catch (ex: Exception) {
            //Fehlermeldung und -behandlung...
            sqlErrorMessage.postValue(ex.toString())
            sqlStatus.postValue(enSqlStatus.IN_ERROR)
            return false
        }
        //.. die ChgProtokoll-Tabelle des Servers nachpflegen
        val sqlDate = Timestamp(System.currentTimeMillis())
        preparedStatement =
            myConn!!.prepareStatement("INSERT INTO TsysChgProtokoll (Zeitstempel,Datenbank,SatzID,Feldname,Aktion) VALUES(?,?,?,?,?)")
        preparedStatement.setTimestamp(1, sqlDate)
        preparedStatement.setString(2, datenbank)
        preparedStatement.setString(3, satzId)
        preparedStatement.setString(4, null)
        preparedStatement.setInt(5, DB_AKTION_DELETE_DS)
        try {
            preparedStatement.execute()
        } catch (ex: Exception) {
            //Fehlermeldung und -behandlung...
            sqlErrorMessage.postValue(ex.toString())
            sqlStatus.postValue(enSqlStatus.IN_ERROR)
            return false
        }
        return true
    }

    /** ############################################################################################
     *   SQL-Funktionen für den SQL-Server,
     *    - Add Datensatz
     */
    private suspend fun addDsOnSqlServer(datenbank: String, satzId: String): Boolean {
        var preparedStatement: PreparedStatement? = null

        when (datenbank) {
            "TbmvBelege" -> preparedStatement = getQueryForAddTbmvBeleg(satzId)
            "TbmvBelegPos" -> preparedStatement = getQueryForAddTbmvBelegPos(satzId)
            "TbmvMat" -> preparedStatement = getQueryForAddTbmvMat(satzId)
            "TbmvMat_Lager" -> preparedStatement = getQueryForAddTbmvMatToLager(satzId)
            else -> sqlErrorMessage.postValue("ADD-DS zum Server für $datenbank ist noch nicht umgesetzt!")
        }
        try {
            Timber.tag(TAG).d("addDsOnSqlServer: $preparedStatement")
            preparedStatement?.execute()
        } catch (ex: Exception) {
            //Fehlermeldung und -behandlung...
            sqlErrorMessage.postValue(ex.toString())
            sqlStatus.postValue(enSqlStatus.IN_ERROR)
            return false
        }
        //.. die ChgProtokoll-Tabelle des Servers nachpflegen
        val sqlDate = Timestamp(System.currentTimeMillis())
        preparedStatement =
            myConn!!.prepareStatement("INSERT INTO TsysChgProtokoll (Zeitstempel,Datenbank,SatzID,Feldname,Aktion) VALUES(?,?,?,?,?)")
        preparedStatement.setTimestamp(1, sqlDate)
        preparedStatement.setString(2, datenbank)
        preparedStatement.setString(3, satzId)
        preparedStatement.setString(4, null)
        preparedStatement.setInt(5, DB_AKTION_ADD_DS)
        try {
            Timber.tag(TAG).d("addDsOnSqlServer: $preparedStatement")
            preparedStatement.execute()
        } catch (ex: Exception) {
            //Fehlermeldung und -behandlung...
            sqlErrorMessage.postValue(ex.toString())
            sqlStatus.postValue(enSqlStatus.IN_ERROR)
            return false
        }
        return true
    }

    /** ###############################################################################
     *  Erzeugung der SQL-Queries zum Anlegen von Datensätzen auf Serverseite
     *   .. für alle relevanten Tabellen
     */
    private suspend fun getQueryForAddTbmvBeleg(satzId: String): PreparedStatement? {
        val tbmvBelege = mainRepo.getBelegZuBelegId(satzId) ?: return null
        var id = 1
        val preparedStatement =
            myConn!!.prepareStatement("INSERT INTO TbmvBelege (ID, BelegTyp, BelegDatum, BelegUserGUID, ZielLagerGUID, ZielUserGUID, BelegStatus, ToAck, Notiz) VALUES(?,?,?,?,?,?,?,?,?)")
        preparedStatement.setString(id++, tbmvBelege.id)
        preparedStatement.setString(id++, tbmvBelege.belegTyp)
        preparedStatement.setTimestamp(id++, Timestamp(tbmvBelege.belegDatum!!.time))
        preparedStatement.setString(id++, tbmvBelege.belegUserGuid)
        preparedStatement.setString(id++, tbmvBelege.zielLagerGuid)
        preparedStatement.setString(id++, tbmvBelege.zielUserGuid)
        preparedStatement.setString(id++, tbmvBelege.belegStatus)
        preparedStatement.setInt(id++, tbmvBelege.toAck)
        preparedStatement.setString(id++, tbmvBelege.notiz)
        return preparedStatement
    }

    private suspend fun getQueryForAddTbmvBelegPos(satzId: String): PreparedStatement? {
        val tbmvBelegPos = mainRepo.getBelegPosZuBelegPosId(satzId) ?: return null
        var id = 1
        val preparedStatement =
            myConn!!.prepareStatement("INSERT INTO TbmvBelegPos (ID, BelegID, Pos, MatGUID, Menge, VonLagerGUID, AckDatum) VALUES(?,?,?,?,?,?,?)")
        preparedStatement.setString(id++, tbmvBelegPos.id)
        preparedStatement.setString(id++, tbmvBelegPos.belegId)
        preparedStatement.setInt(id++, tbmvBelegPos.pos)
        preparedStatement.setString(id++, tbmvBelegPos.matGuid)
        preparedStatement.setFloat(id++, tbmvBelegPos.menge)
        preparedStatement.setString(id++, tbmvBelegPos.vonLagerGuid)
        if (tbmvBelegPos.ackDatum == null) {
            preparedStatement.setTimestamp(id++, null)
        } else {
            preparedStatement.setTimestamp(id++, Timestamp(tbmvBelegPos.ackDatum!!.time))
        }
        return preparedStatement
    }

    private suspend fun getQueryForAddTbmvMat(satzId: String): PreparedStatement? {
        val tbmvMat = mainRepo.getMaterialByMatID(satzId) ?: return null
        var id = 1
        val preparedStatement =
            myConn!!.prepareStatement("INSERT INTO TbmvMat (ID, Scancode, Typ, Matchcode, MatGruppeGUID, Beschreibung, Hersteller, Modell, Seriennummer, UserGUID, MatStatus, BildGUID, BildBmp) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?)")
        preparedStatement.setString(id++, tbmvMat.id)
        preparedStatement.setString(id++, tbmvMat.scancode)
        preparedStatement.setString(id++, tbmvMat.typ)
        preparedStatement.setString(id++, tbmvMat.matchcode)
        preparedStatement.setString(id++, tbmvMat.matGruppeGuid)
        preparedStatement.setString(id++, tbmvMat.beschreibung)
        preparedStatement.setString(id++, tbmvMat.hersteller)
        preparedStatement.setString(id++, tbmvMat.modell)
        preparedStatement.setString(id++, tbmvMat.seriennummer)
        preparedStatement.setString(id++, tbmvMat.userGuid)
        preparedStatement.setString(id++, tbmvMat.matStatus)
        if (tbmvMat.bildBmp == null) {
            preparedStatement.setBytes(id++, null)
        } else {
            val byteOutputStream = ByteArrayOutputStream()
            tbmvMat.bildBmp!!.compress(Bitmap.CompressFormat.PNG, 0, byteOutputStream)
            val bytesImage = byteOutputStream.toByteArray()
            preparedStatement.setBytes(id++, bytesImage)
        }
        return preparedStatement
    }

    private suspend fun getQueryForAddTbmvMatToLager(satzId: String): PreparedStatement? {
        val tbmvMatInLager = mainRepo.getMatToLagerByID(satzId) ?: return null
        var id = 1
        val preparedStatement =
            myConn!!.prepareStatement("INSERT INTO TbmvMat_Lager (ID, MatGUID, LagerGUID, [Default], Bestand) VALUES(?,?,?,?,?)")
        preparedStatement.setString(id++, tbmvMatInLager.id)
        preparedStatement.setString(id++, tbmvMatInLager.matId)
        preparedStatement.setString(id++, tbmvMatInLager.lagerId)
        preparedStatement.setInt(id++, tbmvMatInLager.isDefault)
        preparedStatement.setFloat(id++, tbmvMatInLager.bestand)
        return preparedStatement
    }


    /** ############################################################################################
     *   SQL-Funktionen für den SQL-Server,
     *    - Update Datensatz
     */
    private suspend fun editDsOnSqlServer(
        datenbank: String,
        satzId: String,
        fieldNames: List<String>
    ): Boolean {
        var preparedStatement: PreparedStatement? = null

        when (datenbank) {
            "TbmvBelege" -> preparedStatement = getQueryForEditTbmvBeleg(satzId, fieldNames)
            "TbmvBelegPos" -> preparedStatement = getQueryForEditTbmvBelegPos(satzId, fieldNames)
            "TbmvMat" -> preparedStatement = getQueryForEditTbmvMat(satzId, fieldNames)
            "TbmvMat_Lager" -> preparedStatement = getQueryForEditTbmvMatToLager(satzId, fieldNames)
            "TsysUser" -> preparedStatement = getQueryForEditTsysUser(satzId, fieldNames)
            else -> sqlErrorMessage.postValue("UPDATE-DS zum Server für $datenbank ist noch nicht umgesetzt!")
        }
        try {
            Timber.tag(TAG).d("editDsOnSqlServer: ${preparedStatement.toString()}")
            preparedStatement?.execute()
        } catch (ex: Exception) {
            //Fehlermeldung und -behandlung...
            sqlErrorMessage.postValue(ex.toString())
            sqlStatus.postValue(enSqlStatus.IN_ERROR)
            return false
        }
        //.. die ChgProtokoll-Tabelle des Servers nachpflegen
        val sqlDate = Timestamp(System.currentTimeMillis())
        preparedStatement =
            myConn!!.prepareStatement("INSERT INTO TsysChgProtokoll (Zeitstempel,Datenbank,SatzID,Feldname,Aktion) VALUES(?,?,?,?,?)")
        preparedStatement.setTimestamp(1, sqlDate)
        preparedStatement.setString(2, datenbank)
        preparedStatement.setString(3, satzId)
        preparedStatement.setString(4, null)
        preparedStatement.setInt(5, DB_AKTION_UPDATE_DS)
        try {
            fieldNames.forEach {
                preparedStatement.setString(4, it.firstSignToUpper())
                Timber.tag(TAG).d("editDsOnSqlServer, Feld $it: $preparedStatement")
                preparedStatement.execute()
            }
        } catch (ex: Exception) {
            //Fehlermeldung und -behandlung...
            sqlErrorMessage.postValue(ex.toString())
            sqlStatus.postValue(enSqlStatus.IN_ERROR)
            return false
        }
        return true
    }

    /** ###############################################################################
     *  Erzeugung der SQL-Queries zum Ändern von Datensätzen auf Serverseite
     *   .. für alle relevanten Tabellen
     */
    private suspend fun getQueryForEditTbmvBeleg(
        satzId: String,
        fieldNames: List<String>
    ): PreparedStatement? {
        val tbmvBelege = mainRepo.getBelegZuBelegId(satzId) ?: return null
        if (fieldNames.isEmpty()) return null
        var strQuery = ""
        // zuerst den Query mit allen Feldern definieren...
        fieldNames.forEach {
            if (strQuery != "") strQuery += ", "
            when (it) {
                "belegTyp" -> strQuery += "BelegTyp = ?"
                "belegDatum" -> strQuery += "BelegDatum = ?"
                "belegUserGuid" -> strQuery += "BelegUserGUID = ?"
                "zielLagerGuid" -> strQuery += "ZielLagerGUID = ?"
                "zielUserGuid" -> strQuery += "ZielUserGUID = ?"
                "belegStatus" -> strQuery += "BelegStatus = ?"
                "toAck" -> strQuery += "ToAck = ?"
                "notiz" -> strQuery += "Notiz = ?"
            }
        }
        val preparedStatement =
            myConn!!.prepareStatement("UPDATE TbmvBelege SET $strQuery WHERE (ID = '$satzId')")
        // ...dann die Werte zuweisen...
        var id = 1
        fieldNames.forEach {
            when (it) {
                "belegTyp" -> preparedStatement.setString(id++, tbmvBelege.belegTyp)
                "belegDatum" -> preparedStatement.setTimestamp(
                    id++,
                    tbmvBelege.belegDatum?.time?.let { it1 -> Timestamp(it1) }
                )
                "belegUserGuid" -> preparedStatement.setString(id++, tbmvBelege.belegUserGuid)
                "zielLagerGuid" -> preparedStatement.setString(id++, tbmvBelege.zielLagerGuid)
                "zielUserGuid" -> preparedStatement.setString(id++, tbmvBelege.zielUserGuid)
                "belegStatus" -> preparedStatement.setString(id++, tbmvBelege.belegStatus)
                "toAck" -> preparedStatement.setInt(id++, tbmvBelege.toAck)
                "notiz" -> preparedStatement.setString(id++, tbmvBelege.notiz)
            }
        }
        return preparedStatement
    }

    private suspend fun getQueryForEditTbmvBelegPos(
        satzId: String,
        fieldNames: List<String>
    ): PreparedStatement? {
        val tbmvBelegPos = mainRepo.getBelegPosZuBelegPosId(satzId) ?: return null
        if (fieldNames.isEmpty()) return null
        var strQuery = ""
        // zuerst den Query mit allen Feldern definieren...
        fieldNames.forEach {
            if (strQuery != "") strQuery += ", "
            when (it) {
                "belegId" -> strQuery += "BelegID = ?"
                "pos" -> strQuery += "Pos = ?"
                "matGuid" -> strQuery += "MatGUID = ?"
                "menge" -> strQuery += "Menge = ?"
                "vonLagerGuid" -> strQuery += "VonLagerGuid = ?"
                "ackDatum" -> strQuery += "AckDatum = ?"
            }
        }
        val preparedStatement =
            myConn!!.prepareStatement("UPDATE TbmvBelegPos SET $strQuery WHERE (ID = '$satzId')")
        // ...dann die Werte zuweisen...
        var id = 1
        fieldNames.forEach {
            when (it) {
                "belegId" -> preparedStatement.setString(id++, tbmvBelegPos.belegId)
                "pos" -> preparedStatement.setInt(id++, tbmvBelegPos.pos)
                "matGuid" -> preparedStatement.setString(id++, tbmvBelegPos.matGuid)
                "menge" -> preparedStatement.setFloat(id++, tbmvBelegPos.menge)
                "vonLagerGuid" -> preparedStatement.setString(id++, tbmvBelegPos.vonLagerGuid)
                "ackDatum" -> preparedStatement.setTimestamp(
                    id++,
                    tbmvBelegPos.ackDatum?.time?.let { it1 -> Timestamp(it1) }
                )
            }
        }
        return preparedStatement
    }

    private suspend fun getQueryForEditTbmvMat(
        satzId: String,
        fieldNames: List<String>
    ): PreparedStatement? {
        val tbmvMat = mainRepo.getMaterialByMatID(satzId) ?: return null
        if (fieldNames.isEmpty()) return null
        var strQuery = ""
        // zuerst den Query mit allen Feldern definieren...
        fieldNames.forEach {
            if (strQuery != "") strQuery += ", "
            when (it) {
                "scancode" -> strQuery += "Scancode = ?"
                "typ" -> strQuery += "Typ = ?"
                "matchcode" -> strQuery += "Matchcode = ?"
                "matGruppeGuid" -> strQuery += "MatGruppeGuid = ?"
                "beschreibung" -> strQuery += "Beschreibung = ?"
                "hersteller" -> strQuery += "Hersteller = ?"
                "modell" -> strQuery += "Modell = ?"
                "seriennummer" -> strQuery += "Seriennummer = ?"
                "userGuid" -> strQuery += "UserGUID = ?"
                "matStatus" -> strQuery += "MatStatus = ?"
                "bildBmp" -> strQuery += "BildBmp = ?"
            }
        }
        val preparedStatement =
            myConn!!.prepareStatement("UPDATE TbmvMat SET $strQuery WHERE (ID = '$satzId')")
        // ...dann die Werte zuweisen...
        var id = 1
        fieldNames.forEach {
            when (it) {
                "scancode" -> preparedStatement.setString(id++, tbmvMat.scancode)
                "typ" -> preparedStatement.setString(id++, tbmvMat.typ)
                "matchcode" -> preparedStatement.setString(id++, tbmvMat.matchcode)
                "matGruppeGuid" -> preparedStatement.setString(id++, tbmvMat.matGruppeGuid)
                "beschreibung" -> preparedStatement.setString(id++, tbmvMat.beschreibung)
                "hersteller" -> preparedStatement.setString(id++, tbmvMat.hersteller)
                "modell" -> preparedStatement.setString(id++, tbmvMat.modell)
                "seriennummer" -> preparedStatement.setString(id++, tbmvMat.seriennummer)
                "userGuid" -> preparedStatement.setString(id++, tbmvMat.userGuid)
                "matStatus" -> preparedStatement.setString(id++, tbmvMat.matStatus)
                "bildBmp" -> if (tbmvMat.bildBmp == null) {
                    preparedStatement.setBytes(id++, null)
                } else {
                    val byteOutputStream = ByteArrayOutputStream()
                    tbmvMat.bildBmp!!.compress(Bitmap.CompressFormat.PNG, 0, byteOutputStream)
                    val bytesImage = byteOutputStream.toByteArray()
                    preparedStatement.setBytes(id++, bytesImage)
                }
            }
        }
        return preparedStatement
    }

    private suspend fun getQueryForEditTbmvMatToLager(
        satzId: String,
        fieldNames: List<String>
    ): PreparedStatement? {
        val tbmvMat_Lager = mainRepo.getMatToLagerByID(satzId) ?: return null
        if (fieldNames.isEmpty()) return null
        var strQuery = ""
        // zuerst den Query mit allen Feldern definieren...
        fieldNames.forEach {
            if (strQuery != "") strQuery += ", "
            when (it) {
                "isDefault" -> strQuery += "[Default] = ?"
                "bestand" -> strQuery += "Bestand = ?"
            }
        }
        val preparedStatement =
            myConn!!.prepareStatement("UPDATE TbmvMat_Lager SET $strQuery WHERE (ID = '$satzId')")
        // ...dann die Werte zuweisen...
        var id = 1
        fieldNames.forEach {
            when (it) {
                "isDefault" -> preparedStatement.setInt(id++, tbmvMat_Lager.isDefault)
                "bestand" -> preparedStatement.setFloat(id++, tbmvMat_Lager.bestand)
            }
        }
        return preparedStatement
    }

    // beim TsysUser darf aktuell nur das Passwort geändert werden
    private suspend fun getQueryForEditTsysUser(
        satzId: String,
        fieldNames: List<String>
    ): PreparedStatement? {
        val tsysUser = mainRepo.getUserByID(satzId) ?: return null
        if (fieldNames.isEmpty()) return null
        var strQuery = ""
        // zuerst den Query mit allen Feldern definieren...
        fieldNames.forEach {
            if (strQuery != "") strQuery += ", "
            when (it) {
                "passHash" -> strQuery += "PassHash = ?"
            }
        }
        val preparedStatement =
            myConn!!.prepareStatement("UPDATE TsysUser SET $strQuery WHERE (ID = '$satzId')")
        // ...dann die Werte zuweisen...
        var id = 1
        fieldNames.forEach {
            when (it) {
                "passHash" -> preparedStatement.setString(id++, tsysUser.passHash)
            }
        }
        return preparedStatement
    }

    /** ############################################################################################
     *  Konverter, Hilfsfunktionen zum Formatieren
     */
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

    // formatiert ein Datum für einen SQL-SELECT
    private fun Date.formatedDateToSQL(): String {
        val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.GERMANY)
        return "CONVERT(DATETIME, '" + simpleDateFormat.format(this) + "',102)"
    }

    // formatiert das erste Zeichen eines Strings groß
    private fun String.firstSignToUpper(): String {
        return this.replaceFirstChar { it.uppercase() }
    }
}