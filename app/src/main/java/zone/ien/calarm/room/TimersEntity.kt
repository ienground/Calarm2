package zone.ien.calarm.room

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import java.util.Date

@Entity (tableName = "TimersDatabase")
class TimersEntity(
    var label: String,
    var imageUri: String,
    var lastUseTime: Long
) {
    @PrimaryKey(autoGenerate = true) var id: Long? = null
    @Ignore var subTimers: ArrayList<SubTimerEntity> = arrayListOf()

    override fun toString(): String {
        val builder = StringBuilder("[$id] $label ${Date(lastUseTime)} [")

        for (timer in subTimers) {
            builder.append("${timer}, ")
        }
        builder.append("]")
        return builder.toString()
    }
}