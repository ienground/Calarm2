package zone.ien.calarm.fragment

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.*
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.ItemTouchHelper
import com.google.android.material.snackbar.Snackbar
import zone.ien.calarm.R
import zone.ien.calarm.adapter.MainAlarmListAdapter
import zone.ien.calarm.adapter.MainCalarmDateAdapter
import zone.ien.calarm.adapter.MainCalarmEventAdapter
import zone.ien.calarm.adapter.MainTimerListAdapter
import zone.ien.calarm.databinding.FragmentMainAlarmBinding
import zone.ien.calarm.databinding.FragmentMainCalarmBinding
import zone.ien.calarm.databinding.FragmentMainTimerBinding
import zone.ien.calarm.databinding.FragmentMainTimerClockBinding
import zone.ien.calarm.databinding.FragmentMainTimerListBinding
import java.util.*

class MainTimerClockFragment : Fragment() {

    lateinit var binding: FragmentMainTimerClockBinding
    private var mListener: OnFragmentInteractionListener? = null


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
        fun newInstance() = MainTimerClockFragment().apply {
            val args = Bundle()
            arguments = args
        }
    }
}