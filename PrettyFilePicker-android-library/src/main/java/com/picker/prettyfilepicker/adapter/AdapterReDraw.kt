package com.picker.prettyfilepicker.adapter

import android.os.Build
import android.view.View
import android.webkit.MimeTypeMap
import android.widget.TextView
import androidx.annotation.RequiresApi
import com.google.android.material.card.MaterialCardView
import com.picker.prettyfilepicker.R
import com.picker.prettyfilepicker.models.FileModel
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

class AdapterReDraw(
    private val adapter: PickerListAdapter,
    private val filters: ArrayList<String>
) {
    @RequiresApi(Build.VERSION_CODES.O)
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

    @RequiresApi(Build.VERSION_CODES.O)
    fun openFolder(folderPath: String, view: View) {
        adapter.submitList(
            getFileList(folderPath, view)
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun getFileList(filePath: String, view: View): MutableList<FileModel> {
        view.findViewById<TextView>(R.id.currentDirectoryPath).text = filePath
        val fileList = mutableListOf<FileModel>()
        File(filePath).walk().forEach { file ->
            if(filters.isNotEmpty()) {
                if(!Files.isDirectory(Paths.get(file.absolutePath))){
                    val extension = MimeTypeMap.getFileExtensionFromUrl(file.absolutePath)
                    if (extension != null) {
                        val type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension).toString()
                        if(filters.contains(type)) {
                            fileList.add(
                                FileModel(
                                    file.name,
                                    file.toString()
                                )
                            )
                        }
                    }
                } else {
                    fileList.add(
                        FileModel(
                            file.name,
                            file.toString()
                        )
                    )
                }
            }
        }

        return fileList
    }
}