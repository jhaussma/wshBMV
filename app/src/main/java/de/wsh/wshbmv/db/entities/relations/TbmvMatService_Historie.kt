package de.wsh.wshbmv.db.entities.relations

import androidx.room.Entity
import java.util.*

@Entity(primaryKeys = ["matId","serviceId"])
data class TbmvMatService_Historie(
    var id: String = "",
    var matId: String = "",
    var serviceId: String = "",
    var serviceDatum: Date? = null,
    var abschlussDatum: Date? = null,
    var userGuid: String = ""
)
