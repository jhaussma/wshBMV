package de.wsh.wshbmv.sql_db

import android.os.StrictMode
import android.util.Log
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
        var connString: String?
        try {
            Class.forName("net.sourceforge.jtds.jdbc.Driver")
            connString = "jdbc:jtds:sqlserver://$ip/$db;instance=SQLEXPRESS;encrypt=false;user=$username;password=$password;"
            conn = DriverManager.getConnection(connString)
        } catch (ex : SQLException) {

            Log.e(TAG, ex.message ?:"")
        } catch (ex1 : ClassNotFoundException) {
            Log.e(TAG, ex1.message ?:"")
        } catch (ex2 : Exception) {
            Log.e(TAG, ex2.message ?:"")
        }
        return  conn
    }

    fun disConnect(conn: Connection?) {
        conn?.close()
    }
}