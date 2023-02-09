package org.qp.android.utils

import android.app.Dialog
import android.content.Context
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import org.qp.android.R
import org.qp.android.viewModel.ActivityStock
import java.io.File

class QspDataPicker(val context: Context) {
    private val inflater = LayoutInflater.from(context)
    //private val activityStock: ActivityStock = ActivityStock()

    fun generateDataPicker() {
        val add_phone: ViewGroup = inflater.inflate(R.layout.filepicker_dialog_layout, null) as ViewGroup
        var okay_text: TextView
        var cancel_text: TextView
        val dialog = Dialog(context)
        dialog.setContentView(add_phone)
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.setCancelable(false)

        val directory = File(java.lang.String.valueOf(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)))
        val files: Array<String> = directory.list()!!
        Log.d("Files", "Size: " + files.size)
        val filer = StringBuilder()
        for (file in files) {
            filer.append(file)
        }
        val first: TextView = add_phone.findViewById(R.id.textpath)
        first.text = files.toString()
//        okay_text.setOnClickListener{
//            dialog.dismiss()
//            Toast.makeText(context, "okay clicked", Toast.LENGTH_SHORT).show()
//        }
//
//        cancel_text.setOnClickListener{
//            dialog.dismiss()
//            Toast.makeText(context, "Cancel clicked", Toast.LENGTH_SHORT).show()
//        }

        dialog.show()
        Log.e(
            "test",
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                .toString()
        )
    }
}