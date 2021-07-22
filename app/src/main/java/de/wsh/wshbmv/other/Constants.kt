package de.wsh.wshbmv.other

object Constants {
    const val TAG = "wshBMV"

    const val TBMV_DATABASE_NAME = "tbmv_db"

    /** xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
     *  lokale Speicherung der APP-Userdaten
     */
    const val SHARED_PREFERENCES_NAME = "sharedPrefUser"
    const val KEY_USER_NAME = "KEY_USER_NAME"
    const val KEY_USER_HASH = "KEY_USER_HASH"
    const val KEY_LAGER_ID = "KEY_LAGER_ID"
    const val KEY_LAGER_NAME = "KEY_LAGER_NAME"
    const val KEY_FIRST_TIME = "KEY_FIRST_TIME"
    const val KEY_FIRST_SYNC_DONE = "KEY_FIRST_SYNC_DONE"

    /** xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
     *   SQL-Server-Daten
     */
    const val SQL_CONN_IP = "192.168.15.11:1433"
    const val SQL_CONN_DB = "JogiTestDB"
    const val SQL_USER_NAME = "SA"
    const val SQL_USER_PWD = "Coca10Cola"
//    const val SQL_CONN_IP = "192.168.101.22:1433"
//    const val SQL_CONN_DB = "wshAPlan"
//    const val SQL_USER_NAME = "SA"
//    const val SQL_USER_PWD = "Sy67Ha99"

    const val SQL_SYNC_TABLES = "'TbmvBelege','TbmvBelegPos','TbmvDokumente','TbmvLager','TbmvMat','TbmvMat_Lager','TbmvMat_Service','TbmvMatGruppen','TbmvMatService_Dok','TbmvMatService_Historie','TbmvService_Dok','TbmvServices','TsysUser','TsysUserToGruppe','TsysUserGruppe'"

    /** xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
     *   Datenbank - Konstanten - Parameter
     */
    const val DB_AKTION_ADD_DS = 0
    const val DB_AKTION_UPDATE_DS = 1
    const val DB_AKTION_DELETE_DS = 2

    /** xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
     *   Bild-Scale-Einstellungsparameter
     */
    const val PIC_SCALE_HEIGHT = 500
    const val PIC_SCALE_FILTERING = true

    /** xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
     *   Sync-Timer-Aktivitäten
     *    (alle Zeitangaben in Millisekunden!
     */
    const val SYNCTIMER_INTERVALL = 1000L
    const val SYNCTIMER_LANG_ZYKLUS = 300000L


}