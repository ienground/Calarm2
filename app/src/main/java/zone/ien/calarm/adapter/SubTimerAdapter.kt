package zone.ien.calarm.adapter

import android.animation.ValueAnimator
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.textview.MaterialTextView
import zone.ien.calarm.R
import zone.ien.calarm.activity.TAG
import zone.ien.calarm.callback.EditTimerListCallback
import zone.ien.calarm.databinding.DialogTimerNumBinding
import zone.ien.calarm.room.SubAlarmEntity
import zone.ien.calarm.room.SubTimerEntity
import zone.ien.calarm.utils.ItemActionListener
import zone.ien.calarm.utils.MyUtils.Companion.timeToText

class SubTimerAdapter(var items: ArrayList<SubTimerEntity>, var parentId: Long): RecyclerView.Adapter<SubTimerAdapter.ItemViewHolder>(), ItemActionListener {

    lateinit var context: Context

    private var callbackListener: EditTimerListCallback? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        context = parent.context
        val view = LayoutInflater.from(context).inflate(R.layout.adapter_sub_timer, parent, false)
        return ItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        items[holder.adapterPosition].time.let {
            holder.tvTime.text =
                if (it / 3600 != 0) String.format("%02d:%02d:%02d", it / 3600, (it % 3600) / 60, it % 60)
                else String.format("%02d:%02d", (it % 3600) / 60, it % 60)
            holder.tvLabel.text = items[holder.adapterPosition].label
        }

        holder.btnInsertUp.setOnClickListener {
            getNumpadDialog(false, holder.adapterPosition).show()
        }

        holder.btnInsertDown.setOnClickListener {
            getNumpadDialog(false, holder.adapterPosition + 1).show()
        }

        holder.tvLabel.setOnClickListener {
            MaterialAlertDialogBuilder(context).apply {
                val view = LayoutInflater.from(context).inflate(R.layout.dialog_input, LinearLayout(context), false)
                val inputLayout: TextInputLayout = view.findViewById(R.id.inputLayout)
                inputLayout.hint = context.getString(R.string.label)
                inputLayout.editText?.setText(items[holder.adapterPosition].label)

                setPositiveButton(android.R.string.ok) { dialog, id ->
                    items[holder.adapterPosition].label = inputLayout.editText?.text.toString()
                    notifyItemChanged(holder.adapterPosition)
                    dialog.dismiss()
                }

                setNegativeButton(android.R.string.cancel) { dialog, id -> }

                setView(view)
            }.show()
        }

        holder.tvTime.setOnClickListener {
            getNumpadDialog(true, holder.adapterPosition).show()
        }
//        holder.switchOn.isChecked = items[holder.adapterPosition].isEnabled
        holder.btnDelete.setOnClickListener {
            items.removeAt(holder.adapterPosition)
            notifyItemRemoved(holder.adapterPosition)
        }

    }

    override fun getItemCount(): Int = items.size

    private fun getNumpadDialog(isEdit: Boolean, position: Int): MaterialAlertDialogBuilder {
        return MaterialAlertDialogBuilder(context).apply {
            val binding: DialogTimerNumBinding = DataBindingUtil.inflate(LayoutInflater.from(context), R.layout.dialog_timer_num, null, false)
            val btnNum = listOf(binding.btnNum0, binding.btnNum1, binding.btnNum2, binding.btnNum3, binding.btnNum4, binding.btnNum5, binding.btnNum6, binding.btnNum7, binding.btnNum8, binding.btnNum9)
            val nums: ArrayList<Int> = arrayListOf()

            binding.display.text = timeToText(context, nums, R.color.colorBLUE, 32, 16)
            btnNum.forEachIndexed { index, materialButton ->
                materialButton.setOnClickListener {
                    if (nums.size < 6 && !(nums.isEmpty() && index == 0)) {
                        nums.add(index)
                        binding.display.text = timeToText(context, nums, R.color.colorBLUE, 32, 16)
                    }
                }
            }

            binding.btnDelete.setOnClickListener {
                if (nums.isNotEmpty()) {
                    nums.removeLast()
                    binding.display.text = timeToText(context, nums, R.color.colorBLUE, 32, 16)
                }
            }

            binding.btnDelete.setOnLongClickListener {
                nums.clear()
                binding.display.text = timeToText(context, nums, R.color.colorBLUE, 32, 16)
                true
            }

            setPositiveButton(android.R.string.ok) { dialog, id ->
                var duration = 0
                val data: ArrayList<Int> = arrayListOf()
                for (i in 0 until 6 - nums.size) data.add(0)
                data.addAll(nums)

                duration += 60 * 60 * (data[0] * 10 + data[1])
                duration += 60 * (data[2] * 10 + data[3])
                duration += (data[4] * 10 + data[5])

                if (isEdit) {
                    items[position].time = duration
                    notifyItemChanged(position)
                } else {
                    items.add(position, SubTimerEntity(parentId, "", duration, 0, ""))
                    notifyItemInserted(position)
                }
                callbackListener?.updateTotalDuration()
                dialog.dismiss()
            }

            setNegativeButton(android.R.string.cancel) { dialog, id ->

            }

            setView(binding.root)
        }
    }

    fun add(item: SubAlarmEntity) {
//        items.add(item)
//        items.sortBy { it.time }
//        val newIndex = items.indexOf(item)
//        notifyItemInserted(newIndex)
    }

    fun setCallbackListener(callbackListener: EditTimerListCallback) {
        this.callbackListener = callbackListener
    }

    override fun onItemSwiped(position: Int) {
        items.removeAt(position)
        notifyItemRemoved(position)
        callbackListener?.updateTotalDuration()
    }

    inner class ItemViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        val tvLabel: MaterialTextView = itemView.findViewById(R.id.tv_label)
        val tvTime: MaterialTextView = itemView.findViewById(R.id.tv_time)
        val btnInsertUp: ImageButton = itemView.findViewById(R.id.btn_insert_up)
        val btnInsertDown: ImageButton = itemView.findViewById(R.id.btn_insert_down)
        val btnStart: MaterialButton = itemView.findViewById(R.id.btn_start)
        val btnDelete: MaterialButton = itemView.findViewById(R.id.btn_delete)
    }
}