package de.wsh.wshbmv.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import de.wsh.wshbmv.db.entities.*

@Database(
    entities = [
        TbmvMat::class,
        TsysUser::class,
        TsysUserGruppe::class,
        TsysUserInGruppe::class,
        TbmvLager::class
    ],
    version = 1
)

@TypeConverters(Converters::class)
abstract class TbmvDatabase : RoomDatabase() {

    abstract fun getTbmvDAO(): TbmvDAO

}