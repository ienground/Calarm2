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
import zone.ien.calarm.constant.IntentKey
import zone.ien.calarm.databinding.ActivityAlarmRingBinding
import zone.ien.calarm.receiver.AlarmOffReceiver
import zone.ien.calarm.receiver.AlarmSnoozeReceiver
import zone.ien.calarm.room.AlarmDatabase
import zone.ien.calarm.room.SubAlarmDatabase
import java.text.SimpleDateFormat
import java.util.*

class AlarmRingActivity : AppCompatActivity() {

    lateinit var binding: ActivityAlarmRingBinding
    private var alarmDatabase: AlarmDatabase? = null
    private var subAlarmDatabase: SubAlarmDatabase? = null

    @OptIn(DelicateCoroutinesApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_alarm_ring)
        binding.activity = this

        alarmDatabase = AlarmDatabase.getInstance(this)
        subAlarmDatabase = SubAlarmDatabase.getInstance(this)

        turnScreenOnAndKeyguardOff()

        val apmFormat = SimpleDateFormat("a", Locale.getDefault())
        val timeFormat = SimpleDateFormat("hh\nmm", Locale.getDefault())
        val timeFormat2 = SimpleDateFormat("hh:mm", Locale.getDefault())

        binding.tvTime.text = timeFormat.format(Date(System.currentTimeMillis()))
        binding.tvApm.text = apmFormat.format(Date(System.currentTimeMillis()))

        val id = intent.getLongExtra(IntentKey.ITEM_ID, -1)
        val subAlarmId = intent.getLongExtra(IntentKey.SUBALARM_ID, -1)

        binding.bgSnooze.visibility = if (subAlarmId != -1L) View.GONE else View.VISIBLE
        binding.icSnooze.visibility = if (subAlarmId != -1L) View.GONE else View.VISIBLE
        binding.cardParent.visibility = if (subAlarmId == -1L) View.GONE else View.VISIBLE

        if (id != -1L) {
            GlobalScope.launch(Dispatchers.IO) {
                val data = alarmDatabase?.getDao()?.get(id)
                if (data != null) {
                    val snoozeIntent = Intent(applicationContext, AlarmSnoozeReceiver::class.java).apply {
                        putExtra(IntentKey.ITEM_ID, id)
                    }
                    val offIntent = Intent(applicationContext, AlarmOffReceiver::class.java).apply {
                        putExtra(IntentKey.ITEM_ID, id)
                        putExtra(IntentKey.SUBALARM_ID, subAlarmId)
                    }
                    val calendar = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, data.time / 60)
                        set(Calendar.MINUTE, data.time % 60)
                    }

                    withContext(Dispatchers.Main) {
                        val initialGap = data.time - Calendar.getInstance().let { it.get(Calendar.HOUR_OF_DAY) * 60 + it.get(Calendar.MINUTE) }

                        binding.tvLabel.text = data.label
                        binding.tvParentApm.text = apmFormat.format(calendar.time)
                        binding.tvParentTime.text = timeFormat2.format(calendar.time)
                        binding.tvTimeDiff.text = if (initialGap / 60 != 0 && initialGap % 60 != 0) getString(R.string.time_format_before_hour_minute, initialGap / 60, initialGap % 60)
                        else if (initialGap % 60 == 0) getString(R.string.time_format_before_hour, initialGap / 60)
                        else getString(R.string.time_format_before_minute, initialGap % 60)

                        binding.bgSnooze.setOnClickListener { sendBroadcast(snoozeIntent); finish() }
                        binding.bgAlarmOff.setOnClickListener { sendBroadcast(offIntent); finish() }

                        registerReceiver(object : BroadcastReceiver() {
                            override fun onReceive(context: Context, intent: Intent) {
                                val gap = data.time - Calendar.getInstance().let { it.get(Calendar.HOUR_OF_DAY) * 60 + it.get(Calendar.MINUTE) }
                                binding.tvTime.text = timeFormat.format(Date(System.currentTimeMillis()))
                                binding.tvApm.text = apmFormat.format(Date(System.currentTimeMillis()))
                                binding.tvTimeDiff.text = if (gap / 60 != 0 && gap % 60 != 0) context.getString(R.string.time_format_before_hour_minute, gap / 60, gap % 60)
                                else if (gap % 60 == 0) context.getString(R.string.time_format_before_hour, gap / 60)
                                else context.getString(R.string.time_format_before_minute, gap % 60)
                            }
                        }, IntentFilter(Intent.ACTION_TIME_TICK))
                    }
                }
            }
        }

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