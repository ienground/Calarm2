package zone.ien.calarm

import android.app.Application
import com.google.android.material.color.DynamicColors

class MyApplication: Application() {


    companion object {

        private var timerWindowActivated = false
        fun isTimerWindowActivated(): Boolean = timerWindowActivated

        fun timerWindowResumed() {
            timerWindowActivated = true
        }

        fun timerWindowPaused() {
            timerWindowActivated = false
        }
    }

    override fun onCreate() {
        super.onCreate()
        DynamicColors.applyToActivitiesIfAvailable(this)
    }


}