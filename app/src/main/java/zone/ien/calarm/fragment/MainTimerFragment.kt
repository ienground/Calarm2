package zone.ien.calarm.fragment

import android.content.*
import android.net.Uri
import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import kotlinx.coroutines.*
import zone.ien.calarm.R
import zone.ien.calarm.adapter.MainTimerPageAdapter
import zone.ien.calarm.callback.TimerFragmentCallback
import zone.ien.calarm.databinding.FragmentMainTimerBinding
import zone.ien.calarm.room.SubTimerDatabase
import zone.ien.calarm.room.TimersDatabase
import java.util.*

class MainTimerFragment : Fragment() {

    lateinit var binding: FragmentMainTimerBinding
    private var mListener: OnFragmentInteractionListener? = null
    private var timersDatabase: TimersDatabase? = null
    private var subTimerDatabase: SubTimerDatabase? = null

    private var pagePosition = TIMER_PAGE_NUMPAD
    lateinit var pages: List<Fragment>
    lateinit var timerClockFragment: MainTimerClockFragment
    lateinit var timerListFragment: MainTimerListFragment
    lateinit var timerNumFragment: MainTimerNumFragment

    private val timerFragmentCallback: TimerFragmentCallback = object: TimerFragmentCallback {
        override fun scrollTo(page: Int) {
            pagePosition = page
            binding.viewpager.setCurrentItem(page, true)
        }

        override fun addNewTimer(id: Long) {
            // from numpad to list
            binding.viewpager.setCurrentItem(TIMER_PAGE_LIST, true)
            timerListFragment.refreshList()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_main_timer, container, false)
        binding.fragment = this

        setHasOptionsMenu(true)
//        (requireActivity() as AppCompatActivity).setSupportActionBar(binding.toolbar)
        (requireActivity() as AppCompatActivity).supportActionBar?.title = null

        return binding.root
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        timersDatabase = TimersDatabase.getInstance(requireContext())
        subTimerDatabase = SubTimerDatabase.getInstance(requireContext())

        timerClockFragment = MainTimerClockFragment().apply { setCallbackListener(timerFragmentCallback) }
        timerListFragment = MainTimerListFragment().apply { setCallbackListener(timerFragmentCallback) }
        timerNumFragment = MainTimerNumFragment().apply { setCallbackListener(timerFragmentCallback) }
        pages = listOf(timerClockFragment, timerListFragment, timerNumFragment)
        binding.viewpager.offscreenPageLimit = 3
        binding.viewpager.adapter = MainTimerPageAdapter(this, pages)
        binding.viewpager.setCurrentItem(pagePosition, false)
        binding.viewpager.isUserInputEnabled = false

        binding.viewpager.registerOnPageChangeCallback(object: ViewPager2.OnPageChangeCallback() {
            var currentState = 0
            var currentPos = 0

            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
                if (currentState == ViewPager2.SCROLL_STATE_DRAGGING && currentPos == position) {
                    if (currentPos == 0) binding.viewpager.currentItem = 2
                    else if (currentPos == 2) binding.viewpager.currentItem = 0
                }
                super.onPageScrolled(position, positionOffset, positionOffsetPixels)
            }

            override fun onPageSelected(position: Int) {
                currentPos = position
                super.onPageSelected(position)
            }

            override fun onPageScrollStateChanged(state: Int) {
                currentState = state
                super.onPageScrollStateChanged(state)
            }
        })

        GlobalScope.launch(Dispatchers.IO) {
            val data = timersDatabase?.getDao()?.getAll()
            withContext(Dispatchers.Main) {
                pagePosition = if (data?.isNotEmpty() == true) TIMER_PAGE_LIST else TIMER_PAGE_NUMPAD
                binding.viewpager.setCurrentItem(pagePosition, true)
            }
        }

    }

    override fun onStart() {
        super.onStart()
        binding.viewpager.setCurrentItem(pagePosition, false)
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

    interface OnFragmentInteractionListener {
        fun onFragmentInteraction(uri: Uri)
    }

    companion object {
        @JvmStatic
        fun newInstance() = MainTimerFragment().apply {
            val args = Bundle()
            arguments = args
        }

        const val TIMER_PAGE_TIMER = 0
        const val TIMER_PAGE_LIST = 1
        const val TIMER_PAGE_NUMPAD = 2
    }

}