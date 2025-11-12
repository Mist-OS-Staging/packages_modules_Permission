/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.permissioncontroller.role.ui;

import static com.android.permissioncontroller.PermissionControllerStatsLog.ROLE_SETTINGS_CONFIRMATION_DIALOG_ACTION_REPORTED;
import static com.android.permissioncontroller.PermissionControllerStatsLog.ROLE_SETTINGS_CONFIRMATION_DIALOG_ACTION_REPORTED__RESULT__RESULT_CANCELLED;
import static com.android.permissioncontroller.PermissionControllerStatsLog.ROLE_SETTINGS_CONFIRMATION_DIALOG_ACTION_REPORTED__RESULT__RESULT_OK;
import static com.android.permissioncontroller.PermissionControllerStatsLog.ROLE_SETTINGS_CONFIRMATION_DIALOG_ACTION_REPORTED__RESULT__RESULT_UNSPECIFIED;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

import com.android.permissioncontroller.PermissionControllerStatsLog;

/**
 * {@link DialogFragment} for confirmation before setting a default app.
 */
public class DefaultAppConfirmationDialogFragment extends DialogFragment {
    private String mRoleName;
    private String mPackageName;
    private int mUid;
    private CharSequence mMessage;

    /**
     * Create a new instance of this fragment.
     *
     * @param roleName the name of the role being changed
     * @param packageName the package name of the application
     * @param uid the UID the specified package is running in
     * @param message the confirmation message
     *
     * @return a new instance of this fragment
     *
     * @see #show(String, String, int, CharSequence, Fragment)
     */
    @NonNull
    public static DefaultAppConfirmationDialogFragment newInstance(@NonNull String roleName,
            @NonNull String packageName, int uid, @NonNull CharSequence message) {
        DefaultAppConfirmationDialogFragment fragment = new DefaultAppConfirmationDialogFragment();
        Bundle arguments = new Bundle();
        arguments.putString(Intent.EXTRA_ROLE_NAME, roleName);
        arguments.putString(Intent.EXTRA_PACKAGE_NAME, packageName);
        arguments.putInt(Intent.EXTRA_UID, uid);
        arguments.putCharSequence(Intent.EXTRA_TEXT, message);
        fragment.setArguments(arguments);
        return fragment;
    }

    /**
     * Show a new instance of this fragment.
     *
     * @param roleName the name of the role being changed
     * @param packageName the package name of the application
     * @param uid the UID the specified package is running in
     * @param message the confirmation message
     * @param fragment the parent fragment
     *
     * @see #newInstance(String, String, int, CharSequence)
     */
    public static void show(@NonNull String roleName, @NonNull String packageName, int uid,
            @NonNull CharSequence message, @NonNull Fragment fragment) {
        newInstance(roleName, packageName, uid, message)
                .show(fragment.getChildFragmentManager(), null);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle arguments = getArguments();
        mRoleName = arguments.getString(Intent.EXTRA_ROLE_NAME);
        mPackageName = arguments.getString(Intent.EXTRA_PACKAGE_NAME);
        mUid = arguments.getInt(Intent.EXTRA_UID);
        mMessage = arguments.getCharSequence(Intent.EXTRA_TEXT);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        return new AlertDialog.Builder(requireContext(), getTheme())
                .setMessage(mMessage)
                .setPositiveButton(android.R.string.ok,
                        (dialog, which) -> onPositiveButtonClicked())
                .setNegativeButton(android.R.string.cancel,
                        (dialog, which) -> onNegativeButtonClicked())
                .create();
    }

    private void onPositiveButtonClicked() {
        PermissionControllerStatsLog.write(ROLE_SETTINGS_CONFIRMATION_DIALOG_ACTION_REPORTED, mUid,
                mPackageName, mRoleName,
                ROLE_SETTINGS_CONFIRMATION_DIALOG_ACTION_REPORTED__RESULT__RESULT_OK);
        Listener listener = (Listener) getParentFragment();
        listener.setDefaultApp(mPackageName, mUid);
    }

    private void onNegativeButtonClicked() {
        PermissionControllerStatsLog.write(ROLE_SETTINGS_CONFIRMATION_DIALOG_ACTION_REPORTED, mUid,
                mPackageName, mRoleName,
                ROLE_SETTINGS_CONFIRMATION_DIALOG_ACTION_REPORTED__RESULT__RESULT_CANCELLED);
    }

    @Override
    public void onCancel(@NonNull DialogInterface dialog) {
        super.onCancel(dialog);
        PermissionControllerStatsLog.write(ROLE_SETTINGS_CONFIRMATION_DIALOG_ACTION_REPORTED, mUid,
                mPackageName, mRoleName,
                ROLE_SETTINGS_CONFIRMATION_DIALOG_ACTION_REPORTED__RESULT__RESULT_UNSPECIFIED);
    }

    /**
     * Listener for {@link DefaultAppConfirmationDialogFragment}.
     */
    public interface Listener {

        /**
         * Set an application as the default app.
         *
         * @param packageName the package name of the application
         */
        void setDefaultApp(@NonNull String packageName, int uid);
    }
}
