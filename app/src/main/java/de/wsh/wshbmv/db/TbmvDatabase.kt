package de.wsh.wshbmv.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import de.wsh.wshbmv.db.entities.*
import de.wsh.wshbmv.db.entities.relations.*

@Database(
    entities = [
        TsysUser::class,
        TsysUserGruppe::class,
        TsysUserInGruppe::class,
        TbmvBeleg::class,
        TbmvBelegPos::class,
        TbmvDokument::class,
        TbmvMat::class,
        TbmvMatGruppe::class,
        TbmvLager::class,
        TbmvService::class,
        TbmvMat_Lager::class,
        TbmvMat_Service::class,
        TbmvMatService_Dok::class,
        TbmvMatService_Historie::class,
        TbmvService_Dok::class,
        TappChgProtokoll::class,
        TappSyncReport::class,
        TbmvInventur::class,
        TbmvInventurMat::class
    ],
    version = 1
)

@TypeConverters(Converters::class)
abstract class TbmvDatabase : RoomDatabase() {

    abstract fun getTbmvDAO(): TbmvDAO

}