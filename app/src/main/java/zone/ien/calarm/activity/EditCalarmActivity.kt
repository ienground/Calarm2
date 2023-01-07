package zone.ien.calarm.activity

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.database.Cursor
import android.location.Address
import android.location.Geocoder
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.CalendarContract
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.ItemTouchHelper
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import kotlinx.coroutines.*
import zone.ien.calarm.R
import zone.ien.calarm.adapter.SubCalarmAdapter
import zone.ien.calarm.callback.ItemTouchHelperCallback
import zone.ien.calarm.constant.IntentKey
import zone.ien.calarm.constant.IntentValue
import zone.ien.calarm.constant.SharedDefault
import zone.ien.calarm.constant.SharedKey
import zone.ien.calarm.data.CalendarEvent
import zone.ien.calarm.databinding.ActivityEditCalarmBinding
import zone.ien.calarm.room.*
import zone.ien.calarm.utils.MyUtils
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class EditCalarmActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnMapClickListener {

    lateinit var binding: ActivityEditCalarmBinding
    private lateinit var sharedPreferences: SharedPreferences
    private var calarmDatabase: CalarmDatabase? = null
    private var subCalarmDatabase: SubCalarmDatabase? = null
    private lateinit var adapter: SubCalarmAdapter
    private lateinit var am: AlarmManager
    private lateinit var mapFragment: SupportMapFragment
    lateinit var map: GoogleMap

    private val apmFormat = SimpleDateFormat("a", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("h:mm", Locale.getDefault())

    private var ringtones: Map<String, String> = mapOf()
    private lateinit var geocoder: Geocoder

    private var item = CalarmEntity(0, 0, false, "", SharedDefault.HOME_LATITUDE, SharedDefault.HOME_LONGITUDE, "", "", false)
    private var dataId = -1L
    private var preRingtone = ""
    private var preVibrate = false

    @OptIn(DelicateCoroutinesApi::class)
    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            GlobalScope.launch(Dispatchers.IO) {
                item.sound = preRingtone
                item.vibrate = preVibrate

                calarmDatabase?.getDao()?.add(item)
                finish()
            }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_edit_calarm)
        binding.activity = this
        onBackPressedDispatcher.addCallback(onBackPressedCallback)

        am = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        sharedPreferences = getSharedPreferences("${packageName}_preferences", Context.MODE_PRIVATE)
        geocoder = Geocoder(this)
        calarmDatabase = CalarmDatabase.getInstance(this)
        subCalarmDatabase = SubCalarmDatabase.getInstance(this)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = null
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.tvApm.visibility = if (Locale.getDefault() == Locale.KOREA) View.GONE else View.VISIBLE
        binding.tvApmKo.visibility = if (Locale.getDefault() != Locale.KOREA) View.GONE else View.VISIBLE

        mapFragment = supportFragmentManager.findFragmentById(R.id.mapContainer) as SupportMapFragment

        dataId = intent.getLongExtra(IntentKey.ITEM_ID, -1)
        ringtones = MyUtils.getAlarmRingtones(this)
        item.sound = sharedPreferences.getString(SharedKey.LAST_ALARM_SOUND, ringtones.values.first()) ?: ringtones.values.first()

        if (dataId != -1L) {
            GlobalScope.launch(Dispatchers.IO) {
                calarmDatabase?.getDao()?.getByDataId(dataId).let { it ->
                    val calendarEvent = getCalendarEventByID(applicationContext, dataId).first()
                    if (it != null) {
                        item = it
                        item.time = calendarEvent.startDate
                        item.label = calendarEvent.title
                        item.subCalarms = subCalarmDatabase?.getDao()?.getByParentId(dataId) as ArrayList<SubCalarmEntity>
                        item.subCalarms.sortBy { it.time }
                    } else {
                        item = calendarEvent.let { CalarmEntity(it.id, it.calendarId, false, it.eventLocation, SharedDefault.HOME_LATITUDE, SharedDefault.HOME_LONGITUDE, "", sharedPreferences.getString(SharedKey.LAST_ALARM_SOUND, ringtones.values.first()) ?: ringtones.values.first(), false)}
                        item.time = calendarEvent.startDate
                        item.label = calendarEvent.title
                    }
                    preRingtone = item.sound
                    preVibrate = item.vibrate
                }
                withContext(Dispatchers.Main) {
                    inflateData(item)
                }
            }

        }

        binding.groupRing.setOnClickListener {
            val mediaPlayer = MediaPlayer().apply { setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build()) }
            MaterialAlertDialogBuilder(this, R.style.Theme_Calarm_MaterialAlertDialog).apply {
                var index = ringtones.values.indexOf(item.sound)
                var checkedUri = item.sound

                setIcon(R.drawable.ic_ring)
                setTitle(R.string.alarm_ring)
                setSingleChoiceItems(ringtones.getKeyArray(), index) { _, which ->
                    mediaPlayer.stop()
                    val uri = Uri.parse(ringtones[ringtones.getKeyArray()[which]])
                    try {
                        mediaPlayer.reset()
                        mediaPlayer.setDataSource(applicationContext, uri)
                        mediaPlayer.prepare()
                        mediaPlayer.start()
                    } catch (e: Exception) { e.printStackTrace() }

                    index = which
                    checkedUri = ringtones[ringtones.getKeyArray()[which]] ?: ""
                }
                setOnDismissListener {
                    mediaPlayer.stop()
                }
                setPositiveButton(android.R.string.ok) { dialog, _ ->
                    binding.tvRing.text = with(ringtones.filterValues { it == checkedUri }.getKeyArray()) { if (this.isNotEmpty()) first() else "" }
                    item.sound = checkedUri
                    sharedPreferences.edit().putString(SharedKey.LAST_ALARM_SOUND, checkedUri).apply()
                    mediaPlayer.stop()
                }
                setNegativeButton(android.R.string.cancel) { dialog, _ ->
                    mediaPlayer.stop()
                }
            }.show()
        }
        binding.switchVibrate.setOnCheckedChangeListener { compoundButton, b ->
            item.vibrate = b
        }
        binding.groupVibrate.setOnClickListener { binding.switchVibrate.toggle() }
        binding.btnAdd.setOnClickListener {
            val timePicker = MaterialTimePicker.Builder()
                .setHour(0)
                .setMinute(10)
                .setTitleText(R.string.sub_alarm_dialog_title)
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .build()
            timePicker.addOnPositiveButtonClickListener {
                val entity = SubCalarmEntity(item.dataId, timePicker.hour * 60 + timePicker.minute, true)
                if (adapter.items.find { it.time == timePicker.hour * 60 + timePicker.minute } == null) {
                    adapter.add(entity)
                } else {
                    Snackbar.make(window.decorView.rootView, getString(R.string.sub_alarm_exists), Snackbar.LENGTH_SHORT).show()
                }
            }
            timePicker.show(supportFragmentManager, "SUB_TIME_PICKER")
        }
    }

    private fun inflateData(data: CalarmEntity) {
        val time = Calendar.getInstance().apply { timeInMillis = data.time }
        val location = data.address
        var address: List<Address> = listOf()

        mapFragment.getMapAsync(this)

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                geocoder.getFromLocationName(location, 1) { address = it }
            } else {
                address = geocoder.getFromLocationName(location, 1) as List<Address>
            }

            Handler(Looper.getMainLooper()).postDelayed({
                if (address.isNotEmpty()) {
                    val latitude = address.first().latitude.toFloat()
                    val longitude = address.first().longitude.toFloat()
                    data.latitude = latitude
                    data.longitude = longitude

                    map.animateCamera(CameraUpdateFactory.newLatLng(LatLng(latitude.toDouble(), longitude.toDouble())))
                }
            }, 1000)
        } catch (_: Exception) { }

        binding.tvLabel.text = item.label

        binding.tvApm.text = apmFormat.format(time.time)
        binding.tvApmKo.text = apmFormat.format(time.time)
        binding.tvTime.text = timeFormat.format(time.time)
        binding.tvRing.text = with(ringtones.filterValues { it == item.sound }.getKeyArray()) { if (this.isNotEmpty()) first() else "" }
        binding.switchVibrate.isChecked = data.vibrate

        adapter = SubCalarmAdapter(data.subCalarms)
        val itemTouchHelper = ItemTouchHelper(ItemTouchHelperCallback(adapter))
        itemTouchHelper.attachToRecyclerView(binding.listSubAlarm)
        binding.listSubAlarm.adapter = adapter
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_edit, menu)
        menu?.findItem(R.id.menu_delete)?.isVisible = false
        return super.onCreateOptionsMenu(menu)
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
            }
            R.id.menu_save -> {
                this.item.isEnabled = true
                GlobalScope.launch(Dispatchers.IO) {
                    val id = calarmDatabase?.getDao()?.add(this@EditCalarmActivity.item)
                    MyUtils.deleteCalarmClock(applicationContext, am, this@EditCalarmActivity.item)
                    withContext(Dispatchers.IO) {
                        this@EditCalarmActivity.item.id = id
                        subCalarmDatabase?.getDao()?.deleteParentId(dataId)
                        for (entity in this@EditCalarmActivity.item.subCalarms) {
                            entity.parentId = dataId
                            subCalarmDatabase?.getDao()?.add(entity)
                        }
                        MyUtils.setCalarmClock(applicationContext, am, this@EditCalarmActivity.item)
                        setResult(RESULT_OK, Intent().apply {
                            putExtra(IntentKey.ITEM_ID, id)
                            putExtra(IntentKey.ACTION_TYPE, IntentValue.ACTION_EDIT)
                        })
                        finish()
                    }
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun Map<String, String>.getKeyArray(): Array<CharSequence> {
        val result = mutableListOf<CharSequence>()
        for (key in keys) result.add(key)
        return result.toTypedArray()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        map.setOnMapClickListener(this)
        map.uiSettings.setAllGesturesEnabled(false)

        val cameraPosition = LatLng(item.latitude.toDouble(), item.longitude.toDouble())
        val markerOptions = MarkerOptions().apply {
            position(cameraPosition)
        }

        map.addMarker(markerOptions)
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(cameraPosition, 18.5f))
    }

    override fun onMapClick(latLng: LatLng) {}

    private fun getCalendarEventByID(context: Context, id: Long): ArrayList<CalendarEvent> {
        val events = ArrayList<CalendarEvent>()

        context.contentResolver.query(
            CalendarContract.Events.CONTENT_URI,
            arrayOf(
                CalendarContract.Events._ID,
                CalendarContract.Events.CALENDAR_ID,
                CalendarContract.Events.TITLE,
                CalendarContract.Events.DESCRIPTION,
                CalendarContract.Events.DTSTART,
                CalendarContract.Events.EVENT_TIMEZONE,
                CalendarContract.Events.DTEND,
                CalendarContract.Events.EVENT_END_TIMEZONE,
                CalendarContract.Events.ALL_DAY,
                CalendarContract.Events.EVENT_COLOR,
                CalendarContract.Events.EVENT_LOCATION,
            ), "(( ${CalendarContract.Events._ID} = ${id} ) AND ( deleted != 1 ))", null, CalendarContract.Events.DTSTART)?.use { cursor ->
            while (cursor.moveToNext()) {
                val event = CalendarEvent(
                    cursor.getSafeLong(cursor.getColumnIndex(CalendarContract.Events._ID), 0L),
                    cursor.getSafeLong(cursor.getColumnIndex(CalendarContract.Events.CALENDAR_ID), 0L),
                    cursor.getSafeString(cursor.getColumnIndex(CalendarContract.Events.TITLE), ""),
                    cursor.getSafeString(cursor.getColumnIndex(CalendarContract.Events.DESCRIPTION), ""),
                    cursor.getSafeLong(cursor.getColumnIndex(CalendarContract.Events.DTSTART), 0L),
                    cursor.getSafeString(cursor.getColumnIndex(CalendarContract.Events.EVENT_TIMEZONE), ""),
                    cursor.getSafeLong(cursor.getColumnIndex(CalendarContract.Events.DTEND), 0L),
                    cursor.getSafeString(cursor.getColumnIndex(CalendarContract.Events.EVENT_END_TIMEZONE), ""),
                    cursor.getInt(cursor.getColumnIndexOrThrow(CalendarContract.Events.ALL_DAY)) == 1,
                    cursor.getSafeString(cursor.getColumnIndex(CalendarContract.Events.EVENT_COLOR), "0"),
                    cursor.getSafeString(cursor.getColumnIndex(CalendarContract.Events.EVENT_LOCATION), ""),
                )
                events.add(event)
            }
        }

        return events
    }

    private fun Cursor.getSafeLong(columnIndex: Int, defaultValue: Long) = if (isNull(columnIndex)) { defaultValue } else { getLong(columnIndex) }
    private fun Cursor.getSafeString(columnIndex: Int, defaultValue: String) = if (isNull(columnIndex)) { defaultValue } else { getString(columnIndex) }
}