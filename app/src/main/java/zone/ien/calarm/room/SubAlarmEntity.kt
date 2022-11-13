package zone.ien.calarm.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity (tableName = "SubAlarmDatabase")
class SubAlarmEntity(
    var parentId: Long,
    var time: Int,
    var isEnabled: Boolean,
) {
    @PrimaryKey(autoGenerate = true) var id: Long? = null

    override fun toString(): String = "[$id] from $parentId - ${time / 60}:${time % 60} / isEnabled $isEnabled"
}