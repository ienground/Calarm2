package zone.ien.calarm.fragment

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.app.Activity
import android.content.*
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.CalendarContract
import android.util.Log
import android.view.*
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.michalsvec.singlerowcalendar.calendar.CalendarChangesObserver
import com.michalsvec.singlerowcalendar.calendar.CalendarViewManager
import com.michalsvec.singlerowcalendar.calendar.SingleRowCalendarAdapter
import com.michalsvec.singlerowcalendar.selection.CalendarSelectionManager
import kotlinx.coroutines.*
import zone.ien.calarm.R
import zone.ien.calarm.activity.EditAlarmActivity
import zone.ien.calarm.activity.EditCalarmActivity
import zone.ien.calarm.activity.SettingsActivity
import zone.ien.calarm.activity.TAG
import zone.ien.calarm.adapter.MainCalarmEventAdapter
import zone.ien.calarm.callback.AlarmListCallback
import zone.ien.calarm.constant.IntentKey
import zone.ien.calarm.constant.IntentValue
import zone.ien.calarm.data.CalendarEvent
import zone.ien.calarm.databinding.FragmentMainCalarmBinding
import zone.ien.calarm.room.*
import zone.ien.calarm.utils.MyUtils
import zone.ien.calarm.utils.MyUtils.Companion.getSafeLong
import zone.ien.calarm.utils.MyUtils.Companion.getSafeString
import zone.ien.calarm.utils.MyUtils.Companion.timeZero
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class MainCalarmFragment : Fragment() {

    lateinit var binding: FragmentMainCalarmBinding
    private var mListener: OnFragmentInteractionListener? = null
    lateinit var editActivityResultLauncher: ActivityResultLauncher<Intent>

    lateinit var sharedPreferences: SharedPreferences
    var calendarViewSelected = ArrayList(Collections.nCopies(61, false))

    private var calarmDatabase: CalarmDatabase? = null
    private var subCalarmDatabase: SubCalarmDatabase? = null
    private lateinit var adapter: MainCalarmEventAdapter

    @OptIn(DelicateCoroutinesApi::class)
    private val alarmListCallback = object: AlarmListCallback {
        override fun callBack(position: Int, id: Long) {
            editActivityResultLauncher.launch(Intent(requireContext(), EditCalarmActivity::class.java).apply {
                putExtra(IntentKey.POSITION, position)
                putExtra(IntentKey.ITEM_ID, id)
            })
        }

        override fun toggle(position: Int, id: Long, isEnabled: Boolean) {
            GlobalScope.launch(Dispatchers.IO) {
                adapter.items[position].isEnabled = isEnabled
                calarmDatabase?.getDao()?.update(adapter.items[position])
//                if (isEnabled) {
//                    val alarmTime = MyUtils.setAlarmClock(requireContext(), am, adapter.items[position])
//                    withContext(Dispatchers.Main) {
//                        Toast.makeText(requireContext(), MyUtils.timeDiffToString(requireContext(), Calendar.getInstance(), alarmTime), Toast.LENGTH_SHORT).show()
//                    }
//                } else {
//                    MyUtils.deleteAlarmClock(requireContext(), am, adapter.items[position])
//                }
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_main_calarm, container, false)
        binding.fragment = this

        sharedPreferences = requireContext().getSharedPreferences("${requireContext().packageName}_preferences", Context.MODE_PRIVATE)
        calarmDatabase = CalarmDatabase.getInstance(requireContext())
        subCalarmDatabase = SubCalarmDatabase.getInstance(requireContext())

        setHasOptionsMenu(true)
        (requireActivity() as AppCompatActivity).setSupportActionBar(binding.toolbar)
        (requireActivity() as AppCompatActivity).supportActionBar?.title = null

        return binding.root
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val calendarIdList = getCalendarIdList()

        val calendarViewManager = object: CalendarViewManager {
            override fun setCalendarViewResourceId(position: Int, date: Date, isSelected: Boolean): Int = R.layout.adapter_main_calarm_date
            override fun bindDataToCalendarView(holder: SingleRowCalendarAdapter.CalendarViewHolder, date: Date, position: Int, isSelected: Boolean) {
                val calendar = Calendar.getInstance().apply { time = date }
                val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
                val dateFormat = SimpleDateFormat("dd", Locale.getDefault())
                val tvDate: TextView = holder.itemView.findViewById(R.id.tv_date)
                val tvDay: TextView = holder.itemView.findViewById(R.id.tv_day)

                if (position == 60) {
                    val params = holder.itemView.layoutParams as (ViewGroup.MarginLayoutParams)
                    params.marginEnd = MyUtils.dpToPx(requireContext(), 16f)
                }

                tvDay.text = dayFormat.format(calendar.time)
                tvDate.text = dateFormat.format(calendar.time)

                val white = ContextCompat.getColor(requireContext(), R.color.white)
                val black = ContextCompat.getColor(requireContext(), R.color.black)
                val colorAccent = ContextCompat.getColor(requireContext(), R.color.colorAccent)
                val transparent = Color.parseColor("#26212A")

                if (calendarViewSelected[position]) {
                    if (isSelected) {
                        holder.itemView.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.colorAccent))
                        tvDay.setTextColor(black)
                        tvDate.setTextColor(black)
                    } else {

                        ValueAnimator.ofObject(ArgbEvaluator(), colorAccent, transparent).apply {
                            duration = 300
                            addUpdateListener {
                                holder.itemView.backgroundTintList = ColorStateList.valueOf(it.animatedValue as Int)
                            }
                        }.start()
                        ValueAnimator.ofObject(ArgbEvaluator(), black, white).apply {
                            duration = 300
                            addUpdateListener {
                                tvDay.setTextColor(it.animatedValue as Int)
                                tvDate.setTextColor(it.animatedValue as Int)
                            }
                        }.start()
                        calendarViewSelected[position] = false
                    }
                } else {
                    if (isSelected) {
                        ValueAnimator.ofObject(ArgbEvaluator(), transparent, colorAccent).apply {
                            duration = 500
                            addUpdateListener {
                                holder.itemView.backgroundTintList = ColorStateList.valueOf(it.animatedValue as Int)
                            }
                        }.start()
                        ValueAnimator.ofObject(ArgbEvaluator(), white, black).apply {
                            duration = 500
                            addUpdateListener {
                                tvDay.setTextColor(it.animatedValue as Int)
                                tvDate.setTextColor(it.animatedValue as Int)
                            }
                        }.start()
                        calendarViewSelected[position] = true
                    } else {
                        tvDay.setTextColor(white)
                        tvDate.setTextColor(white)
                    }
                }
            }
        }
        val calendarSelectionManager = object: CalendarSelectionManager {
            override fun canBeItemSelected(position: Int, date: Date): Boolean {
                val events = ArrayList<CalendarEvent>()
                val datas = ArrayList<CalarmEntity>()
                val calendar = Calendar.getInstance().apply { time = date }.timeZero()
                GlobalScope.launch(Dispatchers.IO) {
                    for (id in calendarIdList) {
                        if (id.second) {
                            events.addAll(getCalendarEventByDate(calendar.time, id.first))
                        }
                    }

                    for (event in events) {
                        val eventCalendar = Calendar.getInstance().apply { timeInMillis = event.startDate }
                        val data = calarmDatabase?.getDao()?.getByDataId(event.id) ?: CalarmEntity(event.id, event.calendarId, false, event.eventLocation, 0f, 0f, "", "", true)
                        data.subCalarms = subCalarmDatabase?.getDao()?.getByParentId(event.id) as ArrayList<SubCalarmEntity>
                        data.time = eventCalendar.timeInMillis
                        data.label = event.title

                        datas.add(data)
                    }

                    withContext(Dispatchers.Main) {
                        adapter = MainCalarmEventAdapter(datas).apply { setClickCallback(alarmListCallback) }
                        binding.listEvent.adapter = adapter
                    }

                }

                return true
            }
        }
        val calendarChangesObserver = object: CalendarChangesObserver {}

        binding.listDate.apply {
            this.calendarViewManager = calendarViewManager
            this.calendarSelectionManager = calendarSelectionManager
            this.calendarChangesObserver = calendarChangesObserver
            pastDaysCount = 30
            futureDaysCount = 30
            includeCurrentDate = true
            initialPositionIndex = 30
            init()
        }

        calendarViewSelected[30] = true
        binding.listDate.select(30)

        editActivityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val id = result.data?.getLongExtra(IntentKey.ITEM_ID, -1) ?: -1
                when (result.data?.getIntExtra(IntentKey.ACTION_TYPE, -1)) {
                    IntentValue.ACTION_EDIT -> {
//                        GlobalScope.launch(Dispatchers.IO) {
//                            val item = alarmDatabase?.getDao()?.get(id)
//                            item?.subAlarms = subAlarmDatabase?.getDao()?.getByParentId(id) as ArrayList<SubAlarmEntity>
//                            if (item != null) {
//                                withContext(Dispatchers.Main) {
//                                    adapter.edit(id, item)
//                                }
//                            }
//                            withContext(Dispatchers.Main) {
//                                binding.icNoAlarms.visibility = if (adapter.items.isEmpty()) View.VISIBLE else View.GONE
//                                binding.tvNoAlarms.visibility = if (adapter.items.isEmpty()) View.VISIBLE else View.GONE
//                            }
//                        }
                    }
                    IntentValue.ACTION_DELETE -> {
//                        adapter.delete(id)
//                        binding.icNoAlarms.visibility = if (adapter.items.isEmpty()) View.VISIBLE else View.GONE
//                        binding.tvNoAlarms.visibility = if (adapter.items.isEmpty()) View.VISIBLE else View.GONE
                    }
                }
            }
        }

    }

    private fun getCalendarIdList(): ArrayList<Pair<Long, Boolean>> {
        val list = ArrayList<Pair<Long, Boolean>>()
        val cursor = requireContext().contentResolver.query(Uri.parse("content://com.android.calendar/calendars"), arrayOf(CalendarContract.Calendars._ID), null, null, null)

        if (cursor != null) {
            if (cursor.count > 0) {
                cursor.moveToFirst()

                for (i in 0 until cursor.count) {
                    val value = sharedPreferences.getBoolean("calendar_id_${cursor.getLong(0)}", true)
                    list.add(Pair(cursor.getLong(0), value))

                    cursor.moveToNext()
                }
            }
        }

        cursor?.close()

        return list
    }

    private fun getCalendarEventByDate(date: Date, calendarId: Long): ArrayList<CalendarEvent> {
        val events = ArrayList<CalendarEvent>()
        val startTime = Calendar.getInstance().apply { time = date }.timeZero()
        val endTime = Calendar.getInstance().apply {
            time = startTime.time
            add(Calendar.DATE, 1)
        }

        requireContext().contentResolver.query(CalendarContract.Events.CONTENT_URI,
            arrayOf(
                CalendarContract.Events._ID,
                CalendarContract.Events.TITLE,
                CalendarContract.Events.DESCRIPTION,
                CalendarContract.Events.DTSTART,
                CalendarContract.Events.EVENT_TIMEZONE,
                CalendarContract.Events.DTEND,
                CalendarContract.Events.EVENT_END_TIMEZONE,
                CalendarContract.Events.ALL_DAY,
                CalendarContract.Events.EVENT_COLOR,
                CalendarContract.Events.EVENT_LOCATION,
            ), "(( ${CalendarContract.Events.DTSTART} >= ${startTime.timeInMillis} ) AND ( ${CalendarContract.Events.DTSTART} <= ${endTime.timeInMillis} ) AND ( ${CalendarContract.Events.CALENDAR_ID} = ${calendarId} ) AND ( deleted != 1 ))", null, CalendarContract.Events.DTSTART)?.use { cursor ->

            if (cursor.moveToFirst()) {
                do {
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
                } while (cursor.moveToNext())
            }

        }

        return events
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
        mListener = null
    }

    override fun onCreateOptionsMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.menu_main, menu)
        super.onCreateOptionsMenu(menu, menuInflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_settings -> {
                startActivity(Intent(requireContext(), SettingsActivity::class.java))
            }
        }
        return super.onOptionsItemSelected(item)
    }

    interface OnFragmentInteractionListener {
        fun onFragmentInteraction(uri: Uri)
    }

    companion object {
        @JvmStatic
        fun newInstance() = MainCalarmFragment().apply {
            val args = Bundle()
            arguments = args
        }
    }
}