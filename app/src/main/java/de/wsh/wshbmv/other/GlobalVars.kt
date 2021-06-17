package de.wsh.wshbmv.other

import de.wsh.wshbmv.db.entities.TbmvLager
import de.wsh.wshbmv.db.entities.TsysUser


object GlobalVars {
    /**
     *  globale Statusinformationen zur aktuellen Benutzung der App
     */
    var isFirstAppStart = true          // True: es gibt noch keine lokale DB und USER-Einstellungen
    var firstSyncCompleted = false      // True: erste Komplettsynchronisierung war erfolgreich
    var myUser : TsysUser? = null
    var myLager : TbmvLager? = null
    var myLagers = listOf<TbmvLager>()  // enthält alle Lager, die ich aufgrund meiner Berechtigung sehen darf



    /**
     *  SQL-Statusinformationen
     */
    var sqlServerConnected = false      // True: wir haben medi1one-Server-Verbindung
    var sqlStatus : enSqlStatus = enSqlStatus.INIT  // Status der SQL-Serververbindung
    var sqlUserLoaded = false           // True: bei der Komplett-Synchronisierung sind die User-Daten und Lager geladen...
    var sqlUserNewPassHash = false      // True: beim nächsten Abgleich muss das Passwort des SQL-Servers aktualisiert werden...
    var sqlSynchronized = true          // FALSE: eine Synchronisierung mit der SQL-Datenbank wäre mal wieder angebracht


}