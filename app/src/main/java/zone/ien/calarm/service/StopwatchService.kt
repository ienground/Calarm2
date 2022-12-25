package zone.ien.calarm.service

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.os.CountDownTimer
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import zone.ien.calarm.R
import zone.ien.calarm.activity.TAG
import zone.ien.calarm.activity.TimerRingActivity
import zone.ien.calarm.constant.*
import zone.ien.calarm.data.StopwatchLapse
import zone.ien.calarm.receiver.TimerOffReceiver
import zone.ien.calarm.room.SubTimerDatabase
import zone.ien.calarm.room.SubTimerEntity
import zone.ien.calarm.room.TimersDatabase
import zone.ien.calarm.room.TimersEntity
import zone.ien.calarm.utils.MyUtils.Companion.round
import java.util.*
import kotlin.collections.ArrayList


class StopwatchService : Service() {

    lateinit var nm: NotificationManager

    private var duration = 0L

    private var timerNotification: NotificationCompat.Builder? = null
    lateinit var playPausePendingIntent: PendingIntent
    private lateinit var sharedPreferences: SharedPreferences

    private var timer: Timer? = null

    override fun onCreate() {
        super.onCreate()
        isRunning = true
    }

    override fun onDestroy() {
        nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(NotificationID.CALARM_STOPWATCH)
        timer?.cancel()
        time = 0L
        isPaused = true
        isRunning = false
        lapses.clear()
        sharedPreferences.edit().putBoolean(SharedKey.IS_STOPWATCH_SCHEDULED, false).apply()

        sendBroadcast(Intent(IntentID.STOP_STOPWATCH))

        Log.d(TAG, "onDestroy ${lapses}")

        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(NotificationChannel(ChannelID.STOPWATCH_ID, getString(R.string.stopwatch), NotificationManager.IMPORTANCE_DEFAULT))
        sharedPreferences = getSharedPreferences("${packageName}_preferences", Context.MODE_PRIVATE)
        playPausePendingIntent = PendingIntent.getBroadcast(applicationContext, 0, Intent(IntentID.PLAY_PAUSE_STOPWATCH), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        duration = intent?.getLongExtra(IntentKey.DURATION, 0L) ?: 0L
        stopwatchStart()
        isPaused = false

        registerReceiver(object: BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                isPaused = !isPaused
                Log.d(TAG, "$isPaused")
                if (isPaused) {
                    timer?.cancel()
                    timerNotification?.clearActions()
                    timerNotification?.addAction(R.drawable.ic_play_arrow, getString(R.string.resume), playPausePendingIntent)
                    timerNotification?.setSmallIcon(R.drawable.ic_timer)
                    nm.notify(NotificationID.CALARM_STOPWATCH, timerNotification?.build())
                } else {
                    stopwatchStart()
                    timerNotification?.clearActions()
                    timerNotification?.addAction(R.drawable.ic_pause, getString(R.string.pause), playPausePendingIntent)
                    timerNotification?.setSmallIcon(R.drawable.ic_timer)
                    nm.notify(NotificationID.CALARM_STOPWATCH, timerNotification?.build())
                }

                sendBroadcast(Intent(IntentID.PLAY_PAUSE_STOPWATCH_RESULT))
            }
        }, IntentFilter(IntentID.PLAY_PAUSE_STOPWATCH))

        registerReceiver(object: BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val flag = intent.getStringExtra(IntentKey.LAP_FLAG) ?: ""

                context.sendBroadcast(Intent(IntentID.LAP_STOPWATCH_RESULT).apply {
                    putExtra(IntentKey.LAP_FLAG, flag)
                    putExtra(IntentKey.LAP_TIME, time)
                })
            }
        }, IntentFilter(IntentID.LAP_STOPWATCH))

        NotificationCompat.Builder(applicationContext, ChannelID.STOPWATCH_ID).apply {
            setContentTitle("")
            setContentText("")
            setSmallIcon(R.drawable.ic_timer)
            setOnlyAlertOnce(true)

            startForeground(NotificationID.CALARM_STOPWATCH, build())
            nm.cancel(NotificationID.CALARM_STOPWATCH)
        }

        return super.onStartCommand(intent, flags, startId)
    }

    private fun stopwatchStart() {
        val timerTask = object: TimerTask() {
            override fun run() {
                time += 10
                sendBroadcast(Intent(IntentID.STOPWATCH_TICK).apply {
                    putExtra(IntentKey.COUNTDOWN_TIME, time)
                })
                if (time % 500 == 0L) {
                    timerNotification = NotificationCompat.Builder(applicationContext, ChannelID.STOPWATCH_ID).apply {
                        setContentTitle((time / 1000).let {
                            if (it / 3600 != 0L) String.format("%02d:%02d:%02d", it / 3600, (it % 3600) / 60, it % 60)
                            else String.format("%02d:%02d", (it % 3600) / 60, it % 60)
                        })
                        setContentText("J")
                        setSmallIcon(R.drawable.ic_timer)
                        setOnlyAlertOnce(true)
                        setShowWhen(false)
                    }

                    nm.notify(NotificationID.CALARM_STOPWATCH, timerNotification?.build())
                }
            }
        }


        timer = Timer()
        timer?.schedule(timerTask, 0, 10)
    }

    companion object {
        var isRunning = false
        var isPaused = false
        var lapses: ArrayList<StopwatchLapse> = arrayListOf()
        var time = 0L
    }


}