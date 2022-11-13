package zone.ien.calarm.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.textview.MaterialTextView
import zone.ien.calarm.R
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class MainCalarmEventAdapter(var items: ArrayList<String>): RecyclerView.Adapter<MainCalarmEventAdapter.ItemViewHolder>() {

    lateinit var context: Context

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        context = parent.context
        val view = LayoutInflater.from(context).inflate(R.layout.adapter_main_calarm_event, parent, false)
        return ItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        items[holder.adapterPosition].let {
            if (it != "") {
                holder.tvLabel.visibility = View.VISIBLE
                holder.tvLabel.text = it
            } else {
                holder.tvLabel.visibility = View.GONE
            }
        }

        val chip = Chip(context)
        val chip2 = Chip(context)
        chip.text = "H1"
        chip2.text = "H2"
        holder.chipsMiniAlarm.addView(chip)
        holder.chipsMiniAlarm.addView(chip2)
    }

    override fun getItemCount(): Int = items.size

    inner class ItemViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        val tvLabel: MaterialTextView = itemView.findViewById(R.id.tv_label)
        val switchOn: MaterialSwitch = itemView.findViewById(R.id.switch_on)
        val tvAddress: MaterialTextView = itemView.findViewById(R.id.tv_address)
        val tvTime: MaterialTextView = itemView.findViewById(R.id.tv_time)
        val chipsMiniAlarm: ChipGroup = itemView.findViewById(R.id.chips_mini_alarm)
    }
}