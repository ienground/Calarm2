package zone.ien.calarm.receiver

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.database.Cursor
import android.location.Address
import android.location.Geocoder
import android.location.Geocoder.GeocodeListener
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.CalendarContract
import android.util.Log
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import zone.ien.calarm.activity.TAG
import zone.ien.calarm.constant.SharedDefault
import zone.ien.calarm.constant.SharedKey
import zone.ien.calarm.data.CalendarEvent
import zone.ien.calarm.room.CalarmDatabase
import zone.ien.calarm.room.CalarmEntity
import java.util.ArrayList
import java.util.Calendar

class CalendarChangeListener: BroadcastReceiver() {
    private var calarmDatabase: CalarmDatabase? = null
    lateinit var geocoder: Geocoder
    lateinit var sharedPreferences: SharedPreferences

    @OptIn(DelicateCoroutinesApi::class)
    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "CalendarChangeReceiver onReceive")

        calarmDatabase = CalarmDatabase.getInstance(context)
        geocoder = Geocoder(context)
        sharedPreferences = context.getSharedPreferences("${context.packageName}_preferences", Context.MODE_PRIVATE)

        GlobalScope.launch(Dispatchers.IO) {
            val datas = calarmDatabase?.getDao()?.getAll()
            val list = ArrayList<CalendarEvent>()

            if (datas != null) {
                for (data in datas) {
                    list.addAll(getCalendarEventByID(context, data.dataId))
                }
            }

            for (data in list) {
                if (data.startDate < System.currentTimeMillis() || data.startDate - System.currentTimeMillis() >= AlarmManager.INTERVAL_DAY) continue
                val location = data.eventLocation
                var address: List<Address> = listOf()

                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        geocoder.getFromLocationName(list.first().eventLocation, 1) { address = it }
                    } else {
                        address = geocoder.getFromLocationName(list.first().eventLocation, 1) as List<Address>
                    }

                    Handler(Looper.getMainLooper()).postDelayed({
                        if (address.isNotEmpty()) {
                            val latitude = address.first().latitude.toFloat()
                            val longitude = address.first().longitude.toFloat()
                            val entity = calarmDatabase?.getDao()?.getByDataId(data.id) ?: CalarmEntity(data.id, data.calendarId, true, data.eventLocation, latitude, longitude, "", "", true)
                            entity.latitude = latitude
                            entity.longitude = longitude
                            entity.address = location
                            Log.d(TAG, "$latitude $longitude $location")
                            entity.sound = sharedPreferences.getString(SharedKey.LAST_ALARM_SOUND, "") ?: ""
                            // todo isEnabled : by option
                            calarmDatabase?.getDao()?.update(entity)
                        }
                    }, 1000)
                } catch (e: Exception) {
                    val entity = calarmDatabase?.getDao()?.getByDataId(data.id) ?: CalarmEntity(data.id, data.calendarId, true, data.eventLocation, SharedDefault.HOME_LATITUDE, SharedDefault.HOME_LONGITUDE, "", "", true)
                    entity.address = location
                    entity.sound = sharedPreferences.getString(SharedKey.LAST_ALARM_SOUND, "") ?: ""

                    calarmDatabase?.getDao()?.update(entity)
                }
            }
        }
    }

    private fun getCalendarEventByID(context: Context, id: Long): ArrayList<CalendarEvent> {
        val events = ArrayList<CalendarEvent>()

        context.contentResolver.query(
            CalendarContract.Events.CONTENT_URI,
            arrayOf(
                CalendarContract.Events._ID,
                CalendarContract.Events.CALENDAR_ID,
                CalendarContract.Events.TITLE,
                CalendarContract.Events.DESCRIPTION,
                CalendarContract.Events.DTSTART,
                CalendarContract.Events.EVENT_TIMEZONE,
                CalendarContract.Events.DTEND,
                CalendarContract.Events.EVENT_END_TIMEZONE,
                CalendarContract.Events.ALL_DAY,
                CalendarContract.Events.EVENT_COLOR,
                CalendarContract.Events.EVENT_LOCATION,
            ), "(( ${CalendarContract.Events._ID} = ${id} ) AND ( deleted != 1 ))", null, CalendarContract.Events.DTSTART)?.use { cursor ->
            while (cursor.moveToNext()) {
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
            }
        }

        return events
    }

    private fun Cursor.getSafeLong(columnIndex: Int, defaultValue: Long) = if (isNull(columnIndex)) { defaultValue } else { getLong(columnIndex) }
    private fun Cursor.getSafeString(columnIndex: Int, defaultValue: String) = if (isNull(columnIndex)) { defaultValue } else { getString(columnIndex) }
}