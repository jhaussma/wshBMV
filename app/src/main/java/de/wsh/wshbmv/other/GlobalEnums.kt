package de.wsh.wshbmv.other

enum class enSqlStatus {
    INIT, IN_PROCESS, DISCONNECTED, IN_ERROR, PROCESS_ENDED, PROCESS_ABORTED, NO_CONTACT
}

enum class SortType {
    MATCHCODE, SCANCODE, SERIENNUMMER, HERSTELLER, MODELL, STATUS
}

enum class TransDir {
    ANMICH, VONMIR
}

enum class BelegFilterStatus {
    OFFEN,INARBEIT,ERLEDIGT,ALLE
}

enum class BelegStatus {
    UNDEFINED,USEREDIT,USERDELETE,ZIELACK,READONLY
}
