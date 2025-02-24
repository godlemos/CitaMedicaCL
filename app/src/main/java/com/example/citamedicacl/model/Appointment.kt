package com.example.citamedicacl.model

data class Appointment(
    val id: String = "",
    val patientId: String = "",
    val patientName: String = "",
    val doctorId: String = "",
    val doctorName: String = "",
    val date: String = "",
    val time: String = "",
    val status: String = "pendiente", // pendiente, confirmada, cancelada
    val scheduledBy: String = "", // Nombre de quien agendó la cita
    val scheduledById: String = "" // ID de quien agendó la cita
) {
    constructor() : this("", "", "", "", "", "", "", "pendiente", "", "")
} 