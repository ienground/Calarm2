package zone.ien.calarm.activity

import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.provider.CalendarContract
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import kotlinx.coroutines.*
import zone.ien.calarm.R
import zone.ien.calarm.callback.SwipeAnimationListener
import zone.ien.calarm.constant.IntentID
import zone.ien.calarm.constant.IntentKey
import zone.ien.calarm.data.CalendarEvent
import zone.ien.calarm.databinding.ActivityCalarmRingBinding
import zone.ien.calarm.receiver.CalarmOffReceiver
import zone.ien.calarm.room.CalarmDatabase
import zone.ien.calarm.room.SubCalarmDatabase
import zone.ien.calarm.utils.MyUtils.Companion.getSafeLong
import zone.ien.calarm.utils.MyUtils.Companion.getSafeString
import java.text.SimpleDateFormat
import java.util.*

class CalarmRingActivity : AppCompatActivity() {

    lateinit var binding: ActivityCalarmRingBinding
    private var calarmDatabase: CalarmDatabase? = null
    private var subCalarmDatabase: SubCalarmDatabase? = null

    @OptIn(DelicateCoroutinesApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_calarm_ring)
        binding.activity = this

        calarmDatabase = CalarmDatabase.getInstance(this)
        subCalarmDatabase = SubCalarmDatabase.getInstance(this)

        turnScreenOnAndKeyguardOff()

        binding.tvApm.visibility = if (Locale.getDefault() == Locale.KOREA) View.GONE else View.VISIBLE
        binding.tvApmKo.visibility = if (Locale.getDefault() != Locale.KOREA) View.GONE else View.VISIBLE
        binding.tvApmParent.visibility = if (Locale.getDefault() == Locale.KOREA) View.GONE else View.VISIBLE
        binding.tvApmKoParent.visibility = if (Locale.getDefault() != Locale.KOREA) View.GONE else View.VISIBLE

        val apmFormat = SimpleDateFormat("a", Locale.getDefault())
        val timeFormat = SimpleDateFormat("hh:mm", Locale.getDefault())
        val dateFormat = SimpleDateFormat(getString(R.string.dateFormatNoYear), Locale.getDefault())

        binding.tvDate.text = dateFormat.format(Date(System.currentTimeMillis()))
        binding.tvTime.text = timeFormat.format(Date(System.currentTimeMillis()))
        binding.tvApm.text = apmFormat.format(Date(System.currentTimeMillis()))
        binding.tvApmKo.text = apmFormat.format(Date(System.currentTimeMillis()))

        val id = intent.getLongExtra(IntentKey.ITEM_ID, -1)
        val subCalarmId = intent.getLongExtra(IntentKey.SUBCALARM_ID, -1)

        if (id != -1L) {
            GlobalScope.launch(Dispatchers.IO) {
                val data = calarmDatabase?.getDao()?.get(id)
                if (data != null) {
                    data.let {
                        val calendarEvent = getCalendarEventByID(applicationContext, data.dataId).first()
                        it.time = calendarEvent.startDate
                        it.label = calendarEvent.title
                    }
                    val offIntent = Intent(applicationContext, CalarmOffReceiver::class.java).apply {
                        putExtra(IntentKey.ITEM_ID, id)
                        putExtra(IntentKey.SUBCALARM_ID, subCalarmId)
                    }
                    val calendar = Calendar.getInstance().apply {
                        timeInMillis = data.time
                    }

                    withContext(Dispatchers.Main) {
                        val initialGap = calendar.let { it.get(Calendar.HOUR_OF_DAY) * 60 + it.get(Calendar.MINUTE) } - Calendar.getInstance().let { it.get(Calendar.HOUR_OF_DAY) * 60 + it.get(Calendar.MINUTE) }

                        binding.tvLabel.text = data.label
                        binding.tvApmParent.text = apmFormat.format(calendar.time)
                        binding.tvApmKoParent.text = apmFormat.format(calendar.time)
                        binding.tvTimeParent.text = timeFormat.format(calendar.time)

                        val timeDiffArray: ArrayList<String> = arrayListOf()
                        if (initialGap / 60 == 1) timeDiffArray.add(getString(R.string.time_format_1hour))
                        else if (initialGap / 60 != 0) timeDiffArray.add(getString(R.string.time_format_hour, initialGap / 60))
                        if (initialGap % 60 == 1) timeDiffArray.add(getString(R.string.time_format_1minute))
                        else if (initialGap % 60 != 0) timeDiffArray.add(getString(R.string.time_format_minute, initialGap % 60))

                        binding.slider.setOnSwipeAnimationListener(object: SwipeAnimationListener {
                            override fun onSwiped(isRight: Boolean) {
                                if (isRight) {
                                    sendBroadcast(offIntent); finish()
                                }
                            }
                        })

                        registerReceiver(object : BroadcastReceiver() {
                            override fun onReceive(context: Context, intent: Intent) {
                                val gap = calendar.let { it.get(Calendar.HOUR_OF_DAY) * 60 + it.get(Calendar.MINUTE) } - Calendar.getInstance().let { it.get(Calendar.HOUR_OF_DAY) * 60 + it.get(Calendar.MINUTE) }
                                binding.tvTime.text = timeFormat.format(Date(System.currentTimeMillis()))
                                binding.tvApm.text = apmFormat.format(Date(System.currentTimeMillis()))
                                binding.tvApmKo.text = apmFormat.format(Date(System.currentTimeMillis()))

                                timeDiffArray.clear()
                                if (gap / 60 == 1) timeDiffArray.add(getString(R.string.time_format_1hour))
                                else if (gap / 60 != 0) timeDiffArray.add(getString(R.string.time_format_hour, gap / 60))
                                if (gap % 60 == 1) timeDiffArray.add(getString(R.string.time_format_1minute))
                                else if (gap % 60 != 0) timeDiffArray.add(getString(R.string.time_format_minute, gap % 60))

                                binding.tvTimeDiff.text = getString(R.string.time_format_before, timeDiffArray.joinToString(" "))
                            }
                        }, IntentFilter(Intent.ACTION_TIME_TICK))
                    }
                }
            }
        }

        LocalBroadcastManager.getInstance(applicationContext).registerReceiver(object: BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                finish()
            }
        }, IntentFilter(IntentID.STOP_CALARM))

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

    private fun turnScreenOnAndKeyguardOff(){
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                or WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON)
        val keyguardMgr = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        keyguardMgr.requestDismissKeyguard(this, null)
    }
}