package com.example.kgucapstone

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.example.kgucapstone.model.Medication

class SelectMedicationAdapter(
    private val medications: List<Medication>,
    private val onItemClick: (Medication) -> Unit
) : RecyclerView.Adapter<SelectMedicationAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardView: CardView = itemView as CardView
        val nameTextView: TextView = itemView.findViewById(R.id.tv_medication_name)
        val dosageTextView: TextView = itemView.findViewById(R.id.tv_medication_dosage)
        val descriptionTextView: TextView = itemView.findViewById(R.id.tv_medication_description)

        fun bind(medication: Medication) {
            nameTextView.text = medication.name
            dosageTextView.text = medication.dosage
            descriptionTextView.text = medication.description

            cardView.setOnClickListener {
                onItemClick(medication)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_select_medication, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(medications[position])
    }

    override fun getItemCount() = medications.size
}