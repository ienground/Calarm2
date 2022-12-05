package zone.ien.calarm.receiver

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
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
import zone.ien.calarm.constant.ActionKey
import zone.ien.calarm.constant.IntentID
import zone.ien.calarm.constant.IntentKey
import zone.ien.calarm.constant.NotificationID
import zone.ien.calarm.room.AlarmDatabase
import zone.ien.calarm.room.SubAlarmDatabase
import zone.ien.calarm.room.SubAlarmEntity
import zone.ien.calarm.service.TimerService
import zone.ien.calarm.utils.MyUtils
import java.util.*
import kotlin.collections.ArrayList

class TimerOffReceiver: BroadcastReceiver() {

    lateinit var nm: NotificationManager
    lateinit var am: AlarmManager

    override fun onReceive(context: Context, intent: Intent) {
        nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            manager.defaultVibrator
        } else {
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        vibrator.cancel()

        val id = intent.getLongExtra(IntentKey.ITEM_ID, -1)

//        LocalBroadcastManager.getInstance(context).sendBroadcast(Intent(IntentID.STOP_ALARM).apply { putExtra(IntentKey.ITEM_ID, id) })

        Log.d(TAG, "TimerOffReceiver")
        context.stopService(Intent(context, TimerService::class.java))
        nm.cancel((NotificationID.CALARM_TIMER_FINISHED))
        context.sendBroadcast(Intent(IntentID.STOP_TIMER))
    }
}