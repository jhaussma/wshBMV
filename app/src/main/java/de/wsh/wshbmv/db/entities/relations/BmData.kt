package de.wsh.wshbmv.db.entities.relations

import androidx.room.Embedded
import de.wsh.wshbmv.db.entities.TbmvLager
import de.wsh.wshbmv.db.entities.TbmvMat
import de.wsh.wshbmv.db.entities.TbmvMatGruppe
import de.wsh.wshbmv.db.entities.TsysUser
import java.util.*

data class BmData(
    @Embedded var tbmvMat: TbmvMat?,
    @Embedded var tbmvMatGruppe: TbmvMatGruppe?,
    @Embedded var tsysUser: TsysUser?,
    var nextServiceDatum: Date?,
    @Embedded var matLager: TbmvLager?,
    @Embedded var matHautpLager: TbmvLager?
    )
