package zone.ien.calarm.activity

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.google.android.material.timepicker.MaterialTimePicker
import zone.ien.calarm.R
import zone.ien.calarm.adapter.MainCalarmDateAdapter
import zone.ien.calarm.adapter.MainTimerPageAdapter
import zone.ien.calarm.databinding.ActivityMainBinding
import zone.ien.calarm.fragment.*

class MainActivity : AppCompatActivity(),
    MainAlarmFragment.OnFragmentInteractionListener,
    MainCalarmFragment.OnFragmentInteractionListener,
    MainTimerFragment.OnFragmentInteractionListener,
    MainStopwatchFragment.OnFragmentInteractionListener,
    MainTimerListFragment.OnFragmentInteractionListener,
    MainTimerClockFragment.OnFragmentInteractionListener,
    MainTimerNumFragment.OnFragmentInteractionListener{

    lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        binding.activity = this

        loadFragment(MainAlarmFragment())

        binding.bottomNav.setOnItemSelectedListener {
            loadFragment(
                when (it.itemId) {
                    R.id.navigation_alarm -> MainAlarmFragment()
                    R.id.navigation_calarm -> MainCalarmFragment()
                    R.id.navigation_timer -> MainTimerFragment()
                    R.id.navigation_stopwatch -> MainStopwatchFragment()
                    else -> MainAlarmFragment()
                }
            )
        }

        MaterialTimePicker
            .Builder()
            .setTitleText("Select a time")
            .build()
//            .show(supportFragmentManager, "TIME_PICKER")

//        startActivity(Intent(this, EditAlarmActivity::class.java))


    }

    private fun loadFragment(fragment: Fragment?): Boolean {
        if (fragment != null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.container, fragment)
                .commit()
            return true
        }
        return false
    }

    override fun onFragmentInteraction(uri: Uri) {}
}