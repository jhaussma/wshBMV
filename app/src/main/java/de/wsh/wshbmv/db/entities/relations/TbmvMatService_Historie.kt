package de.wsh.wshbmv.db.entities.relations

import androidx.room.Entity

@Entity(primaryKeys = ["matId","serviceId"])
data class TbmvMatService_Historie(
    var id: String = "",
    var matId: String = "",
    var serviceId: String = "",
    var serviceDatum: Long = 0L,
    var abschlussDatum: Long? = null,
    var userGuid: String = ""
)
