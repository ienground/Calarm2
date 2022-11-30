package zone.ien.calarm.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.CountDownTimer
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import zone.ien.calarm.R
import zone.ien.calarm.activity.AlarmRingActivity
import zone.ien.calarm.activity.TAG
import zone.ien.calarm.activity.TimerRingActivity
import zone.ien.calarm.constant.ChannelID
import zone.ien.calarm.constant.IntentID
import zone.ien.calarm.constant.IntentKey
import zone.ien.calarm.receiver.AlarmOffReceiver
import zone.ien.calarm.receiver.AlarmSnoozeReceiver
import zone.ien.calarm.room.SubTimerDatabase
import zone.ien.calarm.room.SubTimerEntity
import zone.ien.calarm.room.TimersDatabase
import zone.ien.calarm.room.TimersEntity
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList


class TimerService : Service() {

    lateinit var nm: NotificationManager
    private var countDownTimer: CountDownTimer? = null
    private var timersDatabase: TimersDatabase? = null
    private var subTimerDatabase: SubTimerDatabase? = null

    private var time = 0L
    private var item: TimersEntity? = null

    override fun onCreate() {
        super.onCreate()


    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
    }

    override fun onBind(intent: Intent): IBinder? = null

    @OptIn(DelicateCoroutinesApi::class)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        timersDatabase = TimersDatabase.getInstance(applicationContext)
        subTimerDatabase = SubTimerDatabase.getInstance(applicationContext)
        nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(NotificationChannel(ChannelID.COUNTDOWN_ID, getString(R.string.countdown), NotificationManager.IMPORTANCE_DEFAULT))

        GlobalScope.launch(Dispatchers.IO) {
            item = timersDatabase?.getDao()?.get(intent?.getLongExtra(IntentKey.ITEM_ID, -1) ?: -1)
            if (item != null) {
                item?.subTimers = subTimerDatabase?.getDao()?.getByParentId(intent?.getLongExtra(IntentKey.ITEM_ID, -1) ?: -1) as ArrayList<SubTimerEntity>
                val timeUntilFinished: ArrayList<Long> = arrayListOf()
                var order = 0
                for (timer in item?.subTimers ?: arrayListOf()) {
                    timeUntilFinished.add(time + timer.time)
                    time += timer.time
                }
                var standardTime = time * 1000L

                withContext(Dispatchers.Main) {
                    countDownTimer = object: CountDownTimer(time * 1000L, 500) {
                        override fun onTick(millisUntilFinished: Long) {
                            if (timeUntilFinished[order] * 1000L < time * 1000L - millisUntilFinished) {
                                order++
                                standardTime = millisUntilFinished
                            }

                            sendBroadcast(Intent(IntentID.COUNTDOWN_TICK).apply {
                                putExtra(IntentKey.ITEM_ID, item?.id)
                                putExtra(IntentKey.DURATION, time * 1000L)
                                putExtra(IntentKey.ORDER, order)
                                putExtra(IntentKey.COUNTDOWN_TIME, millisUntilFinished)
                                putExtra(IntentKey.STANDARD_TIME, standardTime)
                            })

                            NotificationCompat.Builder(applicationContext, ChannelID.COUNTDOWN_ID).apply {
                                setContentTitle("")
                                setContentText((millisUntilFinished / 1000).toInt().let {
                                    if (it / 3600 != 0) String.format("%02d:%02d:%02d", it / 3600, (it % 3600) / 60, it % 60)
                                    else String.format("%02d:%02d", (it % 3600) / 60, it % 60)
                                })
                                setSmallIcon(R.drawable.ic_timer)
                                setOngoing(true)
                                setShowWhen(false)

                                nm.notify(12345, build())
                            }
                        }

                        override fun onFinish() {
                            stopForeground(STOP_FOREGROUND_REMOVE)
                            time = 0

                            val fullScreenPendingIntent = PendingIntent.getActivity(applicationContext, 0, Intent(applicationContext, TimerRingActivity::class.java).apply {
                                action = "fullscreen_activity"
                                this.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            }, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

                            val builder = NotificationCompat.Builder(applicationContext, ChannelID.DEFAULT_ID).apply {
                                setSmallIcon(R.drawable.ic_alarm)
                                setContentTitle(getString(R.string.calarm_alarm))
                                setAutoCancel(false)
                                setOngoing(true)
                                setDefaults(Notification.DEFAULT_LIGHTS or Notification.DEFAULT_VIBRATE)
                                setCategory(NotificationCompat.CATEGORY_ALARM)
                                setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                                setLocalOnly(true)
                                priority = NotificationCompat.PRIORITY_MAX
                                setContentIntent(fullScreenPendingIntent)
                                setFullScreenIntent(fullScreenPendingIntent, true)

                                color = ContextCompat.getColor(applicationContext, R.color.amber)
                            }
                        }
                    }
                    countDownTimer?.start()
                }


            }
        }

        NotificationCompat.Builder(applicationContext, ChannelID.COUNTDOWN_ID).apply {
            setContentTitle("")
            setContentText("")
            setSmallIcon(R.drawable.ic_timer)

            startForeground(12345, build())
        }

        return super.onStartCommand(intent, flags, startId)
    }



}