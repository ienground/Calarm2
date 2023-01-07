package zone.ien.calarm.receiver

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.*
import android.util.Log
import android.util.TypedValue
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
import zone.ien.calarm.constant.*
import zone.ien.calarm.room.AlarmDatabase
import zone.ien.calarm.room.SubAlarmDatabase
import java.text.SimpleDateFormat
import java.util.*

class AlarmReceiver: BroadcastReceiver() {

    private var alarmDatabase: AlarmDatabase? = null
    private var subAlarmDatabase: SubAlarmDatabase? = null
    private lateinit var nm: NotificationManager
    private lateinit var sharedPreferences: SharedPreferences

    @OptIn(DelicateCoroutinesApi::class)
    override fun onReceive(context: Context, intent: Intent) {
        alarmDatabase = AlarmDatabase.getInstance(context)
        subAlarmDatabase = SubAlarmDatabase.getInstance(context)
        nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        sharedPreferences = context.getSharedPreferences("${context.packageName}_preferences", Context.MODE_PRIVATE)
        nm.createNotificationChannel(NotificationChannel(ChannelID.DEFAULT_ID, context.getString(R.string.app_name), NotificationManager.IMPORTANCE_HIGH))
        nm.createNotificationChannel(NotificationChannel(ChannelID.ALARM_ID, context.getString(R.string.calarm_alarm), NotificationManager.IMPORTANCE_HIGH).apply {
            vibrationPattern = longArrayOf(0L)
            enableVibration(true)
        })
        nm.createNotificationChannel(NotificationChannel(ChannelID.MISSING_ID, context.getString(R.string.missed_notification), NotificationManager.IMPORTANCE_HIGH))

        val mediaPlayer = MediaPlayer().apply { setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build()) }
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        val id = intent.getLongExtra(IntentKey.ITEM_ID, -1)
        val subAlarmId = intent.getLongExtra(IntentKey.SUBALARM_ID, -1)

        Log.d(TAG, "id: ${id} subalarm $subAlarmId")
        if (id != -1L) {
            GlobalScope.launch(Dispatchers.IO) {
                val data = alarmDatabase?.getDao()?.get(id)
                val subAlarm = subAlarmDatabase?.getDao()?.get(subAlarmId)
                if (data != null) {
                    if ((data.isEnabled && subAlarmId == -1L) || (data.isEnabled && subAlarm != null && subAlarm.isEnabled)) {
                        val ringtoneUri: Uri = if (data.sound != "") Uri.parse(data.sound) else RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)

                        if (data.vibrate) {
                            vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(500, 1000), 0))
                        }

                        try {
                            mediaPlayer.setDataSource(context, ringtoneUri)
                            mediaPlayer.prepare()
                            mediaPlayer.isLooping = true
                            mediaPlayer.start()
                        } catch (_: Exception) { }

                        val fullScreenPendingIntent = PendingIntent.getActivity(context, NotificationID.CALARM_ALARM, Intent(context, AlarmRingActivity::class.java).apply {
                            action = "fullscreen_activity"
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            putExtra(IntentKey.ITEM_ID, id)
                            putExtra(IntentKey.SUBALARM_ID, subAlarmId)
                        }, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

                        val timeFormat = SimpleDateFormat(context.getString(R.string.alarm_content_format), Locale.getDefault())
                        val snoozeIntent = Intent(context, AlarmSnoozeReceiver::class.java).apply { putExtra(IntentKey.ITEM_ID, id) }
                        val snoozePendingIntent = PendingIntent.getBroadcast(context, (300000 + id).toInt(), snoozeIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
                        val offIntent = Intent(context, AlarmOffReceiver::class.java).apply { putExtra(IntentKey.ITEM_ID, id) }
                        val offPendingIntent = PendingIntent.getBroadcast(context, (200000 + id).toInt(), offIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
                        val builder = NotificationCompat.Builder(context, ChannelID.ALARM_ID).apply {
                            setSmallIcon(R.drawable.ic_alarm)
                            setContentTitle(context.getString(R.string.calarm_alarm))
                            setContentText(timeFormat.format(Calendar.getInstance().time))
                            setAutoCancel(false)
                            setOngoing(true)
                            setDefaults(Notification.DEFAULT_LIGHTS or Notification.DEFAULT_VIBRATE)
                            setSound(null)
                            setVibrate(longArrayOf(0L))
                            setCategory(NotificationCompat.CATEGORY_ALARM)
                            setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                            setLocalOnly(true)
                            priority = NotificationCompat.PRIORITY_MAX
                            if (subAlarmId == -1L) addAction(R.drawable.ic_snooze, context.getString(R.string.snooze), snoozePendingIntent)
                            addAction(R.drawable.ic_alarm_off, context.getString(R.string.alarm_off), offPendingIntent)
                            setContentIntent(fullScreenPendingIntent)
                            setFullScreenIntent(fullScreenPendingIntent, true)

                            color = ContextCompat.getColor(context, R.color.colorAccent)
                        }

                        nm.notify((NotificationID.CALARM_ALARM + id).toInt(), builder.build())

                        Handler(Looper.getMainLooper()).postDelayed({
                            context.sendBroadcast(offIntent)
                            val missingBuilder = NotificationCompat.Builder(context, ChannelID.MISSING_ID).apply {
                                setSmallIcon(R.drawable.ic_icon)
                                setContentTitle(context.getString(R.string.missed_alarm))
                                setContentText(timeFormat.format(Calendar.getInstance().apply {
                                    set(Calendar.HOUR_OF_DAY, data.time / 60)
                                    set(Calendar.MINUTE, data.time % 60)
                                }.time))
                                setAutoCancel(true)
                                setOngoing(false)
                                setDefaults(Notification.DEFAULT_LIGHTS or Notification.DEFAULT_VIBRATE)
                                setCategory(NotificationCompat.CATEGORY_EVENT)
                                setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                                priority = NotificationCompat.PRIORITY_DEFAULT
//                               TODO  setContentIntent(fullScreenPendingIntent)

                                color = ContextCompat.getColor(context, R.color.colorAccent)
                            }

                            if (subAlarmId == -1L) {
                                Log.d(TAG, "NOTIFY!")
                                nm.notify((NotificationID.CALARM_ALARM_MISSING + id).toInt(), missingBuilder.build())
                                LocalBroadcastManager.getInstance(context).sendBroadcast(Intent(IntentID.STOP_ALARM))
                            }
                        }, sharedPreferences.getInt(SharedKey.ALARM_DISMISS_TIME, SharedDefault.ALARM_DISMISS_TIME) * 60 * 1000L)
                    }
                }
            }

            LocalBroadcastManager.getInstance(context).registerReceiver(object: BroadcastReceiver() {
                override fun onReceive(context: Context , intent: Intent) {
                    if (mediaPlayer.isPlaying) {
                        mediaPlayer.stop()
                    }
                    vibrator.cancel()
                }
            }, IntentFilter(IntentID.STOP_ALARM))
        }
    }
}