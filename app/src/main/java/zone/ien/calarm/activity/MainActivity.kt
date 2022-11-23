package zone.ien.calarm.activity

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.AlarmClock
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.google.android.material.timepicker.MaterialTimePicker
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import zone.ien.calarm.R
import zone.ien.calarm.adapter.MainCalarmDateAdapter
import zone.ien.calarm.adapter.MainTimerPageAdapter
import zone.ien.calarm.constant.ActionKey
import zone.ien.calarm.constant.ChannelID
import zone.ien.calarm.constant.IntentKey
import zone.ien.calarm.databinding.ActivityMainBinding
import zone.ien.calarm.fragment.*
import zone.ien.calarm.receiver.AlarmReceiver
import zone.ien.calarm.room.AlarmDatabase
import zone.ien.calarm.room.SubAlarmDatabase
import zone.ien.calarm.room.SubAlarmEntity
import zone.ien.calarm.utils.MyUtils
import java.util.*
import kotlin.collections.ArrayList


const val TAG = "CalarmTAG"

class MainActivity : AppCompatActivity(),
    MainAlarmFragment.OnFragmentInteractionListener,
    MainCalarmFragment.OnFragmentInteractionListener,
    MainTimerFragment.OnFragmentInteractionListener,
    MainStopwatchFragment.OnFragmentInteractionListener,
    MainTimerListFragment.OnFragmentInteractionListener,
    MainTimerClockFragment.OnFragmentInteractionListener,
    MainTimerNumFragment.OnFragmentInteractionListener {

    lateinit var binding: ActivityMainBinding
    private var alarmDatabase: AlarmDatabase? = null
    private var subAlarmDatabase: SubAlarmDatabase? = null

    lateinit var am: AlarmManager
    lateinit var nm: NotificationManager

    @OptIn(DelicateCoroutinesApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        binding.activity = this

        alarmDatabase = AlarmDatabase.getInstance(this)
        subAlarmDatabase = SubAlarmDatabase.getInstance(this)
        am = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        nm.createNotificationChannel(NotificationChannel(ChannelID.DEFAULT_ID, getString(R.string.calarm_alarm), NotificationManager.IMPORTANCE_HIGH))


        loadFragment(MainAlarmFragment())

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 0)
        }

        binding.bottomNav.setOnItemSelectedListener {
            loadFragment(
                when (it.itemId) {
                    R.id.navigation_alarm -> MainAlarmFragment()
                    R.id.navigation_calarm -> MainCalarmFragment()
                    R.id.navigation_timer -> MainTimerFragment()
                    R.id.navigation_stopwatch -> MainStopwatchFragment()
                    else -> MainAlarmFragment()
                }
            )
        }

        // Alarm Activity
        GlobalScope.launch(Dispatchers.IO) {
            val alarms = alarmDatabase?.getDao()?.getAll()
            if (alarms != null) {
                for (alarm in alarms) {
                    val subAlarms = subAlarmDatabase?.getDao()?.getByParentId(alarm.id ?: -1)
                    alarm.subAlarms = subAlarms as ArrayList<SubAlarmEntity>
                }

                for (alarm in alarms) {
                    if (alarm.isEnabled) {
                        MyUtils.setAlarmClock(applicationContext, am, alarm)
                    }
                }
            }
        }


    }

    private fun loadFragment(fragment: Fragment?): Boolean {
        if (fragment != null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.container, fragment)
                .commit()
            return true
        }
        return false
    }

    override fun onFragmentInteraction(uri: Uri) {}
}