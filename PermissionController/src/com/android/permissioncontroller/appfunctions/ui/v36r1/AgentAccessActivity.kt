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
package com.android.permissioncontroller.appfunctions.ui.v36r1

import android.content.Intent
import android.os.Bundle
import android.util.Log
import com.android.permissioncontroller.appfunctions.ui.handheld.v36r1.HandheldAgentAccessFragment
import com.android.permissioncontroller.role.ui.SettingsActivity

/** Activity to manage app function agent access. */
class AgentAccessActivity : SettingsActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val agentPackageName: String? = intent.getStringExtra(Intent.EXTRA_PACKAGE_NAME)
        if (agentPackageName.isNullOrEmpty()) {
            Log.e(LOG_TAG, "Unknown package: $agentPackageName")
            finish()
            return
        }

        if (savedInstanceState == null) {
            val fragment = HandheldAgentAccessFragment.newInstance(agentPackageName)
            supportFragmentManager.beginTransaction().add(android.R.id.content, fragment).commit()
        }
    }

    companion object {
        private val LOG_TAG = AgentAccessActivity::class.java.simpleName
    }
}
