package com.picker.prettyfilepicker

import android.app.Activity
import android.app.AlertDialog
import android.os.Build
import android.os.Environment
import androidx.annotation.RequiresApi
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.picker.prettyfilepicker.adapter.AdapterReDraw
import com.picker.prettyfilepicker.adapter.PickerListAdapter

class PrettyFilePicker(
    private val activity: Activity,
    private val title: String,
    private val returnAsDocumentFile: Boolean = false,
    private val filters: Array<String> = arrayOf()
) {
    private val builder = AlertDialog.Builder(activity)
    private lateinit var createdPickerInstance: AlertDialog
    var returnedData: MutableLiveData<Any> = MutableLiveData()

    @RequiresApi(Build.VERSION_CODES.O)
    fun runFilePicker(callback: (Any) -> Unit) {
        this.build()

        returnedData.observe(activity as LifecycleOwner) { returnedData ->
            callback.invoke(returnedData)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun build() {
        val view = activity.layoutInflater.inflate(R.layout.alert_dialog_layout, null)
        val defaultPath =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
                .toString()
        val adapter = PickerListAdapter(
            view,
            this,
            returnAsDocumentFile,
            filters
        )
        val adapterReDrawer = AdapterReDraw(adapter, filters)

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