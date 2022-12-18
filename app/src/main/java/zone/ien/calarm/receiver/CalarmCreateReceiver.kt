package zone.ien.calarm.receiver

import android.app.Activity
import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.provider.CalendarContract
import android.util.Log
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import kotlinx.coroutines.*
import zone.ien.calarm.R
import zone.ien.calarm.activity.TAG
import zone.ien.calarm.adapter.MainCalarmEventAdapter
import zone.ien.calarm.constant.SharedDefault
import zone.ien.calarm.constant.SharedKey
import zone.ien.calarm.constant.SharedValue
import zone.ien.calarm.data.CalendarEvent
import zone.ien.calarm.room.CalarmDatabase
import zone.ien.calarm.room.CalarmEntity
import zone.ien.calarm.room.SubCalarmDatabase
import zone.ien.calarm.room.SubCalarmEntity
import zone.ien.calarm.utils.MyUtils.Companion.getAlarmRingtones
import zone.ien.calarm.utils.MyUtils.Companion.getSafeLong
import zone.ien.calarm.utils.MyUtils.Companion.getSafeString
import zone.ien.calarm.utils.MyUtils.Companion.setCalarmClock
import zone.ien.calarm.utils.MyUtils.Companion.timeZero
import java.net.URLEncoder
import java.util.*
import javax.microedition.khronos.opengles.GL11ExtensionPack
import kotlin.collections.ArrayList

class CalarmCreateReceiver: BroadcastReceiver() {
    private var calarmDatabase: CalarmDatabase? = null
    private var subCalarmDatabase: SubCalarmDatabase? = null
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var am: AlarmManager

    private var defaultRingtone = ""

    @OptIn(DelicateCoroutinesApi::class)
    override fun onReceive(context: Context, intent: Intent) {
        // run at 12:05 am

        Log.d(TAG, "CalarmCreateReceiver onReceive")

        calarmDatabase = CalarmDatabase.getInstance(context)
        subCalarmDatabase = SubCalarmDatabase.getInstance(context)
        sharedPreferences = context.getSharedPreferences("${context.packageName}_preferences", Context.MODE_PRIVATE)
        am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        defaultRingtone = sharedPreferences.getString(SharedKey.LAST_ALARM_SOUND, "") ?: ""

        val calendarIdList = getCalendarIdList(context)
        val datas = ArrayList<CalarmEntity>()
        val events = ArrayList<CalendarEvent>()
        val startCalendar = Calendar.getInstance().timeZero()
        val endCalendar = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 5)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        GlobalScope.launch(Dispatchers.IO) {
            for (id in calendarIdList) {
                if (id.second) {
                    events.addAll(getCalendarEventByDate(context, startCalendar, endCalendar, id.first))
                }
            }

            for (event in events) {
                val eventCalendar = Calendar.getInstance().apply { timeInMillis = event.startDate }
                val data = calarmDatabase?.getDao()?.getByDataId(event.id) ?: CalarmEntity(event.id, event.calendarId, false, event.eventLocation, SharedDefault.HOME_LATITUDE, SharedDefault.HOME_LONGITUDE, "", "", true)
                data.subCalarms = subCalarmDatabase?.getDao()?.getByParentId(event.id) as ArrayList<SubCalarmEntity>
                data.time = eventCalendar.timeInMillis
                data.label = event.title

                datas.add(data)
            }

            if (datas.isNotEmpty()) {
                when (sharedPreferences.getString(SharedKey.CALARM_CREATE_OPTION, SharedDefault.CALARM_CREATE_OPTION)) {
                    SharedValue.CALARM_CREATE_FIRST_EVENT -> {
                        GlobalScope.launch(Dispatchers.IO) {
                            val url = "https://maps.googleapis.com/maps/api/distancematrix/json?units=metric&mode=transit&origins=${sharedPreferences.getFloat(SharedKey.HOME_LATITUDE, SharedDefault.HOME_LATITUDE)},${sharedPreferences.getFloat(SharedKey.HOME_LONGITUDE, SharedDefault.HOME_LONGITUDE)}&destinations=${URLEncoder.encode(datas.first().address, "UTF-8")}&region=KR&key=${context.getString(R.string.distance_api_key)}"

                            val jsonObjectRequest = JsonObjectRequest(Request.Method.GET, url, null, { response ->
                                val row = response.getJSONArray("rows")
                                Log.d(TAG, response.toString())
                                if (row.length() != 0) {
                                    val item = row.getJSONObject(0)
                                    if (item.length() != 0) {
                                        val element = item.getJSONArray("elements")
                                        if (element.length() != 0) {
                                            val value = element.getJSONObject(0).getJSONObject("duration").getInt("value") / 60
                                            GlobalScope.launch(Dispatchers.IO) {
                                                val entity = calarmDatabase?.getDao()?.getByDataId(datas.first().dataId) ?: CalarmEntity(datas.first().dataId, datas.first().calendarId, true, datas.first().address, SharedDefault.HOME_LATITUDE, SharedDefault.HOME_LONGITUDE, "", defaultRingtone, true)
                                                entity.isEnabled = true
                                                calarmDatabase?.getDao()?.add(entity)
                                                val subEntity = subCalarmDatabase?.getDao()?.getAutoGenerated(datas.first().dataId) ?: SubCalarmEntity(datas.first().dataId, value + sharedPreferences.getInt(SharedKey.READY_TIME, SharedDefault.READY_TIME), true, value)
                                                entity.subCalarms.add(subEntity)
                                                for (subCalarm in entity.subCalarms) {
                                                    val id = subCalarmDatabase?.getDao()?.add(subCalarm)
                                                    subCalarm.id = id
                                                }
                                                setCalarmClock(context, am, entity)
                                            }

                                        }
                                    }
                                }


                            }, { error ->
                                Log.e(TAG, error.toString())
                            })

                            Volley.newRequestQueue(context).add(jsonObjectRequest)
                        }
                    }
                }
            }
        }



    }

    private fun getCalendarEventByDate(context: Context, startCalendar: Calendar, endCalendar: Calendar, calendarId: Long): ArrayList<CalendarEvent> {
        val events = ArrayList<CalendarEvent>()

        context.contentResolver.query(CalendarContract.Events.CONTENT_URI,
            arrayOf(
                CalendarContract.Events._ID,
                CalendarContract.Events.TITLE,
                CalendarContract.Events.DESCRIPTION,
                CalendarContract.Events.DTSTART,
                CalendarContract.Events.EVENT_TIMEZONE,
                CalendarContract.Events.DTEND,
                CalendarContract.Events.EVENT_END_TIMEZONE,
                CalendarContract.Events.ALL_DAY,
                CalendarContract.Events.EVENT_COLOR,
                CalendarContract.Events.EVENT_LOCATION,
            ), "(( ${CalendarContract.Events.DTSTART} >= ${startCalendar.timeInMillis} ) AND ( ${CalendarContract.Events.DTSTART} <= ${endCalendar.timeInMillis} ) AND ( ${CalendarContract.Events.CALENDAR_ID} = ${calendarId} ) AND ( deleted != 1 ))", null, CalendarContract.Events.DTSTART)?.use { cursor ->

            if (cursor.moveToFirst()) {
                do {
                    val event = CalendarEvent(
                        cursor.getSafeLong(cursor.getColumnIndex(CalendarContract.Events._ID), 0L),
                        cursor.getSafeLong(cursor.getColumnIndex(CalendarContract.Events.CALENDAR_ID), 0L),
                        cursor.getSafeString(cursor.getColumnIndex(CalendarContract.Events.TITLE), ""),
                        cursor.getSafeString(cursor.getColumnIndex(CalendarContract.Events.DESCRIPTION), ""),
                        cursor.getSafeLong(cursor.getColumnIndex(CalendarContract.Events.DTSTART), 0L),
                        cursor.getSafeString(cursor.getColumnIndex(CalendarContract.Events.EVENT_TIMEZONE), ""),
                        cursor.getSafeLong(cursor.getColumnIndex(CalendarContract.Events.DTEND), 0L),
                        cursor.getSafeString(cursor.getColumnIndex(CalendarContract.Events.EVENT_END_TIMEZONE), ""),
                        cursor.getInt(cursor.getColumnIndexOrThrow(CalendarContract.Events.ALL_DAY)) == 1,
                        cursor.getSafeString(cursor.getColumnIndex(CalendarContract.Events.EVENT_COLOR), "0"),
                        cursor.getSafeString(cursor.getColumnIndex(CalendarContract.Events.EVENT_LOCATION), ""),
                    )

                    events.add(event)
                } while (cursor.moveToNext())
            }
        }

        return events
    }

    private fun getCalendarIdList(context: Context): ArrayList<Pair<Long, Boolean>> {
        val list = ArrayList<Pair<Long, Boolean>>()
        val cursor = context.contentResolver.query(Uri.parse("content://com.android.calendar/calendars"), arrayOf(CalendarContract.Calendars._ID), null, null, null)

        if (cursor != null) {
            if (cursor.count > 0) {
                cursor.moveToFirst()

                for (i in 0 until cursor.count) {
                    val value = sharedPreferences.getBoolean("calendar_id_${cursor.getLong(0)}", true)
                    list.add(Pair(cursor.getLong(0), value))

                    cursor.moveToNext()
                }
            }
        }

        cursor?.close()

        return list
    }

}