package zone.ien.calarm.activity

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.ItemTouchHelper
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import kotlinx.coroutines.*
import zone.ien.calarm.R
import zone.ien.calarm.adapter.SubAlarmAdapter
import zone.ien.calarm.callback.ItemTouchHelperCallback
import zone.ien.calarm.constant.IntentKey
import zone.ien.calarm.constant.IntentValue
import zone.ien.calarm.constant.SharedKey
import zone.ien.calarm.databinding.ActivityEditAlarmBinding
import zone.ien.calarm.room.AlarmDatabase
import zone.ien.calarm.room.AlarmEntity
import zone.ien.calarm.room.SubAlarmDatabase
import zone.ien.calarm.room.SubAlarmEntity
import zone.ien.calarm.utils.MyUtils
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.pow

class EditAlarmActivity : AppCompatActivity() {

    lateinit var binding: ActivityEditAlarmBinding
    private lateinit var sharedPreferences: SharedPreferences
    private var alarmDatabase: AlarmDatabase? = null
    private var subAlarmDatabase: SubAlarmDatabase? = null
    private lateinit var buttons: List<MaterialButton>
    private lateinit var adapter: SubAlarmAdapter
    private lateinit var am: AlarmManager

    private val apmFormat = SimpleDateFormat("a", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("h:mm", Locale.getDefault())

    private var ringtones: Map<String, String> = mapOf()

    var item = AlarmEntity("", 0, true, 0, "", "", true)
    var id = -1L

    @OptIn(DelicateCoroutinesApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_edit_alarm)
        binding.activity = this

        am = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        sharedPreferences = getSharedPreferences("${packageName}_preferences", Context.MODE_PRIVATE)
        buttons = listOf(
            binding.repeatSun, binding.repeatMon, binding.repeatTue, binding.repeatWed,
            binding.repeatThu, binding.repeatFri, binding.repeatSat
        )

        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = null
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        alarmDatabase = AlarmDatabase.getInstance(this)
        subAlarmDatabase = SubAlarmDatabase.getInstance(this)

        ringtones = MyUtils.getAlarmRingtones(this)
        item.sound = ringtones.values.first()

        id = intent.getLongExtra(IntentKey.ITEM_ID, -1L)
        Calendar.getInstance().let {
            item.time = it.get(Calendar.HOUR_OF_DAY) * 60 + it.get(Calendar.MINUTE) + 1
        }

        if (id != -1L) {
            GlobalScope.launch(Dispatchers.IO) {
                alarmDatabase?.getDao()?.get(id).let {
                    if (it != null) {
                        item = it
                        item.subAlarms = subAlarmDatabase?.getDao()?.getByParentId(id) as ArrayList<SubAlarmEntity>
                        withContext(Dispatchers.Main) {
                            inflateData(item)
                        }
                    }
                }
            }
        } else {
            invalidateMenu()
            inflateData(item)
        }

        binding.groupTime.setOnClickListener {
            val timePicker = MaterialTimePicker.Builder()
                .setTitleText("HI:")
                .setHour(item.time / 60)
                .setMinute(item.time % 60)
                .build()
            timePicker.addOnPositiveButtonClickListener {
                item.time = timePicker.hour * 60 + timePicker.minute
                val time = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, timePicker.hour)
                    set(Calendar.MINUTE, timePicker.minute)
                }
                binding.tvApm.text = apmFormat.format(time.time)
                binding.tvTime.text = timeFormat.format(time.time)
            }
            timePicker.show(supportFragmentManager, "TIME_PICKER")
        }

        buttons.forEachIndexed { index, chip ->
            chip.setOnClickListener {
                if (chip.isChecked) item.repeat += 2.0.pow(6 - index).toInt()
                else item.repeat -= 2.0.pow(6 - index).toInt()
                binding.tvRepeat.text = MyUtils.getRepeatlabel(applicationContext, item.repeat, item.time)
            }
        }

        binding.groupRing.setOnClickListener {
            val mediaPlayer = MediaPlayer().apply { setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build()) }
            MaterialAlertDialogBuilder(this).apply {
                var index = ringtones.values.indexOf(item.sound)
                var checkedUri = item.sound

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
                val entity = SubAlarmEntity(-1L, timePicker.hour * 60 + timePicker.minute, true)
                if (adapter.items.find { it.time == timePicker.hour * 60 + timePicker.minute } == null) {
                    adapter.add(entity)
                } else {
                    Snackbar.make(window.decorView.rootView, getString(R.string.sub_alarm_exists), Snackbar.LENGTH_SHORT).show()
                }
            }
            timePicker.show(supportFragmentManager, "SUB_TIME_PICKER")
        }
    }

    private fun inflateData(data: AlarmEntity) {
        val time = Calendar.getInstance().apply {
            data.time.let {
                set(Calendar.HOUR_OF_DAY, it / 60)
                set(Calendar.MINUTE, it % 60)
            }
        }
        binding.etLabel.editText?.setText(item.label)

        for (i in 0 until 7) {
            buttons[i].isChecked = item.repeat.and(2.0.pow(6 - i).toInt()) != 0
        }
        binding.tvRepeat.text = MyUtils.getRepeatlabel(applicationContext, item.repeat, item.time)
        binding.tvApm.text = apmFormat.format(time.time)
        binding.tvTime.text = timeFormat.format(time.time)
        binding.tvRing.text = with(ringtones.filterValues { it == item.sound }.getKeyArray()) { if (this.isNotEmpty()) first() else "" }
        binding.switchVibrate.isChecked = data.vibrate

        adapter = SubAlarmAdapter(data.subAlarms)
//        Log.d(TAG, data.subAlarms.toString())
        val itemTouchHelper = ItemTouchHelper(ItemTouchHelperCallback(adapter))
        itemTouchHelper.attachToRecyclerView(binding.listSubAlarm)
        binding.listSubAlarm.adapter = adapter
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_edit, menu)
        if (id == -1L) menu?.findItem(R.id.menu_delete)?.isVisible = false
        return super.onCreateOptionsMenu(menu)
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
            }
            R.id.menu_save -> {
                this.item.label = binding.etLabel.editText?.text.toString()
                this.item.isEnabled = true
                GlobalScope.launch(Dispatchers.IO) {
                    val id = alarmDatabase?.getDao()?.add(this@EditAlarmActivity.item)
                    MyUtils.deleteAlarmClock(applicationContext, am, this@EditAlarmActivity.item)
                    withContext(Dispatchers.IO) {
                        this@EditAlarmActivity.item.id = id
                        subAlarmDatabase?.getDao()?.deleteParentId(id ?: -1)
                        for (entity in this@EditAlarmActivity.item.subAlarms) {
                            entity.parentId = id ?: -1
                            subAlarmDatabase?.getDao()?.add(entity)
                        }
                        val alarmTime = MyUtils.setAlarmClock(applicationContext, am, this@EditAlarmActivity.item)
                        withContext(Dispatchers.Main) {
                            Toast.makeText(applicationContext, MyUtils.timeDiffToString(applicationContext, Calendar.getInstance(), alarmTime), Toast.LENGTH_SHORT).show()
                        }
                        setResult(RESULT_OK, Intent().apply {
                            putExtra(IntentKey.ITEM_ID, id)
                            putExtra(IntentKey.ACTION_TYPE, IntentValue.ACTION_EDIT)
                        })
                        finish()
                    }
                }
            }
            R.id.menu_delete -> {
                MaterialAlertDialogBuilder(this).apply {
                    setMessage(R.string.delete_title)
                    setPositiveButton(android.R.string.ok) { _, _ ->
                        GlobalScope.launch(Dispatchers.IO) {
                            MyUtils.deleteAlarmClock(applicationContext, am, this@EditAlarmActivity.item)
                            alarmDatabase?.getDao()?.delete(this@EditAlarmActivity.item.id ?: -1)
                            subAlarmDatabase?.getDao()?.deleteParentId(this@EditAlarmActivity.item.id ?: -1)
                        }
                        setResult(RESULT_OK, Intent().apply {
                            putExtra(IntentKey.ITEM_ID, id)
                            putExtra(IntentKey.ACTION_TYPE, IntentValue.ACTION_DELETE)
                        })
                        finish()
                    }
                    setNegativeButton(android.R.string.cancel) { _, _ -> }
                }.show()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun Map<String, String>.getKeyArray(): Array<CharSequence> {
        val result = mutableListOf<CharSequence>()
        for (key in keys) result.add(key)
        return result.toTypedArray()
    }
}