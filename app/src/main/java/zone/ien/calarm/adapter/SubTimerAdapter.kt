package zone.ien.calarm.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.textview.MaterialTextView
import zone.ien.calarm.R
import zone.ien.calarm.room.SubAlarmEntity
import zone.ien.calarm.room.SubTimerEntity

class SubTimerAdapter(var items: ArrayList<SubTimerEntity>): RecyclerView.Adapter<SubTimerAdapter.ItemViewHolder>() {

    lateinit var context: Context

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        context = parent.context
        val view = LayoutInflater.from(context).inflate(R.layout.adapter_sub_timer, parent, false)
        return ItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        items[holder.adapterPosition].time.let {
            holder.tvTime.text =
                if (it / 60 != 0 && it % 60 != 0) context.getString(R.string.time_format_hour_minute, it / 60, it % 60)
                else if (it / 60 != 0) context.getString(R.string.time_format_hour, it / 60)
                else context.getString(R.string.time_format_minute, it % 60)
        }
//        holder.switchOn.isChecked = items[holder.adapterPosition].isEnabled
//        holder.btnDelete.setOnClickListener {
//            items.removeAt(holder.adapterPosition)
//            notifyItemRemoved(holder.adapterPosition)
//        }

    }

    override fun getItemCount(): Int = items.size

    fun add(item: SubAlarmEntity) {
//        items.add(item)
//        items.sortBy { it.time }
//        val newIndex = items.indexOf(item)
//        notifyItemInserted(newIndex)
    }

    inner class ItemViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
//        val btnDelete: ImageButton = itemView.findViewById(R.id.btn_delete)
        val tvTime: MaterialTextView = itemView.findViewById(R.id.tv_time)
    }
}