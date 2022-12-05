package zone.ien.calarm.receiver

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.*
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import zone.ien.calarm.R
import zone.ien.calarm.activity.AlarmRingActivity
import zone.ien.calarm.activity.TAG
import zone.ien.calarm.constant.ChannelID
import zone.ien.calarm.constant.IntentID
import zone.ien.calarm.constant.IntentKey
import zone.ien.calarm.constant.NotificationID
import zone.ien.calarm.room.*
import zone.ien.calarm.service.TimerService
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class TimerAlarmReceiver: BroadcastReceiver() {

    private var timersDatabase: TimersDatabase? = null

    @OptIn(DelicateCoroutinesApi::class)
    override fun onReceive(context: Context, intent: Intent) {
        timersDatabase = TimersDatabase.getInstance(context)

        val id = intent.getLongExtra(IntentKey.ITEM_ID, -1)

        Log.d(TAG, "id: ${id}")
        if (id != -1L) {
            GlobalScope.launch(Dispatchers.IO) {
                val data = timersDatabase?.getDao()?.get(id)

                if (data != null) {
                    if (data.isScheduled) {
                        context.startForegroundService(Intent(context, TimerService::class.java).apply {
                            putExtra(IntentKey.ITEM_ID, id)
                        })

                        data.isScheduled = false
                        timersDatabase?.getDao()?.update(data)
                    }
                }
            }
        }
    }
}