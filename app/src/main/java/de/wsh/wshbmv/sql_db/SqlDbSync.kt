package de.wsh.wshbmv.sql_db

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import de.wsh.wshbmv.db.entities.TappSyncReport
import de.wsh.wshbmv.db.entities.relations.ChangeProtokoll
import de.wsh.wshbmv.other.Constants.DB_AKTION_ADD_DS
import de.wsh.wshbmv.other.Constants.DB_AKTION_DELETE_DS
import de.wsh.wshbmv.other.Constants.DB_AKTION_UPDATE_DS
import de.wsh.wshbmv.other.Constants.SQL_SYNC_TABLES
import de.wsh.wshbmv.other.Constants.TAG
import de.wsh.wshbmv.other.GlobalVars
import de.wsh.wshbmv.other.GlobalVars.sqlErrorMessage
import de.wsh.wshbmv.other.GlobalVars.sqlStatus
import de.wsh.wshbmv.other.enSqlStatus
import de.wsh.wshbmv.repositories.MainRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.lang.Exception
import java.sql.Connection
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

/** ################################################################################################
 *  Synchronisierung aller Daten der mobilen App mit dem WSH-Server
 */
class SqlDbSync @Inject constructor(
    val mainRepo: MainRepository
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
            GlobalVars.sqlErrorMessage.postValue("")
            try {
                GlobalVars.sqlStatus.postValue(enSqlStatus.INIT)
                myConn = connectionClass.dbConn()
                if (myConn == null) {
                    Timber.tag(TAG).e("Keine Verbindung zum SQL-Server!")
                    GlobalVars.sqlStatus.postValue(enSqlStatus.NO_CONTACT)
                } else {
                    GlobalVars.sqlServerConnected = true
                    Timber.tag(TAG).d("Verbindung zum SQL-Server und TbmvMat steht!")

                    // hier wird nun synchronisiert ....
                    if (syncDatabase()) {
                        GlobalVars.sqlStatus.postValue(enSqlStatus.PROCESS_ENDED)

                    } else {
                        GlobalVars.sqlStatus.postValue(enSqlStatus.PROCESS_ABORTED)
                    }

                    // .....

                }
            } catch (ex: Exception) {
                GlobalVars.sqlStatus.postValue(enSqlStatus.IN_ERROR)   // Ende ohne Erfolg!
                Timber.tag(TAG).e("Fehler ist aufgetreten: ${ex.message ?: ""}")
            }
            connectionClass.disConnect(myConn)
        }
    }


    /**
     *  eigentliche Synchronisierungsfunktion der SQL-DB mit der mobilen sqLite-DB
     */
    private suspend fun syncDatabase(): Boolean {
        GlobalVars.sqlStatus.postValue(enSqlStatus.IN_PROCESS)
        // wir nehmen die aktuelle Zeit für den Start der Synchronisierung
        endTimeInMillis = System.currentTimeMillis()
        if (syncIsNeeded()) {
            // wir lesen das Änderungsprotokoll vom Server ein, gruppiert und sortiert nach Datenbank, SatzID...
            val statement = myConn!!.createStatement()
            val dtChgFromServer = Date(lastChgFromServer + 1000)
            val dtSyncFromServer = Date(tappSyncReport!!.lastFromServerTime + 1000)
            val dtChgToServer = Date(lastChgToServer + 1000)
            val dtSyncToServer = Date(tappSyncReport!!.lastToServerTime + 1000)
            var sqlQuery: String =
                "SELECT Datenbank, SatzID, MAX(Zeitstempel) AS MaxZeitstempel, SUM(CASE Aktion WHEN 0 THEN 1 ELSE 0 END) AS AddDS, SUM(CASE Aktion WHEN 1 THEN 1 ELSE 0 END) AS EditDS, SUM(CASE Aktion WHEN 2 THEN 1 ELSE 0 END) AS DelDS "
            sqlQuery += "FROM TsysChgProtokoll "
            sqlQuery += "WHERE (Zeitstempel BETWEEN ${dtSyncFromServer.formatedDateToSQL()} AND ${dtChgFromServer.formatedDateToSQL()})"
            sqlQuery += " AND (Datenbank IN ($SQL_SYNC_TABLES)) "
            sqlQuery += "GROUP BY Datenbank, SatzID ORDER BY Datenbank, SatzID"
            var resultSet = statement.executeQuery(sqlQuery)
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
            Timber.tag(TAG).d("syncDatabase, gefunden: ${mobilChangeProtokoll.size} Datensätze im Mobil-Client")

            // ... und starten nun die Auswertung, zuerst von der Mobil-Seite aus:
            //       (Prio 1 sind DEL-Befehle, Prio 2 sind ADD-Befehle, zum Schluss dann die UPDATES...
            mobilChangeProtokoll.forEach() { mobilChangeDS ->
                Timber.tag(TAG).d("syncDatabase, DS: $mobilChangeDS")
                if (mobilChangeDS.delDS > 0) {
                    // Ein Löschbefehl ersetzt immer alle anderen Einträge zur SatzID
                    // ..im ServerChangeProtokoll
                    Timber.tag(TAG).d("syncDatabase, DEL to Server gefunden, ${mobilChangeDS.datenbank} mit ${mobilChangeDS.satzId}...")
                    val filteredProtokoll =
                        sqlChangeProtokoll.filter { sqlChangeDS: ChangeProtokoll ->
                            sqlChangeDS.datenbank == mobilChangeDS.datenbank && sqlChangeDS.satzId == mobilChangeDS.satzId
                        }
                    if (filteredProtokoll.isNotEmpty()) {
                        Timber.tag(TAG).d("syncDatabase, sqlChangeProtokoll-Satz wird gelöscht wegen DELETE-Auftrag")
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
                    Timber.tag(TAG).d("syncDatabase, ADD to Server gefunden, ${mobilChangeDS.datenbank} mit ${mobilChangeDS.satzId}...")
                    if (!addDsOnSqlServer(
                            mobilChangeDS.datenbank!!,
                            mobilChangeDS.satzId!!
                        )
                    ) return false
                } else {
                    // dann bleibt nur eine Änderung: (mobilChangeDS.editDS > 0)
                    // Prüfung auf evtl. gegenstehender Löschbefehl auf Serverseite...
                    Timber.tag(TAG).d("syncDatabase, UPDATE to Server gefunden, ${mobilChangeDS.datenbank} mit ${mobilChangeDS.satzId}...")
                    val filteredProtokoll =
                        sqlChangeProtokoll.filter { sqlChangeDS: ChangeProtokoll ->
                            sqlChangeDS.datenbank == mobilChangeDS.datenbank && sqlChangeDS.satzId == mobilChangeDS.satzId && sqlChangeDS.delDS > 0
                        }
                    if (filteredProtokoll.isEmpty()) {
                        //..ermittle alle Feldnamen, deren Inhalte geändert wurden..
                        val mobilChgProtokollFilterd = mainRepo.getChgProtokollsFiltered(
                            dtSyncToServer,
                            dtChgToServer,
                            mobilChangeDS.datenbank!!,
                            mobilChangeDS.satzId!!
                        )
                        Timber.tag(TAG).d(".. ergibt: $mobilChgProtokollFilterd")
                        //.. sammle alle geänderten Feldnamen ein..
                        var fieldnameList = mutableListOf<String>()
                        mobilChgProtokollFilterd.forEach {
                            it.feldname?.let { feldname -> fieldnameList.add(feldname) }
                        }
                        // Übergabe des Änderungsbefehls an den SQL-Server
                        Timber.tag(TAG).d("syncDatabase, UPDATE to Server gefunden, ${mobilChangeDS.datenbank} mit ${mobilChangeDS.satzId}, ${fieldnameList}...")
                        if (!editDsOnSqlServer(
                                mobilChangeDS.datenbank,
                                mobilChangeDS.satzId,
                                fieldnameList
                            )
                        ) return false
                    }
                }
            }

        } else {
            Timber.tag(TAG).d("tappSyncReport: ${tappSyncReport.toString()}")
            Timber.tag(TAG).d("lastChgToServer = $lastChgToServer")
            Timber.tag(TAG).d("lastChgFromServer = $lastChgFromServer")

        }

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
        var resultSet =
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
     *   SQL-Funktionen für den SQL-Server,
     *    - Delete Datensatz
     */
    private fun delDsOnSqlServer(datenbank: String, satzId: String): Boolean {
        val statement = myConn!!.createStatement()
        var sqlQuery = "DELETE FROM $datenbank WHERE (ID = '$satzId')"
        try {
            Timber.tag(TAG).d("delDsOnSqlServer: $sqlQuery")
            statement.execute(sqlQuery)
        } catch (ex: Exception) {
            //Fehlermeldung und -behandlung...
            sqlErrorMessage.postValue(ex.toString())
            sqlStatus.postValue(enSqlStatus.IN_ERROR)
            return false
        }
        //.. die ChgProtokoll-Tabelle des Servers nachpflegen
        val chgDate = Date()
        sqlQuery = "INSERT INTO TsysChgProtokoll (Zeitstempel,Datenbank,SatzID,Feldname,Aktion) "
        sqlQuery += "VALUES(${chgDate.formatedDateToSQL()},'$datenbank','$satzId',NULL,$DB_AKTION_DELETE_DS)"
        try {
            statement.execute(sqlQuery)
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
        val statement = myConn!!.createStatement()
        var sqlQuery: String? = null

        when (datenbank) {
            "TbmvBelege" -> sqlQuery = getQueryForAddTbmvBeleg(satzId)
            "TbmvBelegPos" -> sqlQuery = getQueryForAddTbmvBelegPos(satzId)
            "TbmvMat" -> sqlQuery = getQueryForAddTbmvMat(satzId)
            "TbmvMat_Lager" -> sqlQuery = getQueryForAddTbmvMatToLager(satzId)
            else -> sqlErrorMessage.postValue("ADD-DS zum Server für $datenbank ist noch nicht umgesetzt!")
        }
        try {
            Timber.tag(TAG).d("addDsOnSqlServer: $sqlQuery")
            sqlQuery?.let {
                statement.execute(sqlQuery)
            }
        } catch (ex: Exception) {
            //Fehlermeldung und -behandlung...
            sqlErrorMessage.postValue(ex.toString())
            sqlStatus.postValue(enSqlStatus.IN_ERROR)
            return false
        }
        //.. die ChgProtokoll-Tabelle des Servers nachpflegen
        val chgDate = Date()
        sqlQuery = "INSERT INTO TsysChgProtokoll (Zeitstempel,Datenbank,SatzID,Feldname,Aktion) "
        sqlQuery += "VALUES(${chgDate.formatedDateToSQL()},'$datenbank','$satzId',NULL,$DB_AKTION_ADD_DS)"
        try {
            Timber.tag(TAG).d("addDsOnSqlServer: $sqlQuery")
            statement.execute(sqlQuery)
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
    private suspend fun getQueryForAddTbmvBeleg(satzId: String): String? {
        val tbmvBelege = mainRepo.getBelegZuBelegId(satzId) ?: return null
        val strFeldnamen =
            "(ID, BelegTyp, BelegDatum, BelegUserGUID, ZielLagerGUID, ZielUserGUID, BelegStatus, ToAck, Notiz)"
        var strValues = "VALUES("
        strValues += "'${tbmvBelege.id}'"
        strValues += ",'${tbmvBelege.belegTyp}'"
        strValues += ",${tbmvBelege.belegDatum!!.formatedDateToSQL()}"
        strValues += ",'${tbmvBelege.belegUserGuid}'"
        strValues += ",'${tbmvBelege.zielLagerGuid}'"
        strValues += ",'${tbmvBelege.zielUserGuid}'"
        strValues += ",'${tbmvBelege.belegStatus}'"
        strValues += ",${tbmvBelege.toAck}"
        strValues += ",'${tbmvBelege.notiz}'"
        strValues += ")"
        return "INSERT INTO TbmvBelege $strFeldnamen $strValues"
    }

    private suspend fun getQueryForAddTbmvBelegPos(satzId: String): String? {
        val tbmvBelegPos = mainRepo.getBelegPosZuBelegPosId(satzId) ?: return null
        val strFeldnamen = "(ID, BelegID, Pos, MatGUID, Menge, VonLagerGUID, AckDatum)"
        var strValues = "VALUES("
        strValues += "'${tbmvBelegPos.id}'"
        strValues += ",'${tbmvBelegPos.belegId}'"
        strValues += ",${tbmvBelegPos.pos}"
        strValues += ",'${tbmvBelegPos.matGuid}'"
        strValues += ",'${tbmvBelegPos.menge}'"
        strValues += if (tbmvBelegPos.vonLagerGuid == null) {
            ",NULL"
        } else {
            ",'${tbmvBelegPos.vonLagerGuid}'"
        }
        strValues += if (tbmvBelegPos.ackDatum == null) {
            ",NULL"
        } else {
            ",${tbmvBelegPos.ackDatum!!.formatedDateToSQL()}"
        }
        strValues += ")"
        return "INSERT INTO TbmvBelegPos $strFeldnamen $strValues"
    }

    private suspend fun getQueryForAddTbmvMat(satzId: String): String? {
        val tbmvMat = mainRepo.getMaterialByMatID(satzId) ?: return null
        val strFeldnamen =
            "(ID, Scancode, Typ, Matchcode, MatGruppeGUID, Beschreibung, Hersteller, Modell, Seriennummer, UserGUID, MatStatus, BildGUID, BildBmp)"
        var strValues = "VALUES("
        strValues += "'${tbmvMat.id}'"
        strValues += ",'${tbmvMat.scancode}'"
        strValues += ",'${tbmvMat.typ}'"
        strValues += ",'${tbmvMat.matchcode}'"
        strValues += ",'${tbmvMat.matGruppeGuid}'"
        strValues += ",'${tbmvMat.beschreibung}'"
        strValues += ",'${tbmvMat.hersteller}'"
        strValues += ",'${tbmvMat.modell}'"
        strValues += ",'${tbmvMat.seriennummer}'"
        strValues += ",'${tbmvMat.userGuid}'"
        strValues += ",'${tbmvMat.matStatus}'"
        strValues += ",NULL"
        // TODO: BitMap-Umsetzung auf varbinary klären...
        if (tbmvMat.bildBmp == null) {
            strValues += ",NULL"
        } else {
            strValues += ",CAST('${tbmvMat.bildBmp.toString()}' AS varbinary(max))"
        }
        strValues += ")"
        return "INSERT INTO TbmvMat $strFeldnamen $strValues"
    }

    private suspend fun getQueryForAddTbmvMatToLager(satzId: String): String? {
        val tbmvMatInLager = mainRepo.getMat_LagerByID(satzId) ?: return null
        val strFeldnamen = "(ID, MatGUID, LagerGUID, Default, Bestand)"
        var strValues = "VALUES("
        strValues += "'${tbmvMatInLager.id}'"
        strValues += ",'${tbmvMatInLager.matId}'"
        strValues += ",'${tbmvMatInLager.lagerId}'"
        strValues += ",${tbmvMatInLager.isDefault}"
        strValues += ",${tbmvMatInLager.bestand}"
        strValues += ")"
        return "INSERT INTO TbmvMat_Lager $strFeldnamen $strValues"
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
        val statement = myConn!!.createStatement()
        var sqlQuery: String? = null

        when (datenbank) {
            "TbmvBelege" -> sqlQuery = getQueryForEditTbmvBeleg(satzId, fieldNames)
            "TbmvBelegPos" -> sqlQuery = getQueryForEditTbmvBelegPos(satzId, fieldNames)
            "TbmvMat" -> sqlQuery = getQueryForEditTbmvMat(satzId, fieldNames)
            "TbmvMat_Lager" -> sqlQuery = getQueryForEditTbmvMatToLager(satzId, fieldNames)
            "TsysUser" -> sqlQuery = getQueryForEditTsysUser(satzId, fieldNames)
            else -> sqlErrorMessage.postValue("UPDATE-DS zum Server für $datenbank ist noch nicht umgesetzt!")
        }
        try {
            Timber.tag(TAG).d("editDsOnSqlServer: $sqlQuery")
            sqlQuery?.let {
                statement.execute(sqlQuery)
            }
        } catch (ex: Exception) {
            //Fehlermeldung und -behandlung...
            sqlErrorMessage.postValue(ex.toString())
            sqlStatus.postValue(enSqlStatus.IN_ERROR)
            return false
        }
        //.. die ChgProtokoll-Tabelle des Servers nachpflegen
        val chgDate = Date()
        sqlQuery = "INSERT INTO TsysChgProtokoll (Zeitstempel,Datenbank,SatzID,Feldname,Aktion) "
        sqlQuery += "VALUES(${chgDate.formatedDateToSQL()},'$datenbank','$satzId',NULL,$DB_AKTION_UPDATE_DS)"
        try {
            fieldNames.forEach() {
                sqlQuery =
                    "INSERT INTO TsysChgProtokoll (Zeitstempel,Datenbank,SatzID,Feldname,Aktion) "
                sqlQuery += "VALUES(${chgDate.formatedDateToSQL()},'$datenbank','$satzId','$it',$DB_AKTION_UPDATE_DS)"
                Timber.tag(TAG).d("editDsOnSqlServer: $sqlQuery")
                statement.execute(sqlQuery)
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
    ): String? {
        val tbmvBelege = mainRepo.getBelegZuBelegId(satzId) ?: return null
        if (fieldNames.isEmpty()) return null
        var strQuery = ""
        fieldNames.forEach() {
            if (strQuery != "") strQuery += ", "
            when (it) {
                "belegTyp" -> strQuery += "BelegTyp = '${tbmvBelege.belegTyp}'"
                "belegDatum" -> strQuery += "BelegDatum = ${tbmvBelege.belegDatum!!.formatedDateToSQL()}"
                "belegUserGuid" -> strQuery += "BelegUserGUID = '${tbmvBelege.belegUserGuid}'"
                "zielLagerGuid" -> strQuery += "ZielLagerGUID = '${tbmvBelege.zielLagerGuid}'"
                "zielUserGuid" -> strQuery += "ZielUserGUID = '${tbmvBelege.zielUserGuid}'"
                "belegStatus" -> strQuery += "BelegStatus = '${tbmvBelege.belegStatus}'"
                "toAck" -> strQuery += "ToAck = ${tbmvBelege.toAck}"
                "notiz" -> strQuery += "Notiz = '${tbmvBelege.notiz}'"
            }
        }
        return "UPDATE TbmvBelege SET $strQuery WHERE (ID = '$satzId')"
    }

    private suspend fun getQueryForEditTbmvBelegPos(
        satzId: String,
        fieldNames: List<String>
    ): String? {
        val tbmvBelegPos = mainRepo.getBelegPosZuBelegPosId(satzId) ?: return null
        if (fieldNames.isEmpty()) return null
        var strQuery = ""
        fieldNames.forEach() {
            if (strQuery != "") strQuery += ", "
            when (it) {
                "belegId" -> strQuery += "BelegID = '${tbmvBelegPos.belegId}'"
                "pos" -> strQuery += "Pos = ${tbmvBelegPos.pos}"
                "matGuid" -> strQuery += "MatGUID = '${tbmvBelegPos.matGuid}'"
                "menge" -> strQuery += "Menge = ${tbmvBelegPos.menge}"
                "vonLagerGuid" -> strQuery += if (tbmvBelegPos.vonLagerGuid == null) {
                    "VonLagerGuid = NULL"
                } else {
                    "VonLagerGuid = '${tbmvBelegPos.vonLagerGuid}'"
                }

                "ackDatum" -> strQuery += if (tbmvBelegPos.ackDatum == null) {
                    "BelegStatus = NULL"
                } else {
                    "BelegStatus = '${tbmvBelegPos.ackDatum!!.formatedDateToSQL()}'"
                }
            }
        }
        return "UPDATE TbmvBelegPos SET $strQuery WHERE (ID = '$satzId')"
    }

    private suspend fun getQueryForEditTbmvMat(
        satzId: String,
        fieldNames: List<String>
    ): String? {
        val tbmvMat = mainRepo.getMaterialByMatID(satzId) ?: return null
        if (fieldNames.isEmpty()) return null
        var strQuery = ""
        fieldNames.forEach() {
            if (strQuery != "") strQuery += ", "
            when (it) {
                "scancode" -> strQuery += "Scancode = '${tbmvMat.scancode}'"
                "typ" -> strQuery += "Typ = '${tbmvMat.typ}'"
                "matchcode" -> strQuery += "Matchcode = '${tbmvMat.matchcode}'"
                "matGruppeGuid" -> strQuery += "MatGruppeGuid = '${tbmvMat.matGruppeGuid}'"
                "beschreibung" -> strQuery += "Beschreibung = '${tbmvMat.beschreibung}'"
                "hersteller" -> strQuery += "Hersteller = '${tbmvMat.hersteller}'"
                "modell" -> strQuery += "Modell = '${tbmvMat.modell}'"
                "seriennummer" -> strQuery += "Seriennummer = '${tbmvMat.seriennummer}'"
                "userGuid" -> strQuery += "UserGUID = '${tbmvMat.userGuid}'"
                "matStatus" -> strQuery += "MatStatus = '${tbmvMat.matStatus}'"
                "bildBmp" -> strQuery += if (tbmvMat.bildBmp == null) {
                    "BildBmp = NULL"
                } else {
                    // TODO: hier muss vermutlich nachgeschärft werden!!
                    "BildBmp = CAST('${tbmvMat.bildBmp.toString()}' AS varbinary(max))"
                }
            }
        }
        return "UPDATE TbmvMat SET $strQuery WHERE (ID = '$satzId')"
    }

    private suspend fun getQueryForEditTbmvMatToLager(
        satzId: String,
        fieldNames: List<String>
    ): String? {
        val tbmvMat_Lager = mainRepo.getMat_LagerByID(satzId) ?: return null
        if (fieldNames.isEmpty()) return null
        var strQuery = ""
        fieldNames.forEach() {
            if (strQuery != "") strQuery += ", "
            when (it) {
                "isDefault" -> strQuery += "Default = ${tbmvMat_Lager.isDefault}"
                "bestand" -> strQuery += "Bestand = ${tbmvMat_Lager.bestand}"
            }
        }
        return "UPDATE TbmvMat_Lager SET $strQuery WHERE (ID = '$satzId')"
    }

    // beim TsysUser darf aktuell nur das Passwort geändert werden
    private suspend fun getQueryForEditTsysUser(
        satzId: String,
        fieldNames: List<String>
    ): String? {
        val tsysUser = mainRepo.getUserByID(satzId) ?: return null
        if (fieldNames.isEmpty()) return null
        var strQuery = ""
        fieldNames.forEach() {
            if (strQuery != "") strQuery += ", "
            when (it) {
                "passHash" -> strQuery += "PassHash = '${tsysUser.passHash}'"
            }
        }
        return "UPDATE TsysUser SET $strQuery WHERE (ID = '$satzId')"
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
        var simpleDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        return "CONVERT(DATETIME, '" + simpleDateFormat.format(this) + "',102)"
    }
}