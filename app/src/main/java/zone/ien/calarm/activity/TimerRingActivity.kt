package zone.ien.calarm.activity

import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.databinding.DataBindingUtil
import kotlinx.coroutines.*
import zone.ien.calarm.R
import zone.ien.calarm.constant.IntentID
import zone.ien.calarm.constant.IntentKey
import zone.ien.calarm.databinding.ActivityAlarmRingBinding
import zone.ien.calarm.databinding.ActivityTimerRingBinding
import zone.ien.calarm.receiver.AlarmOffReceiver
import zone.ien.calarm.receiver.AlarmSnoozeReceiver
import zone.ien.calarm.receiver.TimerOffReceiver
import zone.ien.calarm.room.AlarmDatabase
import zone.ien.calarm.room.SubAlarmDatabase
import zone.ien.calarm.room.TimersDatabase
import java.text.SimpleDateFormat
import java.util.*

class TimerRingActivity : AppCompatActivity() {

    lateinit var binding: ActivityTimerRingBinding
    private var timersDatabase: TimersDatabase? = null

    @OptIn(DelicateCoroutinesApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_timer_ring)
        binding.activity = this

        timersDatabase = TimersDatabase.getInstance(this)

        turnScreenOnAndKeyguardOff()

        val id = intent.getLongExtra(IntentKey.ITEM_ID, -1)

        binding.bgTimerOff.setOnClickListener {
            sendBroadcast(Intent(this, TimerOffReceiver::class.java).apply {
                putExtra(IntentKey.ITEM_ID, id)
            })
            finish()
        }

        registerReceiver(object: BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                binding.tvTime.text = "-${intent.getIntExtra(IntentKey.COUNTDOWN_TIME, 0).let {
                    if (it / 3600 != 0) String.format("%02d:%02d:%02d", it / 3600, (it % 3600) / 60, it % 60)
                    else if (it / 60 != 0) String.format("%02d:%02d", (it % 3600) / 60, it % 60)
                    else it.toString()
                }}"
            }
        }, IntentFilter(IntentID.COUNTDOWN_TICK_TIMEOUT))

    }

    private fun turnScreenOnAndKeyguardOff(){
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                or WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON)
        val keyguardMgr = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        keyguardMgr.requestDismissKeyguard(this, null)
    }
}