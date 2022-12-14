package zone.ien.calarm.room

import androidx.room.*

@Dao
interface CalarmDao {
    @Query("SELECT * FROM CalarmDatabase")
    fun getAll(): List<CalarmEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun add(data: CalarmEntity): Long

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun update(data: CalarmEntity)

    @Query("UPDATE CalarmDatabase SET id = :id, calendarId = :calendarId, isEnabled = :isEnabled, address = :address, latitude = :latitude, longitude = :longitude, imageUri = :imageUri, sound = :sound, vibrate = :vibrate WHERE dataId = :dataId")
    fun updateByDataId(id: Long, dataId: Long, calendarId: Long, isEnabled: Boolean, address: String, latitude: Float, longitude: Float, imageUri: String, sound: String, vibrate: Boolean)

    @Transaction
    fun updateByDataId(data: CalarmEntity) {
        updateByDataId(data.id ?: -1, data.dataId, data.calendarId, data.isEnabled, data.address, data.latitude, data.longitude, data.imageUri, data.sound, data.vibrate)
    }

    @Query("SELECT * FROM CalarmDatabase WHERE id = :id")
    fun get(id: Long): CalarmEntity

    @Query("SELECT * FROM CalarmDatabase WHERE dataId = :dataId")
    fun getByDataId(dataId: Long): CalarmEntity

    @Query("DELETE FROM CalarmDatabase WHERE id = :id")
    fun delete(id: Long)

    @Query("SELECT EXISTS(SELECT * FROM CalarmDatabase WHERE id = :id)")
    fun checkIsAlreadyInDB(id: Long): Boolean
}