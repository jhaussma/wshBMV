package de.wsh.wshbmv.db.entities.relations

import androidx.room.Embedded
import de.wsh.wshbmv.db.entities.TbmvBeleg
import de.wsh.wshbmv.db.entities.TbmvLager
import de.wsh.wshbmv.db.entities.TsysUser

data class BelegData(
    @Embedded var tbmvBeleg: TbmvBeleg? = null,
    @Embedded var belegUser: TsysUser? = null,
    @Embedded var zielLager: TbmvLager? = null,
    @Embedded var zielUser: TsysUser? = null
)
