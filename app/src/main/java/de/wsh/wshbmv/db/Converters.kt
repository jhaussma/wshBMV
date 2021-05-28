package de.wsh.wshbmv.db

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.room.TypeConverter
import de.wsh.wshbmv.other.Constants.TAG
import timber.log.Timber
import java.io.ByteArrayOutputStream

class Converters {

    @TypeConverter
    fun toBitmap(bytes: ByteArray?): Bitmap? {
        Timber.tag(TAG).d("toBitmap Converter wird benutzt...")
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

}