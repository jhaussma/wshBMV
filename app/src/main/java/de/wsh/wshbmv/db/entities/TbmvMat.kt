package de.wsh.wshbmv.db.entities

import android.graphics.Bitmap
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class TbmvMat(
    @PrimaryKey(autoGenerate = false) var id: String = "",
    var scancode: String = "",
    var typ: String = "",
    var matchcode: String = "",
    var matGruppeGuid: String = "",
    var beschreibung: String = "",
    var hersteller: String = "",
    var modell: String = "",
    var seriennummer: String = "",
    var userGuid: String = "",
    var matStatus: String = "",
    var bildBmp: Bitmap? = null
)
