package com.aikodasistani.aikodasistani.ui

import android.app.Activity
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.aikodasistani.aikodasistani.R

class AttachmentOptionsBottomSheet(private val activity: Activity) {

    fun show(
        onCamera: () -> Unit,
        onGallery: () -> Unit,
        onFile: () -> Unit,
        onVideo: () -> Unit,
        onRecordVideo: () -> Unit,
        onUrl: () -> Unit
    ) {
        Log.d("AttachmentOptionsBS", "show() called")
        try {
            val dialog = BottomSheetDialog(activity)
            Log.d("AttachmentOptionsBS", "BottomSheetDialog created")

            val content = LayoutInflater.from(activity).inflate(R.layout.bottom_sheet_attachment_options, null)
            Log.d("AttachmentOptionsBS", "Inflated bottom_sheet_attachment_options layout")

            content.findViewById<View>(R.id.optionCamera).setOnClickListener {
                Log.d("AttachmentOptionsBS", "optionCamera clicked")
                dialog.dismiss()
                onCamera()
            }
            content.findViewById<View>(R.id.optionRecordVideo).setOnClickListener {
                Log.d("AttachmentOptionsBS", "optionRecordVideo clicked")
                dialog.dismiss()
                onRecordVideo()
            }
            content.findViewById<View>(R.id.optionGallery).setOnClickListener {
                Log.d("AttachmentOptionsBS", "optionGallery clicked")
                dialog.dismiss()
                onGallery()
            }
            content.findViewById<View>(R.id.optionFile).setOnClickListener {
                Log.d("AttachmentOptionsBS", "optionFile clicked")
                dialog.dismiss()
                onFile()
            }
            content.findViewById<View>(R.id.optionVideo).setOnClickListener {
                Log.d("AttachmentOptionsBS", "optionVideo clicked")
                dialog.dismiss()
                onVideo()
            }
            content.findViewById<View>(R.id.optionUrl).setOnClickListener {
                Log.d("AttachmentOptionsBS", "optionUrl clicked")
                dialog.dismiss()
                onUrl()
            }

            dialog.setContentView(content)
            Log.d("AttachmentOptionsBS", "Content view set, showing dialog")
            dialog.show()
            Log.d("AttachmentOptionsBS", "dialog.show() returned")
        } catch (e: Exception) {
            Log.e("AttachmentOptionsBS", "Failed to show bottom sheet", e)
            throw e
        }
    }
}
