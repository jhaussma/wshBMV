package de.wsh.wshbmv.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class TbmvServices(
    @PrimaryKey(autoGenerate = false) var id: String = "",
    var name: String = "",
    var beschreibung: String = "",
    var intervalNum: Int = 0,
    var intervalUnit: String = "",
    var doInfo: Int = 0,
    var infoNum: Int? = null,
    var infoUnit: String = ""
)
