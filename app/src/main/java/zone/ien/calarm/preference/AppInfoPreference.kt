package zone.ien.calarm.preference

import android.content.Context
import android.graphics.Typeface
import android.util.AttributeSet
import android.widget.TextView
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import zone.ien.calarm.R


class AppInfoPreference(context: Context, attrs: AttributeSet): Preference(context, attrs) {
    init {
        widgetLayoutResource = R.layout.preference_app_info
    }
}