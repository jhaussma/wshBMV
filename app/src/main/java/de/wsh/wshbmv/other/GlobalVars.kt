package de.wsh.wshbmv.other

import dagger.hilt.android.AndroidEntryPoint
import de.wsh.wshbmv.db.entities.TbmvLager
import de.wsh.wshbmv.db.entities.TsysUser
import javax.inject.Inject
import javax.inject.Named


object GlobalVars {
    /**
     *  globale Statusinformationen zur aktuellen Benutzung der App
     */
    var isFirstAppStart = true          // True: es gibt noch keine lokale DB und USER-Einstellungen
    var LagerID = ""                    // leer, wenn aktuell kein Lagerort eingestellt wurde -> Grundvoraussetzung
    var LagerMatchcode = ""
    var firstSyncCompleted = false      // True: erste Komplettsynchronisierung war erfolgreich

    /**
     *  SQL-Statusinformationen
     */
    var sqlServerConnected = false      // True: wir haben medi1one-Server-Verbindung
    var sqlStatus : enSqlStatus = enSqlStatus.INIT  // Status der SQL-Serververbindung
    var sqlUserLoaded = false           // True: bei der Komplett-Synchronisierung sind die User-Daten und Lager geladen...

    var myUser : TsysUser? = null
    var myLager : TbmvLager? = null

}