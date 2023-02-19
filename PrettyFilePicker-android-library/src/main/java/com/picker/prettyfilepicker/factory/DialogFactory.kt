package com.picker.prettyfilepicker.factory

import android.app.Activity
import android.app.AlertDialog
import android.os.Environment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.picker.prettyfilepicker.R
import com.picker.prettyfilepicker.adapter.AdapterReDraw
import com.picker.prettyfilepicker.adapter.PickerListAdapter

class DialogFactory(
    private val activity: Activity
    ) {
    private val builder = AlertDialog.Builder(activity)
    private lateinit var createdPickerInstance: AlertDialog
    fun build(
        title: String
    ) {
        val view = activity.layoutInflater.inflate(R.layout.alert_dialog_layout,null)
        val defaultPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).toString()
        val adapter = PickerListAdapter(
            view,
            this
        )
        val adapterReDrawer = AdapterReDraw(adapter)

        builder.setTitle(title)

        val recyclerView: RecyclerView = view.findViewById(R.id.alertDialogRecyclerView)

        recyclerView.layoutManager =
            LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)

        recyclerView.adapter = adapter

        adapter.submitList(adapterReDrawer.getFileList(defaultPath, view))
        builder.setView(view)

        adapterReDrawer.reDrawList(view)

        createdPickerInstance = builder.create()
        createdPickerInstance.show()
    }

    fun destroy() {
        createdPickerInstance.dismiss()
    }
}