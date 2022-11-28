package zone.ien.calarm.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.CountDownTimer
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import zone.ien.calarm.R
import zone.ien.calarm.activity.TAG
import zone.ien.calarm.constant.ChannelID
import zone.ien.calarm.constant.IntentKey
import zone.ien.calarm.room.SubTimerDatabase
import zone.ien.calarm.room.TimersDatabase


class TimerService : Service() {

    lateinit var nm: NotificationManager
    private var countDownTimer: CountDownTimer? = null
    private var timersDatabase: TimersDatabase? = null
    private var subTimerDatabase: SubTimerDatabase? = null

    private var time = 0L

    override fun onCreate() {
        super.onCreate()


    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
    }

    override fun onBind(intent: Intent): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(NotificationChannel(ChannelID.COUNTDOWN_ID, getString(R.string.countdown), NotificationManager.IMPORTANCE_DEFAULT))

        NotificationCompat.Builder(applicationContext, ChannelID.COUNTDOWN_ID).apply {
            setContentTitle("")
            setContentText("")
            setSmallIcon(R.drawable.ic_timer)

            startForeground(12345, build())
        }

        countDownTimer = object: CountDownTimer(intent?.getLongExtra(IntentKey.COUNTDOWN_TIME, 0L) ?: 0L, 500) {
            override fun onTick(millisUntilFinished: Long) {
                Log.d(TAG, "$millisUntilFinished")
            }

            override fun onFinish() {
                stopForeground(STOP_FOREGROUND_REMOVE)
            }
        }
        countDownTimer?.start()

        return super.onStartCommand(intent, flags, startId)
    }



}