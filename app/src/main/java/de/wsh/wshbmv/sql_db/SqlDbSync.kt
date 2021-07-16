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

                    // .....

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
            //       (Prio 1 sind DEL-Befehle, Prio 2 sind ADD-Befehle, zum Schluss dann die UPDATES...
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
                        val mobilChgProtokollFilterd = mainRepo.getChgProtokollsFiltered(
                            dtSyncToServer,
                            dtChgToServer,
                            mobilChangeDS.datenbank!!,
                            mobilChangeDS.satzId!!
                        )
                        Timber.tag(TAG).d(".. ergibt: $mobilChgProtokollFilterd")
                        //.. sammle alle geänderten Feldnamen ein..
                        val fieldnameList = mutableListOf<String>()
                        mobilChgProtokollFilterd.forEach {
                            it.feldname?.let { feldname -> fieldnameList.add(feldname) }
                        }
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
     *   SQL-Funktionen für den SQL-Server,
     *    - Delete Datensatz
     */
    private fun delDsOnSqlServer(datenbank: String, satzId: String): Boolean {
        var preparedStatement = myConn!!.prepareStatement("DELETE FROM $datenbank WHERE (ID = '$satzId')")
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
        preparedStatement = myConn!!.prepareStatement("INSERT INTO TsysChgProtokoll (Zeitstempel,Datenbank,SatzID,Feldname,Aktion) VALUES(?,?,?,?,?)")
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
        preparedStatement = myConn!!.prepareStatement("INSERT INTO TsysChgProtokoll (Zeitstempel,Datenbank,SatzID,Feldname,Aktion) VALUES(?,?,?,?,?)")
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
        val preparedStatement = myConn!!.prepareStatement("INSERT INTO TbmvBelege (ID, BelegTyp, BelegDatum, BelegUserGUID, ZielLagerGUID, ZielUserGUID, BelegStatus, ToAck, Notiz) VALUES(?,?,?,?,?,?,?,?,?)")
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
        val preparedStatement = myConn!!.prepareStatement("INSERT INTO TbmvBelege (ID, BelegID, Pos, MatGUID, Menge, VonLagerGUID, AckDatum) VALUES(?,?,?,?,?,?,?)")
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
        val preparedStatement = myConn!!.prepareStatement("INSERT INTO TbmvMat (ID, Scancode, Typ, Matchcode, MatGruppeGUID, Beschreibung, Hersteller, Modell, Seriennummer, UserGUID, MatStatus, BildGUID, BildBmp) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?)")
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
        val tbmvMatInLager = mainRepo.getMat_LagerByID(satzId) ?: return null
        var id = 1
        val preparedStatement = myConn!!.prepareStatement("INSERT INTO TbmvMat_Lager (ID, MatGUID, LagerGUID, Default, Bestand) VALUES(?,?,?,?,?)")
        preparedStatement.setString(id++, tbmvMatInLager.id)
        preparedStatement.setString(id++,tbmvMatInLager.matId)
        preparedStatement.setString(id++,tbmvMatInLager.lagerId)
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
        preparedStatement = myConn!!.prepareStatement("INSERT INTO TsysChgProtokoll (Zeitstempel,Datenbank,SatzID,Feldname,Aktion) VALUES(?,?,?,?,?)")
        preparedStatement.setTimestamp(1, sqlDate)
        preparedStatement.setString(2, datenbank)
        preparedStatement.setString(3, satzId)
        preparedStatement.setString(4, null)
        preparedStatement.setInt(5, DB_AKTION_UPDATE_DS)
//        sqlQuery = "INSERT INTO TsysChgProtokoll (Zeitstempel,Datenbank,SatzID,Feldname,Aktion) "
//        sqlQuery += "VALUES(${chgDate.formatedDateToSQL()},'$datenbank','$satzId',NULL,$DB_AKTION_UPDATE_DS)"
        try {
            fieldNames.forEach {
                preparedStatement.setString(4, it)
                Timber.tag(TAG).d("editDsOnSqlServer, Feld $it: $preparedStatement")
                preparedStatement.execute()
//                sqlQuery =
//                    "INSERT INTO TsysChgProtokoll (Zeitstempel,Datenbank,SatzID,Feldname,Aktion) "
//                sqlQuery += "VALUES(${chgDate.formatedDateToSQL()},'$datenbank','$satzId','$it',$DB_AKTION_UPDATE_DS)"
//                Timber.tag(TAG).d("editDsOnSqlServer: $sqlQuery")
//                statement.execute(sqlQuery)
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
                    tbmvBelege.belegDatum as Timestamp?
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
                    tbmvBelegPos.ackDatum as Timestamp?
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
        val tbmvMat_Lager = mainRepo.getMat_LagerByID(satzId) ?: return null
        if (fieldNames.isEmpty()) return null
        var strQuery = ""
        // zuerst den Query mit allen Feldern definieren...
        fieldNames.forEach {
            if (strQuery != "") strQuery += ", "
            when (it) {
                "isDefault" -> strQuery += "Default = ?"
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
        val preparedStatement = myConn!!.prepareStatement("UPDATE TsysUser SET $strQuery WHERE (ID = '$satzId')")
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
}