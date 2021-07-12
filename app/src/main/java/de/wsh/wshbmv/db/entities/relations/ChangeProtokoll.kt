package de.wsh.wshbmv.db.entities.relations

import java.util.*

data class ChangeProtokoll(
    val datenbank: String? = "",
    val satzId: String? = "",
    val maxZeitstempel: Date? = null,
    val addDS: Int = -1,
    val editDS: Int = -1,
    val delDS: Int = -1
)