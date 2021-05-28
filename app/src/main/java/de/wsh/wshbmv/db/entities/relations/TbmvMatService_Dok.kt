package de.wsh.wshbmv.db.entities.relations

import androidx.room.Entity

@Entity(primaryKeys = ["matServiceId","dokId"])
data class TbmvMatService_Dok(
    var id: String = "",
    var matServiceId: String = "",
    var dokId: String = ""
)
