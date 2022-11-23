package zone.ien.calarm.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.textview.MaterialTextView
import zone.ien.calarm.R
import zone.ien.calarm.callback.TimerListCallback
import zone.ien.calarm.room.TimersEntity

class MainTimerListAdapter(var items: ArrayList<TimersEntity>): RecyclerView.Adapter<MainTimerListAdapter.ItemViewHolder>() {

    lateinit var context: Context

    private var callbackListener: TimerListCallback? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        context = parent.context
        val view = LayoutInflater.from(context).inflate(R.layout.adapter_main_timer_list, parent, false)
        return ItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        items[holder.adapterPosition].label.let {
            if (it != "") {
                holder.tvLabel.visibility = View.VISIBLE
                holder.tvLabel.text = it
            } else {
                holder.tvLabel.visibility = View.GONE
            }
        }
        var duration = 0
        for (timer in items[holder.adapterPosition].subTimers) {
            duration += timer.time

            val chip = Chip(context)
            chip.typeface = ResourcesCompat.getFont(context, R.font.pretendard_regular)
            chip.text = String.format("%02d:%02d", timer.time / 60, timer.time % 60)

            holder.chipsMiniAlarm.addView(chip)
        }
        holder.tvTime.text = String.format("%02d:%02d", duration / 60, duration % 60)
        holder.badge.text = items[holder.adapterPosition].subTimers.size.toString()
        holder.btnDelete.setOnClickListener {
            callbackListener?.delete(holder.adapterPosition, items[holder.adapterPosition].id ?: -1L)
        }
        holder.btnStart.setOnClickListener {
            callbackListener?.start(holder.adapterPosition, items[holder.adapterPosition].id ?: -1L)
        }
        holder.itemView.setOnClickListener {
            callbackListener?.callBack(holder.adapterPosition, items[holder.adapterPosition].id ?: -1L)
        }
    }

    override fun getItemCount(): Int = items.size

    fun edit(id: Long, item: TimersEntity) {
//        val position = items.indexOfFirst { it.id == id }
//        if (position != -1) {
//            items[position] = item
//            items.sortBy { it.time }
//            val newPosition = items.indexOfFirst { it.id == id }
//
//            notifyItemMoved(position, newPosition)
//            notifyItemChanged(newPosition)
//        } else {
//            items.add(item)
//            items.sortBy { it.time }
//            val newPosition = items.indexOfFirst { it.id == id }
//
//            notifyItemInserted(newPosition)
//        }
    }

    fun delete(id: Long) {
        val position = items.indexOfFirst { it.id == id }
        items.removeAt(position)
        notifyItemRemoved(position)
    }

    fun setClickCallback(callbackListener: TimerListCallback) {
        this.callbackListener = callbackListener
    }

    inner class ItemViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        val tvLabel: MaterialTextView = itemView.findViewById(R.id.tv_label)
        val btnStart: ImageButton = itemView.findViewById(R.id.btn_start)
        val btnDelete: ImageButton = itemView.findViewById(R.id.btn_delete)
        val badge: MaterialTextView = itemView.findViewById(R.id.badge)
        val tvTime: MaterialTextView = itemView.findViewById(R.id.tv_time)
        val chipsMiniAlarm: ChipGroup = itemView.findViewById(R.id.chips_mini_alarm)
    }
}