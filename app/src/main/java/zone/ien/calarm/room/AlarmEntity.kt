package zone.ien.calarm.room

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey

@Entity (tableName = "AlarmDatabase")
class AlarmEntity(
    var label: String,
    var time: Int,
    var isEnabled: Boolean,
    var repeat: Int,
    var imageUri: String,
    var sound: String,
    var vibrate: Boolean
) {
    @PrimaryKey(autoGenerate = true) var id: Long? = null
    @Ignore var subAlarms: ArrayList<SubAlarmEntity> = arrayListOf()

    override fun toString(): String {
        val builder = StringBuilder("[$id] $label - ${time / 60}:${time % 60} : isEnabled $isEnabled ")
        builder.append("${repeat.toString(2)} [")

        for (alarm in subAlarms) {
            builder.append("${alarm}, ")
        }
        builder.append("]")
        return builder.toString()
    }
}