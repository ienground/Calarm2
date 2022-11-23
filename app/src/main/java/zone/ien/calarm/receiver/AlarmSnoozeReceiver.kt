package zone.ien.calarm.receiver

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import zone.ien.calarm.activity.TAG
import zone.ien.calarm.constant.*
import zone.ien.calarm.room.AlarmDatabase
import zone.ien.calarm.room.SubAlarmDatabase
import zone.ien.calarm.utils.MyUtils
import java.util.*

class AlarmSnoozeReceiver: BroadcastReceiver() {

    private var alarmDatabase: AlarmDatabase? = null
    lateinit var nm: NotificationManager
    lateinit var am: AlarmManager
    lateinit var sharedPreferences: SharedPreferences

    @OptIn(DelicateCoroutinesApi::class)
    override fun onReceive(context: Context, intent: Intent) {
        alarmDatabase = AlarmDatabase.getInstance(context)
        nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        sharedPreferences = context.getSharedPreferences("${context.packageName}_preferences", Context.MODE_PRIVATE)

        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            manager.defaultVibrator
        } else {
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        vibrator.cancel()

        val id = intent.getLongExtra(IntentKey.ITEM_ID, -1)

        LocalBroadcastManager.getInstance(context).sendBroadcast(Intent(IntentID.SNOOZE_ALARM).apply { putExtra(IntentKey.ITEM_ID, id) })

        if (id != -1L) {
            nm.cancel((250000 + id).toInt())
            GlobalScope.launch(Dispatchers.IO) {
                val data = alarmDatabase?.getDao()?.get(id)
                if (data != null) {
                    val alarmIntent = Intent(context, AlarmReceiver::class.java).apply {
                        action = ActionKey.TIMER_ALARM
                        putExtra(IntentKey.ITEM_ID, data.id)
                    }
                    val alarmPendingIntent = PendingIntent.getBroadcast(context, data.id?.toInt() ?: -1, alarmIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
                    val time = data.time + sharedPreferences.getInt(SharedKey.SNOOZE_TIME, SharedDefault.SNOOZE_TIME)
                    val alarmTime = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, time / 60)
                        set(Calendar.MINUTE, time % 60)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    am.setAlarmClock(AlarmManager.AlarmClockInfo(alarmTime.timeInMillis, alarmPendingIntent), alarmPendingIntent)
                    Log.d(TAG, "alarmTime:" + alarmTime.time.toString())
                }
            }
        }
    }
}