package de.wsh.wshbmv.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

@Entity
data class TbmvBelegPos(
    @PrimaryKey(autoGenerate = false) var id: String = "",
    var belegId: String = "",
    var pos: Int = 0,
    var matGuid: String = "",
    var menge: Float = 0f,
    var vonLagerGuid: String? = null,
    var ackDatum: Date? = null
)
