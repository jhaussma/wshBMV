package de.wsh.wshbmv.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class TsysUser(
    @PrimaryKey(autoGenerate = false) var id: String = "",
    var vorname: String = "",
    var nachname: String = "",
    var anrede: String = "",
    var benutzerStatus: String = "",
    var email: String = "",
    var telefon: String = "",
    var kurzZeichen: String = "",
    var userKennung: String = "",
    var titel: String = "",
    var dw: String = "",
    var admin: Int = 0,
    var terminW: Int = 0,
    var stammR: Int = 0,
    var stammW: Int = 0,
    var kundenR: Int = 0,
    var kundenW: Int = 0,
    var vorlagenR: Int = 0,
    var vorlagenW: Int = 0,
    var monteur: Int = 0,
    var vertragR: Int = 0,
    var vertragW: Int = 0,
    var rechnungR: Int = 0,
    var rechnungW: Int = 0,
    var bmvR: Int = 0,
    var bmvW: Int = 0,
    var bmvAdmin: Int = 0,
    var passHash: String? = ""
)
