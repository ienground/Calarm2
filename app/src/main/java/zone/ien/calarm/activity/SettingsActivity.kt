package zone.ien.calarm.activity

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.location.Address
import android.location.Geocoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.CalendarContract
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.preference.DropDownPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import zone.ien.calarm.R
import zone.ien.calarm.adapter.ChannelIdAdapter
import zone.ien.calarm.adapter.SelectCalendarParentAdapter
import zone.ien.calarm.callback.ChannelIdCallback
import zone.ien.calarm.constant.SharedDefault
import zone.ien.calarm.constant.SharedKey
import zone.ien.calarm.constant.SharedValue
import zone.ien.calarm.data.CalendarObject
import zone.ien.calarm.databinding.ActivitySettingsBinding
import zone.ien.calarm.databinding.DialogNotiChannelBinding
import zone.ien.calarm.databinding.DialogSelectCalendarBinding
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class SettingsActivity : AppCompatActivity() {

    lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_settings)
        binding.activity = this
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction().replace(R.id.settings, SettingsFragment()).commit()
        }
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    class SettingsFragment : PreferenceFragmentCompat() {

        lateinit var sharedPreferences: SharedPreferences
        lateinit var notiChannelFilterPreferences: SharedPreferences
        lateinit var geocoder: Geocoder

        private lateinit var timeFormat: SimpleDateFormat

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)

            sharedPreferences = requireContext().getSharedPreferences("${requireContext().packageName}_preferences", Context.MODE_PRIVATE)
            notiChannelFilterPreferences = requireContext().getSharedPreferences("${requireContext().packageName}_noti_channel", Context.MODE_PRIVATE)
            geocoder = Geocoder(requireContext())
            timeFormat = SimpleDateFormat(requireContext().getString(R.string.apmTimeFormat), Locale.getDefault())

            val prefSelectCalendar = findPreference<Preference>("select_calendar")
            val prefSetHome = findPreference<Preference>("home")
            val prefCalarmCreateOption = findPreference<DropDownPreference>(SharedKey.CALARM_CREATE_OPTION)
            val prefFixedTime = findPreference<Preference>(SharedKey.FIXED_TIME)
            val prefReadyTime = findPreference<Preference>(SharedKey.READY_TIME)
            val prefHiddenNotiChannel = findPreference<Preference>("hidden_noti_channels")

            val fixedTimeCalendar = Calendar.getInstance().apply {
                val time = sharedPreferences.getInt(SharedKey.FIXED_TIME, SharedDefault.FIXED_TIME)
                set(Calendar.HOUR_OF_DAY, time / 60)
                set(Calendar.MINUTE, time % 60)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val cursor = requireContext().contentResolver.query(
                Uri.parse("content://com.android.calendar/calendars"),
                arrayOf(CalendarContract.Calendars._ID, CalendarContract.Calendars.ACCOUNT_NAME, CalendarContract.Calendars.ACCOUNT_TYPE, CalendarContract.Calendars.CALENDAR_DISPLAY_NAME, CalendarContract.Calendars.OWNER_ACCOUNT, CalendarContract.Calendars.CALENDAR_COLOR
                ), null, null, null)
            val calendarObjectMap: MutableMap<String, ArrayList<CalendarObject>> = mutableMapOf()

            if (cursor != null) {
                if (cursor.count > 0) {
                    cursor.moveToFirst()

                    for (i in 0 until cursor.count) {
                        val categoryKey = "${cursor.getString(2)}|${cursor.getString(1)}"
                        val icon = ContextCompat.getDrawable(requireContext(), R.drawable.circle)
                        icon?.setTint(cursor.getString(5).toInt())

                        if (!calendarObjectMap.containsKey(categoryKey)) {
                            calendarObjectMap[categoryKey] = arrayListOf()
                        }

                        calendarObjectMap[categoryKey]?.add(CalendarObject(
                            id = cursor.getInt(0),
                            accountName = cursor.getString(1),
                            accountType = cursor.getString(2),
                            calendarDisplayName = cursor.getString(3),
                            ownerAccount = cursor.getString(4),
                            calendarColor = cursor.getString(5).toInt()
                        ))

                        cursor.moveToNext()
                    }
                }
            }

            cursor?.close()

            val locationSetActivityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == RESULT_OK) {
                    val latitude = sharedPreferences.getFloat(SharedKey.HOME_LATITUDE, SharedDefault.HOME_LATITUDE.toFloat()).toDouble()
                    val longitude = sharedPreferences.getFloat(SharedKey.HOME_LONGITUDE, SharedDefault.HOME_LONGITUDE.toFloat()).toDouble()
                    var address: List<Address> = listOf()
                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            geocoder.getFromLocation(latitude, longitude, 1) { address = it }
                        } else {
                            address = geocoder.getFromLocation(latitude, longitude, 1) as List<Address>
                        }
                    } catch (e: Exception) {
                        listOf<Address>()
                    }

                    Handler(Looper.getMainLooper()).postDelayed({
                        val currentLocationAddress = if (address.isNotEmpty()) {
                            val firstAddress = address.first().getAddressLine(0).replace(address.first().countryName, "")
                            if (firstAddress.isNotBlank() && firstAddress.first() == ' ') firstAddress.substring(1) else firstAddress
                        } else {
                            getString(R.string.cannot_get_address)
                        }

                        prefSetHome?.summary = currentLocationAddress

                    }, 1000)
                }
            }

            val latitude = sharedPreferences.getFloat(SharedKey.HOME_LATITUDE, SharedDefault.HOME_LATITUDE.toFloat()).toDouble()
            val longitude = sharedPreferences.getFloat(SharedKey.HOME_LONGITUDE, SharedDefault.HOME_LONGITUDE.toFloat()).toDouble()
            var address: List<Address> = listOf()
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    geocoder.getFromLocation(latitude, longitude, 1) { address = it }
                } else {
                    address = geocoder.getFromLocation(latitude, longitude, 1) as List<Address>
                }
            } catch (e: Exception) {
                listOf<Address>()
            }

            Handler(Looper.getMainLooper()).postDelayed({
                val currentLocationAddress = if (address.isNotEmpty()) {
                    val firstAddress = address.first().getAddressLine(0).replace(address.first().countryName, "")
                    if (firstAddress.isNotBlank() && firstAddress.first() == ' ') firstAddress.substring(1) else firstAddress
                } else {
                    getString(R.string.cannot_get_address)
                }

                prefSetHome?.summary = currentLocationAddress

            }, 1000)

            prefSelectCalendar?.setOnPreferenceClickListener {
                MaterialAlertDialogBuilder(requireContext()).apply {
                    val binding: DialogSelectCalendarBinding = DataBindingUtil.inflate(LayoutInflater.from(requireContext()), R.layout.dialog_select_calendar, null, false)

                    binding.list.adapter = SelectCalendarParentAdapter(calendarObjectMap)

                    setPositiveButton(android.R.string.ok) { dialog, id ->
                        dialog.cancel()
                    }

                    setView(binding.root)
                }.show()
                true
            }

            prefSetHome?.setOnPreferenceClickListener {
                locationSetActivityResultLauncher.launch(Intent(requireContext(), LocationActivity::class.java))
                true
            }

            prefCalarmCreateOption?.setDefaultValue(SharedValue.CALARM_CREATE_FIRST_EVENT)
            prefCalarmCreateOption?.setOnPreferenceChangeListener { preference, newValue ->
                prefFixedTime?.isEnabled = newValue == SharedValue.CALARM_CREATE_FIXED_TIME_EVENT
                true
            }
            prefFixedTime?.isEnabled = prefCalarmCreateOption?.value == SharedValue.CALARM_CREATE_FIXED_TIME_EVENT
            prefFixedTime?.summary = timeFormat.format(fixedTimeCalendar.time)
            prefFixedTime?.setOnPreferenceClickListener { preference ->
                val timePicker = MaterialTimePicker.Builder()
                    .setTitleText(R.string.set_fixed_time)
                    .setPositiveButtonText(android.R.string.ok)
                    .setNegativeButtonText(android.R.string.cancel)
                    .setHour(fixedTimeCalendar.get(Calendar.HOUR_OF_DAY))
                    .setMinute(fixedTimeCalendar.get(Calendar.MINUTE))
                    .build()
                timePicker.addOnPositiveButtonClickListener {
                    fixedTimeCalendar.set(Calendar.HOUR_OF_DAY, timePicker.hour)
                    fixedTimeCalendar.set(Calendar.MINUTE, timePicker.minute)
                    sharedPreferences.edit().putInt(SharedKey.FIXED_TIME, timePicker.hour * 60 + timePicker.minute).apply()
                    preference.summary = timeFormat.format(fixedTimeCalendar.time)
                }
                timePicker.show(parentFragmentManager, "TIME_PICKER")

                true
            }

            prefReadyTime?.summary = sharedPreferences.getInt(SharedKey.READY_TIME, SharedDefault.READY_TIME).let {
                if (it / 60 != 0 && it % 60 != 0) getString(R.string.time_format_hour_minute, it / 60, it % 60)
                else if (it / 60 == 0) getString(R.string.time_format_minute, it % 60)
                else getString(R.string.time_format_hour, it / 60)
            }
            prefReadyTime?.setOnPreferenceClickListener { preference ->
                val value = sharedPreferences.getInt(SharedKey.READY_TIME, SharedDefault.READY_TIME)
                val timePicker = MaterialTimePicker.Builder()
                    .setTitleText(R.string.set_fixed_time)
                    .setTimeFormat(TimeFormat.CLOCK_24H)
                    .setPositiveButtonText(android.R.string.ok)
                    .setNegativeButtonText(android.R.string.cancel)
                    .setHour(value / 60)
                    .setMinute(value % 60)
                    .build()
                timePicker.addOnPositiveButtonClickListener {
                    sharedPreferences.edit().putInt(SharedKey.READY_TIME, timePicker.hour * 60 + timePicker.minute).apply()
                    preference.summary = if (timePicker.hour != 0 && timePicker.minute != 0) getString(R.string.time_format_hour_minute, timePicker.hour, timePicker.minute)
                    else if (timePicker.hour == 0) getString(R.string.time_format_minute, timePicker.minute)
                    else getString(R.string.time_format_hour, timePicker.hour)
                }
                timePicker.show(parentFragmentManager, "READY_TIME_PICKER")

                true
            }

            prefHiddenNotiChannel?.setOnPreferenceClickListener {
                MaterialAlertDialogBuilder(requireContext()).apply {
                    val binding: DialogNotiChannelBinding = DataBindingUtil.inflate(LayoutInflater.from(requireContext()), R.layout.dialog_noti_channel, null, false)

                    var adapter: ChannelIdAdapter? = null
                    val callback = object: ChannelIdCallback {
                        override fun delete(position: Int, id: String) {
                            notiChannelFilterPreferences.edit().remove(id).apply()
                            adapter?.delete(position)

                            if (adapter?.isEmpty() == true) binding.tvEmpty.visibility = View.VISIBLE
                        }
                    }
                    val channels = ArrayList<Pair<String, String>>()
                    notiChannelFilterPreferences.all.keys.forEach {
                        if (!notiChannelFilterPreferences.getBoolean(it, true)) {
                            val data = it.split("☆")
                            if (data.size == 2) channels.add(Pair(data.first(), data.last()))
                        }
                    }
                    adapter = ChannelIdAdapter(channels)
                    binding.list.adapter = adapter.apply { setClickCallback(callback) }

                    setTitle(R.string.hidden_noti_channels)

                    if (channels.isEmpty()) binding.tvEmpty.visibility = View.VISIBLE

                    setView(binding.root)
                }.show()
                true
            }
        }
    }
}