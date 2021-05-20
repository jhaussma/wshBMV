package de.wsh.wshbmv

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.StrictMode
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet
import java.sql.Statement
import javax.inject.Inject
import javax.inject.Named

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var app: MyApplication

    @Inject
    @Named("String1")
    lateinit var testString: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Log.d("MainActivity", "Test String from MainAcitivity: $testString")
        Log.d("MainActivity", "Testnachricht ${app.getString(R.string.app_name)}")

        startConnSQL()
    }




    /**
     * die nachfolgende Testfunktion kommuniziert mit MS-SQL und wird später verlagert in ein eigenes Modul...
     * TODO
     */
    private fun startConnSQL() {
        val coroutine = CoroutineScope(Dispatchers.IO)
        Log.d("Conn", "Wir bauen eine Verbindung zum SQL-Server auf...")

        val username = "SA"
        val password = "Coca10Cola"
        val db = "JogiTestDB"
        val server = "192.168.15.11:1433"
        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)
        val conn: Connection?
//            val ConnURL: String? = null
//            var result: ResultSet? = null

        try {
            val driver = "net.sourceforge.jtds.jdbc.Driver"
            Class.forName(driver).newInstance()
            val connString =
                "jdbc:jtds:sqlserver://$server/$db;instance=SQLEXPRESS;encrypt=false;"
            conn = DriverManager.getConnection(connString, username, password)
            val stmt: Statement = conn.createStatement()
            // Versuch zur Manipulation eines Datensatzes in MS-SQL:
             stmt.execute("UPDATE TwshGebBrandschutz SET Zeile = 1 WHERE [ID] = '00000001-0000-0000-0000-000000000000';")

            // wir fragen Daten aus MS-SQL ab...
            val myResult: ResultSet = stmt.executeQuery("SELECT * FROM TwshGebBrandschutz;")
            Log.d("Conn", "Hat Super geklappt!")
            while (myResult.next()) {
                Log.i("Conn", myResult.getString(1))
            }

        } catch (e: Exception) {
            Log.w("Conn", "Mist!!!")
            Log.w("Conn", e.message.toString())
//            } finally {
//                if (Result != null) {
//                    Log.i("wshBMV", result.toString())
//                    result.close()
//                }
//                conn?.close()
//
        }

        Log.d("Conn", "Verbindungsversuch ist abgeschlossen...")

    }

}