package com.example.citamedicacl.patient

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
import com.google.android.material.appbar.MaterialToolbar
import com.applandeo.materialcalendarview.CalendarView
import com.applandeo.materialcalendarview.EventDay
import com.applandeo.materialcalendarview.listeners.OnDayClickListener
import java.util.Date
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.datepicker.MaterialDatePicker
import android.app.TimePickerDialog
import java.text.SimpleDateFormat
import java.util.*
import android.widget.AutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import android.widget.ArrayAdapter
import com.example.citamedicacl.adapters.AppointmentAdapter
import com.example.citamedicacl.model.Appointment
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import com.example.citamedicacl.utils.NotificationHelper
import android.content.pm.PackageManager

class PatientActivity : AppCompatActivity() {
    private val authManager = AuthManager()
    private lateinit var calendarView: CalendarView
    private lateinit var appointmentsRecyclerView: RecyclerView
    private lateinit var fabAddAppointment: FloatingActionButton
    private val db = FirebaseFirestore.getInstance()
    private lateinit var appointmentAdapter: AppointmentAdapter
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_patient)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        // Solicitar permiso de notificaciones
        NotificationHelper(this).requestNotificationPermission(this)

        calendarView = findViewById(R.id.calendarView)
        appointmentsRecyclerView = findViewById(R.id.appointmentsRecyclerView)
        appointmentsRecyclerView.layoutManager = LinearLayoutManager(this)

        fabAddAppointment = findViewById(R.id.fabAddAppointment)
        fabAddAppointment.setOnClickListener {
            showNewAppointmentDialog()
        }

        setupCalendar()
        setupRecyclerView()
        loadAppointments()
    }

    private fun setupCalendar() {
        calendarView.setOnDayClickListener(object : OnDayClickListener {
            override fun onDayClick(eventDay: EventDay) {
                val selectedDate = eventDay.calendar.time
                loadAppointmentsForDate(selectedDate)
            }
        })
    }

    private fun loadAppointmentsForDate(date: Date) {
        // TODO: Implementar carga de citas para la fecha seleccionada
        // Aquí cargaremos solo las citas del paciente actual
    }

    private fun setupRecyclerView() {
        appointmentAdapter = AppointmentAdapter(
            appointments = emptyList(),
            onCancelClick = { appointment -> showCancelConfirmationDialog(appointment) },
            onEditClick = { /* No implementado para pacientes */ }
        )
        appointmentsRecyclerView.adapter = appointmentAdapter
    }

    private fun loadAppointments() {
        scope.launch {
            try {
                val currentUser = authManager.getCurrentUser()
                if (currentUser != null) {
                    val appointments = withContext(Dispatchers.IO) {
                        // Obtener las citas del paciente actual
                        val appointmentDocs = db.collection("appointments")
                            .whereEqualTo("patientId", currentUser.id)
                            .get()
                            .await()
                            .documents

                        // Obtener el nombre del paciente de la colección patients
                        val patientDoc = db.collection("patients")
                            .document(currentUser.id)
                            .get()
                            .await()
                        
                        val patientName = patientDoc.getString("name") ?: ""

                        // Mapear las citas con el nombre del paciente
                        appointmentDocs.mapNotNull { doc ->
                            doc.toObject(Appointment::class.java)?.copy(
                                id = doc.id,
                                patientName = patientName
                            )
                        }
                    }
                    appointmentAdapter.updateAppointments(appointments)
                }
            } catch (e: Exception) {
                Toast.makeText(this@PatientActivity, 
                    "Error al cargar las citas: ${e.message}", 
                    Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.patient_menu, menu)
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
                // TODO: Validar y guardar la cita
                val doctor = doctorSpinner.text.toString()
                val date = dateEditText.text.toString()
                val time = timeSpinner.text.toString()
                
                if (doctor.isNotEmpty() && date.isNotEmpty() && time.isNotEmpty()) {
                    createAppointment(doctor, date, time)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
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

    private fun createAppointment(doctor: String, date: String, time: String) {
        scope.launch {
            try {
                val currentUser = authManager.getCurrentUser()
                if (currentUser != null) {
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
                            this@PatientActivity,
                            "Este horario ya está ocupado. Por favor seleccione otro horario.",
                            Toast.LENGTH_LONG
                        ).show()
                        return@launch
                    }

                    // Si no existe una cita, procedemos a crear la nueva
                    val appointment = Appointment(
                        id = db.collection("appointments").document().id,
                        patientId = currentUser.id,
                        patientName = currentUser.name,
                        doctorName = doctor,
                        date = date,
                        time = time,
                        scheduledBy = currentUser.name,
                        scheduledById = currentUser.id
                    )

                    withContext(Dispatchers.IO) {
                        db.collection("appointments")
                            .document(appointment.id)
                            .set(appointment)
                            .await()
                    }

                    // Mostrar notificación
                    NotificationHelper(this@PatientActivity)
                        .showAppointmentNotification(doctor, date, time)

                    Toast.makeText(
                        this@PatientActivity, 
                        "Cita agendada exitosamente", 
                        Toast.LENGTH_SHORT
                    ).show()
                    loadAppointments()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@PatientActivity, 
                    "Error al agendar la cita: ${e.message}", 
                    Toast.LENGTH_SHORT
                ).show()
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
                Toast.makeText(this@PatientActivity, 
                    "Cita eliminada exitosamente", 
                    Toast.LENGTH_SHORT).show()
                loadAppointments()
            } catch (e: Exception) {
                Toast.makeText(this@PatientActivity, 
                    "Error al eliminar la cita: ${e.message}", 
                    Toast.LENGTH_SHORT).show()
            }
        }
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