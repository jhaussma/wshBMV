package de.wsh.wshbmv.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class TbmvInventur(
    @PrimaryKey(autoGenerate = false) var id: String = "",
    var lagerId: String = "",
    var status: Int = 0,
    var startDatum: Long = System.currentTimeMillis(),
    var endDatum: Long? = null,
    var userGuid: String = ""
)
