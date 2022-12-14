package zone.ien.calarm.adapter

import android.content.Context
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.textview.MaterialTextView
import zone.ien.calarm.R
import zone.ien.calarm.data.CalendarObject

class SelectCalendarItemAdapter(var parentChecked: Boolean, var items: ArrayList<CalendarObject>): RecyclerView.Adapter<SelectCalendarItemAdapter.ItemViewHolder>() {
    lateinit var context: Context
    lateinit var sharedPreferences: SharedPreferences

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        context = parent.context
        sharedPreferences = context.getSharedPreferences("${context.packageName}_preferences", Context.MODE_PRIVATE)
        val view = LayoutInflater.from(context).inflate(R.layout.adapter_select_calendar_item, parent, false)
        return ItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        holder.switchCalendar.isEnabled = parentChecked
        holder.switchCalendar.isChecked = sharedPreferences.getBoolean("calendar_id_${items[holder.adapterPosition].id}", true)
        holder.ivColor.setImageDrawable(ColorDrawable(items[holder.adapterPosition].calendarColor))
        holder.tvCalendarLabel.text = items[holder.adapterPosition].calendarDisplayName
        holder.switchCalendar.setOnCheckedChangeListener { compoundButton, b ->
            sharedPreferences.edit().putBoolean("calendar_id_${items[holder.adapterPosition].id}", b).apply()
        }
        holder.itemView.setOnClickListener { holder.switchCalendar.toggle() }
    }

    override fun getItemCount(): Int = items.size

    inner class ItemViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        val ivColor: ImageView = itemView.findViewById(R.id.iv_color)
        val tvCalendarLabel: MaterialTextView = itemView.findViewById(R.id.tv_calendar_label)
        val switchCalendar: MaterialSwitch = itemView.findViewById(R.id.switch_calendar)
    }
}