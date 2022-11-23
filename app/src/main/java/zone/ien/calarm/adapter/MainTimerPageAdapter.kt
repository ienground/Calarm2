package zone.ien.calarm.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import zone.ien.calarm.fragment.MainTimerClockFragment
import zone.ien.calarm.fragment.MainTimerListFragment
import zone.ien.calarm.fragment.MainTimerNumFragment

class MainTimerPageAdapter(fragment: Fragment): FragmentStateAdapter(fragment) {

    override fun createFragment(position: Int): Fragment {
        return when (position % PAGE_NUMBER) {
            0 -> MainTimerClockFragment()
            1 -> MainTimerListFragment()
            2 -> MainTimerNumFragment()
            else -> MainTimerListFragment()
        }
    }

    override fun getItemCount(): Int = Int.MAX_VALUE

    companion object {
        internal const val PAGE_NUMBER = 3
        const val LOOP_COUNT = 1000
    }
}