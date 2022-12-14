package zone.ien.calarm.activity

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.CalendarContract
import android.util.Log
import android.util.TypedValue
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.android.material.color.DynamicColors
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import zone.ien.calarm.R
import zone.ien.calarm.constant.ChannelID
import zone.ien.calarm.constant.SharedDefault
import zone.ien.calarm.constant.SharedKey
import zone.ien.calarm.data.CalendarEvent
import zone.ien.calarm.databinding.ActivityMainBinding
import zone.ien.calarm.fragment.*
import zone.ien.calarm.receiver.CalarmCreateReceiver
import zone.ien.calarm.room.*
import zone.ien.calarm.service.NotificationListener
import zone.ien.calarm.utils.MyUtils
import zone.ien.calarm.utils.MyUtils.Companion.getSafeLong
import zone.ien.calarm.utils.MyUtils.Companion.getSafeString
import zone.ien.calarm.utils.MyUtils.Companion.timeZero
import java.net.URLEncoder
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
    private var calarmDatabase: CalarmDatabase? = null
    private var subCalarmDatabase: SubCalarmDatabase? = null

    lateinit var am: AlarmManager
    lateinit var nm: NotificationManager
    lateinit var lm: LocationManager
    lateinit var sharedPreferences: SharedPreferences

    @OptIn(DelicateCoroutinesApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        binding.activity = this

        alarmDatabase = AlarmDatabase.getInstance(this)
        subAlarmDatabase = SubAlarmDatabase.getInstance(this)
        calarmDatabase = CalarmDatabase.getInstance(this)
        subCalarmDatabase = SubCalarmDatabase.getInstance(this)
        am = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        lm = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        sharedPreferences = getSharedPreferences("${packageName}_preferences", Context.MODE_PRIVATE)

        nm.createNotificationChannel(NotificationChannel(ChannelID.DEFAULT_ID, getString(R.string.app_name), NotificationManager.IMPORTANCE_HIGH))
        nm.createNotificationChannel(NotificationChannel(ChannelID.ALARM_ID, getString(R.string.calarm_alarm), NotificationManager.IMPORTANCE_HIGH).apply {
            vibrationPattern = longArrayOf(0L)
            enableVibration(true)
        })
        nm.createNotificationChannel(NotificationChannel(ChannelID.CALARM_ID, getString(R.string.calarm_calendar), NotificationManager.IMPORTANCE_HIGH).apply {
            vibrationPattern = longArrayOf(0L)
            enableVibration(true)
        })

        loadFragment(MainAlarmFragment())

        val permissions = arrayListOf(Manifest.permission.READ_CALENDAR, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val locationListener = LocationListener { location ->
            if (sharedPreferences.getBoolean(SharedKey.IS_FIRST_VISIT, true)) {
                sharedPreferences.edit().putFloat(SharedKey.HOME_LATITUDE, location.latitude.toFloat()).apply()
                sharedPreferences.edit().putFloat(SharedKey.HOME_LONGITUDE, location.longitude.toFloat()).apply()
                sharedPreferences.edit().putBoolean(SharedKey.IS_FIRST_VISIT, false).apply()
            }
        }

        val calarmCreateCalendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 5)
        }
        val calarmCreatePendingIntent = PendingIntent.getBroadcast(applicationContext, 0, Intent(applicationContext, CalarmCreateReceiver::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        am.setInexactRepeating(AlarmManager.RTC_WAKEUP, calarmCreateCalendar.timeInMillis, AlarmManager.INTERVAL_DAY, calarmCreatePendingIntent) //        binding.navigationRail?.setOnLongClickListener {
        //            sendBroadcast(Intent(applicationContext, CalarmCreateReceiver::class.java))
        //            true
        //        }

        val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { group ->
            group.forEach {
                when (it.key) {
                    Manifest.permission.READ_CALENDAR -> {}
                    Manifest.permission.POST_NOTIFICATIONS -> {}
                    Manifest.permission.ACCESS_FINE_LOCATION -> {
                        if (sharedPreferences.getBoolean(SharedKey.IS_FIRST_VISIT, true)) {
                            val isGPSEnabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
                            val isNetworkEnabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

                            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                                requestPermissions(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, android.Manifest.permission.ACCESS_FINE_LOCATION), 0)
                            } else {
                                if (isNetworkEnabled) {
                                    lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 10f, locationListener)
                                } else if (isGPSEnabled) {
                                    lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 10f, locationListener)
                                }
                            }
                        }
                    }
                }
            }
        }
        permissionLauncher.launch(permissions.toTypedArray())

        binding.bottomNav?.setOnItemSelectedListener {
            loadFragment(when (it.itemId) {
                R.id.navigation_alarm -> MainAlarmFragment()
                R.id.navigation_calarm -> MainCalarmFragment()
                R.id.navigation_timer -> MainTimerFragment()
                R.id.navigation_stopwatch -> MainStopwatchFragment()
                else -> MainAlarmFragment()
            })
        }
        binding.navigationRail?.setOnItemSelectedListener {
            loadFragment(when (it.itemId) {
                R.id.navigation_alarm -> MainAlarmFragment()
                R.id.navigation_calarm -> MainCalarmFragment()
                R.id.navigation_timer -> MainTimerFragment()
                R.id.navigation_stopwatch -> MainStopwatchFragment()
                else -> MainAlarmFragment()
            })
        }

        binding.bottomNav?.setOnItemReselectedListener { }
        binding.navigationRail?.setOnItemReselectedListener { }

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

//        startActivity(Intent(this, AlarmRingActivity::class.java))
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