package de.wsh.wshbmv.db.entities.relations

import androidx.room.Entity

@Entity(primaryKeys = ["serviceId", "dokId"])
data class TbmvService_Dok(
    var id: String = "",
    var serviceId: String = "",
    var dokId: String = ""
)
