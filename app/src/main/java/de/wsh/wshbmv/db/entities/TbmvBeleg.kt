package de.wsh.wshbmv.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

@Entity
data class TbmvBeleg(
    @PrimaryKey(autoGenerate = false) var id: String = "",
    var belegTyp: String = "",
    var belegDatum: Date? = null,
    var belegUserGuid: String = "",
    var zielLagerGuid: String = "",
    var zielUserGuid: String = "",
    var belegStatus: String = "",
    var toAck: Int = 0,
    var notiz: String = ""
)
