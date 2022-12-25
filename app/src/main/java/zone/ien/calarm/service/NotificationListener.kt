package zone.ien.calarm.service

import android.app.Notification
import android.app.NotificationManager
import android.content.*
import android.content.pm.PackageManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import zone.ien.calarm.constant.IntentKey

class NotificationListener : NotificationListenerService() {

    lateinit var pm: PackageManager
    lateinit var nm: NotificationManager
    lateinit var sharedPreferences: SharedPreferences

    private val removeNotificationReceiver = object: BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val key = intent.getStringExtra("key")
            this@NotificationListener.cancelNotification(key)
        }
    }

    override fun onCreate() {
        super.onCreate()

        pm = packageManager
        nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        sharedPreferences = getSharedPreferences("${packageName}_preferences", Context.MODE_PRIVATE)
        LocalBroadcastManager.getInstance(this).sendBroadcast(Intent(IntentKey.LISTENER_READY))
        registerReceiver(removeNotificationReceiver, IntentFilter(packageName))
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        super.onNotificationPosted(sbn)
        val notification = sbn.notification
        val extras = notification.extras

        var isInitialized = false

        for (n in activeNotifications.sortedBy { it.postTime }) {
            if (n != sbn && n.id !in activeNotificationId) {
                if (n.notification.extras.get(Notification.EXTRA_MEDIA_SESSION) == null) {
                    activeNotification.add(n)
                    activeNotificationId.add(n.id)

                    isInitialized = true
                } else {
                    mediaNotification = n
                    LocalBroadcastManager.getInstance(this).sendBroadcast(Intent(IntentKey.MEDIA_CHANGE))
                }
            }
        }

        if (isInitialized) LocalBroadcastManager.getInstance(this).sendBroadcast(Intent(IntentKey.NOTIFICATION_INIT))

        if (extras.get(Notification.EXTRA_MEDIA_SESSION) != null) {
            mediaNotification = sbn
            LocalBroadcastManager.getInstance(this).sendBroadcast(Intent(IntentKey.MEDIA_CHANGE))
        } else {
            addedNotification = sbn
            LocalBroadcastManager.getInstance(this).sendBroadcast(Intent(IntentKey.NOTIFICATION_ADD))

            activeNotification.add(sbn)
            activeNotificationId.add(sbn.id)
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        super.onNotificationRemoved(sbn)

        val notification = sbn.notification
        val extras = notification.extras

        var isInitialized = false

        for (n in activeNotifications.sortedBy { it.postTime }) {
            if (n != sbn && n.id !in activeNotificationId) {
                if (n.notification.extras.get(Notification.EXTRA_MEDIA_SESSION) == null) {
                    activeNotification.add(n)
                    activeNotificationId.add(n.id)

                    isInitialized = true
                } else {
                    mediaNotification = n
                    LocalBroadcastManager.getInstance(this).sendBroadcast(Intent(IntentKey.MEDIA_CHANGE))
                }
            }
        }

        if (isInitialized) LocalBroadcastManager.getInstance(this).sendBroadcast(Intent(IntentKey.NOTIFICATION_INIT))

        removeNotificationId = sbn.id
        activeNotificationId.remove(removeNotificationId)
        activeNotification.removeIf { it.id == removeNotificationId }
        LocalBroadcastManager.getInstance(this).sendBroadcast(Intent(IntentKey.NOTIFICATION_REMOVE))
    }

    override fun onDestroy() {
        unregisterReceiver(removeNotificationReceiver)
        super.onDestroy()
    }

    companion object {
        var activeNotification: ArrayList<StatusBarNotification> = arrayListOf()
        var activeNotificationId: ArrayList<Int> = arrayListOf()
        var mediaNotification: StatusBarNotification? = null
        var addedNotification: StatusBarNotification? = null
        var removeNotificationId = -1
    }
}
