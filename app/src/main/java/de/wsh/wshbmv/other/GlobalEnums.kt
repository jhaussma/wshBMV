package de.wsh.wshbmv.other

enum class enSqlStatus {
    INIT, IN_PROCESS, PAUSED, IN_ERROR, PROCESS_ENDED
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
