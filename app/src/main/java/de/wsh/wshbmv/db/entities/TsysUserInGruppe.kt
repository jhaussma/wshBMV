package de.wsh.wshbmv.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class TsysUserInGruppe(
    @PrimaryKey(autoGenerate = false) var id: String = "",
    var gruppeId: String = "",
    var userId: String = ""
)
