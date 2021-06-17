package de.wsh.wshbmv.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class TappChgProtokoll(
    var timeStamp: Long = 0L,
    var datenbank: String = "",
    var satzID: String = "",
    var Aktion: Int = -1
) {
    @PrimaryKey(autoGenerate = true)
    var id: Long? = null
}
