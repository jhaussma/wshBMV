package de.wsh.wshbmv.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import de.wsh.wshbmv.db.entities.TbmvMat
import de.wsh.wshbmv.db.entities.TsysUser

@Database(
    entities = [
        TbmvMat::class,
        TsysUser::class
    ],
    version = 1
)

@TypeConverters(Converters::class)
abstract class TbmvDatabase : RoomDatabase() {

    abstract fun getTbmvDAO(): TbmvDAO

}