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

import android.app.role.RoleManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.permission.flags.Flags
import android.util.Log
import androidx.fragment.app.FragmentActivity

class RequestAssistStructureActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!isAssistStructureUiEnabled(this)) {
            Log.w(
                LOG_TAG,
                "Assist Structure privacy improvements aren't enabled: Either the platform is" +
                    " not supported or the UI flag " +
                    "FLAG_ASSIST_SETTINGS_PRIVACY_IMPROVEMENTS_ENABLED isn't enabled.",
            )
            setResultAndFinish(RESULT_CANCELED)
            return
        }

        val packageName = callingPackage
        if (packageName.isNullOrEmpty() || !isDefaultAssistant(packageName, this)) {
            Log.e(LOG_TAG, "Unknown/Invalid role holder package: $packageName.")
            setResultAndFinish(RESULT_CANCELED)
            return
        }

        // TODO: We should finish() the activity if the access is already granted.

        val fragment = RequestAssistStructureFragment.Companion.newInstance(packageName)
        supportFragmentManager.beginTransaction().add(fragment, null).commit()
    }

    private fun setResultAndFinish(resultCode: Int) {
        setResult(resultCode)
        finish()
    }

    private fun isAssistStructureUiEnabled(context: Context): Boolean {
        val packageManager = context.packageManager
        return Flags.assistSettingsPrivacyImprovementsEnabled() &&
            !packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK) &&
            !packageManager.hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE) &&
            !packageManager.hasSystemFeature(PackageManager.FEATURE_WATCH)
    }

    private fun isDefaultAssistant(packageName: String, context: Context): Boolean {
        val roleManager = context.getSystemService(RoleManager::class.java)
        val currentPackageNames = roleManager.getRoleHolders(RoleManager.ROLE_ASSISTANT)
        return packageName in currentPackageNames
    }

    companion object {
        private val LOG_TAG = RequestAssistStructureActivity::class.java.simpleName
    }
}
