package de.wsh.wshbmv.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class TappSyncReport(
    var timeStamp: Long = 0L,
    var lastFromServerTime: Long = 0L,
    var lastToServerTime: Long = 0L,
    var errorFlag: Int = 0
) {
    @PrimaryKey(autoGenerate = true)
    var id: Long? = null
}
