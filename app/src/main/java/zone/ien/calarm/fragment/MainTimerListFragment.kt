package zone.ien.calarm.fragment

import android.app.Activity
import android.content.*
import android.net.Uri
import android.os.Bundle
import android.view.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.*
import zone.ien.calarm.R
import zone.ien.calarm.activity.EditTimerActivity
import zone.ien.calarm.adapter.MainTimerListAdapter
import zone.ien.calarm.callback.TimerFragmentCallback
import zone.ien.calarm.callback.TimerListCallback
import zone.ien.calarm.constant.IntentKey
import zone.ien.calarm.constant.IntentValue
import zone.ien.calarm.databinding.FragmentMainTimerListBinding
import zone.ien.calarm.room.*
import zone.ien.calarm.service.TimerService
import java.util.*
import kotlin.collections.ArrayList

class MainTimerListFragment : Fragment() {

    lateinit var binding: FragmentMainTimerListBinding
    private var mListener: OnFragmentInteractionListener? = null
    lateinit var editActivityResultLauncher: ActivityResultLauncher<Intent>

    private var timersDatabase: TimersDatabase? = null
    private var subTimerDatabase: SubTimerDatabase? = null
    private lateinit var adapter: MainTimerListAdapter
    private var callbackListener: TimerFragmentCallback? = null

    @OptIn(DelicateCoroutinesApi::class)
    private val timerListCallback: TimerListCallback = object: TimerListCallback {
        override fun callBack(position: Int, id: Long) {
            editActivityResultLauncher.launch(Intent(requireContext(), EditTimerActivity::class.java).apply {
                putExtra(IntentKey.ITEM_ID, id)
            })
        }

        override fun delete(position: Int, id: Long) {
            MaterialAlertDialogBuilder(requireContext()).apply {
                setMessage(R.string.delete_title)
                setPositiveButton(android.R.string.ok) { _, _ ->
                    GlobalScope.launch(Dispatchers.IO) {
                        timersDatabase?.getDao()?.delete(id)
                        subTimerDatabase?.getDao()?.deleteParentId(id)

                        withContext(Dispatchers.Main) {
                            adapter.delete(id)
                            if (adapter.items.isEmpty()) {
                                callbackListener?.scrollTo(MainTimerFragment.TIMER_PAGE_NUMPAD)
                            }
                        }
                    }
                }
                setNegativeButton(android.R.string.cancel) { _, _ -> }
            }.show()
        }

        override fun start(position: Int, id: Long) {
            callbackListener?.scrollTo(MainTimerFragment.TIMER_PAGE_TIMER)
            requireActivity().startForegroundService(Intent(requireContext(), TimerService::class.java).apply {
                putExtra(IntentKey.ITEM_ID, id)
            })
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_main_timer_list, container, false)
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

        refreshList()

        binding.btnAdd.setOnClickListener {
            callbackListener?.scrollTo(MainTimerFragment.TIMER_PAGE_NUMPAD)
        }

        editActivityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val id = result.data?.getLongExtra(IntentKey.ITEM_ID, -1) ?: -1
                when (result.data?.getIntExtra(IntentKey.ACTION_TYPE, -1)) {
                    IntentValue.ACTION_EDIT -> {
                        GlobalScope.launch(Dispatchers.IO) {
                            val item = timersDatabase?.getDao()?.get(id)
                            item?.subTimers = subTimerDatabase?.getDao()?.getByParentId(id) as ArrayList<SubTimerEntity>
                            if (item != null) {
                                withContext(Dispatchers.Main) {
                                    adapter.edit(id, item)
                                }
                            }
                        }
                    }
                    IntentValue.ACTION_DELETE -> {
                        adapter.delete(id)
                        if (adapter.items.isEmpty()) {
                            callbackListener?.scrollTo(MainTimerFragment.TIMER_PAGE_NUMPAD)
                        }
                    }
                }
            }
        }

    }

    @OptIn(DelicateCoroutinesApi::class)
    fun refreshList() {
        GlobalScope.launch(Dispatchers.IO) {
            val timers = timersDatabase?.getDao()?.getAll()
            if (timers != null) {
                for (timer in timers) {
                    val subTimers = subTimerDatabase?.getDao()?.getByParentId(timer.id ?: -1L)
                    timer.subTimers = subTimers as ArrayList<SubTimerEntity>
                }

                withContext(Dispatchers.Main) {
                    adapter = MainTimerListAdapter(timers as ArrayList<TimersEntity>).apply {
                        setClickCallback(timerListCallback)
                    }
                    binding.list.adapter = adapter
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
        fun newInstance() = MainTimerListFragment().apply {
            val args = Bundle()
            arguments = args
        }
    }
}