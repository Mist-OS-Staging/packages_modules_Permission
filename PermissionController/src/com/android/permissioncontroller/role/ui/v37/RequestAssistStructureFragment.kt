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

package com.android.permissioncontroller.role.ui.v37

import android.app.Activity
import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Process
import android.view.LayoutInflater
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Lifecycle.State
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.android.permissioncontroller.R
import com.android.permissioncontroller.common.model.Stateful
import com.android.permissioncontroller.pm.data.repository.v31.PackageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class RequestAssistStructureFragment : DialogFragment() {
    private lateinit var packageName: String

    private lateinit var viewModel: RequestAssistStructureViewModel

    private lateinit var iconImage: ImageView
    private lateinit var titleText: TextView
    private lateinit var positiveButton: Button
    private lateinit var negativeButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val arguments = arguments!!
        packageName = arguments.getString(Intent.EXTRA_PACKAGE_NAME)!!
    }

    override fun onStart() {
        super.onStart()

        val factory =
            RequestAssistStructureViewModelFactory(requireActivity().getApplication(), packageName)

        viewModel =
            ViewModelProvider(this, factory).get(RequestAssistStructureViewModel::class.java)

        val packageRepository = PackageRepository.createInstance(requireContext())

        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(State.STARTED) {
                viewModel.uiStateFlow
                    .map { uiState ->
                        when (uiState) {
                            is Stateful.Failure -> Stateful.Failure(throwable = uiState.throwable)
                            is Stateful.Loading -> Stateful.Loading()
                            is Stateful.Success -> {
                                val icon =
                                    packageRepository.getBadgedPackageIcon(
                                        packageName,
                                        Process.myUserHandle(),
                                    )
                                val label =
                                    packageRepository.getPackageLabel(
                                        packageName,
                                        Process.myUserHandle(),
                                    )
                                Stateful.Success(
                                    RequestAssistStructureRichUiState(icon, label, uiState.value)
                                )
                            }
                        }
                    }
                    .flowOn(Dispatchers.Default)
                    .collect(::onUiStateChanged)
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val activity = requireActivity()
        val view =
            LayoutInflater.from(activity)
                .inflate(R.layout.request_assist_structure_dialog, null)
                .apply {
                    iconImage = requireViewById<ImageView>(R.id.icon)
                    titleText = requireViewById<TextView>(R.id.title)
                    positiveButton = requireViewById<Button>(R.id.allow_button)
                    negativeButton = requireViewById<Button>(R.id.dont_allow_button)
                }
        positiveButton.apply { setOnClickListener { onAssistStructureGrant() } }
        negativeButton.apply { setOnClickListener { dialog!!.cancel() } }
        return Dialog(activity).apply { setContentView(view) }
    }

    private fun onUiStateChanged(uiState: Stateful<RequestAssistStructureRichUiState>) {
        if (uiState is Stateful.Loading || uiState is Stateful.Failure) {
            // TODO: We should show a loading indicator in the dialog when in loading state
            return
        }

        val uiData = uiState.value!!

        if (uiData.requestAssistState == RequestAssistState.ALLOWED) {
            setResultAndFinish(Activity.RESULT_OK)
        } else if (uiData.requestAssistState == RequestAssistState.UNREQUESTABLE) {
            setResultAndFinish(Activity.RESULT_CANCELED)
        }

        val title = getString(R.string.request_assist_structure_dialog_title, uiData.label)

        iconImage.setImageDrawable(uiData.icon)
        titleText.text = title
    }

    fun onAssistStructureGrant() {
        viewModel.setAllowed()
        setResultAndFinish(Activity.RESULT_OK)
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        viewModel.markRequestDenied()
        setResultAndFinish(Activity.RESULT_CANCELED)
    }

    private fun setResultAndFinish(resultCode: Int) {
        val activity = requireActivity()
        activity.setResult(resultCode)
        activity.finish()
    }

    /** The data class for UI state of RequestAssistStructure dialog. */
    data class RequestAssistStructureRichUiState(
        val icon: Drawable?,
        val label: String,
        val requestAssistState: RequestAssistState,
    )

    companion object {
        fun newInstance(packageName: String): RequestAssistStructureFragment =
            RequestAssistStructureFragment().apply {
                arguments = Bundle().apply { putString(Intent.EXTRA_PACKAGE_NAME, packageName) }
            }
    }
}
