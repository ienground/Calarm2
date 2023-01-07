package zone.ien.calarm.activity

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.widget.LinearLayout.LayoutParams
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import zone.ien.calarm.R
import zone.ien.calarm.databinding.ActivityTestBinding
import zone.ien.calarm.utils.MyUtils

class TestActivity : AppCompatActivity() {

    lateinit var binding: ActivityTestBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_test)

        android.R.color.system_accent1_0
//        android.R.color.
        val type = listOf("accent1", "accent2", "accent3", "neutral1", "neutral2")
        val colors = listOf(0, 10, 50, 100, 200, 300, 400, 500, 600, 700, 800, 900, 1000)

        type.forEach { t ->
            colors.forEach { color ->
                val textView = TextView(applicationContext)
                textView.text = "android.R.color.system_${t}_${color}"
                val c = resources.getIdentifier("system_${t}_${color}", "color", "android")
                textView.setBackgroundColor(ContextCompat.getColor(applicationContext, c))
                val params = LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, MyUtils.dpToPx(applicationContext, 80f))

                binding.layout.addView(textView, params)
            }
        }


    }
}