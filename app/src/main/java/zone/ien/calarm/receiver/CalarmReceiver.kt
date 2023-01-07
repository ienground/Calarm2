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
import zone.ien.calarm.activity.CalarmRingActivity
import zone.ien.calarm.activity.TAG
import zone.ien.calarm.constant.ChannelID
import zone.ien.calarm.constant.IntentID
import zone.ien.calarm.constant.IntentKey
import zone.ien.calarm.constant.NotificationID
import zone.ien.calarm.room.CalarmDatabase
import zone.ien.calarm.room.SubCalarmDatabase
import java.text.SimpleDateFormat
import java.util.*

class CalarmReceiver: BroadcastReceiver() {

    private var calarmDatabase: CalarmDatabase? = null
    private var subCalarmDatabase: SubCalarmDatabase? = null
    private lateinit var nm: NotificationManager
    private lateinit var sharedPreferences: SharedPreferences

    @OptIn(DelicateCoroutinesApi::class)
    override fun onReceive(context: Context, intent: Intent) {
        calarmDatabase = CalarmDatabase.getInstance(context)
        subCalarmDatabase = SubCalarmDatabase.getInstance(context)
        nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        sharedPreferences = context.getSharedPreferences("${context.packageName}_preferences", Context.MODE_PRIVATE)
        nm.createNotificationChannel(NotificationChannel(ChannelID.DEFAULT_ID, context.getString(R.string.app_name), NotificationManager.IMPORTANCE_HIGH))
        nm.createNotificationChannel(NotificationChannel(ChannelID.CALARM_ID, context.getString(R.string.calarm_calendar), NotificationManager.IMPORTANCE_HIGH).apply {
            vibrationPattern = longArrayOf(0L)
            enableVibration(true)
        })
        nm.createNotificationChannel(NotificationChannel(ChannelID.MISSING_ID, context.getString(R.string.missed_notification_calarm), NotificationManager.IMPORTANCE_HIGH))

        val mediaPlayer = MediaPlayer().apply { setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build()) }
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        val id = intent.getLongExtra(IntentKey.ITEM_ID, -1)
        val subCalarmId = intent.getLongExtra(IntentKey.SUBCALARM_ID, -1)

        Log.d(TAG, "id: ${id}")
        if (id != -1L) {
            GlobalScope.launch(Dispatchers.IO) {
                val data = calarmDatabase?.getDao()?.get(id)
                val subCalarm = subCalarmDatabase?.getDao()?.get(data?.dataId ?: -1)
                if (data != null) {
                    if ((data.isEnabled && subCalarmId == -1L) || (data.isEnabled && subCalarm != null && subCalarm.isEnabled)) {
                        val ringtoneUri: Uri = if (data.sound != "") Uri.parse(data.sound) else RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)

                        if (data.vibrate) {
                            vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(500, 1000), 0))
                        }

                        try {
                            mediaPlayer.setDataSource(context, ringtoneUri)
                            mediaPlayer.prepare()
                            mediaPlayer.isLooping = true
                            mediaPlayer.start()
                        } catch (_: Exception) {
                        }

                        val fullScreenPendingIntent = PendingIntent.getActivity(context, NotificationID.CALARM_CALARM, Intent(context, CalarmRingActivity::class.java).apply {
                            action = "fullscreen_activity"
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            putExtra(IntentKey.ITEM_ID, id)
                            putExtra(IntentKey.SUBCALARM_ID, subCalarmId)
                        }, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

                        val timeFormat = SimpleDateFormat(context.getString(R.string.alarm_content_format), Locale.getDefault())
                        val offIntent = Intent(context, CalarmOffReceiver::class.java).apply { putExtra(IntentKey.ITEM_ID, id) }
                        val offPendingIntent = PendingIntent.getBroadcast(context, (200000 + id).toInt(), offIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
                        val builder = NotificationCompat.Builder(context, ChannelID.CALARM_ID).apply {
                            setSmallIcon(R.drawable.ic_calarm_simplify)
                            setContentTitle(context.getString(R.string.calarm_calendar))
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
                            addAction(R.drawable.ic_alarm_off, context.getString(R.string.alarm_off), offPendingIntent)
                            setContentIntent(fullScreenPendingIntent)
                            setFullScreenIntent(fullScreenPendingIntent, true)

                            color = ContextCompat.getColor(context, R.color.colorAccent)
                        }

                        nm.notify((NotificationID.CALARM_CALARM + id).toInt(), builder.build())

//                        Handler(Looper.getMainLooper()).postDelayed({
//                            val missingBuilder = NotificationCompat.Builder(context, ChannelID.MISSING_ID).apply {
//                                setSmallIcon(R.drawable.ic_icon)
//                                setContentTitle(context.getString(R.string.missed_calarm))
//                                setContentText(timeFormat.format(Calendar.getInstance().apply {
//                                    set(Calendar.HOUR_OF_DAY, data.time / 60)
//                                    set(Calendar.MINUTE, data.time % 60)
//                                }.time))
//                                setAutoCancel(true)
//                                setOngoing(false)
//                                setDefaults(Notification.DEFAULT_LIGHTS or Notification.DEFAULT_VIBRATE)
//                                setCategory(NotificationCompat.CATEGORY_EVENT)
//                                setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
//                                priority = NotificationCompat.PRIORITY_DEFAULT
////                               TODO  setContentIntent(fullScreenPendingIntent)
//
//                                color = ContextCompat.getColor(context, R.color.colorAccent)
//                            }
//
//                            if (subCalarmId == -1L) {
//                                nm.notify((NotificationID.CALARM_CALARM + id).toInt(), missingBuilder.build())
//                                LocalBroadcastManager.getInstance(context).sendBroadcast(Intent(IntentID.STOP_CALARM))
//                            }
//                        }, 3 * 60 * 1000)
//                    }
                    }
                }

                LocalBroadcastManager.getInstance(context).registerReceiver(object : BroadcastReceiver() {
                    override fun onReceive(context: Context, intent: Intent) {
                        if (mediaPlayer.isPlaying) {
                            mediaPlayer.stop()
                        }
                        vibrator.cancel()
                    }
                }, IntentFilter(IntentID.STOP_CALARM))
            }
        }
    }
}