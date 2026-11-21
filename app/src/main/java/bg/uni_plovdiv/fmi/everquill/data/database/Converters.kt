package bg.uni_plovdiv.fmi.everquill.data.database

import androidx.room.TypeConverter
import bg.uni_plovdiv.fmi.everquill.data.entity.BookStatus

class Converters {
    @TypeConverter
    fun fromBookStatus(status: BookStatus): String = status.name

    @TypeConverter
    fun toBookStatus(name: String): BookStatus = BookStatus.valueOf(name)
}
