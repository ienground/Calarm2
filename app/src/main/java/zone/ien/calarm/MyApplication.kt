package zone.ien.calarm

import android.app.Application

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


}