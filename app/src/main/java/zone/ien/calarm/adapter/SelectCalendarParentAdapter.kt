package zone.ien.calarm.adapter

import android.content.Context
import android.content.SharedPreferences
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.textview.MaterialTextView
import zone.ien.calarm.R
import zone.ien.calarm.data.CalendarObject

class SelectCalendarParentAdapter(var items: MutableMap<String, ArrayList<CalendarObject>>): RecyclerView.Adapter<SelectCalendarParentAdapter.ItemViewHolder>() {
    lateinit var context: Context
    lateinit var sharedPreferences: SharedPreferences

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        context = parent.context
        sharedPreferences = context.getSharedPreferences("${context.packageName}_preferences", Context.MODE_PRIVATE)
        val view = LayoutInflater.from(context).inflate(R.layout.adapter_select_calendar_parent, parent, false)
        return ItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val adapter = SelectCalendarItemAdapter(true, items[items.keys.toList()[holder.adapterPosition]] ?: arrayListOf())
        holder.tvAccount.text = items[items.keys.toList()[holder.adapterPosition]]?.first()?.accountName
        holder.list.adapter = adapter
        holder.list.itemAnimator = null
        holder.switchAccount.isChecked = sharedPreferences.getBoolean(items.keys.toList()[holder.adapterPosition], true)
        adapter.parentChecked = holder.switchAccount.isChecked
        adapter.notifyItemRangeChanged(0, adapter.itemCount)
        holder.switchAccount.setOnCheckedChangeListener { compoundButton, b ->
            sharedPreferences.edit().putBoolean(items.keys.toList()[holder.adapterPosition], b).apply()
            adapter.items.forEachIndexed { index, calendarObject ->
                sharedPreferences.edit().putBoolean("calendar_id_${calendarObject.id}", b).apply()
                adapter.parentChecked = b
            }
            adapter.notifyItemRangeChanged(0, adapter.itemCount)
        }
        holder.cardAccount.setOnClickListener { holder.switchAccount.toggle() }
    }

    override fun getItemCount(): Int = items.size

    inner class ItemViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        val tvAccount: MaterialTextView = itemView.findViewById(R.id.tv_account)
        val cardAccount: MaterialCardView = itemView.findViewById(R.id.card_account)
        val switchAccount: MaterialSwitch = itemView.findViewById(R.id.switch_account)
        val list: RecyclerView = itemView.findViewById(R.id.list)
    }
}