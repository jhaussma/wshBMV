package de.wsh.wshbmv.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class TsysUserGruppe(
    @PrimaryKey(autoGenerate = false) var id: String = "",
    var nameGruppe: String = ""
)
