package zone.ien.calarm.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.textview.MaterialTextView
import zone.ien.calarm.R
import zone.ien.calarm.room.SubAlarmEntity
import zone.ien.calarm.utils.ItemActionListener

class SubAlarmAdapter(var items: ArrayList<SubAlarmEntity>): RecyclerView.Adapter<SubAlarmAdapter.ItemViewHolder>(), ItemActionListener {

    lateinit var context: Context

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        context = parent.context
        val view = LayoutInflater.from(context).inflate(R.layout.adapter_sub_alarm, parent, false)
        return ItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        items[holder.adapterPosition].time.let {
            holder.tvTime.text =
                if (it / 60 != 0 && it % 60 != 0) context.getString(R.string.time_format_before_hour_minute, it / 60, it % 60)
                else if (it / 60 != 0) context.getString(R.string.time_format_before_hour, it / 60)
                else context.getString(R.string.time_format_before_minute, it % 60)
        }
        holder.switchOn.setOnCheckedChangeListener { compoundButton, b ->
            holder.tvTime.typeface = ResourcesCompat.getFont(context, if (b) R.font.pretendard_black else R.font.pretendard)
        }
        holder.switchOn.isChecked = items[holder.adapterPosition].isEnabled
        holder.btnDelete.setOnClickListener {
            items.removeAt(holder.adapterPosition)
            notifyItemRemoved(holder.adapterPosition)
        }
    }

    override fun getItemCount(): Int = items.size

    fun add(item: SubAlarmEntity) {
        items.add(item)
        items.sortBy { it.time }
        val newIndex = items.indexOf(item)
        notifyItemInserted(newIndex)
    }

    override fun onItemSwiped(position: Int) {
        items.removeAt(position)
        notifyItemRemoved(position)
    }

    inner class ItemViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        val btnDelete: MaterialButton = itemView.findViewById(R.id.btn_delete)
        val tvTime: MaterialTextView = itemView.findViewById(R.id.tv_time)
        val switchOn: MaterialSwitch = itemView.findViewById(R.id.switch_on)
    }
}