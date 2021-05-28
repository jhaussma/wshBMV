package de.wsh.wshbmv.db.entities.relations

import androidx.room.Entity

@Entity(primaryKeys = ["serviceID", "dokID"])
data class TbmvService_Dok(
    var id: String = "",
    var serviceID: String = "",
    var dokID: String = ""
)
