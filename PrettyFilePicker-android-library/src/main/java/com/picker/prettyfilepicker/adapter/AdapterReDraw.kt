package com.picker.prettyfilepicker.adapter

import android.view.View
import android.widget.TextView
import com.google.android.material.card.MaterialCardView
import com.picker.prettyfilepicker.R
import com.picker.prettyfilepicker.models.FileModel
import java.io.File

class AdapterReDraw(
    private val adapter: PickerListAdapter
) {
    fun reDrawList(view: View) {
        view.findViewById<MaterialCardView>(R.id.returnToPrevious).setOnClickListener {
            val path = File(
                view.findViewById<TextView>(R.id.currentDirectoryPath).text.toString()
            ).parentFile
            if (path != null) {
                adapter.submitList(
                    getFileList(path.toString(), view)
                )
            }
        }
    }

    fun openFolder(folderPath: String, view: View) {
        adapter.submitList(
            getFileList(folderPath, view)
        )
    }

    fun getFileList(filePath: String, view: View): MutableList<FileModel> {
        view.findViewById<TextView>(R.id.currentDirectoryPath).text = filePath
        val fileList = mutableListOf<FileModel>()
        File(filePath).walk().forEach { file ->
            fileList.add(
                FileModel(
                    file.name,
                    file.toString()
                )
            )
        }

        return fileList
    }
}