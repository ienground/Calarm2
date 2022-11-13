package zone.ien.calarm.room

import androidx.room.*

@Dao
interface AlarmDao {
    @Query("SELECT * FROM AlarmDatabase")
    fun getAll(): List<AlarmEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun add(data: AlarmEntity)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun update(data: AlarmEntity)

    @Query("SELECT * FROM AlarmDatabase WHERE id = :id")
    fun get(id: Int): AlarmEntity

    @Query("DELETE FROM AlarmDatabase WHERE id = :id")
    fun delete(id: Long)

    @Query("SELECT EXISTS(SELECT * FROM AlarmDatabase WHERE id = :id)")
    fun checkIsAlreadyInDB(id: Long): Boolean
}