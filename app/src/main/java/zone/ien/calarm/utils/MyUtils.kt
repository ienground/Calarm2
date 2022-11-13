package zone.ien.calarm.utils

import android.content.Context
import zone.ien.calarm.R
import java.util.*

class MyUtils {
    companion object {
        fun List<Boolean>.isEveryday(): Boolean {
            var result = true
            for (i in this) { if (!i) { result = false; break } }
            return result
        }

        fun List<Boolean>.isWeekday(): Boolean = !this[0] && this[1] && this[2] && this[3] && this[4] && this[5] && !this[6]

        fun List<Boolean>.isWeekend(): Boolean = this[0] && !this[1] && !this[2] && !this[3] && !this[4] && !this[5] && this[6]

        fun getRepeatlabel(context: Context, data: List<Boolean>, isRepeat: Boolean, time: Int): String {
            val Text = listOf(context.getString(R.string.sun), context.getString(R.string.mon), context.getString(
                R.string.tue), context.getString(R.string.wed), context.getString(R.string.thu), context.getString(
                R.string.fri), context.getString(R.string.sat))
            val now = Calendar.getInstance()

            return when {
                !isRepeat -> {
                    when {
                        time / 60 < now.get(Calendar.HOUR_OF_DAY) -> context.getString(R.string.tomorrow)
                        time / 60 > now.get(Calendar.HOUR_OF_DAY) -> context.getString(R.string.today)
                        time % 60 <= now.get(Calendar.MINUTE) -> context.getString(R.string.tomorrow)
                        else -> context.getString(R.string.today)
                    }
                }

                data.isEveryday() -> context.getString(R.string.everyday)
                data.isWeekday() -> context.getString(R.string.weekday)
                data.isWeekend() -> context.getString(R.string.weekend)
                else -> {
                    val builder = StringBuilder()
                    for (i in data.indices) {
                        if (data[i]) {
                            if (builder.toString() == "") builder.append(Text[i])
                            else builder.append(", ${Text[i]}")
                        }
                    }
                    builder.toString()
                }
            }
        }
    }
}