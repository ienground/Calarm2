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
import zone.ien.calarm.constant.ActionKey
import zone.ien.calarm.constant.IntentID
import zone.ien.calarm.constant.IntentKey
import zone.ien.calarm.constant.NotificationID
import zone.ien.calarm.room.AlarmDatabase
import zone.ien.calarm.room.SubAlarmDatabase
import zone.ien.calarm.room.SubAlarmEntity
import zone.ien.calarm.utils.MyUtils
import java.util.*
import kotlin.collections.ArrayList

class AlarmOffReceiver: BroadcastReceiver() {

    private var alarmDatabase: AlarmDatabase? = null
    private var subAlarmDatabase: SubAlarmDatabase? = null
    lateinit var nm: NotificationManager
    lateinit var am: AlarmManager

    @OptIn(DelicateCoroutinesApi::class)
    override fun onReceive(context: Context, intent: Intent) {
        alarmDatabase = AlarmDatabase.getInstance(context)
        subAlarmDatabase = SubAlarmDatabase.getInstance(context)
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
        val subAlarmId = intent.getLongExtra(IntentKey.SUBALARM_ID, -1)

        LocalBroadcastManager.getInstance(context).sendBroadcast(Intent(IntentID.STOP_ALARM).apply { putExtra(IntentKey.ITEM_ID, id) })

        if (id != -1L) {
            nm.cancel((NotificationID.CALARM_ALARM + id).toInt())
            GlobalScope.launch(Dispatchers.IO) {
                val data = alarmDatabase?.getDao()?.get(id)
                data?.subAlarms = subAlarmDatabase?.getDao()?.getByParentId(id) as ArrayList<SubAlarmEntity>
                val subAlarm = if (subAlarmId != -1L) subAlarmDatabase?.getDao()?.get(subAlarmId) else null
                if (data != null) {
                    if (data.repeat == 0) {
                        if (subAlarm != null) {
                            subAlarm.isEnabled = false
                            subAlarmDatabase?.getDao()?.update(subAlarm)
                        }
                        if (subAlarm == null) {
                            data.isEnabled = false
                            alarmDatabase?.getDao()?.update(data)
                        }
                    } else {
                        MyUtils.setAlarmClock(context, am, data)
                    }
                }
            }
        }
    }
}