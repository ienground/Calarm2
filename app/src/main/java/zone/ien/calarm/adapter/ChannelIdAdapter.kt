package zone.ien.calarm.adapter

import android.animation.ValueAnimator
import android.app.AlertDialog
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.service.notification.StatusBarNotification
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import zone.ien.calarm.R
import zone.ien.calarm.activity.TAG
import zone.ien.calarm.callback.ChannelIdCallback
import zone.ien.calarm.callback.NotificationCallback
import java.text.SimpleDateFormat
import java.util.*


class ChannelIdAdapter(var items: ArrayList<Pair<String, String>>): RecyclerView.Adapter<ChannelIdAdapter.ItemViewHolder>() {

    lateinit var context: Context
    lateinit var pm: PackageManager
    lateinit var nm: NotificationManager

    private lateinit var timeFormat: SimpleDateFormat
    private var callbackListener: ChannelIdCallback? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        context = parent.context
        pm = context.packageManager
        nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        timeFormat = SimpleDateFormat(context.getString(R.string.apmTimeFormat), Locale.getDefault())

        return ItemViewHolder(LayoutInflater.from(context).inflate(R.layout.adapter_channel_id, parent, false))
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val applicationLabel = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getApplicationLabel(pm.getApplicationInfo(items[holder.adapterPosition].first, PackageManager.ApplicationInfoFlags.of(0L)))
            } else {
                pm.getApplicationLabel(pm.getApplicationInfo(items[holder.adapterPosition].first, 0))
            }
        } catch (e: PackageManager.NameNotFoundException) { "NoName" }
        val applicationIcon = try {
            pm.getApplicationIcon(items[holder.adapterPosition].first)
        } catch (e: Exception){
            null
        }

        holder.appName.text = applicationLabel
        holder.icon.setImageDrawable(applicationIcon)
        holder.title.text = items[holder.adapterPosition].second

        holder.btnClose.setOnClickListener {
            Log.d(TAG, "${callbackListener}")
            callbackListener?.delete(holder.adapterPosition, "${items[holder.adapterPosition].first}☆${items[holder.adapterPosition].second}")
        }

    }

    fun delete(position: Int) {
        items.removeAt(position)
        notifyItemRemoved(position)
    }

    fun isEmpty(): Boolean = items.isEmpty()

    fun setClickCallback(callbackListener: ChannelIdCallback) {
        this.callbackListener = callbackListener
    }

    inner class ItemViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        val icon: ImageView = itemView.findViewById(R.id.iv_icon)
        val appName: TextView = itemView.findViewById(R.id.tv_app_name)
        val title: TextView = itemView.findViewById(R.id.tv_title)
        val btnClose: MaterialButton = itemView.findViewById(R.id.btn_close)
    }

}