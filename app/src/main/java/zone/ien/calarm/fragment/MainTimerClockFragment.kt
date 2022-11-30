package zone.ien.calarm.fragment

import android.animation.ValueAnimator
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import kotlinx.coroutines.*
import zone.ien.calarm.R
import zone.ien.calarm.activity.TAG
import zone.ien.calarm.callback.TimerFragmentCallback
import zone.ien.calarm.constant.IntentID
import zone.ien.calarm.constant.IntentKey
import zone.ien.calarm.databinding.FragmentMainTimerClockBinding
import zone.ien.calarm.room.SubTimerDatabase
import zone.ien.calarm.room.SubTimerEntity
import zone.ien.calarm.room.TimersDatabase
import zone.ien.calarm.room.TimersEntity

class MainTimerClockFragment : Fragment() {

    lateinit var binding: FragmentMainTimerClockBinding
    private var mListener: OnFragmentInteractionListener? = null

    private var callbackListener: TimerFragmentCallback? = null
    private var timersDatabase: TimersDatabase? = null
    private var subTimersDatabase: SubTimerDatabase? = null

    private var item: TimersEntity? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_main_timer_clock, container, false)
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
        subTimersDatabase = SubTimerDatabase.getInstance(requireContext())

        binding.btnAdd.setOnClickListener {
            callbackListener?.scrollTo(MainTimerFragment.TIMER_PAGE_NUMPAD)
        }

        binding.btnList.setOnClickListener {
            callbackListener?.scrollTo(MainTimerFragment.TIMER_PAGE_LIST)
        }

        binding.progressSub.setProgressFormatter { _, _ -> "" }
        binding.progressTotal.setProgressFormatter { _, _ -> "" }

        requireContext().registerReceiver(object: BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                Log.d(TAG, "clock ${intent.getLongExtra(IntentKey.COUNTDOWN_TIME, -1)}")
                val duration = intent.getLongExtra(IntentKey.DURATION, -1).toInt()
                if (item == null) {
                    GlobalScope.launch(Dispatchers.IO) {
                        val id = intent.getLongExtra(IntentKey.ITEM_ID, -1)
                        item = timersDatabase?.getDao()?.get(id)
                        item?.subTimers = subTimersDatabase?.getDao()?.getByParentId(id) as ArrayList<SubTimerEntity>

                        withContext(Dispatchers.Main) {
                            binding.progressTotal.max = duration
                            binding.tvLabel.text = item?.label
                        }
                    }
                }

                val countdownTime = intent.getLongExtra(IntentKey.COUNTDOWN_TIME, -1).toInt()
                val order = intent.getIntExtra(IntentKey.ORDER, 0)
                val subCountdownTime = (item?.subTimers?.get(order)?.time ?: 0) * 1000 - (intent.getLongExtra(IntentKey.STANDARD_TIME, 0).toInt() - countdownTime)

                binding.tvLabelSub.text = item?.subTimers?.get(order)?.label
                binding.progressTotal.progress = countdownTime
                binding.tvTimeTotal.text = (countdownTime / 1000).let {
                    if (it / 3600 != 0) String.format("%02d:%02d:%02d", it / 3600, (it % 3600) / 60, it % 60)
                    else String.format("%02d:%02d", (it % 3600) / 60, it % 60)
                }
                binding.tvTimeSub.text = (subCountdownTime / 1000).let {
                    if (it / 3600 != 0) String.format("%02d:%02d:%02d", it / 3600, (it % 3600) / 60, it % 60)
                    else String.format("%02d:%02d", (it % 3600) / 60, it % 60)
                }
                binding.progressSub.max = (item?.subTimers?.get(order)?.time ?: 0) * 1000
                ValueAnimator.ofInt(countdownTime, if (countdownTime >= 500) countdownTime - 500 else 0).apply {
                    this.duration = 500
                    addUpdateListener {
                        binding.progressTotal.progress = (it.animatedValue as Int)
                    }
                }.start()
                ValueAnimator.ofInt(subCountdownTime, if (subCountdownTime >= 500) subCountdownTime - 500 else 0).apply {
                    this.duration = 500
                    addUpdateListener {
                        binding.progressSub.progress = (it.animatedValue as Int)
                    }
                }.start()
            }
        }, IntentFilter(IntentID.COUNTDOWN_TICK))

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
            //            R.id.menu_share -> {
            //                startActivity(Intent(MainActivity.instance, ShareImageActivity::class.java))
            //            }
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
        fun newInstance() = MainTimerClockFragment().apply {
            val args = Bundle()
            arguments = args
        }
    }
}