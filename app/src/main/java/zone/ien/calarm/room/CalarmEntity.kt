package zone.ien.calarm.room

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey

@Entity (tableName = "CalarmDatabase")
class CalarmEntity(
    var dataId: Long,
    var calendarId: Long,
    var isEnabled: Boolean,
    var address: String,
    var latitude: Float,
    var longitude: Float,
    var imageUri: String,
    var sound: String,
    var vibrate: Boolean
) {
    @PrimaryKey(autoGenerate = true) var id: Long? = null

    @Ignore var subCalarms: ArrayList<SubCalarmEntity> = arrayListOf()
    @Ignore var time: Long = 0L
    @Ignore var label: String = ""


//    override fun toString(): String {
//        val builder = StringBuilder("[$id] $label - ${time / 60}:${time % 60} : isEnabled $isEnabled ")
//        builder.append("${repeat.toString(2)} [")
//
//        for (calarm in subCalarms) {
//            builder.append("${calarm}, ")
//        }
//        builder.append("]")
//        return builder.toString()
//    }
}