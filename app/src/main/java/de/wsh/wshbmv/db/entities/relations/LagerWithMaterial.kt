package de.wsh.wshbmv.db.entities.relations

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import de.wsh.wshbmv.db.entities.TbmvLager
import de.wsh.wshbmv.db.entities.TbmvMat

data class LagerWithMaterial(
    @Embedded val mat_lager: TbmvMat_Lager,
    @Relation(
        parentColumn = "lagerId",
        entityColumn = "id"
    )
    val lager: List<TbmvLager>
)
