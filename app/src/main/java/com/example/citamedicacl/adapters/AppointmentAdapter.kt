package com.example.citamedicacl.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Button
import androidx.recyclerview.widget.RecyclerView
import com.example.citamedicacl.R
import com.example.citamedicacl.model.Appointment

class AppointmentAdapter(
    private var appointments: List<Appointment>,
    private val onCancelClick: (Appointment) -> Unit,
    private val onEditClick: (Appointment) -> Unit
) : RecyclerView.Adapter<AppointmentAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val patientNameText: TextView = view.findViewById(R.id.patientNameText)
        val doctorName: TextView = view.findViewById(R.id.doctorNameText)
        val dateTime: TextView = view.findViewById(R.id.dateTimeText)
        val status: TextView = view.findViewById(R.id.statusText)
        val cancelButton: Button = view.findViewById(R.id.cancelButton)
        val editButton: Button = view.findViewById(R.id.editButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_appointment, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val appointment = appointments[position]
        
        // Agregar log para depuración
        Log.d("AppointmentAdapter", "PatientName: ${appointment.patientName}")
        Log.d("AppointmentAdapter", "Appointment: $appointment")

        holder.patientNameText.text = appointment.patientName
        holder.doctorName.text = "Doctor: ${appointment.doctorName}"
        holder.dateTime.text = "Fecha: ${appointment.date} ${appointment.time}"
        
        holder.status.text = when(appointment.status) {
            "pendiente" -> "Estado: Pendiente"
            "confirmada" -> "Estado: Confirmada"
            "cancelada" -> "Estado: Cancelada"
            else -> "Estado: ${appointment.status}"
        }

        // Configurar visibilidad y listeners de los botones
        if (appointment.status == "pendiente") {
            holder.cancelButton.visibility = View.VISIBLE
            holder.editButton.visibility = View.VISIBLE
            holder.cancelButton.setOnClickListener { onCancelClick(appointment) }
            holder.editButton.setOnClickListener { onEditClick(appointment) }
        } else if (appointment.status == "confirmada") {
            holder.cancelButton.visibility = View.VISIBLE
            holder.cancelButton.setOnClickListener { onCancelClick(appointment) }
            holder.editButton.visibility = View.GONE
        } else {
            holder.cancelButton.visibility = View.GONE
            holder.editButton.visibility = View.GONE
        }
    }

    override fun getItemCount() = appointments.size

    fun updateAppointments(newAppointments: List<Appointment>) {
        appointments = newAppointments
        notifyDataSetChanged()
    }
} 