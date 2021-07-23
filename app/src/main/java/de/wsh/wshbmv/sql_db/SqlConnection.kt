package de.wsh.wshbmv.sql_db

import android.os.StrictMode
import de.wsh.wshbmv.other.Constants.SQL_CONN_DB
import de.wsh.wshbmv.other.Constants.SQL_CONN_IP
import de.wsh.wshbmv.other.Constants.SQL_USER_NAME
import de.wsh.wshbmv.other.Constants.SQL_USER_PWD
import de.wsh.wshbmv.other.Constants.TAG
import timber.log.Timber
import java.lang.Exception
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException

class SqlConnection {

    private val ip = SQL_CONN_IP
    private val db = SQL_CONN_DB
    private val username = SQL_USER_NAME
    private val password = SQL_USER_PWD

    fun dbConn() : Connection? {
        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)
        var conn : Connection? = null
        val connString: String?
        try {
            Class.forName("net.sourceforge.jtds.jdbc.Driver")
            connString = "jdbc:jtds:sqlserver://$ip/$db;instance=SQLEXPRESS;encrypt=false;user=$username;password=$password;"
            conn = DriverManager.getConnection(connString)
        } catch (ex : SQLException) {
            Timber.tag(TAG).e(ex.message ?:"")
        } catch (ex1 : ClassNotFoundException) {
            Timber.tag(TAG).e(ex1.message ?:"")
        } catch (ex2 : Exception) {
            Timber.tag(TAG).e( ex2.message ?:"")
        }
        return  conn
    }

    fun disConnect(conn: Connection?) {
        conn?.close()
    }
}