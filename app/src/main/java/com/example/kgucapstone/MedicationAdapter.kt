package com.example.kgucapstone

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.kgucapstone.model.Medication
import com.example.kgucapstone.model.MedicationRecord

class MedicationAdapter(
    private val medications: List<Medication>,
    private val medicationRecords: List<MedicationRecord> = emptyList(),
    private val onCheckedChangeListener: (Medication, Boolean) -> Unit,
    private val onDeleteClickListener: (Medication) -> Unit
) : RecyclerView.Adapter<MedicationAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameTextView: TextView = itemView.findViewById(R.id.tv_medication_name)
        val dosageTextView: TextView = itemView.findViewById(R.id.tv_medication_dosage)
        val descriptionTextView: TextView = itemView.findViewById(R.id.tv_medication_description)
        val takenCheckBox: CheckBox = itemView.findViewById(R.id.cb_taken)
        val deleteButton: ImageButton = itemView.findViewById(R.id.btn_delete_medication)

        fun bind(medication: Medication) {
            nameTextView.text = medication.name
            dosageTextView.text = medication.dosage
            descriptionTextView.text = medication.description

            // 기록에서 복용 여부 확인하여 체크박스 상태 설정
            val record = medicationRecords.find { it.medicationId == medication.id }
            takenCheckBox.isChecked = record?.taken ?: false

            // 체크박스 리스너 설정
            takenCheckBox.setOnCheckedChangeListener { _, isChecked ->
                onCheckedChangeListener(medication, isChecked)
            }

            // 삭제 버튼 리스너 설정
            deleteButton.setOnClickListener {
                onDeleteClickListener(medication)
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

    override fun getItemCount() = medications.size

    // 복용 기록 업데이트 메서드
    fun updateMedicationRecords(newRecords: List<MedicationRecord>) {
        notifyDataSetChanged()
    }
}