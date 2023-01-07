package zone.ien.calarm.receiver

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import zone.ien.calarm.activity.TAG
import zone.ien.calarm.constant.IntentID
import zone.ien.calarm.constant.IntentKey
import zone.ien.calarm.constant.NotificationID
import zone.ien.calarm.room.*
import zone.ien.calarm.utils.MyUtils
import java.util.*
import kotlin.collections.ArrayList

class CalarmOffReceiver: BroadcastReceiver() {

    private var calarmDatabase: CalarmDatabase? = null
    private var subCalarmDatabase: SubCalarmDatabase? = null
    lateinit var nm: NotificationManager
    lateinit var am: AlarmManager

    @OptIn(DelicateCoroutinesApi::class)
    override fun onReceive(context: Context, intent: Intent) {
        calarmDatabase = CalarmDatabase.getInstance(context)
        subCalarmDatabase = SubCalarmDatabase.getInstance(context)
        nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val id = intent.getLongExtra(IntentKey.ITEM_ID, -1)
        val subCalarmId = intent.getLongExtra(IntentKey.SUBCALARM_ID, -1)

        LocalBroadcastManager.getInstance(context).sendBroadcast(Intent(IntentID.STOP_CALARM).apply { putExtra(IntentKey.ITEM_ID, id) })

        Log.d(TAG, "calarm off $id")
        if (id != -1L) {
            nm.cancel((NotificationID.CALARM_CALARM + id).toInt())
            GlobalScope.launch(Dispatchers.IO) {
                val data = calarmDatabase?.getDao()?.get(id)
                data?.subCalarms = subCalarmDatabase?.getDao()?.getByParentId(data?.dataId ?: -1) as ArrayList<SubCalarmEntity>
                val subAlarm = if (subCalarmId != -1L) subCalarmDatabase?.getDao()?.get(subCalarmId) else null
                if (data != null) {
                    if (subAlarm != null) {
                        subAlarm.isEnabled = false
                        subCalarmDatabase?.getDao()?.update(subAlarm)
                    }
                    if (subAlarm == null) {
                        data.isEnabled = false
                        calarmDatabase?.getDao()?.update(data)
                    }
                }
            }
        }
    }
}