package zone.ien.calarm.room

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity (tableName = "SubTimerDatabase")
class SubTimerEntity(
    var parentId: Long,
    var label: String,
    var time: Int,
    var order: Int,
    var imageUri: String,
) {
    @PrimaryKey(autoGenerate = true) var id: Long? = null

    override fun toString(): String = "[$id] from $parentId - $label - ${time / 60}:${time % 60} / order $order"
}