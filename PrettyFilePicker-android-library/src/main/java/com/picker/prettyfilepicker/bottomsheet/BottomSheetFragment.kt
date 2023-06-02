package com.picker.prettyfilepicker.bottomsheet

import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.picker.prettyfilepicker.PrettyFilePicker
import com.picker.prettyfilepicker.R
import com.picker.prettyfilepicker.adapter.AdapterReDraw
import com.picker.prettyfilepicker.adapter.PickerListAdapter

class BottomSheetFragment(
    private val prettyFilePicker: PrettyFilePicker,
    private val returnAsDocumentFile: Boolean,
    private val filters: Array<String>
) : BottomSheetDialogFragment() {

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.picker_layout, container, false)
        val defaultPath =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
                .toString()
        val adapter = PickerListAdapter(
            view,
            prettyFilePicker,
            this,
            returnAsDocumentFile,
            filters
        )
        val adapterReDrawer = AdapterReDraw(adapter, filters)

        val recyclerView: RecyclerView = view.findViewById(R.id.alertDialogRecyclerView)

        recyclerView.layoutManager =
            LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)

        recyclerView.adapter = adapter

        adapter.submitList(adapterReDrawer.getFileList(defaultPath, view))

        adapterReDrawer.reDrawList(view)
        return view
    }

    companion object {
        const val TAG = "ModalBottomSheet"
    }
}