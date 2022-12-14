package zone.ien.calarm.data

import java.text.SimpleDateFormat
import java.util.*

class CalendarEvent(
        var id: Long,
        var calendarId: Long,
        var title: String,
        var description: String,
        var startDate: Long,
        var eventTimeZone: String,
        var endDate: Long,
        var eventEndTimeZone: String,
        var isAllday: Boolean,
        var color: String,
        var eventLocation: String
) {
        override fun toString(): String {
                val dateTimeFormat = SimpleDateFormat("yyyy.MM.dd hh:mm:ss", Locale.getDefault())
                return "ID : ${id} from ${calendarId} | ${title} : ${description} | ${dateTimeFormat.format(Date(startDate))} | ${eventLocation}"
        }
}