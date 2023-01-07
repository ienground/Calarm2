package zone.ien.calarm.activity

import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import kotlinx.coroutines.*
import zone.ien.calarm.R
import zone.ien.calarm.callback.SwipeAnimationListener
import zone.ien.calarm.constant.IntentID
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

        binding.tvApm.visibility = if (Locale.getDefault() == Locale.KOREA) View.GONE else View.VISIBLE
        binding.tvApmKo.visibility = if (Locale.getDefault() != Locale.KOREA) View.GONE else View.VISIBLE
        binding.tvApmParent.visibility = if (Locale.getDefault() == Locale.KOREA) View.GONE else View.VISIBLE
        binding.tvApmKoParent.visibility = if (Locale.getDefault() != Locale.KOREA) View.GONE else View.VISIBLE

        val apmFormat = SimpleDateFormat("a", Locale.getDefault())
        val timeFormat = SimpleDateFormat("hh:mm", Locale.getDefault())
        val dateFormat = SimpleDateFormat(getString(R.string.dateFormatNoYear), Locale.getDefault())

        binding.tvDate.text = dateFormat.format(Date(System.currentTimeMillis()))
        binding.tvTime.text = timeFormat.format(Date(System.currentTimeMillis()))
        binding.tvApm.text = apmFormat.format(Date(System.currentTimeMillis()))
        binding.tvApmKo.text = apmFormat.format(Date(System.currentTimeMillis()))

        val id = intent.getLongExtra(IntentKey.ITEM_ID, -1)
        val subAlarmId = intent.getLongExtra(IntentKey.SUBALARM_ID, -1)

        binding.cardParent.visibility = if (subAlarmId == -1L) View.GONE else View.VISIBLE

        if (subAlarmId != -1L) {
            binding.slider.setRightSwipeText("")
            binding.slider.setRightSwipeEnabled(false)
        }

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
                        binding.tvApmParent.text = apmFormat.format(calendar.time)
                        binding.tvApmKoParent.text = apmFormat.format(calendar.time)
                        binding.tvTimeParent.text = timeFormat.format(calendar.time)

                        val timeDiffArray: ArrayList<String> = arrayListOf()
                        if (initialGap / 60 == 1) timeDiffArray.add(getString(R.string.time_format_1hour))
                        else if (initialGap / 60 != 0) timeDiffArray.add(getString(R.string.time_format_hour, initialGap / 60))
                        if (initialGap % 60 == 1) timeDiffArray.add(getString(R.string.time_format_1minute))
                        else if (initialGap % 60 != 0) timeDiffArray.add(getString(R.string.time_format_minute, initialGap % 60))

                        binding.tvTimeDiff.text = getString(R.string.time_format_before, timeDiffArray.joinToString(" "))

                        binding.slider.setOnSwipeAnimationListener(object: SwipeAnimationListener {
                            override fun onSwiped(isRight: Boolean) {
                                if (isRight) {
                                    sendBroadcast(snoozeIntent); finish()
                                } else {
                                    sendBroadcast(offIntent); finish()
                                }
                            }
                        })

                        registerReceiver(object : BroadcastReceiver() {
                            override fun onReceive(context: Context, intent: Intent) {
                                val gap = data.time - Calendar.getInstance().let { it.get(Calendar.HOUR_OF_DAY) * 60 + it.get(Calendar.MINUTE) }
                                binding.tvTime.text = timeFormat.format(Date(System.currentTimeMillis()))
                                binding.tvApm.text = apmFormat.format(Date(System.currentTimeMillis()))
                                binding.tvApmKo.text = apmFormat.format(Date(System.currentTimeMillis()))

                                timeDiffArray.clear()
                                if (gap / 60 == 1) timeDiffArray.add(getString(R.string.time_format_1hour))
                                else if (gap / 60 != 0) timeDiffArray.add(getString(R.string.time_format_hour, gap / 60))
                                if (gap % 60 == 1) timeDiffArray.add(getString(R.string.time_format_1minute))
                                else if (gap % 60 != 0) timeDiffArray.add(getString(R.string.time_format_minute, gap % 60))

                                binding.tvTimeDiff.text = getString(R.string.time_format_before, timeDiffArray.joinToString(" "))
                            }
                        }, IntentFilter(Intent.ACTION_TIME_TICK))
                    }
                }
            }
        }

        LocalBroadcastManager.getInstance(applicationContext).registerReceiver(object: BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                finish()
            }
        }, IntentFilter(IntentID.STOP_ALARM))

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