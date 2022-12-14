package zone.ien.calarm.utils

import android.app.Activity
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.media.RingtoneManager
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.AbsoluteSizeSpan
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.util.TypedValue
import androidx.core.content.ContextCompat
import zone.ien.calarm.R
import zone.ien.calarm.activity.TAG
import zone.ien.calarm.constant.ActionKey
import zone.ien.calarm.constant.IntentKey
import zone.ien.calarm.receiver.AlarmReceiver
import zone.ien.calarm.receiver.CalarmReceiver
import zone.ien.calarm.room.AlarmEntity
import zone.ien.calarm.room.CalarmEntity
import java.util.*
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.roundToLong

class MyUtils {
    companion object {
        fun getRepeatlabel(context: Context, data: Int, time: Int): String {
            val Text = listOf(context.getString(R.string.sun), context.getString(R.string.mon), context.getString(
                R.string.tue), context.getString(R.string.wed), context.getString(R.string.thu), context.getString(
                R.string.fri), context.getString(R.string.sat))
            val now = Calendar.getInstance()

            return when (data) {
                0 -> {
                    when {
                        time / 60 < now.get(Calendar.HOUR_OF_DAY) -> context.getString(R.string.tomorrow)
                        time / 60 > now.get(Calendar.HOUR_OF_DAY) -> context.getString(R.string.today)
                        time % 60 <= now.get(Calendar.MINUTE) -> context.getString(R.string.tomorrow)
                        else -> context.getString(R.string.today)
                    }
                }

                127 -> context.getString(R.string.everyday)
                62 -> context.getString(R.string.weekday)
                65 -> context.getString(R.string.weekend)
                else -> {
                    val builder = StringBuilder()
                    for (i in 0 until 7) {
                        if (data.and(2.0.pow(6 - i).toInt()) != 0) {
                            if (builder.toString() == "") builder.append(Text[i])
                            else builder.append(", ${Text[i]}")
                        }
                    }
                    builder.toString()
                }
            }
        }

        fun getAlarmRingtones(activity: Activity): Map<String, String> {
            val manager = RingtoneManager(activity).apply { setType(RingtoneManager.TYPE_ALARM) }
            val cursor: Cursor = manager.cursor
            val list: MutableMap<String, String> = mutableMapOf()
            while (cursor.moveToNext()) {
                val title: String = cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX)
                val uri = manager.getRingtoneUri(cursor.position).toString()
                list[title] = uri
            }
            list.toSortedMap()
            return list
        }

        fun Calendar.timeZero(): Calendar {
            val calendar = this.clone() as Calendar
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)

            return calendar
        }

        fun getNextRepeatDay(calendar: Calendar, time: Int, data: Int): Int {
            val repeatDay = arrayListOf<Boolean>()
            val today = calendar.get(Calendar.DAY_OF_WEEK) - 1 // 금요일 5
            val todayTime = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
            var nextRepeatDay = -2
            val range = if (todayTime < time) 0..6 else 1..7

            for (i in 0 until 7) {
                repeatDay.add(data.and(2.0.pow(6 - i).toInt()) != 0)
            }

            for (i in range) {
                val result = repeatDay[(today + i) % 7]
                if (result) {
                    nextRepeatDay = (today + i) % 7
                    break
                }
            }

            return nextRepeatDay + 1
        }

        fun setAlarmClock(context: Context, am: AlarmManager, alarm : AlarmEntity): Calendar {
            val now = Calendar.getInstance()
            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, alarm.time / 60)
                set(Calendar.MINUTE, alarm.time % 60)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)

                if (alarm.repeat != 0) {
                    val nextRepeatDay = getNextRepeatDay(Calendar.getInstance(), alarm.time, alarm.repeat)
                    val add = (nextRepeatDay - get(Calendar.DAY_OF_WEEK) + 6) % 7 + 1
                    if (!(add == 7 && now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE) < alarm.time)) {
                        add(Calendar.DAY_OF_WEEK, add)
                    }
                } else {
                    if (now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE) >= alarm.time) { // 현재 시간보다 과거. 내일 알람 설정 가능
                        add(Calendar.DAY_OF_MONTH, 1)
                    }
                }
            }
            val alarmIntent = Intent(context, AlarmReceiver::class.java).apply {
                action = ActionKey.TIMER_ALARM
                putExtra(IntentKey.ITEM_ID, alarm.id)
            }
            val pendingIntent = PendingIntent.getBroadcast(context, alarm.id?.toInt() ?: -1, alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            am.setAlarmClock(AlarmManager.AlarmClockInfo(calendar.timeInMillis, pendingIntent), pendingIntent)
            for (subAlarm in alarm.subAlarms) {
                if (subAlarm.isEnabled) {
                    val subCalendar = calendar.clone() as Calendar
                    subCalendar.add(Calendar.MINUTE, -subAlarm.time)
                    val subPendingIntent = PendingIntent.getBroadcast(context, 100000 + (subAlarm.id?.toInt() ?: -1), alarmIntent.apply { putExtra(IntentKey.SUBALARM_ID, subAlarm.id) }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                    if (subCalendar.timeInMillis < System.currentTimeMillis()) continue
                    Log.d(TAG, "sub: ${subCalendar.time}")
                    am.setAlarmClock(AlarmManager.AlarmClockInfo(subCalendar.timeInMillis, subPendingIntent), subPendingIntent)
                }
            }

            return calendar
        }

        fun deleteAlarmClock(context: Context, am: AlarmManager, alarm : AlarmEntity) {
            val alarmIntent = Intent(context, AlarmReceiver::class.java).apply {
                action = ActionKey.TIMER_ALARM
                putExtra(IntentKey.ITEM_ID, alarm.id)
            }
            val pendingIntent = PendingIntent.getBroadcast(context, alarm.id?.toInt() ?: -1, alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            am.cancel(pendingIntent)

            for (subAlarm in alarm.subAlarms) {
                val subPendingIntent = PendingIntent.getBroadcast(context, 100000 + (subAlarm.id?.toInt() ?: -1), alarmIntent.apply { putExtra(IntentKey.SUBALARM_ID, subAlarm.id) }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                am.cancel(subPendingIntent)
            }
        }

        fun setCalarmClock(context: Context, am: AlarmManager, calarm : CalarmEntity): Calendar {
            val now = Calendar.getInstance()
            val calendar = Calendar.getInstance().apply { timeInMillis = calarm.time }
            val calarmIntent = Intent(context, CalarmReceiver::class.java).apply {
                action = ActionKey.TIMER_ALARM
                putExtra(IntentKey.ITEM_ID, calarm.id)
            }
            val pendingIntent = PendingIntent.getBroadcast(context, calarm.id?.toInt() ?: -1, calarmIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            am.setAlarmClock(AlarmManager.AlarmClockInfo(calendar.timeInMillis, pendingIntent), pendingIntent)
            for (subCalarm in calarm.subCalarms) {
                if (subCalarm.isEnabled) {
                    val subCalendar = calendar.clone() as Calendar
                    subCalendar.add(Calendar.MINUTE, -subCalarm.time)
                    val subPendingIntent = PendingIntent.getBroadcast(context, 200000 + (subCalarm.id?.toInt() ?: -1), calarmIntent.apply { putExtra(IntentKey.SUBALARM_ID, subCalarm.id) }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                    if (subCalendar.timeInMillis < System.currentTimeMillis()) continue
                    am.setAlarmClock(AlarmManager.AlarmClockInfo(subCalendar.timeInMillis, subPendingIntent), subPendingIntent)
                }
            }

            return calendar
        }

        fun deleteCalarmClock(context: Context, am: AlarmManager, calarm : CalarmEntity) {
            val calarmIntent = Intent(context, CalarmReceiver::class.java).apply {
                action = ActionKey.TIMER_ALARM
                putExtra(IntentKey.ITEM_ID, calarm.id)
            }
            val pendingIntent = PendingIntent.getBroadcast(context, calarm.id?.toInt() ?: -1, calarmIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            am.cancel(pendingIntent)

            for (subCalarm in calarm.subCalarms) {
                val subPendingIntent = PendingIntent.getBroadcast(context, 200000 + (subCalarm.id?.toInt() ?: -1), calarmIntent.apply { putExtra(IntentKey.SUBALARM_ID, subCalarm.id) }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                am.cancel(subPendingIntent)
            }
        }

        fun timeDiffToString(context: Context, t1: Calendar, t2: Calendar): String {
            val difference = t2.timeInMillis - t1.timeInMillis
            val day = difference / AlarmManager.INTERVAL_DAY
            val hour = (difference % AlarmManager.INTERVAL_DAY) / AlarmManager.INTERVAL_HOUR
            val minute = (difference % AlarmManager.INTERVAL_HOUR) / (60 * 1000L)

            val timeBuilder = StringBuilder()
            if (day != 0L) timeBuilder.append("${context.getString(R.string.day_format, day)} ")
            if (hour != 0L) timeBuilder.append("${context.getString(R.string.hour_format, hour)} ")
            if (minute != 0L) timeBuilder.append("${context.getString(R.string.minute_format, minute)} ")

            return if (difference >= 60 * 1000L) context.getString(R.string.alarm_toast_format, timeBuilder.toString()) else context.getString(R.string.alarm_toast_min)
        }

        fun timeToText(context: Context, data: ArrayList<Int>, color: Int, textSize1: Int = 42, textSize2: Int = 24): SpannableStringBuilder {
            val nums: MutableList<Int> = mutableListOf()
            val dataSize = data.size
            for (i in 0 until 6 - data.size) nums.add(0)
            nums.addAll(data)

            val span01 = SpannableStringBuilder("${nums[0]}${nums[1]}").apply {
                if (dataSize >= 5) setSpan(ForegroundColorSpan(ContextCompat.getColor(context, color)), 0, length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                setSpan(AbsoluteSizeSpan(textSize1, true), 0, length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }

            val span02 = SpannableStringBuilder(context.getString(R.string.hour) + "  ").apply {
                if (dataSize >= 5) setSpan(ForegroundColorSpan(ContextCompat.getColor(context, color)), 0, length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                setSpan(AbsoluteSizeSpan(textSize2, true), 0, length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }

            val span03 = SpannableStringBuilder("${nums[2]}${nums[3]}").apply {
                if (dataSize >= 3) setSpan(ForegroundColorSpan(ContextCompat.getColor(context, color)), 0, length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                setSpan(AbsoluteSizeSpan(textSize1, true), 0, length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }

            val span04 = SpannableStringBuilder(context.getString(R.string.minute) + "  ").apply {
                if (dataSize >= 3) setSpan(ForegroundColorSpan(ContextCompat.getColor(context, color)), 0, length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                setSpan(AbsoluteSizeSpan(textSize2, true), 0, length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }

            val span05 = SpannableStringBuilder("${nums[4]}${nums[5]}").apply {
                if (dataSize >= 1) setSpan(ForegroundColorSpan(ContextCompat.getColor(context, color)), 0, length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                setSpan(AbsoluteSizeSpan(textSize1, true), 0, length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }

            val span06 = SpannableStringBuilder(context.getString(R.string.second)).apply {
                if (dataSize >= 1) setSpan(ForegroundColorSpan(ContextCompat.getColor(context, color)), 0, length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                setSpan(AbsoluteSizeSpan(textSize2, true), 0, length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }

            return span01.append(span02).append(span03).append(span04).append(span05).append(span06)
        }

        fun dpToPx(context: Context, size: Float): Int = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, size, context.resources.displayMetrics).toInt()

        fun Cursor.getSafeLong(columnIndex: Int, defaultValue: Long) = if (isNull(columnIndex)) { defaultValue } else { getLong(columnIndex) }
        fun Cursor.getSafeString(columnIndex: Int, defaultValue: String) = if (isNull(columnIndex)) { defaultValue } else { getString(columnIndex) }
        fun Long.round(digit: Int): Long = (this / digit.toFloat()).roundToLong() * digit
        fun Int.round(digit: Int): Int = (this / digit.toFloat()).roundToInt() * digit

    }
}