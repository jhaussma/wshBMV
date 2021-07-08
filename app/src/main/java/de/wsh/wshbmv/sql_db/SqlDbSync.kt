package de.wsh.wshbmv.sql_db

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import de.wsh.wshbmv.other.Constants.TAG
import de.wsh.wshbmv.other.GlobalVars
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

/** ################################################################################################
 *  Synchronisierung aller Daten der mobilen App mit dem WSH-Server
 */
class SqlDbSync @Inject constructor(
    val mainRepository: MainRepository
) {
    lateinit var connection: SqlConnection
    private var myConn: Connection? = null


    init {
        Timber.tag(TAG).d("SqlDbSyn gestartet...")
        GlobalScope.launch(Dispatchers.IO) {
            try {
                GlobalVars.sqlStatus = enSqlStatus.IN_PROCESS
                myConn = connection.dbConn()
                if (myConn == null) {
                    Timber.tag(TAG).e("Fehler bei Kommunikation mit SQL-Server!")
                    GlobalVars.sqlStatus = enSqlStatus.IN_ERROR
                } else {
                    GlobalVars.sqlServerConnected = true
                    Timber.tag(TAG).d("Verbindung zum SQL-Server und TbmvMat steht!")
//                    if (doFirstSync) {
//                        if (firstSyncDatabase()) {
//                            Timber.tag(TAG).d("Erst-Synchronisierung ist durchgelaufen")
//                        } else {
//                            Timber.tag(TAG).d("Erst-Synchronisieriung war nicht erfolgreich!")
//                        }
//                    } else {
//                        GlobalVars.sqlStatus = enSqlStatus.PROCESS_ENDED
//                    }
                }
            } catch (ex: Exception) {
                GlobalVars.sqlStatus = enSqlStatus.IN_ERROR // Ende ohne Erfolg!
                Timber.tag(TAG).e("Fehler ist aufgetreten: ${ex.message ?: ""}")
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