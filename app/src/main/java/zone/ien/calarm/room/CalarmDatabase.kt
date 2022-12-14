package zone.ien.calarm.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [CalarmEntity::class], version = 1)
abstract class CalarmDatabase: RoomDatabase() {
    abstract fun getDao(): CalarmDao

    companion object {
        private var instance: CalarmDatabase? = null
        fun getInstance(context: Context): CalarmDatabase? {
            if (instance == null) {
                synchronized(CalarmDatabase::class) {
                    instance = Room.databaseBuilder(context.applicationContext, CalarmDatabase::class.java, "CalarmDatabase.db").build()
                }
            }

            return instance
        }
    }
}