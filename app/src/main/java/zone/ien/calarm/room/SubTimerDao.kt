package zone.ien.calarm.room

import androidx.room.*

@Dao
interface SubTimerDao {
    @Query("SELECT * FROM SubTimerDatabase")
    fun getAll(): List<SubTimerEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun add(data: SubTimerEntity)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun update(data: SubTimerEntity)

    @Query("SELECT * FROM SubTimerDatabase WHERE id = :id")
    fun get(id: Long): SubTimerEntity

    @Query("SELECT * FROM SubTimerDatabase WHERE parentId = :parentId")
    fun getByParentId(parentId: Long): List<SubTimerEntity>

    @Query("DELETE FROM SubTimerDatabase WHERE id = :id")
    fun delete(id: Long)

    @Query("DELETE FROM SubTimerDatabase WHERE parentId = :parentId")
    fun deleteParentId(parentId: Long)

    @Query("SELECT EXISTS(SELECT * FROM SubTimerDatabase WHERE id = :id)")
    fun checkIsAlreadyInDB(id: Long): Boolean
}