package zone.ien.calarm.adapter

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textview.MaterialTextView
import zone.ien.calarm.R
import zone.ien.calarm.activity.TAG
import zone.ien.calarm.data.StopwatchLapse

class LapseAdapter(var items: ArrayList<StopwatchLapse>): RecyclerView.Adapter<LapseAdapter.ItemViewHolder>() {

    lateinit var context: Context

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        context = parent.context
        val view = LayoutInflater.from(context).inflate(R.layout.adapter_lapse, parent, false)
        return ItemViewHolder(view)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        if (holder.adapterPosition != 0) (holder.itemView as MaterialCardView).setCardBackgroundColor(Color.parseColor("#26212a"))
        else (holder.itemView as MaterialCardView).setCardBackgroundColor(Color.parseColor("#49454f"))

        holder.tvLapNo.text = context.getString(R.string.lap_no, itemCount - holder.adapterPosition)
        holder.tvFlag.text = items[holder.adapterPosition].flag
        holder.tvTimeFull.text = items[holder.adapterPosition].time.let { String.format("%d %02d.%02d", (it / 1000) / 60, (it / 1000) % 60, (it % 1000) / 10) }
        holder.tvTimeLap.text = (items[holder.adapterPosition].time - (if (holder.adapterPosition != itemCount - 1) items[holder.adapterPosition + 1].time else 0)).let { String.format("%d %02d.%02d", (it / 1000) / 60, (it / 1000) % 60, (it % 1000) / 10) }
    }

    fun add(item: StopwatchLapse) {
        items.add(0, item)
        notifyItemInserted(0)
        if (itemCount > 1) notifyItemChanged(1)
    }

    inner class ItemViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        val tvLapNo: MaterialTextView = itemView.findViewById(R.id.tv_lap_no)
        val tvFlag: MaterialTextView = itemView.findViewById(R.id.tv_flag)
        val tvTimeLap: MaterialTextView = itemView.findViewById(R.id.tv_time_lap)
        val tvTimeFull: MaterialTextView = itemView.findViewById(R.id.tv_time_full)
    }
}