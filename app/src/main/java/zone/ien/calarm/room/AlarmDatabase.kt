package zone.ien.calarm.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [AlarmEntity::class], version = 1)
abstract class AlarmDatabase: RoomDatabase() {
    abstract fun getDao(): AlarmDao

    companion object {
        private var instance: AlarmDatabase? = null
        fun getInstance(context: Context): AlarmDatabase? {
            if (instance == null) {
                synchronized(AlarmDatabase::class) {
                    instance = Room.databaseBuilder(context.applicationContext, AlarmDatabase::class.java, "AlarmDatabase.db").build()
                }
            }

            return instance
        }
    }
}