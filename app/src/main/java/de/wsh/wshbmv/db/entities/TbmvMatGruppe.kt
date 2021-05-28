package de.wsh.wshbmv.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class TbmvMatGruppe(
    @PrimaryKey(autoGenerate = false) var id: String = "",
    var matGruppe: String = "",
    var aktiv: Int = 0
)
