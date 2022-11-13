package zone.ien.calarm.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [SubAlarmEntity::class], version = 1)
abstract class SubAlarmDatabase: RoomDatabase() {
    abstract fun getDao(): SubAlarmDao

    companion object {
        private var instance: SubAlarmDatabase? = null
        fun getInstance(context: Context): SubAlarmDatabase? {
            if (instance == null) {
                synchronized(SubAlarmDatabase::class) {
                    instance = Room.databaseBuilder(context.applicationContext, SubAlarmDatabase::class.java, "SubAlarmDatabase.db").build()
                }
            }

            return instance
        }
    }
}