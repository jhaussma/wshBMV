package de.wsh.wshbmv.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class TbmvLager(
    @PrimaryKey(autoGenerate = false) var id: String = "",
    var scancode: String = "",
    var typ: String = "",
    var matchcode: String = "",
    var userGuid: String = "",
    var beschreibung: String = "",
    var status: String = "",
    var bmLager: Int = 0
)
