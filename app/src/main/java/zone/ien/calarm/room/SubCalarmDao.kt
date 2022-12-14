package zone.ien.calarm.room

import androidx.room.*

@Dao
interface SubCalarmDao {
    @Query("SELECT * FROM SubCalarmDatabase")
    fun getAll(): List<SubCalarmEntity>

    @Query("SELECT * FROM SubCalarmDatabase WHERE parentId = :parentId")
    fun getByParentId(parentId: Long): List<SubCalarmEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun add(data: SubCalarmEntity): Long

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun update(data: SubCalarmEntity)

    @Query("SELECT * FROM SubCalarmDatabase WHERE id = :id")
    fun get(id: Long): SubCalarmEntity

    @Query("DELETE FROM SubCalarmDatabase WHERE id = :id")
    fun delete(id: Long)

    @Query("DELETE FROM SubCalarmDatabase WHERE parentId = :parentId")
    fun deleteParentId(parentId: Long)

    @Query("SELECT EXISTS(SELECT * FROM SubCalarmDatabase WHERE id = :id)")
    fun checkIsAlreadyInDB(id: Long): Boolean
}