package zone.ien.calarm.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import zone.ien.calarm.fragment.MainTimerClockFragment
import zone.ien.calarm.fragment.MainTimerListFragment
import zone.ien.calarm.fragment.MainTimerNumFragment

class MainTimerPageAdapter(fragment: Fragment): FragmentStateAdapter(fragment) {
    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> MainTimerListFragment()
            1 -> MainTimerClockFragment()
            2 -> MainTimerNumFragment()
            else -> MainTimerListFragment()
        }
    }

    override fun getItemCount(): Int = PAGE_NUMBER

    companion object {
        internal const val PAGE_NUMBER = 3
    }
}