package de.wsh.wshbmv.db.entities.relations

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(indices = [Index(value = ["serviceId", "dokId"], unique = true)])
data class TbmvService_Dok(
    @PrimaryKey(autoGenerate = false) var id: String = "",
    var serviceId: String = "",
    var dokId: String = ""
)
