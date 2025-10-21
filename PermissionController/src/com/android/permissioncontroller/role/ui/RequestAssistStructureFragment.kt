/*
 * Copyright (C) 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.permissioncontroller.role.ui

import android.app.Activity
import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.android.permissioncontroller.R

class RequestAssistStructureFragment : DialogFragment() {
    private lateinit var packageName: String

    private lateinit var iconImage: ImageView
    private lateinit var titleText: TextView
    private lateinit var positiveButton: Button
    private lateinit var negativeButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val arguments = arguments!!
        packageName = arguments.getString(Intent.EXTRA_PACKAGE_NAME)!!
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val activity = requireActivity()
        val view =
            LayoutInflater.from(activity)
                .inflate(R.layout.request_assist_structure_dialog, null)
                .apply {
                    // TODO(b/454050936): Load the icon for the assistant app.
                    iconImage = requireViewById<ImageView>(R.id.icon)
                    // TODO(b/454050936): Load the text with app label for the assistant app.
                    titleText = requireViewById<TextView>(R.id.title)
                    positiveButton = requireViewById<Button>(R.id.allow_button)
                    negativeButton = requireViewById<Button>(R.id.dont_allow_button)
                }
        positiveButton.apply { setOnClickListener { onAssistStructureGrant() } }
        negativeButton.apply { setOnClickListener { dialog!!.cancel() } }
        return Dialog(activity).apply { setContentView(view) }
    }

    fun onAssistStructureGrant() {
        setResultAndFinish(Activity.RESULT_OK)
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        setResultAndFinish(Activity.RESULT_CANCELED)
    }

    private fun setResultAndFinish(resultCode: Int) {
        val activity = requireActivity()
        activity.setResult(resultCode)
        activity.finish()
    }

    companion object {
        fun newInstance(packageName: String): RequestAssistStructureFragment =
            RequestAssistStructureFragment().apply {
                arguments = Bundle().apply { putString(Intent.EXTRA_PACKAGE_NAME, packageName) }
            }
    }
}
