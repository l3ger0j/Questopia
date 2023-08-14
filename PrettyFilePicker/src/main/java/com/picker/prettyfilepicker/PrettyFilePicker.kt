package com.picker.prettyfilepicker

import android.app.Activity
import android.app.AlertDialog
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import com.picker.prettyfilepicker.bottomsheet.BottomSheetFragment

class PrettyFilePicker(
    private val activity: Activity,
    private val returnAsDocumentFile: Boolean = false,
    private val filters: Array<String> = arrayOf(),
    private val supportFragmentManager: FragmentManager
) {
    private lateinit var createdPickerInstance: AlertDialog
    var returnedData: MutableLiveData<Any> = MutableLiveData()

    @RequiresApi(Build.VERSION_CODES.O)
    fun runFilePicker(callback: (Any) -> Unit) {
        buildPicker()
        returnedData.observe(activity as LifecycleOwner) { returnedData ->
            callback.invoke(returnedData)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun buildPicker() {
        val modalBottomSheet = BottomSheetFragment(
            this,
            returnAsDocumentFile,
            filters
        )
        modalBottomSheet.show(supportFragmentManager, BottomSheetFragment.TAG)
    }

    fun destroy() {
        createdPickerInstance.dismiss()
    }
}