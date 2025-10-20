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

package com.android.permissioncontroller.permission.ui.handheld.v37

import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.RemoteCallback
import android.text.Html
import android.view.LayoutInflater
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import com.airbnb.lottie.LottieAnimationView
import com.android.permissioncontroller.R
import com.android.permissioncontroller.permission.ui.model.v37.LocationButtonViewModel
import com.android.permissioncontroller.permission.ui.model.v37.LocationButtonViewModelFactory
import com.android.settingslib.widget.LottieColorUtils

/** Fragment to show location button consent dialog on handheld devices. */
@RequiresApi(Build.VERSION_CODES.CINNAMON_BUN)
class RequestLocationButtonPermissionsFragment : DialogFragment() {
    private lateinit var viewModel: LocationButtonViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val args = requireArguments()
        val packageName = args.getString(Intent.EXTRA_PACKAGE_NAME)!!
        val remoteCallback =
            args.getParcelable(Intent.EXTRA_REMOTE_CALLBACK, RemoteCallback::class.java)!!

        val factory =
            LocationButtonViewModelFactory(
                requireActivity().application,
                packageName,
                remoteCallback,
            )
        viewModel =
            ViewModelProvider(requireActivity(), factory)[LocationButtonViewModel::class.java]
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val activity = requireActivity()
        val view =
            LayoutInflater.from(activity)
                .inflate(R.layout.request_location_button_permissions_dialog, null)
                .apply {
                    // Location (pin) icon
                    val iconView = requireViewById<ImageView>(R.id.icon)
                    viewModel.getLocationPinIconLiveData().observe(activity) { icon ->
                        iconView.setImageDrawable(icon)
                    }

                    // Title message
                    val messageView = requireViewById<TextView>(R.id.title)
                    viewModel.getAppLabelLiveData().observe(activity) { appLabel ->
                        val escapedAppLabel = Html.escapeHtml(appLabel)
                        val label =
                            Html.fromHtml(
                                resources.getString(
                                    R.string.request_location_button_permissions_dialog_title,
                                    escapedAppLabel,
                                ),
                                Html.FROM_HTML_MODE_COMPACT,
                            )
                        messageView.text = label
                    }

                    val lottieAnimationView =
                        requireViewById<LottieAnimationView>(R.id.illustration)
                    LottieColorUtils.applyMaterialColor(activity, lottieAnimationView)
                    lottieAnimationView.playAnimation()

                    // Allow & Deny buttons
                    requireViewById<Button>(R.id.allow).setOnClickListener {
                        viewModel.onAllow()
                        activity.finish()
                    }
                    requireViewById<Button>(R.id.dont_allow).setOnClickListener {
                        viewModel.onDontAllow()
                        activity.finish()
                    }
                }
        return Dialog(activity).apply { setContentView(view) }
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        requireActivity().finish()
    }

    companion object {
        fun newInstance(
            packageName: String,
            remoteCallback: RemoteCallback,
        ): RequestLocationButtonPermissionsFragment =
            RequestLocationButtonPermissionsFragment().apply {
                arguments =
                    Bundle().apply {
                        putString(Intent.EXTRA_PACKAGE_NAME, packageName)
                        putParcelable(Intent.EXTRA_REMOTE_CALLBACK, remoteCallback)
                    }
            }
    }
}
