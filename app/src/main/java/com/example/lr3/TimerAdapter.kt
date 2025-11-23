package com.example.lr3

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.lr3.R
import java.util.concurrent.TimeUnit

class TimerAdapter(
    private var timers: List<StudentTimer>,
    private val onAction: (Int, TimerAction) -> Unit
) : RecyclerView.Adapter<TimerAdapter.TimerViewHolder>(){
    enum class TimerAction {START, PAUSE, RESET}

    class TimerViewHolder(view : View) : RecyclerView.ViewHolder(view){
        val nameTextView: TextView = view.findViewById(R.id.student_name)
        val timeTextView: TextView = view.findViewById(R.id.timer_time)
        val startButton: Button = view.findViewById(R.id.btn_start)
        val pauseButton: Button = view.findViewById(R.id.btn_pause)
        val resetButton: Button = view.findViewById(R.id.btn_reset)

        fun bind(timer: StudentTimer, onAction: (Int, TimerAction) -> Unit){
            nameTextView.text = timer.name
            timeTextView.text = formatTime(timer.currentTimeMillis)

            startButton.isEnabled = !timer.isRunning
            pauseButton.isEnabled = timer.isRunning

            startButton.setOnClickListener{onAction(timer.id, TimerAction.START)}
            pauseButton.setOnClickListener{onAction(timer.id, TimerAction.PAUSE)}
            resetButton.setOnClickListener{onAction(timer.id, TimerAction.RESET)}
        }

        private fun formatTime(millis: Long):String{
            val hours = TimeUnit.MILLISECONDS.toHours(millis)
            val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
            val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60
            return String.format("%02d:%02d:%02d", hours, minutes, seconds)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewtype: Int): TimerViewHolder{
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_timer, parent, false)
        return TimerViewHolder(view)
    }

    override fun onBindViewHolder(holder: TimerViewHolder, position: Int){
        holder.bind(timers[position], onAction)
    }

    override fun getItemCount() = timers.size

    fun updateData(newTimers: List<StudentTimer>){
        this.timers = newTimers.sortedBy{ it.id }
        notifyDataSetChanged()
    }
}