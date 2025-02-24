package com.example.citamedicacl.patient

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.citamedicacl.R
import com.example.citamedicacl.model.Appointment

class MyAppointmentsFragment : Fragment() {
    private lateinit var recyclerView: RecyclerView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_my_appointments, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recyclerView = view.findViewById(R.id.appointmentsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(context)
        // TODO: Implementar adapter y cargar citas
    }
} 