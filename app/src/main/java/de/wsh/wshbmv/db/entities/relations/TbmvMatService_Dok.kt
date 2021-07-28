package de.wsh.wshbmv.db.entities.relations

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(indices = [Index(value = ["matServiceId", "dokId"], unique = true)])
data class TbmvMatService_Dok(
    @PrimaryKey(autoGenerate = false) var id: String = "",
    var matServiceId: String = "",
    var dokId: String = ""
)
