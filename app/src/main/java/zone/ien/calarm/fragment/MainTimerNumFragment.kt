package zone.ien.calarm.fragment

import android.animation.ValueAnimator
import android.content.*
import android.net.Uri
import android.os.Bundle
import android.view.*
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import kotlinx.coroutines.*
import zone.ien.calarm.R
import zone.ien.calarm.callback.TimerFragmentCallback
import zone.ien.calarm.constant.IntentKey
import zone.ien.calarm.databinding.FragmentMainTimerNumBinding
import zone.ien.calarm.room.SubTimerDatabase
import zone.ien.calarm.room.SubTimerEntity
import zone.ien.calarm.room.TimersDatabase
import zone.ien.calarm.room.TimersEntity
import zone.ien.calarm.service.TimerService
import zone.ien.calarm.utils.MyUtils.Companion.timeToText
import java.util.*
import kotlin.collections.ArrayList

class MainTimerNumFragment : Fragment() {

    lateinit var binding: FragmentMainTimerNumBinding
    private var mListener: OnFragmentInteractionListener? = null

    private var timersDatabase: TimersDatabase? = null
    private var subTimerDatabase: SubTimerDatabase? = null

    private var callbackListener: TimerFragmentCallback? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_main_timer_num, container, false)
        binding.fragment = this

        setHasOptionsMenu(true)
//        (requireActivity() as AppCompatActivity).setSupportActionBar(binding.toolbar)
//        (requireActivity() as AppCompatActivity).supportActionBar?.title = null

        return binding.root
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        timersDatabase = TimersDatabase.getInstance(requireContext())
        subTimerDatabase = SubTimerDatabase.getInstance(requireContext())

        val nums: ArrayList<Int> = arrayListOf()
        val btnNum = listOf(binding.btnNum0, binding.btnNum1, binding.btnNum2, binding.btnNum3, binding.btnNum4, binding.btnNum5, binding.btnNum6, binding.btnNum7, binding.btnNum8, binding.btnNum9)

        binding.display.text = timeToText(requireContext(), nums, R.color.colorBLUE)
        btnNum.forEachIndexed { index, materialButton ->
            materialButton.setOnClickListener {
                if (nums.size < 6 && !(nums.isEmpty() && index == 0)) {
                    if (nums.isEmpty()) {
                        ValueAnimator.ofFloat(0.3f, 1f).apply {
                            addUpdateListener {
                                binding.btnSave.alpha = it.animatedValue as Float
                                binding.btnStart.alpha = it.animatedValue as Float
                            }
                        }.start()
                    }
                    nums.add(index)
                    binding.display.text = timeToText(requireContext(), nums, R.color.colorBLUE)
                }
            }
        }

        binding.btnDelete.setOnClickListener {
            if (nums.isNotEmpty()) {
                nums.removeLast()
                binding.display.text = timeToText(requireContext(), nums, R.color.colorBLUE)

                if (nums.isEmpty()) {
                    ValueAnimator.ofFloat(1f, 0.3f).apply {
                        addUpdateListener {
                            binding.btnSave.alpha = it.animatedValue as Float
                            binding.btnStart.alpha = it.animatedValue as Float
                        }
                    }.start()
                }
            }
        }

        binding.btnDelete.setOnLongClickListener {
            nums.clear()
            ValueAnimator.ofFloat(1f, 0.3f).apply {
                addUpdateListener {
                    binding.btnSave.alpha = it.animatedValue as Float
                    binding.btnStart.alpha = it.animatedValue as Float
                }
            }.start()
            binding.display.text = timeToText(requireContext(), nums, R.color.colorBLUE)
            true
        }

        binding.btnSave.setOnClickListener {
            if (nums.isNotEmpty()) {
                GlobalScope.launch(Dispatchers.IO) {
                    var duration = 0
                    val data: ArrayList<Int> = arrayListOf()
                    for (i in 0 until 6 - nums.size) data.add(0)
                    data.addAll(nums)

                    duration += 60 * 60 * (data[0] * 10 + data[1])
                    duration += 60 * (data[2] * 10 + data[3])
                    duration += (data[4] * 10 + data[5])

                    val timers = TimersEntity("", "", 0L, false, 0L)
                    val id = timersDatabase?.getDao()?.add(timers)

                    if (id != null) {
                        subTimerDatabase?.getDao()?.add(SubTimerEntity(id, "", duration, 0, ""))
                    }

                    withContext(Dispatchers.Main) {
                        nums.clear()
                        binding.display.text = timeToText(requireContext(), nums, R.color.colorBLUE)
                        binding.btnSave.alpha = 0.3f
                        binding.btnStart.alpha = 0.3f
                        callbackListener?.addNewTimer(0)
                    }
                }
            }
        }

        binding.btnStart.setOnClickListener {
            if (nums.isNotEmpty()) {
                var duration = 0L
                val data: ArrayList<Int> = arrayListOf()
                for (i in 0 until 6 - nums.size) data.add(0)
                data.addAll(nums)

                duration += 60 * 60 * (data[0] * 10 + data[1])
                duration += 60 * (data[2] * 10 + data[3])
                duration += (data[4] * 10 + data[5])

                nums.clear()

                binding.display.text = timeToText(requireContext(), nums, R.color.colorBLUE)
                binding.btnSave.alpha = 0.3f
                binding.btnStart.alpha = 0.3f
                callbackListener?.scrollTo(MainTimerFragment.TIMER_PAGE_TIMER)
                requireActivity().startForegroundService(Intent(requireContext(), TimerService::class.java).apply {
                    putExtra(IntentKey.DURATION, duration * 1000L)
                })
            }
        }

        binding.btnClose.setOnClickListener {
            callbackListener?.scrollTo(MainTimerFragment.TIMER_PAGE_LIST)
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
        //        menuInflater.inflate(R.menu.menu_main_home, menu)
        super.onCreateOptionsMenu(menu, menuInflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
        }
        return super.onOptionsItemSelected(item)
    }

    fun setCallbackListener(callbackListener: TimerFragmentCallback?) {
        this.callbackListener = callbackListener
    }

    interface OnFragmentInteractionListener {
        fun onFragmentInteraction(uri: Uri)
    }

    companion object {
        @JvmStatic
        fun newInstance() = MainTimerNumFragment().apply {
            val args = Bundle()
            arguments = args
        }
    }
}