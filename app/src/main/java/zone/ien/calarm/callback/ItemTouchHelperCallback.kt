package zone.ien.calarm.callback

import android.util.Log
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import zone.ien.calarm.activity.TAG
import zone.ien.calarm.adapter.SubAlarmAdapter
import zone.ien.calarm.adapter.SubCalarmAdapter
import zone.ien.calarm.adapter.SubTimerAdapter
import zone.ien.calarm.utils.ItemActionListener

class ItemTouchHelperCallback(private val listener: ItemActionListener): ItemTouchHelper.Callback() {

    override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
        val dragFlags = ItemTouchHelper.DOWN or ItemTouchHelper.UP
        val swipeFlags = ItemTouchHelper.START or ItemTouchHelper.END

        return makeMovementFlags(0, if (viewHolder is SubTimerAdapter.ItemViewHolder || viewHolder is SubAlarmAdapter.ItemViewHolder || viewHolder is SubCalarmAdapter.ItemViewHolder) swipeFlags else 0)
    }

    override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean { return false }
    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        listener.onItemSwiped(viewHolder.adapterPosition)
    }
}