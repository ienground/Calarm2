package zone.ien.calarm.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [TimersEntity::class], version = 1)
abstract class TimersDatabase: RoomDatabase() {
    abstract fun getDao(): TimersDao

    companion object {
        private var instance: TimersDatabase? = null
        fun getInstance(context: Context): TimersDatabase? {
            if (instance == null) {
                synchronized(TimersDatabase::class) {
                    instance = Room.databaseBuilder(context.applicationContext, TimersDatabase::class.java, "TimersDatabase.db").build()
                }
            }

            return instance
        }
    }
}