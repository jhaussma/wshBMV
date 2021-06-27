package de.wsh.wshbmv.db.entities.relations

import androidx.room.Embedded
import androidx.room.Relation
import de.wsh.wshbmv.db.entities.TbmvBelegPos
import de.wsh.wshbmv.db.entities.TbmvLager
import de.wsh.wshbmv.db.entities.TbmvMat

data class BelegposAndMaterialAndLager(
    @Embedded val tbmvBelegPos: TbmvBelegPos,
    @Relation(
        parentColumn = "matGuid",
        entityColumn = "id"
    )
    val tbmvMat: TbmvMat?,
    @Relation(
        parentColumn = "vonLagerGuid",
        entityColumn = "id"
    )
    val tbmvLager: TbmvLager?
)
