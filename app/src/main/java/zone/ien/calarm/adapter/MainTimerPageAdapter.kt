package zone.ien.calarm.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.adapter.FragmentViewHolder
import zone.ien.calarm.callback.TimerFragmentCallback
import zone.ien.calarm.fragment.MainTimerClockFragment
import zone.ien.calarm.fragment.MainTimerListFragment
import zone.ien.calarm.fragment.MainTimerNumFragment

class MainTimerPageAdapter(fragment: Fragment, var pages: List<Fragment>): FragmentStateAdapter(fragment) {

    private var callbackListener: TimerFragmentCallback? = null

    override fun createFragment(position: Int): Fragment {
        return pages[position % pages.size]
    }

    override fun getItemCount(): Int = 3//Int.MAX_VALUE

    companion object {
        internal const val PAGE_NUMBER = 3
        const val LOOP_COUNT = 1000
    }
}