package zone.ien.calarm.fragment

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.*
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.*
import android.view.animation.AnimationUtils
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.timepicker.MaterialTimePicker
import zone.ien.calarm.R
import zone.ien.calarm.activity.SettingsActivity
import zone.ien.calarm.activity.TAG
import zone.ien.calarm.adapter.LapseAdapter
import zone.ien.calarm.constant.IntentID
import zone.ien.calarm.constant.IntentKey
import zone.ien.calarm.constant.SharedDefault
import zone.ien.calarm.constant.SharedKey
import zone.ien.calarm.data.StopwatchLapse
import zone.ien.calarm.databinding.DialogEmojiBinding
import zone.ien.calarm.databinding.FragmentMainStopwatchBinding
import zone.ien.calarm.service.StopwatchService
import zone.ien.calarm.utils.MyUtils.Companion.dpToPx
import java.text.SimpleDateFormat
import java.util.*


class MainStopwatchFragment : Fragment() {

    lateinit var binding: FragmentMainStopwatchBinding
    private var mListener: OnFragmentInteractionListener? = null
    private var isStopwatchRunning = false

    private var time = 0L
    private lateinit var adapter: LapseAdapter
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var am: AlarmManager
    private lateinit var dateTimeFormat: SimpleDateFormat
    private var scheduledTime: Long = System.currentTimeMillis()

    private var lapStopwatchResultReceiver: BroadcastReceiver? = null
    private var stopStopwatchReceiver: BroadcastReceiver? = null
    private var stopwatchTickReceiver: BroadcastReceiver? = null
    private var playPauseStopwatchReceiver: BroadcastReceiver? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_main_stopwatch, container, false)
        binding.fragment = this

        setHasOptionsMenu(true)
        (requireActivity() as AppCompatActivity).setSupportActionBar(binding.toolbar)
        (requireActivity() as AppCompatActivity).supportActionBar?.title = null

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sharedPreferences = requireContext().getSharedPreferences("${requireContext().packageName}_preferences", Context.MODE_PRIVATE)
        am = requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager
        dateTimeFormat = SimpleDateFormat(getString(R.string.dateTimeFormat), Locale.getDefault())

        binding.subTitle.text = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
            in 6..10 -> getString(R.string.user_hello_morning)
            in 11..16 -> getString(R.string.user_hello_afternoon)
            in 17..20 -> getString(R.string.user_hello_evening)
            else -> getString(R.string.user_hello_night)
        }

        val progressAnimator: ValueAnimator = ValueAnimator.ofInt(500, 0)

        if (StopwatchService.isRunning) {
            if (StopwatchService.lapses.isNotEmpty()) {
                time = StopwatchService.time
                adapter = LapseAdapter(StopwatchService.lapses)
                binding.cardLapse.layoutParams = binding.cardLapse.layoutParams.apply { height = dpToPx(requireContext(), 200f) }
                binding.cardLapse.visibility = View.VISIBLE
                binding.progress.max = adapter.items.last().time.toInt()

                progressAnimator.let {
                    it.setIntValues(0, adapter.items.last().time.toInt())
                    it.duration = adapter.items.last().time
                    it.interpolator = AnimationUtils.loadInterpolator(requireContext(), android.R.anim.linear_interpolator)
                    it.addUpdateListener {
                        binding.progress.progress = (it.animatedValue as Int)
                    }
                    it.repeatMode = ValueAnimator.RESTART
                }
                progressAnimator.currentPlayTime = time - adapter.items[1].time
                progressAnimator.start()

//                if (StopwatchService.isPaused) {
//                    progressAnimator.pause()
//                    binding.btnPlay.icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_play_arrow)
//                } else {
//                    binding.btnPlay.layoutParams = binding.btnPlay.layoutParams.apply { width = dpToPx(requireContext(), 120f) }
//                }
            }
        }

        binding.cardSchedule.isChecked = sharedPreferences.getBoolean(SharedKey.IS_STOPWATCH_SCHEDULED, SharedDefault.IS_STOPWATCH_SCHEDULED)
        if (binding.cardSchedule.isChecked) {
            scheduledTime = sharedPreferences.getLong(SharedKey.STOPWATCH_SCHEDULED_TIME, System.currentTimeMillis())
            binding.tvAlarm.text = dateTimeFormat.format(Date(scheduledTime))
            binding.btnPlay.isEnabled = false
        }
        binding.progress.setProgressFormatter { _, _ -> "" }
        binding.btnLap.text = (sharedPreferences.getString(SharedKey.LAST_EMOJI, SharedDefault.LAST_EMOJI) ?: SharedDefault.LAST_EMOJI).split(".").first()

        binding.btnReset.setOnClickListener {
            if (sharedPreferences.getBoolean(SharedKey.NO_SHOW_STOPWATCH_RESET_DIALOG, SharedDefault.NO_SHOW_STOPWATCH_RESET_DIALOG)) {
                requireContext().stopService(Intent(requireContext(), StopwatchService::class.java))
                (binding.list.adapter as LapseAdapter).clear()
            } else {
                MaterialAlertDialogBuilder(requireContext()).apply {
                    setTitle(R.string.delete_title)
                    val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_reset, LinearLayout(requireContext()), false)
                    val checkbox: MaterialCheckBox = dialogView.findViewById(R.id.checkbox)

                    setView(dialogView)

                    setPositiveButton(android.R.string.ok) { dialog, id ->
                        requireContext().stopService(Intent(requireContext(), StopwatchService::class.java))
                        (binding.list.adapter as LapseAdapter).clear()
                        sharedPreferences.edit().putBoolean(SharedKey.NO_SHOW_STOPWATCH_RESET_DIALOG, checkbox.isChecked).apply()
                    }
                    setNegativeButton(android.R.string.cancel) { dialog, id ->
                        dialog.dismiss()
                    }
                }.show()
            }
        }
        binding.btnLap.setOnClickListener {
            requireContext().sendBroadcast(Intent(IntentID.LAP_STOPWATCH).apply {
                putExtra(IntentKey.LAP_FLAG, binding.btnLap.text)
            })
        }
        binding.btnLap.setOnLongClickListener {
            MaterialAlertDialogBuilder(requireContext()).apply {
                val dialogBinding: DialogEmojiBinding = DataBindingUtil.inflate(LayoutInflater.from(context), R.layout.dialog_emoji, LinearLayout(context), false)
                val tvEmojis = listOf(dialogBinding.tvEmojiRecent01, dialogBinding.tvEmojiRecent02, dialogBinding.tvEmojiRecent03, dialogBinding.tvEmojiRecent04, dialogBinding.tvEmojiRecent05, dialogBinding.tvEmojiRecent06, dialogBinding.tvEmojiRecent07)
                val recentEmojis = (sharedPreferences.getString(SharedKey.LAST_EMOJI, SharedDefault.LAST_EMOJI) ?: SharedDefault.LAST_EMOJI).split(".") as ArrayList

                dialogBinding.tvEmojiPreview.text = recentEmojis.first()
                tvEmojis.forEachIndexed { index, tv ->
                    tv.text = recentEmojis[index + 1]
                    tv.setOnClickListener {
                        recentEmojis.removeAt(index + 1)
                        recentEmojis.add(0, tv.text.toString())

                        dialogBinding.tvEmojiPreview.text = recentEmojis.first()
                        tvEmojis.forEachIndexed { i, v ->
                            v.text = recentEmojis[i + 1]
                        }
                    }
                }

                setPositiveButton(android.R.string.ok) { dialog, id ->
                    binding.btnLap.text = recentEmojis.first()
                    sharedPreferences.edit().putString(SharedKey.LAST_EMOJI, recentEmojis.joinToString(".")).apply()
                    dialog.dismiss()
                }

                setNegativeButton(android.R.string.cancel) { dialog, id -> }

                setView(dialogBinding.root)
            }.show()
            true
        }
        binding.btnPlay.setOnClickListener {
            if (!isStopwatchRunning) {
                requireContext().startForegroundService(Intent(requireContext(), StopwatchService::class.java))
            } else {
                requireContext().sendBroadcast(Intent(IntentID.PLAY_PAUSE_STOPWATCH))
            }
        }
        binding.cardSchedule.setOnClickListener {
            if (binding.cardSchedule.isChecked) {
                binding.cardSchedule.isChecked = false
                binding.tvAlarm.text = getString(R.string.tap_to_schedule_stopwatch)
                sharedPreferences.edit().putBoolean(SharedKey.IS_STOPWATCH_SCHEDULED, false).apply()
                binding.btnPlay.isEnabled = true
                val pendingIntent = PendingIntent.getService(requireContext(), 1, Intent(requireContext(), StopwatchService::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                am.cancel(pendingIntent)
            } else {
                getDatePickerDialog().show(parentFragmentManager, "DATE_PICKER")
            }
        }

        lapStopwatchResultReceiver = object: BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val flag = intent.getStringExtra(IntentKey.LAP_FLAG) ?: ""
                val time = intent.getLongExtra(IntentKey.LAP_TIME, 0)
                if (binding.cardLapse.visibility == View.GONE) {
                    binding.cardLapse.visibility = View.VISIBLE
                    ValueAnimator.ofFloat(dpToPx(context, 0.1f).toFloat(), dpToPx(context, 200f).toFloat()).apply {
                        duration = 200
                        interpolator = AnimationUtils.loadInterpolator(binding.cardLapse.context, android.R.anim.accelerate_decelerate_interpolator)
                        addUpdateListener {
                            binding.cardLapse.layoutParams = binding.cardLapse.layoutParams.apply { height = (it.animatedValue as Float).toInt() }
                        }
                    }.start()

                    binding.progress.max = time.toInt()

                    progressAnimator.let {
                        it.setIntValues(0, time.toInt())
                        it.duration = time
                        it.interpolator = AnimationUtils.loadInterpolator(context, android.R.anim.linear_interpolator)
                        it.addUpdateListener {
                            binding.progress.progress = (it.animatedValue as Int)
                        }
                        it.repeatMode = ValueAnimator.RESTART
                    }
                    progressAnimator.start()
                    adapter.add(StopwatchLapse(flag, time))
                }
                adapter.add(StopwatchLapse(flag, time))
                binding.list.smoothScrollToPosition(0)
                binding.progress.progress = 0

                progressAnimator.start()
            }
        }
        stopStopwatchReceiver = object: BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                isStopwatchRunning = false
                time = 0
                binding.tvTime.text = "00"
                binding.tvTimeMilli.text = "00"
                binding.btnReset.visibility = View.GONE
                binding.btnLap.visibility = View.GONE
                binding.cardSchedule.visibility = View.VISIBLE
                progressAnimator.cancel()
                binding.progress.progress = 0

                binding.btnPlay.icon = ContextCompat.getDrawable(context, R.drawable.ic_play_arrow)
                if (binding.btnPlay.layoutParams.width == dpToPx(context, 120f)) {
                    ValueAnimator.ofInt(dpToPx(context, 120f), dpToPx(context, 80f)).apply {
                        interpolator = AnimationUtils.loadInterpolator(context, android.R.anim.accelerate_decelerate_interpolator)
                        addUpdateListener {
                            duration = 300
                            binding.btnPlay.layoutParams = binding.btnPlay.layoutParams.apply { width = it.animatedValue as Int }
                        }
                    }.start()
                }

                if (binding.cardLapse.visibility == View.VISIBLE) {
                    ValueAnimator.ofFloat(dpToPx(context, 200f).toFloat(), dpToPx(context, 0.1f).toFloat()).apply {
                        duration = 200
                        interpolator = AnimationUtils.loadInterpolator(context, android.R.anim.accelerate_decelerate_interpolator)
                        addUpdateListener {
                            binding.cardLapse.layoutParams = binding.cardLapse.layoutParams.apply { height = (it.animatedValue as Float).toInt() }
                        }
                        addListener(object: AnimatorListenerAdapter() {
                            override fun onAnimationEnd(animation: Animator) {
                                super.onAnimationEnd(animation)
                                binding.cardLapse.visibility = View.GONE
                            }
                        })
                    }.start()
                }
            }
        }
        stopwatchTickReceiver = object: BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                time = intent.getLongExtra(IntentKey.COUNTDOWN_TIME, 0)
                binding.tvTimeMilli.text = String.format("%02d", (time % 1000) / 10)
                binding.tvTime.text = (time / 1000).let {
                    if (it / 3600 != 0L) String.format("%02d:%02d:%02d", it / 3600, (it % 3600) / 60, it % 60)
                    else if (it / 60 != 0L) {
                        if (it / 60 > 10) String.format("%02d:%02d", (it % 3600) / 60, it % 60)
                        else String.format("%d:%02d", (it % 3600) / 60, it % 60)
                    }
                    else String.format("%02d", it % 60)
                }
                if (::adapter.isInitialized && !adapter.isEmpty()) adapter.update(StopwatchLapse(binding.btnLap.text.toString(), time))
                if (!isStopwatchRunning) {
                    StopwatchService.isPaused = false
                    adapter = LapseAdapter(StopwatchService.lapses)
                    binding.list.adapter = adapter
                    binding.btnLap.visibility = View.VISIBLE
                    binding.btnReset.visibility = View.VISIBLE
                    binding.cardSchedule.visibility = View.GONE
                    binding.btnPlay.icon = ContextCompat.getDrawable(context, R.drawable.ic_pause)
                    ValueAnimator.ofInt(dpToPx(context, 80f), dpToPx(context, 120f)).apply {
                        interpolator = AnimationUtils.loadInterpolator(context, android.R.anim.accelerate_decelerate_interpolator)
                        addUpdateListener {
                            duration = 300
                            binding.btnPlay.layoutParams = binding.btnPlay.layoutParams.apply { width = it.animatedValue as Int }
                        }
                    }.start()

                    isStopwatchRunning = true
                    if (activity != null) requireActivity().invalidateMenu()
                }
            }
        }
        playPauseStopwatchReceiver = object: BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                binding.btnPlay.icon = ContextCompat.getDrawable(context, if (!StopwatchService.isPaused) R.drawable.ic_pause else R.drawable.ic_play_arrow)
                if (!StopwatchService.isPaused) {
                    progressAnimator.resume()
                    binding.btnLap.visibility = View.VISIBLE
                    ValueAnimator.ofInt(dpToPx(context, 80f), dpToPx(context, 120f)).apply {
                        interpolator = AnimationUtils.loadInterpolator(context, android.R.anim.accelerate_decelerate_interpolator)
                        addUpdateListener {
                            duration = 300
                            binding.btnPlay.layoutParams = binding.btnPlay.layoutParams.apply { width = it.animatedValue as Int }
                        }
                    }.start()
                } else {
                    progressAnimator.pause()
                    binding.btnLap.visibility = View.GONE
                    ValueAnimator.ofInt(dpToPx(context, 120f), dpToPx(context, 80f)).apply {
                        interpolator = AnimationUtils.loadInterpolator(context, android.R.anim.accelerate_decelerate_interpolator)
                        addUpdateListener {
                            duration = 300
                            binding.btnPlay.layoutParams = binding.btnPlay.layoutParams.apply { width = it.animatedValue as Int }
                        }
                    }.start()
                }
            }
        }

        requireContext().registerReceiver(lapStopwatchResultReceiver, IntentFilter(IntentID.LAP_STOPWATCH_RESULT))
        requireContext().registerReceiver(stopStopwatchReceiver, IntentFilter(IntentID.STOP_STOPWATCH))
        requireContext().registerReceiver(stopwatchTickReceiver, IntentFilter(IntentID.STOPWATCH_TICK))
        requireContext().registerReceiver(playPauseStopwatchReceiver, IntentFilter(IntentID.PLAY_PAUSE_STOPWATCH_RESULT))
    }

    private fun getDatePickerDialog(): MaterialDatePicker<Long> {
        val constraintsBuilder= CalendarConstraints.Builder()
            .setValidator(DateValidatorPointForward.now())
        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Hello World")
            .setPositiveButtonText(android.R.string.ok)
            .setNegativeButtonText(android.R.string.cancel)
            .setSelection(scheduledTime)
            .setCalendarConstraints(constraintsBuilder.build())
            .build()
        datePicker.addOnPositiveButtonClickListener {
            val scheduledCalendar = Calendar.getInstance().apply { timeInMillis = scheduledTime }
            val timePicker = MaterialTimePicker.Builder()
                .setTitleText("HV")
                .setPositiveButtonText(android.R.string.ok)
                .setNegativeButtonText(android.R.string.cancel)
                .setHour(scheduledCalendar.get(Calendar.HOUR_OF_DAY))
                .setMinute(scheduledCalendar.get(Calendar.MINUTE))
                .build()
            timePicker.addOnPositiveButtonClickListener { _ ->
                val calendar = Calendar.getInstance().apply { timeInMillis = it }
                calendar.set(Calendar.HOUR_OF_DAY, timePicker.hour)
                calendar.set(Calendar.MINUTE, timePicker.minute)
                binding.cardSchedule.isChecked = true
                binding.btnPlay.isEnabled = false
                scheduledTime = calendar.timeInMillis
                binding.tvAlarm.text = dateTimeFormat.format(calendar.time)

                sharedPreferences.edit().putLong(SharedKey.STOPWATCH_SCHEDULED_TIME, scheduledTime).apply()
                sharedPreferences.edit().putBoolean(SharedKey.IS_STOPWATCH_SCHEDULED, true).apply()

                val pendingIntent = PendingIntent.getService(requireContext(), 1, Intent(requireContext(), StopwatchService::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                am.setAlarmClock(AlarmManager.AlarmClockInfo(scheduledTime, pendingIntent), pendingIntent)
            }
            timePicker.addOnNegativeButtonClickListener {
                binding.cardSchedule.isChecked = false
                binding.tvAlarm.text = getString(R.string.tap_to_schedule_stopwatch)
            }
            timePicker.show(parentFragmentManager, "TIME_PICKER_IN_DATE")
        }
        datePicker.addOnNegativeButtonClickListener {
            binding.cardSchedule.isChecked = false
            binding.tvAlarm.text = getString(R.string.tap_to_schedule_stopwatch)
        }

        return datePicker
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnFragmentInteractionListener) {
            mListener = context
        } else {
            throw RuntimeException("$context must implement OnFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        (mListener as Context).unregisterReceiver(lapStopwatchResultReceiver)
        (mListener as Context).unregisterReceiver(stopStopwatchReceiver)
        (mListener as Context).unregisterReceiver(stopwatchTickReceiver)
        (mListener as Context).unregisterReceiver(playPauseStopwatchReceiver)
        mListener = null
    }

    override fun onCreateOptionsMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.menu_main_stopwatch, menu)
        menu.findItem(R.id.menu_share)?.isVisible = isStopwatchRunning
        super.onCreateOptionsMenu(menu, menuInflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_settings -> {
                startActivity(Intent(requireContext(), SettingsActivity::class.java))
            }
            R.id.menu_share -> {
                val builder = StringBuilder()
                builder.append(getString(R.string.share_stopwatch_title, time.let {
                    if (it >= 60 * 60 * 10 * 1000) String.format("%02d %02d %02d.%02d", (it / 1000) / (60 * 60), (it / 1000 % (60 * 60)) / 60, (it / 1000) % 60, (it % 1000) / 10)
                    else if (it >= 60 * 60 * 1000) String.format("%d %02d %02d.%02d", (it / 1000) / (60 * 60), (it / 1000 % (60 * 60)) / 60, (it / 1000) % 60, (it % 1000) / 10)
                    else if (it >= 60 * 10 * 1000) String.format("%02d %02d.%02d", (it / 1000 % (60 * 60)) / 60, (it / 1000) % 60, (it % 1000) / 10)
                    else String.format("%d %02d.%02d", (it / 1000 % (60 * 60)) / 60, (it / 1000) % 60, (it % 1000) / 10)
                }))

                adapter.items.reversed().forEachIndexed { index, stopwatchLapse ->
                    if (index != adapter.items.lastIndex) builder.append("${index + 1} ${stopwatchLapse.flag} ${(if (index == 0) stopwatchLapse.time else stopwatchLapse.time - adapter.items.reversed()[index - 1].time).let {
                        if (it >= 60 * 60 * 10 * 1000) String.format("%02d %02d %02d.%02d", (it / 1000) / (60 * 60), (it / 1000 % (60 * 60)) / 60, (it / 1000) % 60, (it % 1000) / 10)
                        else if (it >= 60 * 60 * 1000) String.format("%d %02d %02d.%02d", (it / 1000) / (60 * 60), (it / 1000 % (60 * 60)) / 60, (it / 1000) % 60, (it % 1000) / 10)
                        else if (it >= 60 * 10 * 1000) String.format("%02d %02d.%02d", (it / 1000 % (60 * 60)) / 60, (it / 1000) % 60, (it % 1000) / 10)
                        else String.format("%d %02d.%02d", (it / 1000 % (60 * 60)) / 60, (it / 1000) % 60, (it % 1000) / 10)
                    }} | ${stopwatchLapse.time.let {
                        if (it >= 60 * 60 * 10 * 1000) String.format("%02d %02d %02d.%02d", (it / 1000) / (60 * 60), (it / 1000 % (60 * 60)) / 60, (it / 1000) % 60, (it % 1000) / 10)
                        else if (it >= 60 * 60 * 1000) String.format("%d %02d %02d.%02d", (it / 1000) / (60 * 60), (it / 1000 % (60 * 60)) / 60, (it / 1000) % 60, (it % 1000) / 10)
                        else if (it >= 60 * 10 * 1000) String.format("%02d %02d.%02d", (it / 1000 % (60 * 60)) / 60, (it / 1000) % 60, (it % 1000) / 10)
                        else String.format("%d %02d.%02d", (it / 1000 % (60 * 60)) / 60, (it / 1000) % 60, (it % 1000) / 10)
                    }}\n")
                }
                builder.append(getString(R.string.share_stopwatch_end, adapter.itemCount - 1))

                val sharingIntent = Intent(Intent.ACTION_SEND)
                sharingIntent.type = "text/html"
                sharingIntent.putExtra(Intent.EXTRA_TEXT, builder.toString())
                startActivity(Intent.createChooser(sharingIntent, getString(R.string.share)))
            }
        }
        return super.onOptionsItemSelected(item)
    }

    interface OnFragmentInteractionListener {
        fun onFragmentInteraction(uri: Uri)
    }

    companion object {
        @JvmStatic
        fun newInstance() = MainStopwatchFragment().apply {
            val args = Bundle()
            arguments = args
        }
    }

}