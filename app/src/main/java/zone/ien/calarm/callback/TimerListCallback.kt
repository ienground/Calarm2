package zone.ien.calarm.callback

interface TimerListCallback {
    fun callBack(position: Int, id: Long)
    fun delete(position: Int, id: Long)
    fun start(position: Int, id: Long)
}