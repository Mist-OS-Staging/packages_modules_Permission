/*
 * Copyright (C) 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may not use this file except in compliance with the License.
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
package com.android.permissioncontroller.permission.ui.model.v37

import android.Manifest
import android.app.Application
import android.app.permissionui.LocationButtonClient
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Process
import android.os.RemoteCallback
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.android.permissioncontroller.permission.model.AppPermissionGroup
import com.android.permissioncontroller.permission.utils.KotlinUtils
import com.android.permissioncontroller.permission.utils.Utils
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** View model for granting location permissions when user interacts with a location button. */
class LocationButtonViewModel(
    val app: Application,
    private val packageName: String,
    private val remoteCallback: RemoteCallback,
    scope: CoroutineScope? = null,
    val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : AndroidViewModel(app) {
    private val coroutineScope = scope ?: viewModelScope
    private val _appLabel = MutableLiveData<String>()
    private val _locationPinIcon = MutableLiveData<Drawable>()

    // TODO load icon and app label synchronously, remove livedata objects.
    fun getAppLabelLiveData(): LiveData<String> {
        if (!_appLabel.isInitialized) {
            coroutineScope.launch(dispatcher) {
                _appLabel.postValue(
                    KotlinUtils.getPackageLabel(
                        getApplication(),
                        packageName,
                        Process.myUserHandle(),
                    )
                )
            }
        }
        return _appLabel
    }

    fun onAllow() {
        val packageInfo = getPackageInfo() ?: return
        val group =
            AppPermissionGroup.create(
                getApplication(),
                packageInfo,
                Manifest.permission.ACCESS_FINE_LOCATION,
                true,
            )
        if (group != null) {
            // TODO Grant COARSE only when it is not granted already.
            group.grantRuntimePermissions(
                true,
                false,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                ),
            )
            // TODO set GRANTED_BY_LOCATION_BUTTON permission flag.
            group.setOneTime(true)
            group.persistChanges(
                false,
                null,
                setOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                ),
            )
        }
        val bundle =
            Bundle().apply { putBoolean(LocationButtonClient.EXTRA_PERMISSION_RESULT, true) }
        remoteCallback.sendResult(bundle)
    }

    fun onDontAllow() {
        Log.d(LOG_TAG, "Don't allow button clicked.")
    }

    fun getLocationPinIconLiveData(): LiveData<Drawable> {
        if (!_locationPinIcon.isInitialized) {
            coroutineScope.launch(dispatcher) {
                val groupInfo = Utils.getGroupInfo(Manifest.permission_group.LOCATION, app)!!
                _locationPinIcon.postValue(groupInfo.loadIcon(app.packageManager))
            }
        }
        return _locationPinIcon
    }

    private fun getPackageInfo(): PackageInfo? =
        try {
            app.packageManager.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS)
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e(LOG_TAG, "Package not found", e)
            null
        }

    companion object {
        private const val LOG_TAG = "LocationButtonViewModel"
    }
}

/**
 * Factory for a [LocationButtonViewModel]
 *
 * @param packageName The name of the package this viewModel is representing
 * @param remoteCallback The callback to send grant result
 */
class LocationButtonViewModelFactory(
    private val application: Application,
    private val packageName: String,
    private val remoteCallback: RemoteCallback,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return LocationButtonViewModel(application, packageName, remoteCallback) as T
    }
}
