package de.wsh.wshbmv.db

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.room.TypeConverter
import de.wsh.wshbmv.other.Constants.TAG
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.util.*

class Converters {

    @TypeConverter
    fun toBitmap(bytes: ByteArray?): Bitmap? {
        if (bytes != null) {
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } else {
            return null
        }
    }


    @TypeConverter
    fun fromBitmap(bmp: Bitmap?): ByteArray? {
        if (bmp != null) {
            val outputStream = ByteArrayOutputStream()
            bmp.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            return  outputStream.toByteArray()
        } else {
            return null
        }
    }

    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time?.toLong()
    }


}