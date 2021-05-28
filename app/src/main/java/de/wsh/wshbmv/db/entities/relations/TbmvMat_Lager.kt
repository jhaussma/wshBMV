package de.wsh.wshbmv.db.entities.relations

import androidx.room.Entity

@Entity(primaryKeys = ["matId","lagerId"])
data class TbmvMat_Lager(
    var id : String = "",
    var matId : String = "",
    var lagerId : String = "",
    var default : Int = 0,
    var bestand : Float = 0f
)
