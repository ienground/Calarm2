package zone.ien.calarm.fragment

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.app.Activity.RESULT_OK
import android.app.AlarmManager
import android.content.*
import android.net.Uri
import android.os.Bundle
import android.text.StaticLayout
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import kotlinx.coroutines.*
import zone.ien.calarm.R
import zone.ien.calarm.activity.DeskclockActivity
import zone.ien.calarm.activity.EditAlarmActivity
import zone.ien.calarm.activity.SettingsActivity
import zone.ien.calarm.activity.TAG
import zone.ien.calarm.adapter.MainAlarmListAdapter
import zone.ien.calarm.callback.AlarmListCallback
import zone.ien.calarm.constant.IntentKey
import zone.ien.calarm.constant.IntentValue
import zone.ien.calarm.databinding.FragmentMainAlarmBinding
import zone.ien.calarm.room.AlarmDatabase
import zone.ien.calarm.room.AlarmEntity
import zone.ien.calarm.room.SubAlarmDatabase
import zone.ien.calarm.room.SubAlarmEntity
import zone.ien.calarm.utils.MyUtils
import java.util.*
import kotlin.collections.ArrayList

class MainAlarmFragment : Fragment() {

    lateinit var binding: FragmentMainAlarmBinding
    private var mListener: OnFragmentInteractionListener? = null
    lateinit var editActivityResultLauncher: ActivityResultLauncher<Intent>

    private var alarmDatabase: AlarmDatabase? = null
    private var subAlarmDatabase: SubAlarmDatabase? = null
    private lateinit var adapter: MainAlarmListAdapter

    lateinit var am: AlarmManager

    @OptIn(DelicateCoroutinesApi::class)
    private val alarmListCallback = object: AlarmListCallback {
        override fun callBack(position: Int, id: Long) {
            editActivityResultLauncher.launch(Intent(requireContext(), EditAlarmActivity::class.java).apply {
                putExtra(IntentKey.POSITION, position)
                putExtra(IntentKey.ITEM_ID, id)
            })
        }

        override fun toggle(position: Int, id: Long, isEnabled: Boolean) {
            GlobalScope.launch(Dispatchers.IO) {
                adapter.items[position].isEnabled = isEnabled
                alarmDatabase?.getDao()?.update(adapter.items[position])
                if (isEnabled) {
                    val alarmTime = MyUtils.setAlarmClock(requireContext(), am, adapter.items[position])
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), MyUtils.timeDiffToString(requireContext(), Calendar.getInstance(), alarmTime), Toast.LENGTH_SHORT).show()
                    }
                } else {
                    MyUtils.deleteAlarmClock(requireContext(), am, adapter.items[position])
                }

                withContext(Dispatchers.Main) {
                    var activeAlarmsCount = 0
                    for (entity in adapter.items) {
                        if (entity.isEnabled) activeAlarmsCount++
                    }
                    binding.appTitle.text = getString(R.string.active_alarm_count, activeAlarmsCount)
                }
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_main_alarm, container, false)
        binding.fragment = this

        setHasOptionsMenu(true)
        (requireActivity() as AppCompatActivity).setSupportActionBar(binding.toolbar)
        (requireActivity() as AppCompatActivity).supportActionBar?.title = null

        return binding.root
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        alarmDatabase = AlarmDatabase.getInstance(requireContext())
        subAlarmDatabase = SubAlarmDatabase.getInstance(requireContext())
        am = requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager

        binding.subTitle.text = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
            in 6..10 -> getString(R.string.user_hello_morning)
            in 11..16 -> getString(R.string.user_hello_afternoon)
            in 17..20 -> getString(R.string.user_hello_evening)
            else -> getString(R.string.user_hello_night)
        }

        binding.btnAdd.setOnClickListener {
            editActivityResultLauncher.launch(Intent(requireContext(), EditAlarmActivity::class.java))
        }

        binding.shimmerFrame.startShimmer()
        binding.shimmerFrame.visibility = View.VISIBLE
        binding.list.visibility = View.INVISIBLE
        binding.list.alpha = 0f

        GlobalScope.launch(Dispatchers.IO) {
            val item = alarmDatabase?.getDao()?.getAll() as ArrayList
            var activeAlarmsCount = 0
            item.sortBy { it.time }
            for (entity in item) {
                val subAlarms = subAlarmDatabase?.getDao()?.getByParentId(entity.id ?: -1)
                entity.subAlarms = subAlarms as ArrayList<SubAlarmEntity>
                if (entity.isEnabled) activeAlarmsCount++
            }

            adapter = MainAlarmListAdapter(item).apply {
                setClickCallback(alarmListCallback)
            }
            withContext(Dispatchers.Main) {
                delay(1000)
                binding.list.adapter = adapter
                binding.icNoAlarms.visibility = if (item.isEmpty()) View.VISIBLE else View.GONE
                binding.tvNoAlarms.visibility = if (item.isEmpty()) View.VISIBLE else View.GONE
                binding.shimmerFrame.stopShimmer()

                ValueAnimator.ofFloat(0f, 1f).apply {
                    duration = 500
                    addUpdateListener {
                        binding.list.alpha = it.animatedValue as Float
                        binding.appTitle.alpha = it.animatedValue as Float
                        binding.shimmerFrame.alpha = 1f - it.animatedValue as Float
                    }
                    addListener(object: AnimatorListenerAdapter() {
                        override fun onAnimationStart(animation: Animator) {
                            super.onAnimationStart(animation)
                            binding.list.visibility = View.VISIBLE
                            binding.appTitle.text = binding.appTitle.context.getString(R.string.active_alarm_count, activeAlarmsCount)
                        }
                        override fun onAnimationEnd(animation: Animator) {
                            super.onAnimationEnd(animation)
                            binding.shimmerFrame.visibility = View.INVISIBLE
                        }
                    })
                }.start()
            }
        }

        editActivityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val id = result.data?.getLongExtra(IntentKey.ITEM_ID, -1) ?: -1
                when (result.data?.getIntExtra(IntentKey.ACTION_TYPE, -1)) {
                    IntentValue.ACTION_EDIT -> {
                        GlobalScope.launch(Dispatchers.IO) {
                            val item = alarmDatabase?.getDao()?.get(id)
                            item?.subAlarms = subAlarmDatabase?.getDao()?.getByParentId(id) as ArrayList<SubAlarmEntity>
                            if (item != null) {
                                withContext(Dispatchers.Main) {
                                    adapter.edit(id, item)
                                }
                            }
                            withContext(Dispatchers.Main) {
                                binding.icNoAlarms.visibility = if (adapter.items.isEmpty()) View.VISIBLE else View.GONE
                                binding.tvNoAlarms.visibility = if (adapter.items.isEmpty()) View.VISIBLE else View.GONE

                                var activeAlarmsCount = 0
                                for (entity in adapter.items) {
                                    if (entity.isEnabled) activeAlarmsCount++
                                }
                                binding.appTitle.text = getString(R.string.active_alarm_count, activeAlarmsCount)
                            }
                        }
                    }
                    IntentValue.ACTION_DELETE -> {
                        adapter.delete(id)
                        binding.icNoAlarms.visibility = if (adapter.items.isEmpty()) View.VISIBLE else View.GONE
                        binding.tvNoAlarms.visibility = if (adapter.items.isEmpty()) View.VISIBLE else View.GONE

                        var activeAlarmsCount = 0
                        for (entity in adapter.items) {
                            if (entity.isEnabled) activeAlarmsCount++
                        }
                        binding.appTitle.text = getString(R.string.active_alarm_count, activeAlarmsCount)
                    }
                }
            }
        }
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
            R.id.menu_deskclock -> {
                startActivity(Intent(requireContext(), DeskclockActivity::class.java))
            }
        }
        return super.onOptionsItemSelected(item)
    }

    interface OnFragmentInteractionListener {
        fun onFragmentInteraction(uri: Uri)
    }

    companion object {
        @JvmStatic
        fun newInstance() = MainAlarmFragment().apply {
            val args = Bundle()
            arguments = args
        }
    }
}