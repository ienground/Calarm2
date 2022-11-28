package zone.ien.calarm.activity

import android.app.AlarmManager
import android.app.DatePickerDialog
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
import androidx.recyclerview.widget.ItemTouchHelper
import com.google.android.material.chip.Chip
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointBackward
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.datepicker.MaterialStyledDatePickerDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.dialog.MaterialDialogs
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import kotlinx.coroutines.*
import zone.ien.calarm.R
import zone.ien.calarm.adapter.SubAlarmAdapter
import zone.ien.calarm.adapter.SubTimerAdapter
import zone.ien.calarm.callback.EditTimerListCallback
import zone.ien.calarm.callback.ItemTouchHelperCallback
import zone.ien.calarm.callback.TimerListCallback
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
    private lateinit var dateTimeFormat : SimpleDateFormat

    var item = TimersEntity("", "", 0L, false, System.currentTimeMillis())
    var id = -1L

    private val editTimerListCallback: EditTimerListCallback = object: EditTimerListCallback {
        override fun updateTotalDuration() {
            if (id != -1L) {
                val durationSum = item.subTimers.let {
                    var sum = 0
                    for (timers in it) sum += timers.time
                    sum
                }
                binding.tvDurationSum.text = durationSum.let {
                    if (it / 3600 != 0) String.format("%02d:%02d:%02d", it / 3600, (it % 3600) / 60, it % 60)
                    else String.format("%02d:%02d", (it % 3600) / 60, it % 60)
                }
            }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_edit_timer)
        binding.activity = this

        am = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        sharedPreferences = getSharedPreferences("${packageName}_preferences", Context.MODE_PRIVATE)
        dateTimeFormat = SimpleDateFormat(getString(R.string.dateTimeFormat), Locale.getDefault())

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
            inflateData(item)
        }

        binding.groupSchedule.setOnClickListener {
            val datePicker = getDatePickerDialog(isChecked = true)
            datePicker.show(supportFragmentManager, "DATE_PICKER")
        }
    }

    private fun inflateData(data: TimersEntity) {
        binding.etLabel.editText?.setText(item.label)
        binding.switchSchedule.isChecked = data.isScheduled
        binding.groupSchedule.isClickable = data.isScheduled
        binding.tvSchedule.text = if (data.isScheduled) dateTimeFormat.format(Date(data.scheduledTime)) else getString(R.string.not_scheduled)
        binding.switchSchedule.setOnCheckedChangeListener { compoundButton, b ->
            binding.groupSchedule.isClickable = b
            if (b) {
                val datePicker = getDatePickerDialog()
                datePicker.show(supportFragmentManager, "DATE_PICKER")
            } else {
                item.isScheduled = false
                binding.tvSchedule.text = getString(R.string.not_scheduled)
            }
        }

        adapter = SubTimerAdapter(data.subTimers, data.id ?: -1).apply {
            setCallbackListener(editTimerListCallback)
        }

        val itemTouchHelper = ItemTouchHelper(ItemTouchHelperCallback(adapter))
        itemTouchHelper.attachToRecyclerView(binding.listSubTimer)
        val durationSum = data.subTimers.let {
            var sum = 0
            for (timers in it) sum += timers.time
            sum
        }
        binding.listSubTimer.adapter = adapter
        binding.tvDurationSum.text = durationSum.let {
            if (it / 3600 != 0) String.format("%02d:%02d:%02d", it / 3600, (it % 3600) / 60, it % 60)
            else String.format("%02d:%02d", (it % 3600) / 60, it % 60)
        }

    }

    private fun getDatePickerDialog(isChecked: Boolean = false): MaterialDatePicker<Long> {
        val constraintsBuilder= CalendarConstraints.Builder()
            .setValidator(DateValidatorPointForward.now())
        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Hello World")
            .setPositiveButtonText(android.R.string.ok)
            .setNegativeButtonText(android.R.string.cancel)
            .setSelection(item.scheduledTime)
            .setCalendarConstraints(constraintsBuilder.build())
            .build()
        datePicker.addOnPositiveButtonClickListener {
            val scheduledTime = Calendar.getInstance().apply { timeInMillis = item.scheduledTime }
            val timePicker = MaterialTimePicker.Builder()
                .setTitleText("HV")
                .setPositiveButtonText(android.R.string.ok)
                .setNegativeButtonText(android.R.string.cancel)
                .setHour(scheduledTime.get(Calendar.HOUR_OF_DAY))
                .setMinute(scheduledTime.get(Calendar.MINUTE))
                .build()
            timePicker.addOnPositiveButtonClickListener { _ ->
                val calendar = Calendar.getInstance().apply { timeInMillis = it }
                calendar.set(Calendar.HOUR_OF_DAY, timePicker.hour)
                calendar.set(Calendar.MINUTE, timePicker.minute)
                this.item.isScheduled = true
                this.item.scheduledTime = calendar.timeInMillis
                binding.tvSchedule.text = dateTimeFormat.format(calendar.time)
            }
            timePicker.addOnNegativeButtonClickListener { binding.switchSchedule.isChecked = isChecked }
            timePicker.show(supportFragmentManager, "TIME_PICKER_IN_DATE")
        }
        datePicker.addOnNegativeButtonClickListener { binding.switchSchedule.isChecked = isChecked }

        return datePicker
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
//                this.item.scheduledTime
                GlobalScope.launch(Dispatchers.IO) {
                    withContext(Dispatchers.IO) {
                        timersDatabase?.getDao()?.update(this@EditTimerActivity.item)
                        subTimerDatabase?.getDao()?.deleteParentId(id)
                        this@EditTimerActivity.item.subTimers.forEachIndexed { index, entity ->
                            entity.parentId = id
                            entity.order = index
                            subTimerDatabase?.getDao()?.add(entity)
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
                            timersDatabase?.getDao()?.delete(this@EditTimerActivity.item.id ?: -1)
                            subTimerDatabase?.getDao()?.deleteParentId(this@EditTimerActivity.item.id ?: -1)
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