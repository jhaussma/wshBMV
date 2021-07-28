package de.wsh.wshbmv.db.entities.relations

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.*

@Entity(indices = [Index(value = ["matId","serviceId"], unique = true)])
data class TbmvMat_Service(
    @PrimaryKey(autoGenerate = false) var id: String = "",
    var matId: String = "",
    var serviceId: String = "",
    var nextServiceDatum: Date? = null,
    var nextInfoDatum: Date? = null
)
