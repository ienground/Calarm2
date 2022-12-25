package zone.ien.calarm.data

data class StopwatchLapse(var flag: String, var time: Long) {
    override fun toString(): String = "$flag ${time.let {
        if (it >= 60 * 60 * 10 * 1000) String.format("%02d %02d %02d.%02d", (it / 1000) / (60 * 60), (it / 1000 % (60 * 60)) / 60, (it / 1000) % 60, (it % 1000) / 10)
        else if (it >= 60 * 60 * 1000) String.format("%d %02d %02d.%02d", (it / 1000) / (60 * 60), (it / 1000 % (60 * 60)) / 60, (it / 1000) % 60, (it % 1000) / 10)
        else if (it >= 60 * 10 * 1000) String.format("%02d %02d.%02d", (it / 1000 % (60 * 60)) / 60, (it / 1000) % 60, (it % 1000) / 10)
        else String.format("%d %02d.%02d", (it / 1000 % (60 * 60)) / 60, (it / 1000) % 60, (it % 1000) / 10)
    }}"
}
