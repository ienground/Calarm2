package zone.ien.calarm.callback

import android.service.notification.StatusBarNotification

interface NotificationCallback {
    fun click(position: Int, sbn: StatusBarNotification)
    fun longClick(position: Int, sbn: StatusBarNotification)
}