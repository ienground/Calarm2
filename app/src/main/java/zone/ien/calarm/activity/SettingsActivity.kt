package zone.ien.calarm.activity

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.location.Address
import android.location.Geocoder
import android.net.Uri
import android.os.Bundle
import android.provider.CalendarContract
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.preference.CheckBoxPreference
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import zone.ien.calarm.R
import zone.ien.calarm.adapter.SelectCalendarParentAdapter
import zone.ien.calarm.constant.SharedDefault
import zone.ien.calarm.constant.SharedKey
import zone.ien.calarm.data.CalendarObject
import zone.ien.calarm.databinding.DialogSelectCalendarBinding

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction().replace(R.id.settings, SettingsFragment()).commit()
        }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    class SettingsFragment : PreferenceFragmentCompat() {

        lateinit var sharedPreferences: SharedPreferences
        lateinit var geocoder: Geocoder

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)

            sharedPreferences = requireContext().getSharedPreferences("${requireContext().packageName}_preferences", Context.MODE_PRIVATE)
            geocoder = Geocoder(requireContext())

            val prefSelectCalendar = findPreference<Preference>("select_calendar")
            val prefSetHome = findPreference<Preference>("home")
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
                    val address = try {
                        geocoder.getFromLocation(latitude, longitude, 1)
                    } catch (e: Exception) {
                        listOf<Address>()
                    }
                    val currentLocationAddress = if (address != null && address.isNotEmpty()) {
                        val firstAddress = address.first().getAddressLine(0).replace(address.first().countryName, "")
                        if (firstAddress.isNotBlank() && firstAddress.first() == ' ') firstAddress.substring(1) else firstAddress
                    } else {
                        getString(R.string.cannot_get_address)
                    }

                    home?.summary = currentLocationAddress
                }
            }

            val latitude = sharedPreferences.getFloat(SharedKey.HOME_LATITUDE, SharedDefault.HOME_LATITUDE.toFloat()).toDouble()
            val longitude = sharedPreferences.getFloat(SharedKey.HOME_LONGITUDE, SharedDefault.HOME_LONGITUDE.toFloat()).toDouble()
            val address = try {
                geocoder.getFromLocation(latitude, longitude, 1)
            } catch (e: Exception) {
                listOf<Address>()
            }
            val currentLocationAddress = if (address != null && address.isNotEmpty()) {
                val firstAddress = address.first().getAddressLine(0).replace(address.first().countryName, "")
                if (firstAddress.isNotBlank() && firstAddress.first() == ' ') firstAddress.substring(1) else firstAddress
            } else {
                getString(androidx.preference.R.string.cannot_get_address)
            }

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
                startActivity(Intent(requireContext(), LocationActivity::class.java))
                true
            }
        }
    }
}