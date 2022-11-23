package zone.ien.calarm.fragment

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.*
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import zone.ien.calarm.R
import zone.ien.calarm.callback.TimerFragmentCallback
import zone.ien.calarm.databinding.FragmentMainTimerClockBinding

class MainTimerClockFragment : Fragment() {

    lateinit var binding: FragmentMainTimerClockBinding
    private var mListener: OnFragmentInteractionListener? = null

    private var callbackListener: TimerFragmentCallback? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_main_timer_clock, container, false)
        binding.fragment = this

        setHasOptionsMenu(true)
//        (requireActivity() as AppCompatActivity).setSupportActionBar(binding.toolbar)
//        (requireActivity() as AppCompatActivity).supportActionBar?.title = null

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnAdd.setOnClickListener {
            callbackListener?.scrollTo(MainTimerFragment.TIMER_PAGE_NUMPAD)
        }

        binding.btnList.setOnClickListener {
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