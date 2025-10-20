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

package com.android.permissioncontroller.permission.ui.v37

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.RemoteCallback
import android.permission.flags.Flags
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.commit
import com.android.permissioncontroller.permission.ui.handheld.v37.RequestLocationButtonPermissionsFragment

/**
 * This activity opens up location button consent dialog when user first time clicks the location
 * button.
 */
@RequiresApi(Build.VERSION_CODES.CINNAMON_BUN)
class RequestLocationButtonPermissionsActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!Flags.locationButtonEnabled()) {
            finish()
            return
        }
        // TODO finish early if permission isn't requested or permission is system/policy fixed
        val packageName = intent.getStringExtra(Intent.EXTRA_PACKAGE_NAME)
        val remoteCallback =
            intent.getParcelableExtra(Intent.EXTRA_REMOTE_CALLBACK, RemoteCallback::class.java)

        if (packageName == null || remoteCallback == null) {
            Log.e(LOG_TAG, "Package name or remote callback isn't provided.")
            finish()
            return
        }

        if (savedInstanceState == null) {
            val fragment =
                RequestLocationButtonPermissionsFragment.newInstance(packageName, remoteCallback)
            supportFragmentManager.commit { add(fragment, GRANT_FRAGMENT_TAG) }
        }
    }

    companion object {
        private const val LOG_TAG = "LocationButtonActivity"
        private const val GRANT_FRAGMENT_TAG = "grant_location_permission_fragment"
    }
}
