package zone.ien.calarm.adapter

import android.app.AlarmManager
import android.content.Context
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
import zone.ien.calarm.callback.AlarmListCallback
import zone.ien.calarm.room.AlarmEntity
import zone.ien.calarm.utils.MyUtils
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class MainAlarmListAdapter(var items: ArrayList<AlarmEntity>): RecyclerView.Adapter<MainAlarmListAdapter.ItemViewHolder>() {

    lateinit var context: Context

    private var callbackListener: AlarmListCallback? = null
    private lateinit var am: AlarmManager

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        context = parent.context
        am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val view = LayoutInflater.from(context).inflate(R.layout.adapter_main_alarm_list, parent, false)
        return ItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val apmFormat = SimpleDateFormat("a", Locale.getDefault())
        val timeFormat = SimpleDateFormat("h:mm", Locale.getDefault())
        val apmTimeFormat = SimpleDateFormat(context.getString(R.string.apmTimeFormat), Locale.getDefault())
        val time = Calendar.getInstance().apply {
            items[holder.adapterPosition].time.let {
                set(Calendar.HOUR_OF_DAY, it / 60)
                set(Calendar.MINUTE, it % 60)
            }
        }
        holder.tvApm.visibility = if (Locale.getDefault() == Locale.KOREA) View.GONE else View.VISIBLE
        holder.tvApmKo.visibility = if (Locale.getDefault() != Locale.KOREA) View.GONE else View.VISIBLE
        holder.tvRepeatDay.text = MyUtils.getRepeatlabel(context, items[holder.adapterPosition].repeat, items[holder.adapterPosition].time)

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
        holder.tvApmKo.text = apmFormat.format(time.time)
        holder.tvTime.text = timeFormat.format(time.time)

        holder.tvApm.typeface = ResourcesCompat.getFont(context, if (items[holder.adapterPosition].isEnabled) R.font.pretendard_black else R.font.pretendard)
        holder.tvApmKo.typeface = ResourcesCompat.getFont(context, if (items[holder.adapterPosition].isEnabled) R.font.pretendard_black else R.font.pretendard)
        holder.tvTime.typeface = ResourcesCompat.getFont(context, if (items[holder.adapterPosition].isEnabled) R.font.pretendard_black else R.font.pretendard)

        holder.chipsMiniAlarm.removeAllViews()
        items[holder.adapterPosition].subAlarms.forEachIndexed { index, alarm ->
            val chip = Chip(context)
            val calendar = time.clone() as Calendar
            calendar.add(Calendar.MINUTE, -alarm.time)
            chip.isCheckable = true
            chip.typeface = ResourcesCompat.getFont(context, R.font.pretendard_regular)
            chip.text = context.getString(R.string.time_format_before, alarm.time.let {
                val timeArray: ArrayList<String> = arrayListOf()
                if (it / 60 == 1) timeArray.add(context.getString(R.string.time_format_1hour))
                else if (it / 60 != 0) timeArray.add(context.getString(R.string.time_format_hour, it / 60))
                if (it % 60 == 1) timeArray.add(context.getString(R.string.time_format_1minute))
                else if (it % 60 != 0) timeArray.add(context.getString(R.string.time_format_minute, it %60))

                timeArray.joinToString(" ")
            })

            chip.setOnCheckedChangeListener { compoundButton, b ->
                chip.text = if (b) apmTimeFormat.format(calendar.time) else {
                    context.getString(R.string.time_format_before, alarm.time.let {
                        val timeArray: ArrayList<String> = arrayListOf()
                        if (it / 60 == 1) timeArray.add(context.getString(R.string.time_format_1hour))
                        else if (it / 60 != 0) timeArray.add(context.getString(R.string.time_format_hour, it / 60))
                        if (it % 60 == 1) timeArray.add(context.getString(R.string.time_format_1minute))
                        else if (it % 60 != 0) timeArray.add(context.getString(R.string.time_format_minute, it % 60))

                        timeArray.joinToString(" ")
                    })
                }
            }

            holder.chipsMiniAlarm.addView(chip)
        }
        
        if (items[holder.adapterPosition].subAlarms.isEmpty()) {
            val chip = Chip(context)
            chip.typeface = ResourcesCompat.getFont(context, R.font.pretendard_regular)
            chip.text = context.getString(R.string.no_sub_alarms)

            holder.chipsMiniAlarm.addView(chip)
        }

        holder.switchOn.setOnCheckedChangeListener { compoundButton, b ->
            callbackListener?.toggle(holder.adapterPosition, items[holder.adapterPosition].id ?: -1, b)
            holder.tvApm.typeface = ResourcesCompat.getFont(context, if (b) R.font.pretendard_black else R.font.pretendard)
            holder.tvApmKo.typeface = ResourcesCompat.getFont(context, if (b) R.font.pretendard_black else R.font.pretendard)
            holder.tvTime.typeface = ResourcesCompat.getFont(context, if (b) R.font.pretendard_black else R.font.pretendard)
        }

        holder.itemView.setOnClickListener {
            callbackListener?.callBack(holder.adapterPosition, items[holder.adapterPosition].id ?: -1)
        }
    }

    override fun getItemCount(): Int = items.size

    fun edit(id: Long, item: AlarmEntity) {
        val position = items.indexOfFirst { it.id == id }
        if (position != -1) {
            items[position] = item
            items.sortBy { it.time }
            val newPosition = items.indexOfFirst { it.id == id }

            notifyItemMoved(position, newPosition)
            notifyItemChanged(newPosition)
        } else {
            items.add(item)
            items.sortBy { it.time }
            val newPosition = items.indexOfFirst { it.id == id }

            notifyItemInserted(newPosition)
        }
    }

    fun delete(id: Long) {
        val position = items.indexOfFirst { it.id == id }
        items.removeAt(position)
        notifyItemRemoved(position)
    }

    fun setClickCallback(callbackListener: AlarmListCallback) {
        this.callbackListener = callbackListener
    }

    inner class ItemViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        val tvLabel: MaterialTextView = itemView.findViewById(R.id.tv_label)
        val switchOn: MaterialSwitch = itemView.findViewById(R.id.switch_on)
        val tvRepeatDay: MaterialTextView = itemView.findViewById(R.id.tv_repeat_day)
        val tvApm: MaterialTextView = itemView.findViewById(R.id.tv_apm)
        val tvApmKo: MaterialTextView = itemView.findViewById(R.id.tv_apm_ko)
        val tvTime: MaterialTextView = itemView.findViewById(R.id.tv_time)
        val chipsMiniAlarm: ChipGroup = itemView.findViewById(R.id.chips_mini_alarm)
    }
}