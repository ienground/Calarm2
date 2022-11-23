package zone.ien.calarm.utils

import android.app.Activity
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.media.RingtoneManager
import android.util.Log
import zone.ien.calarm.R
import zone.ien.calarm.activity.TAG
import zone.ien.calarm.constant.ActionKey
import zone.ien.calarm.constant.IntentKey
import zone.ien.calarm.receiver.AlarmReceiver
import zone.ien.calarm.room.AlarmEntity
import java.util.*
import kotlin.math.pow

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
    }
}