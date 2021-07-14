package de.wsh.wshbmv.db.entities.relations

import androidx.room.Embedded
import androidx.room.Relation
import de.wsh.wshbmv.db.entities.TbmvBelege
import de.wsh.wshbmv.db.entities.TbmvLager

data class BelegAndZielort(
    @Embedded val tmbvBelege: TbmvBelege,
    @Relation(
        parentColumn = "zielLagerGuid",
        entityColumn = "id"
    )
    val tbmvLager: TbmvLager
)
