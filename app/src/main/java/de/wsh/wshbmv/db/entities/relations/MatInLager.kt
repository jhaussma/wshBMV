package de.wsh.wshbmv.db.entities.relations

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import de.wsh.wshbmv.db.entities.TbmvLager
import de.wsh.wshbmv.db.entities.TbmvMat

data class MatInLager(
    @Embedded val mat_lager: TbmvMat_Lager,
    @Relation(
        parentColumn = "matId",
        entityColumn = "id"
    )
    val material: List<TbmvMat>
)
