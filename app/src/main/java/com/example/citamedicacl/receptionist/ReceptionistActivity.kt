package com.example.citamedicacl.receptionist

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.citamedicacl.MainActivity
import com.example.citamedicacl.R
import com.example.citamedicacl.auth.AuthManager
import com.example.citamedicacl.model.Appointment
import com.example.citamedicacl.adapters.AppointmentAdapter
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import kotlinx.coroutines.*
import android.widget.Toast
import com.google.android.material.datepicker.MaterialDatePicker
import android.widget.AutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import android.widget.ArrayAdapter
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.tasks.await
import com.google.firebase.firestore.DocumentSnapshot
import com.example.citamedicacl.utils.NotificationHelper
import android.content.pm.PackageManager
import android.util.Log

class ReceptionistActivity : AppCompatActivity() {
    private val authManager = AuthManager()
    private lateinit var appointmentsRecyclerView: RecyclerView
    private lateinit var fabAddAppointment: FloatingActionButton
    private val db = FirebaseFirestore.getInstance()
    private lateinit var appointmentAdapter: AppointmentAdapter
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_receptionist)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        // Solicitar permiso de notificaciones
        NotificationHelper(this).requestNotificationPermission(this)

        setupViews()
        setupRecyclerView()
        loadAllAppointments()
    }

    private fun setupViews() {
        appointmentsRecyclerView = findViewById(R.id.appointmentsRecyclerView)
        appointmentsRecyclerView.layoutManager = LinearLayoutManager(this)
        
        fabAddAppointment = findViewById(R.id.fabAddAppointment)
        fabAddAppointment.setOnClickListener {
            showNewAppointmentDialog()
        }
    }

    private fun setupRecyclerView() {
        appointmentAdapter = AppointmentAdapter(
            emptyList(),
            onCancelClick = { appointment -> showCancelConfirmationDialog(appointment) },
            onEditClick = { appointment -> showEditAppointmentDialog(appointment) }
        )
        appointmentsRecyclerView.adapter = appointmentAdapter
    }

    private fun loadAllAppointments() {
        scope.launch {
            try {
                val appointments = withContext(Dispatchers.IO) {
                    // Primero obtenemos todas las citas
                    val appointmentDocs = db.collection("appointments")
                        .get()
                        .await()
                        .documents

                    // Convertimos cada documento a un objeto Appointment
                    appointmentDocs.mapNotNull { doc ->
                        val appointment = doc.toObject(Appointment::class.java)
                        if (appointment != null) {
                            // Obtenemos el documento del paciente usando el patientId
                            val patientDoc = db.collection("patients")
                                .document(appointment.patientId)
                                .get()
                                .await()

                            // Creamos una nueva cita con el nombre del paciente actualizado
                            appointment.copy(
                                id = doc.id,
                                patientName = patientDoc.getString("name") ?: ""
                            )
                        } else null
                    }
                }
                appointmentAdapter.updateAppointments(appointments)
            } catch (e: Exception) {
                Toast.makeText(this@ReceptionistActivity, 
                    "Error al cargar las citas: ${e.message}", 
                    Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showCancelConfirmationDialog(appointment: Appointment) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Cancelar cita")
            .setMessage("¿Estás seguro que deseas cancelar esta cita?")
            .setPositiveButton("Sí") { _, _ ->
                cancelAppointment(appointment)
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun cancelAppointment(appointment: Appointment) {
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    db.collection("appointments")
                        .document(appointment.id)
                        .delete()
                        .await()
                }
                Toast.makeText(this@ReceptionistActivity, 
                    "Cita eliminada exitosamente", 
                    Toast.LENGTH_SHORT).show()
                loadAllAppointments()
            } catch (e: Exception) {
                Toast.makeText(this@ReceptionistActivity, 
                    "Error al eliminar la cita: ${e.message}", 
                    Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showNewAppointmentDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_new_appointment, null)
        val doctorSpinner = dialogView.findViewById<AutoCompleteTextView>(R.id.doctorSpinner)
        val dateEditText = dialogView.findViewById<TextInputEditText>(R.id.dateEditText)
        val timeSpinner = dialogView.findViewById<AutoCompleteTextView>(R.id.timeSpinner)

        // Configurar el selector de médicos
        val doctors = listOf(
            "Dr. Juan Pérez - Cardiología",
            "Dra. María González - Pediatría",
            "Dr. Carlos Rodríguez - Traumatología",
            "Dra. Ana Silva - Dermatología",
            "Dr. Luis Martínez - Oftalmología",
            "Dra. Carmen Ruiz - Ginecología",
            "Dr. Roberto Sánchez - Neurología",
            "Dra. Patricia López - Endocrinología",
            "Dr. Miguel Ángel Torres - Urología",
            "Dra. Isabel Castro - Psiquiatría",
            "Dr. Francisco Morales - Otorrinolaringología",
            "Dra. Laura Mendoza - Medicina Interna"
        )
        val doctorAdapter = ArrayAdapter(this, R.layout.list_item, doctors)
        doctorSpinner.setAdapter(doctorAdapter)

        // Configurar el selector de fecha
        dateEditText.setOnClickListener {
            showDatePicker(dateEditText)
        }

        // Configurar el selector de hora
        val timeSlots = listOf(
            "08:00 AM", "08:30 AM",
            "09:00 AM", "09:30 AM",
            "10:00 AM", "10:30 AM",
            "11:00 AM", "11:30 AM",
            "12:00 PM", "12:30 PM",
            "01:00 PM", "01:30 PM",
            "02:00 PM", "02:30 PM",
            "03:00 PM", "03:30 PM",
            "04:00 PM", "04:30 PM",
            "05:00 PM", "05:30 PM",
            "06:00 PM", "06:30 PM",
            "07:00 PM", "07:30 PM"
        )
        val timeAdapter = ArrayAdapter(this, R.layout.list_item, timeSlots)
        timeSpinner.setAdapter(timeAdapter)

        MaterialAlertDialogBuilder(this)
            .setTitle("Nueva Cita")
            .setView(dialogView)
            .setPositiveButton("Agendar") { _, _ ->
                val doctor = doctorSpinner.text.toString()
                val date = dateEditText.text.toString()
                val time = timeSpinner.text.toString()
                
                if (doctor.isNotEmpty() && date.isNotEmpty() && time.isNotEmpty()) {
                    // Para el recepcionista, podemos pedir el ID del paciente en otro diálogo
                    showPatientIdDialog(doctor, date, time)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showPatientIdDialog(doctor: String, date: String, time: String) {
        val input = TextInputEditText(this).apply {
            hint = "ID del paciente"
            setPadding(50, 50, 50, 50)
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Ingresar ID del paciente")
            .setView(input)
            .setPositiveButton("Confirmar") { _, _ ->
                val patientId = input.text.toString()
                if (patientId.isNotEmpty()) {
                    createAppointment(patientId, doctor, date, time)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showEditAppointmentDialog(appointment: Appointment) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_appointment, null)
        val doctorSpinner = dialogView.findViewById<AutoCompleteTextView>(R.id.doctorSpinner)
        val dateEditText = dialogView.findViewById<TextInputEditText>(R.id.dateEditText)
        val timeSpinner = dialogView.findViewById<AutoCompleteTextView>(R.id.timeSpinner)
        val statusSpinner = dialogView.findViewById<AutoCompleteTextView>(R.id.statusSpinner)
        val patientNameInput = dialogView.findViewById<TextInputEditText>(R.id.patientNameInput)

        // Configurar los valores actuales
        patientNameInput.setText(appointment.patientName)
        
        // Configurar el selector de estado
        val statusOptions = listOf("pendiente", "confirmada")
        val statusAdapter = ArrayAdapter(this, R.layout.list_item, statusOptions)
        statusSpinner.setAdapter(statusAdapter)
        statusSpinner.setText(appointment.status, false)

        // Configurar el selector de médicos
        val doctors = listOf(
            "Dr. Juan Pérez - Cardiología",
            "Dra. María González - Pediatría",
            "Dr. Carlos Rodríguez - Traumatología",
            "Dra. Ana Silva - Dermatología",
            "Dr. Luis Martínez - Oftalmología",
            "Dra. Carmen Ruiz - Ginecología",
            "Dr. Roberto Sánchez - Neurología",
            "Dra. Patricia López - Endocrinología",
            "Dr. Miguel Ángel Torres - Urología",
            "Dra. Isabel Castro - Psiquiatría",
            "Dr. Francisco Morales - Otorrinolaringología",
            "Dra. Laura Mendoza - Medicina Interna"
        )
        val doctorAdapter = ArrayAdapter(this, R.layout.list_item, doctors)
        doctorSpinner.setAdapter(doctorAdapter)
        doctorSpinner.setText(appointment.doctorName, false)

        // Configurar la fecha
        dateEditText.setText(appointment.date)
        dateEditText.setOnClickListener {
            showDatePicker(dateEditText)
        }

        // Configurar el selector de hora
        val timeSlots = listOf(
            "08:00 AM", "08:30 AM",
            "09:00 AM", "09:30 AM",
            "10:00 AM", "10:30 AM",
            "11:00 AM", "11:30 AM",
            "12:00 PM", "12:30 PM",
            "01:00 PM", "01:30 PM",
            "02:00 PM", "02:30 PM",
            "03:00 PM", "03:30 PM",
            "04:00 PM", "04:30 PM",
            "05:00 PM", "05:30 PM",
            "06:00 PM", "06:30 PM",
            "07:00 PM", "07:30 PM"
        )
        val timeAdapter = ArrayAdapter(this, R.layout.list_item, timeSlots)
        timeSpinner.setAdapter(timeAdapter)
        timeSpinner.setText(appointment.time, false)

        MaterialAlertDialogBuilder(this)
            .setTitle("Editar Cita")
            .setView(dialogView)
            .setPositiveButton("Guardar") { _, _ ->
                val doctor = doctorSpinner.text.toString()
                val date = dateEditText.text.toString()
                val time = timeSpinner.text.toString()
                val newStatus = statusSpinner.text.toString()
                
                if (doctor.isNotEmpty() && date.isNotEmpty() && time.isNotEmpty() && newStatus.isNotEmpty()) {
                    updateAppointment(appointment.copy(
                        doctorName = doctor,
                        date = date,
                        time = time,
                        status = newStatus
                    ))
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun updateAppointment(appointment: Appointment) {
        scope.launch {
            try {
                // Solo verificar conflictos si cambió la fecha, hora o doctor
                val shouldCheckConflicts = appointment.doctorName != appointment.doctorName ||
                                         appointment.date != appointment.date ||
                                         appointment.time != appointment.time

                if (shouldCheckConflicts) {
                    val existingAppointment = withContext(Dispatchers.IO) {
                        db.collection("appointments")
                            .whereEqualTo("doctorName", appointment.doctorName)
                            .whereEqualTo("date", appointment.date)
                            .whereEqualTo("time", appointment.time)
                            .whereNotEqualTo("id", appointment.id)
                            .get()
                            .await()
                    }

                    if (!existingAppointment.isEmpty) {
                        Toast.makeText(
                            this@ReceptionistActivity,
                            "Este horario ya está ocupado. Por favor seleccione otro horario.",
                            Toast.LENGTH_LONG
                        ).show()
                        return@launch
                    }
                }

                withContext(Dispatchers.IO) {
                    db.collection("appointments")
                        .document(appointment.id)
                        .update(
                            mapOf(
                                "doctorName" to appointment.doctorName,
                                "date" to appointment.date,
                                "time" to appointment.time,
                                "status" to appointment.status
                            )
                        )
                        .await()
                }

                Toast.makeText(
                    this@ReceptionistActivity,
                    "Cita actualizada exitosamente",
                    Toast.LENGTH_SHORT
                ).show()
                loadAllAppointments()
            } catch (e: Exception) {
                Log.e("ReceptionistActivity", "Error updating appointment", e)
                Toast.makeText(
                    this@ReceptionistActivity,
                    "Error al actualizar la cita: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun createAppointment(patientId: String, doctor: String, date: String, time: String) {
        scope.launch {
            try {
                // Primero verificamos si ya existe una cita con el mismo doctor, fecha y hora
                val existingAppointment = withContext(Dispatchers.IO) {
                    db.collection("appointments")
                        .whereEqualTo("doctorName", doctor)
                        .whereEqualTo("date", date)
                        .whereEqualTo("time", time)
                        .get()
                        .await()
                }

                if (!existingAppointment.isEmpty) {
                    Toast.makeText(
                        this@ReceptionistActivity,
                        "Este horario ya está ocupado. Por favor seleccione otro horario.",
                        Toast.LENGTH_LONG
                    ).show()
                    return@launch
                }

                val currentUser = authManager.getCurrentUser()
                val patientDoc = withContext(Dispatchers.IO) {
                    db.collection("patients")
                        .document(patientId)
                        .get()
                        .await()
                }

                if (!patientDoc.exists()) {
                    Toast.makeText(this@ReceptionistActivity,
                        "Paciente no encontrado",
                        Toast.LENGTH_SHORT).show()
                    return@launch
                }

                // Agregar logs para depuración
                Log.d("ReceptionistActivity", "Patient document: ${patientDoc.data}")
                
                val patientName = patientDoc.getString("name")
                Log.d("ReceptionistActivity", "Patient name from doc: $patientName")

                if (patientName.isNullOrEmpty()) {
                    Toast.makeText(this@ReceptionistActivity,
                        "Error: No se pudo obtener el nombre del paciente",
                        Toast.LENGTH_SHORT).show()
                    return@launch
                }
                
                val appointment = Appointment(
                    id = db.collection("appointments").document().id,
                    patientId = patientId,
                    patientName = patientName,
                    doctorName = doctor,
                    date = date,
                    time = time,
                    status = "pendiente",
                    scheduledBy = currentUser?.name ?: "Recepcionista",
                    scheduledById = currentUser?.id ?: ""
                )

                // Log para verificar el objeto appointment
                Log.d("ReceptionistActivity", "Created appointment: $appointment")

                withContext(Dispatchers.IO) {
                    db.collection("appointments")
                        .document(appointment.id)
                        .set(appointment)
                        .await()
                }

                // Mostrar notificación
                NotificationHelper(this@ReceptionistActivity)
                    .showAppointmentNotification(doctor, date, time)

                Toast.makeText(this@ReceptionistActivity,
                    "Cita agendada exitosamente",
                    Toast.LENGTH_SHORT).show()
                loadAllAppointments()
            } catch (e: Exception) {
                Log.e("ReceptionistActivity", "Error creating appointment", e)
                Toast.makeText(this@ReceptionistActivity,
                    "Error al agendar la cita: ${e.message}",
                    Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showDatePicker(dateEditText: TextInputEditText) {
        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Seleccionar fecha")
            .build()

        datePicker.addOnPositiveButtonClickListener { selection ->
            val date = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                .format(Date(selection))
            dateEditText.setText(date)
        }

        datePicker.show(supportFragmentManager, "DATE_PICKER")
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.receptionist_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_logout -> {
                authManager.logout()
                startActivity(Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                })
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onBackPressed() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Salir")
            .setMessage("¿Estás seguro que deseas salir de la aplicación?")
            .setPositiveButton("Sí") { _, _ ->
                finishAffinity() // Esto cerrará la aplicación completamente
            }
            .setNegativeButton("No", null)
            .setIcon(R.drawable.ic_warning)
            .show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            NotificationHelper.NOTIFICATION_PERMISSION_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, 
                        "Notificaciones habilitadas", 
                        Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
} 