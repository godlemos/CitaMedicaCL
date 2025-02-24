package com.example.citamedicacl.utils

import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import android.view.Window
import android.widget.TextView
import com.example.citamedicacl.R

class ProgressDialog(context: Context) : Dialog(context) {
    init {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setCancelable(false)
        setContentView(R.layout.dialog_progress)
    }

    fun setMessage(message: String) {
        findViewById<TextView>(R.id.progressMessage).text = message
    }
} 