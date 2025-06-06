package com.example.kgucapstone

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.kgucapstone.model.Medication

class MedicationAdapter(
    private val medications: List<Medication>,
    private val onCheckedChangeListener: (Medication, Boolean) -> Unit
) : RecyclerView.Adapter<MedicationAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameTextView: TextView = itemView.findViewById(R.id.tv_medication_name)
        private val dosageTextView: TextView = itemView.findViewById(R.id.tv_medication_dosage)
        private val descriptionTextView: TextView = itemView.findViewById(R.id.tv_medication_description)
        private val takenCheckBox: CheckBox = itemView.findViewById(R.id.cb_taken)

        fun bind(medication: Medication) {
            nameTextView.text = medication.name
            dosageTextView.text = medication.dosage
            descriptionTextView.text = medication.description

            // 체크박스 리스너 설정
            takenCheckBox.setOnCheckedChangeListener { _, isChecked ->
                onCheckedChangeListener(medication, isChecked)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_medication, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(medications[position])
    }

    override fun getItemCount(): Int = medications.size
}