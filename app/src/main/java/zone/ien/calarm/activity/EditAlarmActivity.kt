package zone.ien.calarm.activity

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import androidx.databinding.DataBindingUtil
import zone.ien.calarm.R
import zone.ien.calarm.adapter.SubAlarmAdapter
import zone.ien.calarm.databinding.ActivityEditAlarmBinding

class EditAlarmActivity : AppCompatActivity() {

    lateinit var binding: ActivityEditAlarmBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_edit_alarm)
        binding.activity = this

        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = null
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.listSubAlarm.adapter = SubAlarmAdapter(arrayListOf(120, 55, 202, 39))
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
            }
        }
        return super.onOptionsItemSelected(item)
    }
}