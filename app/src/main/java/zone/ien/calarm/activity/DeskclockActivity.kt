package zone.ien.calarm.activity

import android.animation.ValueAnimator
import android.app.AlertDialog
import android.app.Notification
import android.app.NotificationManager
import android.content.*
import android.graphics.Typeface
import android.media.AudioManager
import android.os.*
import android.provider.Settings
import android.service.notification.StatusBarNotification
import android.view.KeyEvent
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.NotificationCompat
import androidx.databinding.DataBindingUtil
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import zone.ien.calarm.R
import zone.ien.calarm.adapter.NotificationAdapter
import zone.ien.calarm.callback.ItemTouchHelperCallback
import zone.ien.calarm.callback.NotificationCallback
import zone.ien.calarm.constant.IntentKey
import zone.ien.calarm.constant.SavedInstanceExtra
import zone.ien.calarm.databinding.ActivityDeskclockBinding
import zone.ien.calarm.service.NotificationListener
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList


class DeskclockActivity : AppCompatActivity() {

    lateinit var binding: ActivityDeskclockBinding

    lateinit var sharedPreferences: SharedPreferences
    lateinit var notiChannelFilterPreferences: SharedPreferences
    lateinit var nm: NotificationManager
    lateinit var am: AudioManager

    private val apmFormat = SimpleDateFormat("a", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("h:mm", Locale.getDefault())
    private lateinit var dateFormat: SimpleDateFormat
    private lateinit var adapter: NotificationAdapter

    private val callbackListener = object: NotificationCallback {
        override fun click(position: Int, sbn: StatusBarNotification) {

        }

        override fun longClick(position: Int, sbn: StatusBarNotification) {
            notiChannelFilterPreferences.edit().putBoolean("${sbn.packageName}☆${sbn.notification.channelId}", false).apply()
            for (i in adapter.items.size - 1 downTo 0) {
                if ("${sbn.packageName}☆${sbn.notification.channelId}" == "${adapter.items[i].packageName}☆${adapter.items[i].notification.channelId}") {
                    adapter.delete(adapter.items[i].id)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_deskclock)
        binding.activity = this

        sharedPreferences = getSharedPreferences("${packageName}_preferences", Context.MODE_PRIVATE)
        notiChannelFilterPreferences = getSharedPreferences("${packageName}_noti_channel", Context.MODE_PRIVATE)

        if (savedInstanceState != null) {
            binding.tvMediaTitle.text = savedInstanceState.getString(SavedInstanceExtra.TITLE)
            binding.tvMediaArtist.text = savedInstanceState.getString(SavedInstanceExtra.ARTIST)
        } else {
            NotificationListener.activeNotificationId.clear()
            NotificationListener.activeNotification.clear()
        }

        if (!isNotificationPermissionGranted()) {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }

        nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        dateFormat = SimpleDateFormat(getString(R.string.dateFormat), Locale.getDefault())

        binding.progressVolume.max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        binding.progressVolume.progress = am.getStreamVolume(AudioManager.STREAM_MUSIC)

        val valueAnimator = ValueAnimator.ofFloat(1f, 0.6f).apply {
            duration = 500
            addUpdateListener {
                binding.guideline1.setGuidelinePercent(it.animatedValue as Float)
            }
        }
        val valueAnimator2 = ValueAnimator.ofFloat(0.6f, 1f).apply {
            duration = 500
            addUpdateListener {
                binding.guideline1.setGuidelinePercent(it.animatedValue as Float)
            }
        }
        val newNotiAlphaAnimationOn = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 300
            addUpdateListener {
                binding.icNewNoti.alpha = it.animatedValue as Float
            }
        }
        val newNotiAlphaAnimationOff = ValueAnimator.ofFloat(1f, 0f).apply {
            duration = 300
            addUpdateListener {
                binding.icNewNoti.alpha = it.animatedValue as Float
                if (it.animatedValue as Float == 0f) {
                    binding.icNewNoti.visibility = View.GONE
                }
            }
        }

        hideSystemUI()

        window.addFlags(FLAG_KEEP_SCREEN_ON)

        binding.btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        adapter = NotificationAdapter(arrayListOf()).apply { setClickCallback(callbackListener) }
        val itemTouchHelper = ItemTouchHelper(ItemTouchHelperCallback(adapter))
        itemTouchHelper.attachToRecyclerView(binding.recyclerView)
        binding.recyclerView.adapter = adapter
        binding.recyclerView.layoutManager = LinearLayoutManager(this)

        binding.guideline1.setGuidelinePercent(1f)
        binding.icNewNoti.setOnClickListener {
            binding.recyclerView.smoothScrollToPosition(0)
            newNotiAlphaAnimationOff.start()
        }

        if (savedInstanceState != null) {
            if (NotificationListener.activeNotification.isNotEmpty()) {
                if (adapter.isEmpty()) {
                    valueAnimator.start()
                }
                for (notification in NotificationListener.activeNotification) {
                    if (notiChannelFilterPreferences.getBoolean("${notification.packageName}☆${notification.notification.channelId}", true) && notification.id != Int.MAX_VALUE) {
                        adapter.add(notification)
                    }
                }
                binding.recyclerView.smoothScrollToPosition(0)
            }
        }

        LocalBroadcastManager.getInstance(this).registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (NotificationListener.activeNotification.isNotEmpty()) {
                    if (adapter.isEmpty()) {
                        valueAnimator.start()
                    }
                    for (notification in NotificationListener.activeNotification) {
                        if (notiChannelFilterPreferences.getBoolean("${notification.packageName}☆${notification.notification.channelId}", true) && notification.id != Int.MAX_VALUE) {
                            adapter.add(notification)
                        }
                    }
                    binding.recyclerView.smoothScrollToPosition(0)
                }
            }
        }, IntentFilter(IntentKey.NOTIFICATION_INIT))

        Handler(Looper.getMainLooper()).postDelayed({
            if (NotificationListener.activeNotification.isNotEmpty()) {
                if (adapter.isEmpty()) {
                    valueAnimator.start()
                }
                for (notification in NotificationListener.activeNotification) {
                    if (notiChannelFilterPreferences.getBoolean("${notification.packageName}☆${notification.notification.channelId}", true) && notification.id != Int.MAX_VALUE) {
                        adapter.add(notification)
                    }
                }
                binding.recyclerView.smoothScrollToPosition(0)
            }
        }, 5000)

        LocalBroadcastManager.getInstance(this).registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (NotificationListener.addedNotification != null) {
                    if (adapter.isEmpty()) {
                        valueAnimator.start()
                    }

                    NotificationListener.addedNotification.let {
                        if (it != null) {
                            if (notiChannelFilterPreferences.getBoolean("${it.packageName}☆${it.notification.channelId}", true) && it.id != Int.MAX_VALUE) {
                                adapter.add(it)

                                if (binding.recyclerView.computeVerticalScrollOffset() == 0) {
                                    binding.recyclerView.scrollToPosition(0)
                                } else {
                                    binding.icNewNoti.setImageDrawable(NotificationListener.addedNotification!!.notification.smallIcon.loadDrawable(applicationContext))
                                    binding.icNewNoti.alpha = 0f
                                    binding.icNewNoti.visibility = View.VISIBLE
                                    newNotiAlphaAnimationOn.start()

                                    Handler(Looper.getMainLooper()).postDelayed({
                                        newNotiAlphaAnimationOff.start()
                                    }, 5000)
                                }
                            }
                        }
                    }


                }
            }
        }, IntentFilter(IntentKey.NOTIFICATION_ADD))

        LocalBroadcastManager.getInstance(this).registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (NotificationListener.addedNotification != null) {
                    val success = adapter.delete(NotificationListener.removeNotificationId)
                    if (adapter.isEmpty() && success) {
                        valueAnimator2.start()
                    }
                }
            }
        }, IntentFilter(IntentKey.NOTIFICATION_REMOVE))

        val batteryIntentFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        batteryIntentFilter.addAction(Intent.ACTION_POWER_CONNECTED)
        batteryIntentFilter.addAction(Intent.ACTION_POWER_DISCONNECTED)

        registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val chargePlug = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
                val usbCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_USB
                val acCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_AC
                val batteryLevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)

                binding.tvBattery.text = if (usbCharge || acCharge) getString(R.string.charging, batteryLevel) else "${batteryLevel}%"
                binding.icBattery.setImageResource(getBatteryDrawable(batteryLevel, usbCharge || acCharge))
            }
        }, batteryIntentFilter)

        LocalBroadcastManager.getInstance(this).registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (NotificationListener.mediaNotification != null) {
                    val extras = NotificationListener.mediaNotification?.notification?.extras
                    binding.tvMediaTitle.text = extras?.getString(Notification.EXTRA_TITLE)
                    binding.tvMediaArtist.text = extras?.getString(Notification.EXTRA_TEXT)

                    Handler(Looper.getMainLooper()).postDelayed({
                        binding.btnPlayPause.setImageResource(if (am.isMusicActive) R.drawable.ic_media_pause else R.drawable.ic_media_play)
                    }, 100)
                }
            }
        }, IntentFilter(IntentKey.MEDIA_CHANGE))

        binding.btnPlayPause.setImageResource(if (am.isMusicActive) R.drawable.ic_media_pause else R.drawable.ic_media_play)
        binding.btnPlayPause.setOnClickListener { sendButton(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) }
        binding.btnMediaPrev.setOnClickListener { sendButton(KeyEvent.KEYCODE_MEDIA_PREVIOUS) }
        binding.btnMediaNext.setOnClickListener { sendButton(KeyEvent.KEYCODE_MEDIA_NEXT) }
        binding.btnVolumeDown.setOnClickListener {
            am.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, 0)
            binding.progressVolume.setProgress(am.getStreamVolume(AudioManager.STREAM_MUSIC), true)
        }
        binding.btnVolumeUp.setOnClickListener {
            am.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, 0)
            binding.progressVolume.setProgress(am.getStreamVolume(AudioManager.STREAM_MUSIC), true)
        }

        val time = Date(System.currentTimeMillis())
        binding.tvApm.text = apmFormat.format(time)
        binding.tvTime.text = timeFormat.format(time)
        binding.tvDate.text = dateFormat.format(time)

        registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val time2 = Date(System.currentTimeMillis())
                binding.tvApm.text = apmFormat.format(time2)
                binding.tvTime.text = timeFormat.format(time2)
                binding.tvDate.text = dateFormat.format(time2)
            }
        }, IntentFilter(Intent.ACTION_TIME_TICK))
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(SavedInstanceExtra.TITLE, binding.tvMediaTitle.text.toString())
        outState.putString(SavedInstanceExtra.ARTIST, binding.tvMediaArtist.text.toString())
    }

    private fun sendButton(keycode: Int) {
        val downEvent = KeyEvent(KeyEvent.ACTION_DOWN, keycode)
        am.dispatchMediaKeyEvent(downEvent)
        val upEvent = KeyEvent(KeyEvent.ACTION_UP, keycode)
        am.dispatchMediaKeyEvent(upEvent)
    }

    private fun hideSystemUI() {
        supportActionBar?.hide()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
            val controller = window.insetsController
            if (controller != null) {
                controller.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE
                    // Set the content to appear under the system bars so that the
                    // content doesn't resize when the system bars hide and show.
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    // Hide the nav bar and status bar
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN)
        }
    }

    private fun showSystemUI() {
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
    }

    private fun isNotificationPermissionGranted(): Boolean {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        return notificationManager.isNotificationListenerAccessGranted(ComponentName(application, NotificationListener::class.java))
    }

    private fun getBatteryDrawable(battery: Int, isCharging: Boolean): Int {
        return if (isCharging) {
            when {
                battery >= 95 -> R.drawable.ic_battery_charging_full
                battery >= 85 -> R.drawable.ic_battery_charging_90
                battery >= 70 -> R.drawable.ic_battery_charging_80
                battery >= 55 -> R.drawable.ic_battery_charging_60
                battery >= 40 -> R.drawable.ic_battery_charging_50
                battery >= 25 -> R.drawable.ic_battery_charging_30
                battery >= 15 -> R.drawable.ic_battery_charging_20
                battery >= 0 -> R.drawable.ic_battery_charging_20
                else -> R.drawable.ic_battery_unknown
            }
        } else {
            when {
                battery >= 95 -> R.drawable.ic_battery_full
                battery >= 85 -> R.drawable.ic_battery_90
                battery >= 70 -> R.drawable.ic_battery_80
                battery >= 55 -> R.drawable.ic_battery_60
                battery >= 40 -> R.drawable.ic_battery_50
                battery >= 25 -> R.drawable.ic_battery_30
                battery >= 15 -> R.drawable.ic_battery_20
                battery >= 0 -> R.drawable.ic_battery_alert
                else -> R.drawable.ic_battery_unknown
            }
        }
    }
}