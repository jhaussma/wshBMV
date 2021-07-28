package de.wsh.wshbmv.db.entities.relations

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.*

@Entity(indices = [Index(value = ["matId", "serviceId"], unique = true)])
data class TbmvMatService_Historie(
    @PrimaryKey(autoGenerate = false) var id: String = "",
    var matId: String = "",
    var serviceId: String = "",
    var serviceDatum: Date? = null,
    var abschlussDatum: Date? = null,
    var userGuid: String = ""
)
