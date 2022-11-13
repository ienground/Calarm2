package zone.ien.calarm.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textview.MaterialTextView
import zone.ien.calarm.R
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class MainCalarmDateAdapter(var items: ArrayList<Calendar>): RecyclerView.Adapter<MainCalarmDateAdapter.ItemViewHolder>() {

    lateinit var context: Context

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        context = parent.context
        val view = LayoutInflater.from(context).inflate(R.layout.adapter_main_calarm_date, parent, false)
        return ItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
        val dateFormat = SimpleDateFormat("d", Locale.getDefault())

        holder.tvDay.text = dayFormat.format(items[holder.adapterPosition].time)
        holder.tvDate.text = dateFormat.format(items[holder.adapterPosition].time)
    }

    override fun getItemCount(): Int = items.size

    inner class ItemViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        val tvDay: MaterialTextView = itemView.findViewById(R.id.tv_day)
        val tvDate: MaterialTextView = itemView.findViewById(R.id.tv_date)
    }
}