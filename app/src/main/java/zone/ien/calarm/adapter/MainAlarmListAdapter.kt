package zone.ien.calarm.adapter

import android.content.Context
import android.text.SpannableStringBuilder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.textview.MaterialTextView
import zone.ien.calarm.R
import zone.ien.calarm.room.AlarmEntity
import zone.ien.calarm.utils.MyUtils
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.pow

class MainAlarmListAdapter(var items: ArrayList<AlarmEntity>): RecyclerView.Adapter<MainAlarmListAdapter.ItemViewHolder>() {

    lateinit var context: Context

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        context = parent.context
        val view = LayoutInflater.from(context).inflate(R.layout.adapter_main_alarm_list, parent, false)
        return ItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val apmFormat = SimpleDateFormat("a", Locale.getDefault())
        val timeFormat = SimpleDateFormat("h:mm", Locale.getDefault())
        val time = Calendar.getInstance().apply {
            items[holder.adapterPosition].time.let {
                set(Calendar.HOUR_OF_DAY, it / 60)
                set(Calendar.MINUTE, it % 60)
            }
        }
        val repeat: ArrayList<Boolean> = arrayListOf()
        for (i in 0 until 7) {
            repeat.add(items[holder.adapterPosition].repeat.and(2.0.pow(6 - i).toInt()) != 0)
        }
        holder.tvRepeatDay.text = MyUtils.getRepeatlabel(context, repeat, items[holder.adapterPosition].isRepeat, items[holder.adapterPosition].time)

        items[holder.adapterPosition].label.let {
            if (it != "") {
                holder.tvLabel.visibility = View.VISIBLE
                holder.tvLabel.text = it
            } else {
                holder.tvLabel.visibility = View.GONE
            }
        }
        holder.switchOn.isChecked = items[holder.adapterPosition].isEnabled
        holder.tvApm.text = apmFormat.format(time.time)
        holder.tvTime.text = timeFormat.format(time.time)

        holder.tvApm.typeface = ResourcesCompat.getFont(context, if (items[holder.adapterPosition].isEnabled) R.font.pretendard_black else R.font.pretendard)
        holder.tvTime.typeface = ResourcesCompat.getFont(context, if (items[holder.adapterPosition].isEnabled) R.font.pretendard_black else R.font.pretendard)

        for (alarm in items[holder.adapterPosition].subAlarms) {
            val chip = Chip(context)
            chip.typeface = ResourcesCompat.getFont(context, R.font.pretendard_regular)
            chip.text = if (alarm.time / 60 != 0 && alarm.time % 60 != 0) context.getString(R.string.time_format_hour_minute, alarm.time / 60, alarm.time % 60)
            else if (alarm.time % 60 == 0) context.getString(R.string.time_format_hour, alarm.time / 60)
            else context.getString(R.string.time_format_minute, alarm.time % 60)

            holder.chipsMiniAlarm.addView(chip)
        }
        
        if (items[holder.adapterPosition].subAlarms.isEmpty()) {
            val chip = Chip(context)
            chip.typeface = ResourcesCompat.getFont(context, R.font.pretendard_regular)
            chip.text = context.getString(R.string.no_sub_alarms)

            holder.chipsMiniAlarm.addView(chip)
        }
    }

    override fun getItemCount(): Int = items.size

    inner class ItemViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        val tvLabel: MaterialTextView = itemView.findViewById(R.id.tv_label)
        val switchOn: MaterialSwitch = itemView.findViewById(R.id.switch_on)
        val tvRepeatDay: MaterialTextView = itemView.findViewById(R.id.tv_repeat_day)
        val tvApm: MaterialTextView = itemView.findViewById(R.id.tv_apm)
        val tvTime: MaterialTextView = itemView.findViewById(R.id.tv_time)
        val chipsMiniAlarm: ChipGroup = itemView.findViewById(R.id.chips_mini_alarm)
    }
}