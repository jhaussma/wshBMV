package de.wsh.wshbmv.db.entities.relations

import androidx.room.Entity

@Entity(primaryKeys = ["matId","serviceId"])
data class TbmvMat_Service(
    var id: String = "",
    var matId: String = "",
    var serviceId: String = "",
    var nextServiceTime: Long = 0L,
    var nextInfoTime: Long = 0L
)
