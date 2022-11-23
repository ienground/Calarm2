package zone.ien.calarm.callback

interface TimerFragmentCallback {
    fun scrollTo(page: Int)
    fun addNewTimer(id: Long)
}