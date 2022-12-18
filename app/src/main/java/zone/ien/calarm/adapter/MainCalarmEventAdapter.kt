package zone.ien.calarm.adapter

import android.content.Context
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.textview.MaterialTextView
import zone.ien.calarm.R
import zone.ien.calarm.callback.AlarmListCallback
import zone.ien.calarm.room.AlarmEntity
import zone.ien.calarm.room.CalarmEntity
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class MainCalarmEventAdapter(var items: ArrayList<CalarmEntity>): RecyclerView.Adapter<MainCalarmEventAdapter.ItemViewHolder>() {

    lateinit var context: Context

    private var callbackListener: AlarmListCallback? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        context = parent.context
        val view = LayoutInflater.from(context).inflate(R.layout.adapter_main_calarm_event, parent, false)
        return ItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val apmFormat = SimpleDateFormat("a", Locale.getDefault())
        val timeFormat = SimpleDateFormat("h:mm", Locale.getDefault())
        val apmTimeFormat = SimpleDateFormat(context.getString(R.string.apmTimeFormat), Locale.getDefault())
        val time = Calendar.getInstance().apply {
            timeInMillis = items[holder.adapterPosition].time
        }

        items[holder.adapterPosition].label.let {
            if (it != "") {
                holder.tvLabel.visibility = View.VISIBLE
                holder.tvLabel.text = it
            } else {
                holder.tvLabel.visibility = View.GONE
            }
        }
        items[holder.adapterPosition].address.let {
            if (it != "") {
                holder.tvAddress.visibility = View.VISIBLE
                holder.tvAddress.text = it
            } else {
                holder.tvAddress.visibility = View.GONE
            }
        }

        holder.switchOn.isChecked = items[holder.adapterPosition].isEnabled
        holder.tvApm.text = apmFormat.format(time.time)
        holder.tvTime.text = timeFormat.format(time.time)

        holder.tvApm.typeface = ResourcesCompat.getFont(context, if (items[holder.adapterPosition].isEnabled) R.font.pretendard_black else R.font.pretendard)
        holder.tvTime.typeface = ResourcesCompat.getFont(context, if (items[holder.adapterPosition].isEnabled) R.font.pretendard_black else R.font.pretendard)

        holder.chipsMiniAlarm.removeAllViews()
        items[holder.adapterPosition].subCalarms.forEachIndexed { index, calarm ->
            val chip = Chip(context)
            val calendar = time.clone() as Calendar
            calendar.add(Calendar.MINUTE, -calarm.time)
            chip.isCheckable = true
            chip.typeface = ResourcesCompat.getFont(context, R.font.pretendard_regular)
            chip.chipIconTint = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.white))
            if (calarm.timeMoving != 0) chip.chipIcon = ContextCompat.getDrawable(context, R.drawable.ic_bus)
            chip.text = if (calarm.time / 60 != 0 && calarm.time % 60 != 0) context.getString(R.string.time_format_before_hour_minute, calarm.time / 60, calarm.time % 60)
            else if (calarm.time % 60 == 0) context.getString(R.string.time_format_before_hour, calarm.time / 60)
            else context.getString(R.string.time_format_before_minute, calarm.time % 60)

            chip.setOnCheckedChangeListener { compoundButton, b ->
                chip.text = if (b) apmTimeFormat.format(calendar.time) else {
                    if (calarm.time / 60 != 0 && calarm.time % 60 != 0) context.getString(R.string.time_format_before_hour_minute, calarm.time / 60, calarm.time % 60)
                    else if (calarm.time % 60 == 0) context.getString(R.string.time_format_before_hour, calarm.time / 60)
                    else context.getString(R.string.time_format_before_minute, calarm.time % 60)
                }
            }

            holder.chipsMiniAlarm.addView(chip)
        }

        if (items[holder.adapterPosition].subCalarms.isEmpty()) {
            val chip = Chip(context)
            chip.typeface = ResourcesCompat.getFont(context, R.font.pretendard_regular)
            chip.text = context.getString(R.string.no_sub_calarms)

            holder.chipsMiniAlarm.addView(chip)
        }

        holder.switchOn.setOnCheckedChangeListener { compoundButton, b ->
            callbackListener?.toggle(holder.adapterPosition, items[holder.adapterPosition].id ?: -1, b)
            holder.tvApm.typeface = ResourcesCompat.getFont(context, if (b) R.font.pretendard_black else R.font.pretendard)
            holder.tvTime.typeface = ResourcesCompat.getFont(context, if (b) R.font.pretendard_black else R.font.pretendard)
        }

        holder.itemView.setOnClickListener {
            callbackListener?.callBack(holder.adapterPosition, items[holder.adapterPosition].dataId)
        }
    }

    override fun getItemCount(): Int = items.size

    fun edit(id: Long, item: CalarmEntity) {
        val position = items.indexOfFirst { it.id == id }
        if (position != -1) {
            items[position] = item
            notifyItemChanged(position)
        }
    }

    fun setClickCallback(callbackListener: AlarmListCallback) {
        this.callbackListener = callbackListener
    }

    inner class ItemViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        val tvLabel: MaterialTextView = itemView.findViewById(R.id.tv_label)
        val switchOn: MaterialSwitch = itemView.findViewById(R.id.switch_on)
        val tvAddress: MaterialTextView = itemView.findViewById(R.id.tv_address)
        val tvApm: MaterialTextView = itemView.findViewById(R.id.tv_apm)
        val tvTime: MaterialTextView = itemView.findViewById(R.id.tv_time)
        val chipsMiniAlarm: ChipGroup = itemView.findViewById(R.id.chips_mini_alarm)
    }
}