package zone.ien.calarm.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import zone.ien.calarm.room.CalarmDatabase
import zone.ien.calarm.room.SubCalarmDatabase

class CalarmCreateReceiver: BroadcastReceiver() {
    private var calarmDatabase: CalarmDatabase? = null
    private var subCalarmDatabase: SubCalarmDatabase? = null
    private lateinit var sharedPreferences: SharedPreferences

    override fun onReceive(context: Context, intent: Intent) {
        // run at 12:05 am

        calarmDatabase = CalarmDatabase.getInstance(context)
        subCalarmDatabase = SubCalarmDatabase.getInstance(context)
        sharedPreferences = context.getSharedPreferences("${context.packageName}_preferences", Context.MODE_PRIVATE)


    }
}