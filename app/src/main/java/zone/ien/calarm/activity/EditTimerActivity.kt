package zone.ien.calarm.activity

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.databinding.DataBindingUtil
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.dialog.MaterialDialogs
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import kotlinx.coroutines.*
import zone.ien.calarm.R
import zone.ien.calarm.adapter.SubAlarmAdapter
import zone.ien.calarm.adapter.SubTimerAdapter
import zone.ien.calarm.constant.IntentKey
import zone.ien.calarm.constant.IntentValue
import zone.ien.calarm.constant.SharedKey
import zone.ien.calarm.databinding.ActivityEditAlarmBinding
import zone.ien.calarm.databinding.ActivityEditTimerBinding
import zone.ien.calarm.room.*
import zone.ien.calarm.utils.MyUtils
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.pow

class EditTimerActivity : AppCompatActivity() {

    lateinit var binding: ActivityEditTimerBinding
    private lateinit var sharedPreferences: SharedPreferences
    private var timersDatabase: TimersDatabase? = null
    private var subTimerDatabase: SubTimerDatabase? = null
    private lateinit var adapter: SubTimerAdapter
    private lateinit var am: AlarmManager

    private val apmFormat = SimpleDateFormat("a", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("h:mm", Locale.getDefault())

    var item = TimersEntity("", "", 0L, false, 0L)
    var id = -1L

    @OptIn(DelicateCoroutinesApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_edit_timer)
        binding.activity = this

        am = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        sharedPreferences = getSharedPreferences("${packageName}_preferences", Context.MODE_PRIVATE)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = null
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        timersDatabase = TimersDatabase.getInstance(this)
        subTimerDatabase = SubTimerDatabase.getInstance(this)

        id = intent.getLongExtra(IntentKey.ITEM_ID, -1L)



        if (id != -1L) {
            GlobalScope.launch(Dispatchers.IO) {
                timersDatabase?.getDao()?.get(id).let {
                    if (it != null) {
                        item = it
                        item.subTimers = subTimerDatabase?.getDao()?.getByParentId(id) as ArrayList<SubTimerEntity>
                        withContext(Dispatchers.Main) {
                            inflateData(item)
                        }
                    }
                }
            }
        } else {
            invalidateMenu()
        }

        /*

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

        chips.forEachIndexed { index, chip ->
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

         */

//        inflateData(item)

    }

    private fun inflateData(data: TimersEntity) {
//        val time = Calendar.getInstance().apply {
//            data.time.let {
//                set(Calendar.HOUR_OF_DAY, it / 60)
//                set(Calendar.MINUTE, it % 60)
//            }
//        }
        binding.etLabel.editText?.setText(item.label)

        adapter = SubTimerAdapter(data.subTimers)
        binding.listSubTimer.adapter = adapter
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
                /*
                this.item.label = binding.etLabel.editText?.text.toString()
                this.item.isEnabled = true
                GlobalScope.launch(Dispatchers.IO) {
                    val id = alarmDatabase?.getDao()?.add(this@EditTimerActivity.item)
                    MyUtils.deleteAlarmClock(applicationContext, am, this@EditTimerActivity.item)
                    withContext(Dispatchers.IO) {
                        this@EditTimerActivity.item.id = id
                        subAlarmDatabase?.getDao()?.deleteParentId(id ?: -1)
                        for (entity in this@EditTimerActivity.item.subAlarms) {
                            entity.parentId = id ?: -1
                            subAlarmDatabase?.getDao()?.add(entity)
                        }
                        val alarmTime = MyUtils.setAlarmClock(applicationContext, am, this@EditTimerActivity.item)
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

                 */
            }
            R.id.menu_delete -> {
                /*
                MaterialAlertDialogBuilder(this).apply {
                    setMessage(R.string.delete_title)
                    setPositiveButton(android.R.string.ok) { _, _ ->
                        GlobalScope.launch(Dispatchers.IO) {
                            MyUtils.deleteAlarmClock(applicationContext, am, this@EditTimerActivity.item)
                            alarmDatabase?.getDao()?.delete(this@EditTimerActivity.item.id ?: -1)
                            subAlarmDatabase?.getDao()?.deleteParentId(this@EditTimerActivity.item.id ?: -1)
                        }
                        setResult(RESULT_OK, Intent().apply {
                            putExtra(IntentKey.ITEM_ID, id)
                            putExtra(IntentKey.ACTION_TYPE, IntentValue.ACTION_DELETE)
                        })
                        finish()
                    }
                    setNegativeButton(android.R.string.cancel) { _, _ -> }
                }.show()

                 */
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