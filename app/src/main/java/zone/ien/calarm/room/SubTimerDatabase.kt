package zone.ien.calarm.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [SubTimerEntity::class], version = 1)
abstract class SubTimerDatabase: RoomDatabase() {
    abstract fun getDao(): SubTimerDao

    companion object {
        private var instance: SubTimerDatabase? = null
        fun getInstance(context: Context): SubTimerDatabase? {
            if (instance == null) {
                synchronized(SubTimerDatabase::class) {
                    instance = Room.databaseBuilder(context.applicationContext, SubTimerDatabase::class.java, "SubTimerDatabase.db").build()
                }
            }

            return instance
        }
    }
}