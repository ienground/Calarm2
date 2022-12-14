package zone.ien.calarm.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.textview.MaterialTextView
import zone.ien.calarm.R
import zone.ien.calarm.callback.TimerListCallback
import zone.ien.calarm.room.TimersEntity
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class MainTimerListAdapter(var items: ArrayList<TimersEntity>): RecyclerView.Adapter<MainTimerListAdapter.ItemViewHolder>() {

    lateinit var context: Context

    private var callbackListener: TimerListCallback? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        context = parent.context
        val view = LayoutInflater.from(context).inflate(R.layout.adapter_main_timer_list, parent, false)
        return ItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val dateTimeFormat = SimpleDateFormat(context.getString(R.string.dateTimeFormat), Locale.getDefault())
        items[holder.adapterPosition].label.let {
            if (it != "") {
                holder.tvLabel.visibility = View.VISIBLE
                holder.tvLabel.text = it
            } else {
                holder.tvLabel.visibility = View.GONE
            }
        }
        holder.chipsMiniTimer.removeAllViews()
        var duration = 0
        items[holder.adapterPosition].subTimers.forEach { timer ->
            duration += timer.time

            val chip = Chip(context)
            chip.isCheckable = true
            chip.typeface = ResourcesCompat.getFont(context, R.font.pretendard_regular)
            chip.text = timer.time.let {
                if (it / 3600 != 0) String.format("%02d:%02d:%02d", it / 3600, (it % 3600) / 60, it % 60)
                else String.format("%02d:%02d", (it % 3600) / 60, it % 60)
            }

            chip.setOnCheckedChangeListener { compoundButton, b ->
                chip.text = if (b) timer.label.let { if (it != "") it else context.getString(R.string.no_label) } else timer.time.let {
                    if (it / 3600 != 0) String.format("%02d:%02d:%02d", it / 3600, (it % 3600) / 60, it % 60)
                    else String.format("%02d:%02d", (it % 3600) / 60, it % 60)
                }
            }

            holder.chipsMiniTimer.addView(chip)
        }
        if (items[holder.adapterPosition].isScheduled) {
            holder.icSchedule.visibility = View.VISIBLE
            holder.tvSchedule.visibility = View.VISIBLE
            holder.tvSchedule.text = dateTimeFormat.format(Date(items[holder.adapterPosition].scheduledTime))
        } else {
            holder.icSchedule.visibility = View.GONE
            holder.tvSchedule.visibility = View.GONE
        }
        holder.tvTime.text = duration.let {
            if (it / 3600 != 0) String.format("%02d:%02d:%02d", it / 3600, (it % 3600) / 60, it % 60)
            else String.format("%02d:%02d", (it % 3600) / 60, it % 60)
        }
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
        val position = items.indexOfFirst { it.id == id }
        items[position] = item
        notifyItemChanged(position)
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
        val btnStart: MaterialButton = itemView.findViewById(R.id.btn_start)
        val btnDelete: MaterialButton = itemView.findViewById(R.id.btn_delete)
        val badge: MaterialTextView = itemView.findViewById(R.id.badge)
        val tvTime: MaterialTextView = itemView.findViewById(R.id.tv_time)
        val chipsMiniTimer: ChipGroup = itemView.findViewById(R.id.chips_mini_timer)
        val icSchedule: ImageView = itemView.findViewById(R.id.ic_schedule)
        val tvSchedule: MaterialTextView = itemView.findViewById(R.id.tv_schedule)
    }
}