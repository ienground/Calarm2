package zone.ien.calarm.room

import androidx.room.*

@Dao
interface SubAlarmDao {
    @Query("SELECT * FROM SubAlarmDatabase")
    fun getAll(): List<SubAlarmEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun add(data: SubAlarmEntity)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun update(data: SubAlarmEntity)

    @Query("SELECT * FROM SubAlarmDatabase WHERE id = :id")
    fun get(id: Long): SubAlarmEntity

    @Query("SELECT * FROM SubAlarmDatabase WHERE parentId = :parentId")
    fun getByParentId(parentId: Long): List<SubAlarmEntity>

    @Query("DELETE FROM SubAlarmDatabase WHERE id = :id")
    fun delete(id: Long)

    @Query("DELETE FROM SubAlarmDatabase WHERE parentId = :parentId")
    fun deleteParentId(parentId: Long)

    @Query("SELECT EXISTS(SELECT * FROM SubAlarmDatabase WHERE id = :id)")
    fun checkIsAlreadyInDB(id: Long): Boolean
}