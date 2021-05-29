package de.wsh.wshbmv.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

@Entity
data class TbmvDokument(
    @PrimaryKey(autoGenerate = false) var id: String = "",
    var version: String = "",
    var matID: String? = null,
    var serviceID: String? = null,
    var dateiName: String = "",
    var dateiVerzeichnis: String = "",
    var status: String = "",
    var erstellDtm: Date? = null,
    var erstellName: String = "",
    var grobklasse: String = "",
    var stichwort: String = "",
    var extern: Int = 0,
    var inBearbeitung: Int = 0,
    var bearbeiter: String? = null,
    var reservierungTxt: String? = null
)
