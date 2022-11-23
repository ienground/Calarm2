package zone.ien.calarm.room

import androidx.room.*

@Dao
interface TimersDao {
    @Query("SELECT * FROM TimersDatabase")
    fun getAll(): List<TimersEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun add(data: TimersEntity): Long

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun update(data: TimersEntity)

    @Query("SELECT * FROM TimersDatabase WHERE id = :id")
    fun get(id: Long): TimersEntity

    @Query("DELETE FROM TimersDatabase WHERE id = :id")
    fun delete(id: Long)

    @Query("SELECT EXISTS(SELECT * FROM TimersDatabase WHERE id = :id)")
    fun checkIsAlreadyInDB(id: Long): Boolean
}