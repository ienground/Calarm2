package zone.ien.calarm.callback

interface AlarmListCallback {
    fun callBack(position: Int, id: Long)
    fun toggle(position: Int, id: Long, isEnabled: Boolean)
}