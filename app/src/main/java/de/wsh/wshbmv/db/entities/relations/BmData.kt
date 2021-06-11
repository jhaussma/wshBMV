package de.wsh.wshbmv.db.entities.relations

import androidx.room.Embedded
import de.wsh.wshbmv.db.entities.TbmvLager
import de.wsh.wshbmv.db.entities.TbmvMat
import de.wsh.wshbmv.db.entities.TbmvMatGruppe
import de.wsh.wshbmv.db.entities.TsysUser
import java.util.*

data class BmData(
    @Embedded var tbmvMat: TbmvMat? = null,
    @Embedded var tbmvMatGruppe: TbmvMatGruppe? = null,
    @Embedded var tsysUser: TsysUser? = null,
    var nextServiceDatum: Date? = null,
    @Embedded var matLager: TbmvLager? = null,
    @Embedded var matHautpLager: TbmvLager? = null
    )
