package utez.edu.integradora_arfilter.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import utez.edu.integradora_arfilter.R
import utez.edu.integradora_arfilter.models.StepHistory

class StepHistoryAdapter(private var stepHistoryList: List<StepHistory>) :
    RecyclerView.Adapter<StepHistoryAdapter.StepHistoryViewHolder>() {

    class StepHistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val dateTextView: TextView = itemView.findViewById(R.id.textViewDate)
        val stepsTextView: TextView = itemView.findViewById(R.id.textViewSteps)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StepHistoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_step_history, parent, false)
        return StepHistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: StepHistoryViewHolder, position: Int) {
        val stepHistory = stepHistoryList[position]
        holder.dateTextView.text = stepHistory.date
        holder.stepsTextView.text = "${stepHistory.steps} pasos"
    }

    override fun getItemCount() = stepHistoryList.size
}