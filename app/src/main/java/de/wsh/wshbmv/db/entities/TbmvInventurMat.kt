package de.wsh.wshbmv.db.entities

import android.graphics.Bitmap
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class TbmvInventurMat(
    @PrimaryKey(autoGenerate = false) var id: String = "",
    var invId: String = "",
    var matId: String = "",
    var status: String = "",
    var scancode: String = "",
    var seriennummer: String = "",
    var matchcode: String = "",
    var hersteller: String = "",
    var modell: String = "",
    var bildBmp: Bitmap? = null
    )
