package de.wsh.wshbmv.db.entities.relations

import androidx.room.Embedded
import androidx.room.Relation
import de.wsh.wshbmv.db.entities.TbmvBeleg
import de.wsh.wshbmv.db.entities.TbmvLager

data class BelegAndZielort(
    @Embedded val tmbvBeleg: TbmvBeleg,
    @Relation(
        parentColumn = "zielLagerGuid",
        entityColumn = "id"
    )
    val tbmvLager: TbmvLager
)
