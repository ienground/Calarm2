package zone.ien.calarm.fragment

import android.animation.ValueAnimator
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import kotlinx.coroutines.*
import zone.ien.calarm.MyApplication
import zone.ien.calarm.R
import zone.ien.calarm.callback.TimerFragmentCallback
import zone.ien.calarm.constant.IntentID
import zone.ien.calarm.constant.IntentKey
import zone.ien.calarm.databinding.FragmentMainTimerClockBinding
import zone.ien.calarm.receiver.TimerOffReceiver
import zone.ien.calarm.room.SubTimerDatabase
import zone.ien.calarm.room.SubTimerEntity
import zone.ien.calarm.room.TimersDatabase
import zone.ien.calarm.room.TimersEntity
import zone.ien.calarm.service.TimerService
import zone.ien.calarm.utils.MyUtils.Companion.round

class MainTimerClockFragment : Fragment() {

    lateinit var binding: FragmentMainTimerClockBinding
    private var mListener: OnFragmentInteractionListener? = null

    lateinit var nm: NotificationManager
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

        nm = requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        timersDatabase = TimersDatabase.getInstance(requireContext())
        subTimersDatabase = SubTimerDatabase.getInstance(requireContext())

        val blinkAnimation = AlphaAnimation(0f, 1f).apply {
            duration = 500
            repeatMode = Animation.REVERSE
            repeatCount = Animation.INFINITE
        }

        binding.btnAdd.setOnClickListener {
            callbackListener?.scrollTo(MainTimerFragment.TIMER_PAGE_NUMPAD)
        }

        binding.btnList.setOnClickListener {
            callbackListener?.scrollTo(MainTimerFragment.TIMER_PAGE_LIST)
        }

        binding.btnPlay.setOnClickListener {
            requireContext().sendBroadcast(Intent(IntentID.PLAY_PAUSE_TIMER))
        }

        binding.progressSub.setOnClickListener {
            requireContext().sendBroadcast(Intent(IntentID.PLAY_PAUSE_TIMER))
        }

        binding.progressSub.setProgressFormatter { _, _ -> "" }
        binding.progressTotal.setProgressFormatter { _, _ -> "" }

        requireContext().registerReceiver(object: BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val duration = intent.getLongExtra(IntentKey.DURATION, -1).toInt()
                if (item == null) {
                    GlobalScope.launch(Dispatchers.IO) {
                        val id = intent.getLongExtra(IntentKey.ITEM_ID, -1)
                        item = timersDatabase?.getDao()?.get(id)
                        item?.subTimers = subTimersDatabase?.getDao()?.getByParentId(id) as ArrayList<SubTimerEntity>

                        withContext(Dispatchers.Main) {
                            binding.progressTotal.max = duration
                            binding.tvLabel.text = item?.label
                            binding.btnAdd.icon = ContextCompat.getDrawable(context, R.drawable.ic_close)
                            binding.btnAdd.setOnClickListener {
                                context.sendBroadcast(Intent(context, TimerOffReceiver::class.java))
                                callbackListener?.scrollTo(MainTimerFragment.TIMER_PAGE_LIST)
                            }
                        }
                    }
                } else {
                    val countdownTime = intent.getLongExtra(IntentKey.COUNTDOWN_TIME, 0).toInt()
                    val order = intent.getIntExtra(IntentKey.ORDER, 0)
                    val subCountdownTime = (if (countdownTime != 0) (item?.subTimers?.get(order)?.time ?: 0) * 1000 - (intent.getLongExtra(IntentKey.STANDARD_TIME, 0).toInt() - countdownTime) else 0)

                    binding.tvLabelSub.text = item?.subTimers?.get(order)?.label
                    binding.tvTimeTotal.text = (countdownTime.round(1000) / 1000).let {
                        if (it / 3600 != 0) String.format("%02d:%02d:%02d", it / 3600, (it % 3600) / 60, it % 60)
                        else String.format("%02d:%02d", (it % 3600) / 60, it % 60)
                    }
                    binding.tvTimeSub.text = (subCountdownTime.round(1000) / 1000).let {
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

                    if (intent.getBooleanExtra(IntentKey.IS_FINISHED, false)) {
                        item = null
                        binding.backgroundCircle.visibility = View.VISIBLE
                        binding.tvTimeTotal.setTextColor(ContextCompat.getColor(context, R.color.white))
                        binding.tvTimeSub.setTextColor(ContextCompat.getColor(context, R.color.white))
                        binding.tvLabelSub.setTextColor(ContextCompat.getColor(context, R.color.white))
                        binding.tvTimeTotal.visibility = View.GONE
                        binding.tvLabelSub.visibility = View.GONE
                    }
                }
            }
        }, IntentFilter(IntentID.COUNTDOWN_TICK))

        requireContext().registerReceiver(object: BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                binding.tvTimeSub.text = "-${intent.getIntExtra(IntentKey.COUNTDOWN_TIME, 0).let {
                    if (it / 3600 != 0) String.format("%02d:%02d:%02d", it / 3600, (it % 3600) / 60, it % 60)
                    else if (it / 60 != 0) String.format("%02d:%02d", (it % 3600) / 60, it % 60)
                    else it.toString()
                }}"
                binding.btnAdd.setOnClickListener {
                    context.sendBroadcast(Intent(context, TimerOffReceiver::class.java))
                }
            }
        }, IntentFilter(IntentID.COUNTDOWN_TICK_TIMEOUT))

        requireContext().registerReceiver(object: BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                binding.btnPlay.icon = ContextCompat.getDrawable(requireContext(), if (TimerService.isPaused) R.drawable.ic_pause else R.drawable.ic_play_arrow)
                if (TimerService.isPaused) {
                    binding.tvTimeTotal.clearAnimation()
                    binding.tvTimeSub.clearAnimation()
                } else {
                    binding.tvTimeTotal.startAnimation(blinkAnimation)
                    binding.tvTimeSub.startAnimation(blinkAnimation)
                }
            }
        }, IntentFilter(IntentID.PLAY_PAUSE_TIMER))

        requireContext().registerReceiver(object: BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                binding.backgroundCircle.visibility = View.GONE
                binding.tvTimeTotal.setTextColor(ContextCompat.getColor(context, R.color.black))
                binding.tvTimeSub.setTextColor(ContextCompat.getColor(context, R.color.black))
                binding.tvLabelSub.setTextColor(ContextCompat.getColor(context, R.color.black))
                binding.tvTimeTotal.visibility = View.VISIBLE
                binding.tvLabelSub.visibility = View.VISIBLE
                callbackListener?.scrollTo(MainTimerFragment.TIMER_PAGE_LIST)
            }
        }, IntentFilter(IntentID.STOP_TIMER))



    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnFragmentInteractionListener) {
            mListener = context
        } else {
            throw RuntimeException("$context must implement OnFragmentInteractionListener")
        }
    }

    override fun onResume() {
        super.onResume()
        MyApplication.timerWindowResumed()
        binding.btnPlay.icon = ContextCompat.getDrawable(requireContext(), if (!TimerService.isPaused) R.drawable.ic_pause else R.drawable.ic_play_arrow)
    }

    override fun onPause() {
        super.onPause()
        MyApplication.timerWindowPaused()
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