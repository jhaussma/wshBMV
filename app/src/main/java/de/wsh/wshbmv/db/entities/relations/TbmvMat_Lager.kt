package de.wsh.wshbmv.db.entities.relations

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(indices = [Index(value = ["matId", "lagerId"], unique = true)])
data class TbmvMat_Lager(
    @PrimaryKey(autoGenerate = false) var id: String = "",
    var matId: String = "",
    var lagerId: String = "",
    var isDefault: Int = 0,
    var bestand: Float = 0f
)
