package com.picker.prettyfilepicker

import android.app.Activity
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.MutableLiveData

import com.picker.prettyfilepicker.factory.DialogFactory

class PrettyFilePicker(private val context: Activity) {
    fun create(
        title: String
    ) {
        DialogFactory(context).build(title)
    }

    companion object {
        @JvmStatic val fileFromPrettyFilePickerAsString: MutableLiveData<String> = MutableLiveData()
        @JvmStatic val fileFromPrettyFilePickerAsFile: MutableLiveData<DocumentFile> = MutableLiveData()
    }
}