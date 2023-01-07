package zone.ien.calarm.adapter

import android.app.AlertDialog
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.PackageManager.ApplicationInfoFlags
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.service.notification.StatusBarNotification
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Recycler
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textview.MaterialTextView
import de.hdodenhof.circleimageview.CircleImageView
import zone.ien.calarm.R
import zone.ien.calarm.activity.TAG
import zone.ien.calarm.callback.AlarmListCallback
import zone.ien.calarm.callback.ItemTouchHelperCallback
import zone.ien.calarm.callback.NotificationCallback
import zone.ien.calarm.constant.IntentKey
import zone.ien.calarm.utils.ItemActionListener
import java.text.SimpleDateFormat
import java.util.*

class NotificationAdapter(var items: ArrayList<StatusBarNotification>): RecyclerView.Adapter<NotificationAdapter.ItemViewHolder>(), ItemActionListener {

    lateinit var context: Context
    lateinit var sharedPreferences: SharedPreferences

    lateinit var pm: PackageManager
    lateinit var nm: NotificationManager
    private lateinit var timeFormat: SimpleDateFormat

    private var callbackListener: NotificationCallback? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        context = parent.context
        pm = context.packageManager
        nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        sharedPreferences = context.getSharedPreferences("${context.packageName}_preferences", Context.MODE_PRIVATE)
        timeFormat = SimpleDateFormat(context.getString(R.string.apmTimeFormat), Locale.getDefault())

        val view = LayoutInflater.from(context).inflate(R.layout.adapter_notification, parent, false)
        return ItemViewHolder(view)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val applicationLabel = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getApplicationLabel(pm.getApplicationInfo(items[holder.adapterPosition].packageName, ApplicationInfoFlags.of(0L)))
            } else {
                pm.getApplicationLabel(pm.getApplicationInfo(items[holder.adapterPosition].packageName, 0))
            }
        } catch (e: PackageManager.NameNotFoundException) { "NoName" }
        val postTime = timeFormat.format(Date(items[holder.adapterPosition].postTime))

        val extras = items[holder.adapterPosition].notification.extras

        holder.title.text = extras.getString(Notification.EXTRA_TITLE)
        holder.content.text = extras.getString(Notification.EXTRA_TEXT)
        holder.icon.setImageDrawable(items[holder.adapterPosition].notification.smallIcon.loadDrawable(context))
        holder.iconBackground.setImageDrawable(ColorDrawable(items[holder.adapterPosition].notification.color))
        holder.appName.text = applicationLabel
        holder.chipTime.text = postTime

        holder.itemView.setOnClickListener {
            Toast.makeText(context, "${items[holder.adapterPosition].id}", Toast.LENGTH_SHORT).show()
        }
        holder.itemView.setOnLongClickListener {
            MaterialAlertDialogBuilder(context, R.style.Theme_Calarm_MaterialAlertDialog).apply {
                setIcon(R.drawable.ic_notifications_off)
                setTitle(R.string.block_notification)
                setMessage(R.string.block_channel_message)
                setPositiveButton(android.R.string.ok) { _, _ ->
                    callbackListener?.longClick(holder.adapterPosition, items[holder.adapterPosition])
                }
3
                setNegativeButton(android.R.string.cancel) { dialog, _ ->
                    dialog.dismiss()
                }
            }.show()
            true
        }
    }

    fun add(item: StatusBarNotification) {
        val index = items.indexOfFirst { it.id == item.id }
        if (index != -1) { // update
            items.removeIf { it.id == item.id }
            notifyItemRemoved(index)
        }
        items.add(0, item)
        notifyItemInserted(0)
    }

    fun delete(id: Int): Boolean {
        val index = items.indexOfFirst { it.id == id }
        if (index != -1) { // update
            items.removeIf { it.id == id }
            notifyItemRemoved(index)
            return true
        }
        return false
    }

    fun isEmpty(): Boolean = items.isEmpty()

    fun setClickCallback(callbackListener: NotificationCallback) {
        this.callbackListener = callbackListener
    }

    inner class ItemViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        val icon: ImageView = itemView.findViewById(R.id.iv_icon)
        val iconBackground: CircleImageView = itemView.findViewById(R.id.iv_background)
        val appName: MaterialTextView = itemView.findViewById(R.id.tv_app_name)
        val title: MaterialTextView = itemView.findViewById(R.id.tv_title)
        val content: MaterialTextView = itemView.findViewById(R.id.tv_content)
        val chipTime: Chip = itemView.findViewById(R.id.chip_time)
    }

    override fun onItemSwiped(position: Int) {
        context.sendBroadcast(Intent(context.packageName).apply { putExtra(IntentKey.KEY, items[position].key) })
        delete(items[position].id)
    }
}