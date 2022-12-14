package zone.ien.calarm.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [SubCalarmEntity::class], version = 1)
abstract class SubCalarmDatabase: RoomDatabase() {
    abstract fun getDao(): SubCalarmDao

    companion object {
        private var instance: SubCalarmDatabase? = null
        fun getInstance(context: Context): SubCalarmDatabase? {
            if (instance == null) {
                synchronized(SubCalarmDatabase::class) {
                    instance = Room.databaseBuilder(context.applicationContext, SubCalarmDatabase::class.java, "SubCalarmDatabase.db").build()
                }
            }

            return instance
        }
    }
}