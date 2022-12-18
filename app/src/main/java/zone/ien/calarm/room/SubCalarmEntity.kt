package zone.ien.calarm.room

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey

@Entity (tableName = "SubCalarmDatabase")
class SubCalarmEntity(
    var parentId: Long,
    var time: Int,
    var isEnabled: Boolean,
    var timeMoving: Int = 0
) {
    @PrimaryKey(autoGenerate = true) var id: Long? = null

    override fun toString(): String = "$id / $parentId / ${time / 60}:${time % 60} / $isEnabled ${timeMoving}"
}