package de.wsh.wshbmv.db.entities.relations

import androidx.room.Entity
import java.util.*

@Entity(primaryKeys = ["matId","serviceId"])
data class TbmvMat_Service(
    var id: String = "",
    var matId: String = "",
    var serviceId: String = "",
    var nextServiceDatum: Date? = null,
    var nextInfoDatum: Date? = null
)
