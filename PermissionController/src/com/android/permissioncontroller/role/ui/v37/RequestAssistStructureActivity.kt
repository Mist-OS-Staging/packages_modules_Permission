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

import android.app.voiceinteraction.VoiceInteractionManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Process
import android.permission.flags.Flags
import android.util.Log
import androidx.fragment.app.FragmentActivity
import com.android.permissioncontroller.pm.data.repository.v31.PackageRepository

class RequestAssistStructureActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!isReadScreenContextUiEnabled(this)) {
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
        if (packageName.isNullOrEmpty()) {
            Log.e(LOG_TAG, "Unknown/Invalid role holder package: $packageName.")
            setResultAndFinish(RESULT_CANCELED)
            return
        }

        when (getReadScreenContextRequestState(packageName, this)) {
            VoiceInteractionManager.READ_SCREEN_CONTEXT_REQUEST_STATE_GRANTED -> {
                Log.w(LOG_TAG, "READ_SCREEN_CONTEXT already granted")
                setResultAndFinish(RESULT_OK)
                return
            }
            VoiceInteractionManager.READ_SCREEN_CONTEXT_REQUEST_STATE_REQUESTABLE -> {
                // Read screen context is requestable. No action needed within the 'when' block.
                // Execution will continue after this 'when' statement to initiate the request.
            }
            else -> {
                Log.w(LOG_TAG, "Read screen context not requestable for package: $packageName.")
                setResultAndFinish(RESULT_CANCELED)
                return
            }
        }

        val fragment = RequestAssistStructureFragment.Companion.newInstance(packageName)
        supportFragmentManager.beginTransaction().add(fragment, null).commit()
    }

    private fun setResultAndFinish(resultCode: Int) {
        setResult(resultCode)
        finish()
    }

    private fun isReadScreenContextUiEnabled(context: Context): Boolean {
        val packageManager = context.packageManager
        return Flags.assistSettingsPrivacyImprovementsEnabled() &&
            !packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK) &&
            !packageManager.hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE) &&
            !packageManager.hasSystemFeature(PackageManager.FEATURE_WATCH)
    }

    private fun getReadScreenContextRequestState(packageName: String, context: Context): Int {
        val uid =
            PackageRepository.createInstance(context)
                .getPackageUid(packageName, Process.myUserHandle())
        val voiceInteractionManager =
            application.getSystemService(VoiceInteractionManager::class.java)
        return voiceInteractionManager.getReadScreenContextRequestState(uid)
    }

    companion object {
        private val LOG_TAG = RequestAssistStructureActivity::class.java.simpleName
    }
}
