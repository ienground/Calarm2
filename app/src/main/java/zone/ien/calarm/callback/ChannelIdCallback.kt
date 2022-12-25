package zone.ien.calarm.callback

import android.service.notification.StatusBarNotification

interface ChannelIdCallback {
    fun delete(position: Int, id: String)
}