package zone.ien.calarm.service

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.CountDownTimer
import android.os.IBinder
import android.util.Log
import android.util.TypedValue
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import zone.ien.calarm.R
import zone.ien.calarm.activity.TAG
import zone.ien.calarm.activity.TimerRingActivity
import zone.ien.calarm.constant.ChannelID
import zone.ien.calarm.constant.IntentID
import zone.ien.calarm.constant.IntentKey
import zone.ien.calarm.constant.NotificationID
import zone.ien.calarm.receiver.TimerOffReceiver
import zone.ien.calarm.room.SubTimerDatabase
import zone.ien.calarm.room.SubTimerEntity
import zone.ien.calarm.room.TimersDatabase
import zone.ien.calarm.room.TimersEntity
import zone.ien.calarm.utils.MyUtils.Companion.round
import java.util.*
import kotlin.collections.ArrayList


class TimerService : Service() {

    lateinit var nm: NotificationManager
    private var countDownTimer: CountDownTimer? = null
    private var timersDatabase: TimersDatabase? = null
    private var subTimerDatabase: SubTimerDatabase? = null

    private var time = 0L
    private var item: TimersEntity? = null
    private var timer: Timer? = null
    private var id: Long = -1
    private var duration = 0L
    private var label = ""

    private var timeUntilFinished: ArrayList<Long> = arrayListOf()
    private var millisLeft = 0L
    var order = 0
    var standardTime = time * 1000L

    private var timerNotification: NotificationCompat.Builder? = null
    lateinit var playPausePendingIntent: PendingIntent

    override fun onCreate() {
        super.onCreate()
        isRunning = true
    }

    override fun onDestroy() {
        countDownTimer?.cancel()
        isRunning = false
        nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(NotificationID.CALARM_TIMER)
        timer?.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder? = null

    @OptIn(DelicateCoroutinesApi::class)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        timersDatabase = TimersDatabase.getInstance(applicationContext)
        subTimerDatabase = SubTimerDatabase.getInstance(applicationContext)
        nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(NotificationChannel(ChannelID.COUNTDOWN_ID, getString(R.string.countdown), NotificationManager.IMPORTANCE_HIGH).apply {
            vibrationPattern = longArrayOf(0L)
            enableVibration(true)
        })
        playPausePendingIntent = PendingIntent.getBroadcast(applicationContext, 0, Intent(IntentID.PLAY_PAUSE_TIMER), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        id = intent?.getLongExtra(IntentKey.ITEM_ID, -1) ?: -1
        duration = intent?.getLongExtra(IntentKey.DURATION, 0L) ?: 0L
        GlobalScope.launch(Dispatchers.IO) {
            if (id != -1L) {
                item = timersDatabase?.getDao()?.get(id)
                if (item != null) {
                    label = item?.label ?: ""
                    item?.subTimers = subTimerDatabase?.getDao()?.getByParentId(intent?.getLongExtra(IntentKey.ITEM_ID, -1) ?: -1) as ArrayList<SubTimerEntity>
                    timeUntilFinished = arrayListOf()

                    for (timer in item?.subTimers ?: arrayListOf()) {
                        timeUntilFinished.add(time + timer.time)
                        time += timer.time
                    }

                    standardTime = time * 1000L

                    withContext(Dispatchers.Main) {
                        timerStart(time * 1000L)
                        countDownTimer?.start()
                        isPaused = false

                        registerReceiver(object: BroadcastReceiver() {
                            override fun onReceive(context: Context, intent: Intent) {
                                isPaused = !isPaused
                                if (isPaused) {
                                    countDownTimer?.cancel()
                                    timerNotification?.clearActions()
                                    timerNotification?.addAction(R.drawable.ic_play_arrow, getString(R.string.resume), playPausePendingIntent)
                                    timerNotification?.setSmallIcon(R.drawable.ic_hourglass_empty)
                                    nm.notify(NotificationID.CALARM_TIMER, timerNotification?.build())
                                } else {
                                    timerStart(millisLeft)
                                    countDownTimer?.start()
                                    timerNotification?.clearActions()
                                    timerNotification?.addAction(R.drawable.ic_pause, getString(R.string.pause), playPausePendingIntent)
                                    timerNotification?.setSmallIcon(R.drawable.ic_hourglass_full)
                                    nm.notify(NotificationID.CALARM_TIMER, timerNotification?.build())
                                }
                            }
                        }, IntentFilter(IntentID.PLAY_PAUSE_TIMER))
                    }
                }
            } else if (duration != 0L) {
                withContext(Dispatchers.Main) {
                    label = ""
                    timerStart(duration)
                    countDownTimer?.start()
                    isPaused = false

                    registerReceiver(object: BroadcastReceiver() {
                        override fun onReceive(context: Context, intent: Intent) {
                            isPaused = !isPaused
                            if (isPaused) {
                                countDownTimer?.cancel()
                                timerNotification?.clearActions()
                                timerNotification?.addAction(R.drawable.ic_play_arrow, getString(R.string.resume), playPausePendingIntent)
                                timerNotification?.setSmallIcon(R.drawable.ic_hourglass_empty)
                                nm.notify(NotificationID.CALARM_TIMER, timerNotification?.build())
                            } else {
                                timerStart(millisLeft)
                                countDownTimer?.start()
                                timerNotification?.clearActions()
                                timerNotification?.addAction(R.drawable.ic_pause, getString(R.string.pause), playPausePendingIntent)
                                timerNotification?.setSmallIcon(R.drawable.ic_hourglass_full)
                                nm.notify(NotificationID.CALARM_TIMER, timerNotification?.build())
                            }
                        }
                    }, IntentFilter(IntentID.PLAY_PAUSE_TIMER))
                }
            }

        }

        NotificationCompat.Builder(applicationContext, ChannelID.COUNTDOWN_ID).apply {
            setContentTitle("")
            setContentText("")
            setSmallIcon(R.drawable.ic_hourglass_full)
            setOnlyAlertOnce(true)
            setSound(null)
            setVibrate(longArrayOf(0L))

            color = ContextCompat.getColor(applicationContext, R.color.colorAccent)

            startForeground(NotificationID.CALARM_TIMER, build())
            nm.cancel(NotificationID.CALARM_TIMER)
        }

        return super.onStartCommand(intent, flags, startId)
    }

    private fun timerStart(timerTime: Long) {
        countDownTimer = object: CountDownTimer(timerTime, 500) {
            override fun onTick(millisUntilFinished: Long) {
                millisLeft = millisUntilFinished

                if (item != null) {
                    if (timeUntilFinished[order] * 1000L < time * 1000L - millisUntilFinished) {
                        order++
                        standardTime = millisUntilFinished

                        if (order - 1 < (item?.subTimers?.lastIndex ?: -1)) { // not last item
                            NotificationCompat.Builder(applicationContext, ChannelID.COUNTDOWN_ID).apply {
                                setContentTitle("${item?.subTimers?.get(order - 1)?.label.let {
                                    if (it != "") it
                                    else {
                                        item?.subTimers?.get(order - 1)?.time.let { t ->
                                            if (t != null) {
                                                if (t / 3600 != 0) String.format("%02d:%02d:%02d", t / 3600, (t % 3600) / 60, t % 60)
                                                else String.format("%02d:%02d", (t % 3600) / 60, t % 60)
                                            } else {
                                                getString(R.string.no_label)
                                            }
                                        }
                                    }
                                }} ${getString(R.string.finished)}")
                                setContentText(getString(R.string.from_alarm_name, item?.label?.let {
                                    if (it != "") it
                                    else {
                                        time.let { t ->
                                            if (t / 3600 != 0L) String.format("%02d:%02d:%02d", t / 3600, (t % 3600) / 60, t % 60)
                                            else String.format("%02d:%02d", (t % 3600) / 60, t % 60)
                                        }
                                    }
                                }))
                                setSmallIcon(R.drawable.ic_check_circle)
                                setShowWhen(false)
                                setSound(null)
                                setVibrate(longArrayOf(0L))

                                color = ContextCompat.getColor(applicationContext, R.color.colorAccent)

                                nm.notify(NotificationID.CALARM_TIMER_SUB_FINISHED + (item?.subTimers?.get(order - 1)?.id ?: 0).toInt(), build())
                            }
                        }
                    }

                    sendBroadcast(Intent(IntentID.COUNTDOWN_TICK).apply {
                        putExtra(IntentKey.ITEM_ID, item?.id)
                        putExtra(IntentKey.DURATION, time * 1000L)
                        putExtra(IntentKey.ORDER, order)
                        putExtra(IntentKey.COUNTDOWN_TIME, millisUntilFinished)
                        putExtra(IntentKey.STANDARD_TIME, standardTime)
                    })

                    val subCountdownTime = (if (millisUntilFinished != 0L) (item?.subTimers?.get(order)?.time ?: 0) * 1000 - (standardTime.toInt() - millisUntilFinished) else 0).round(1000)
                    val countdownTimeFormatted = (millisUntilFinished.round(1000) / 1000).toInt().let {
                        if (it / 3600 != 0) String.format("%02d:%02d:%02d", it / 3600, (it % 3600) / 60, it % 60)
                        else String.format("%02d:%02d", (it % 3600) / 60, it % 60)
                    }
                    val subCountdownTimeFormatted = (subCountdownTime / 1000).toInt().let {
                        if (it / 3600 != 0) String.format("%02d:%02d:%02d", it / 3600, (it % 3600) / 60, it % 60)
                        else String.format("%02d:%02d", (it % 3600) / 60, it % 60)
                    }

                    timerNotification = NotificationCompat.Builder(applicationContext, ChannelID.COUNTDOWN_ID).apply {
                        setContentTitle("${item?.subTimers?.get(order)?.label.let { if (it != "") it else getString(R.string.no_label) }} / ${item?.label.let { if (it != "") it else getString(R.string.no_label) }}")
                        setContentText("$subCountdownTimeFormatted / $countdownTimeFormatted")
                        setSmallIcon(if (isPaused) R.drawable.ic_hourglass_empty else R.drawable.ic_hourglass_full)
                        setOngoing(true)
                        setShowWhen(false)
                        setOnlyAlertOnce(true)
                        setSound(null)
                        setVibrate(longArrayOf(0L))
                        addAction(if (isPaused) R.drawable.ic_play_arrow else R.drawable.ic_pause, getString(if (isPaused) R.string.resume else R.string.pause), playPausePendingIntent)

                        color = ContextCompat.getColor(applicationContext, R.color.colorAccent)
                    }
                } else if (duration != 0L) {
                    sendBroadcast(Intent(IntentID.COUNTDOWN_TICK).apply {
                        putExtra(IntentKey.DURATION, duration)
                        putExtra(IntentKey.COUNTDOWN_TIME, millisUntilFinished)
                    })

                    val countdownTimeFormatted = (millisUntilFinished.round(1000) / 1000).toInt().let {
                        if (it / 3600 != 0) String.format("%02d:%02d:%02d", it / 3600, (it % 3600) / 60, it % 60)
                        else String.format("%02d:%02d", (it % 3600) / 60, it % 60)
                    }

                    timerNotification = NotificationCompat.Builder(applicationContext, ChannelID.COUNTDOWN_ID).apply {
                        setContentTitle(getString(R.string.no_label)) // to time string
                        setContentText(countdownTimeFormatted)
                        setSmallIcon(if (isPaused) R.drawable.ic_hourglass_empty else R.drawable.ic_hourglass_full)
                        setOngoing(true)
                        setShowWhen(false)
                        setOnlyAlertOnce(true)
                        setSound(null)
                        setVibrate(longArrayOf(0L))
                        addAction(if (isPaused) R.drawable.ic_play_arrow else R.drawable.ic_pause, getString(if (isPaused) R.string.resume else R.string.pause), playPausePendingIntent)

                        color = ContextCompat.getColor(applicationContext, R.color.colorAccent)
                    }
                }

                nm.notify(NotificationID.CALARM_TIMER, timerNotification?.build())
            }

            override fun onFinish() {
                if (item != null) {
                    sendBroadcast(Intent(IntentID.COUNTDOWN_TICK).apply {
                        putExtra(IntentKey.ITEM_ID, item?.id)
                        putExtra(IntentKey.DURATION, time * 1000L)
                        putExtra(IntentKey.ORDER, order)
                        putExtra(IntentKey.COUNTDOWN_TIME, 0)
                        putExtra(IntentKey.STANDARD_TIME, standardTime)
                        putExtra(IntentKey.IS_FINISHED, true)
                    })

                    time = 0
                    order = 0
                    timeUntilFinished.clear()

                    val fullScreenPendingIntent = PendingIntent.getActivity(applicationContext, NotificationID.CALARM_TIMER_FINISHED, Intent(applicationContext, TimerRingActivity::class.java).apply {
                        action = "fullscreen_activity"
                        this.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        putExtra(IntentKey.ITEM_ID, item?.id)
                    }, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

                    var timeoutTime = 0

                    val timerTask = object: TimerTask() {
                        override fun run() {
                            sendBroadcast(Intent(IntentID.COUNTDOWN_TICK_TIMEOUT).apply {
                                putExtra(IntentKey.COUNTDOWN_TIME, ++timeoutTime)
                            })

                            val pendingIntent = PendingIntent.getBroadcast(applicationContext, 0, Intent(applicationContext, TimerOffReceiver::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                            val builder = NotificationCompat.Builder(applicationContext, ChannelID.COUNTDOWN_ID).apply {
                                setSmallIcon(R.drawable.ic_hourglass_half)
                                setContentTitle("-${timeoutTime.let {
                                    if (it / 3600 != 0) String.format("%02d:%02d:%02d", it / 3600, (it % 3600) / 60, it % 60)
                                    else String.format("%02d:%02d", (it % 3600) / 60, it % 60)
                                }}")
                                setContentText(getString(R.string.time_is_up))
                                setAutoCancel(false)
                                setOngoing(true)
                                setDefaults(Notification.DEFAULT_LIGHTS or Notification.DEFAULT_VIBRATE)
                                setCategory(NotificationCompat.CATEGORY_ALARM)
                                setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                                setLocalOnly(true)
                                setOnlyAlertOnce(true)
                                setShowWhen(false)
                                setSound(null)
                                setVibrate(longArrayOf(0L))
                                priority = NotificationCompat.PRIORITY_MAX
                                setContentIntent(fullScreenPendingIntent)
                                setFullScreenIntent(fullScreenPendingIntent, true)
                                addAction(R.drawable.ic_close, getString(R.string.close), pendingIntent)

                                color = ContextCompat.getColor(applicationContext, R.color.colorAccent)
                            }


                            nm.notify(NotificationID.CALARM_TIMER_FINISHED, builder.build())

                        }
                    }

                    timer = Timer()
                    timer?.schedule(timerTask, 0, 1000)
                } else if (duration != 0L) {
                    sendBroadcast(Intent(IntentID.COUNTDOWN_TICK).apply {
                        putExtra(IntentKey.DURATION, duration)
                        putExtra(IntentKey.COUNTDOWN_TIME, 0)
                        putExtra(IntentKey.IS_FINISHED, true)
                    })

                    val fullScreenPendingIntent = PendingIntent.getActivity(applicationContext, NotificationID.CALARM_TIMER_FINISHED, Intent(applicationContext, TimerRingActivity::class.java).apply {
                        action = "fullscreen_activity"
                        this.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

                    var timeoutTime = 0

                    val timerTask = object: TimerTask() {
                        override fun run() {
                            sendBroadcast(Intent(IntentID.COUNTDOWN_TICK_TIMEOUT).apply {
                                putExtra(IntentKey.COUNTDOWN_TIME, ++timeoutTime)
                            })


                            val pendingIntent = PendingIntent.getBroadcast(applicationContext, 0, Intent(applicationContext, TimerOffReceiver::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                            val builder = NotificationCompat.Builder(applicationContext, ChannelID.COUNTDOWN_ID).apply {
                                setSmallIcon(R.drawable.ic_hourglass_half)
                                setContentTitle("-${timeoutTime.let {
                                    if (it / 3600 != 0) String.format("%02d:%02d:%02d", it / 3600, (it % 3600) / 60, it % 60)
                                    else String.format("%02d:%02d", (it % 3600) / 60, it % 60)
                                }}")
                                setContentText(getString(R.string.time_is_up))
                                setAutoCancel(false)
                                setOngoing(true)
                                setDefaults(Notification.DEFAULT_LIGHTS or Notification.DEFAULT_VIBRATE)
                                setCategory(NotificationCompat.CATEGORY_ALARM)
                                setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                                setLocalOnly(true)
                                setOnlyAlertOnce(true)
                                setShowWhen(false)
                                setSound(null)
                                setVibrate(longArrayOf(0L))
                                priority = NotificationCompat.PRIORITY_MAX
                                setContentIntent(fullScreenPendingIntent)
                                setFullScreenIntent(fullScreenPendingIntent, true)
                                addAction(R.drawable.ic_close, getString(R.string.close), pendingIntent)

                                color = ContextCompat.getColor(applicationContext, R.color.colorAccent)
                            }


                            nm.notify(NotificationID.CALARM_TIMER_FINISHED, builder.build())

                        }
                    }

                    timer = Timer()
                    timer?.schedule(timerTask, 0, 1000)
                }
            }
        }
    }

    companion object {
        var isRunning = false
        var isPaused = false
    }


}