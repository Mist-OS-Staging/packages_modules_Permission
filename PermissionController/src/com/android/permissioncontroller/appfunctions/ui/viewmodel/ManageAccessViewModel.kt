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

package com.android.permissioncontroller.appfunctions.ui.viewmodel

import android.app.Application
import android.app.appfunctions.AppFunctionManager
import android.app.appfunctions.AppFunctionManager.OnAppFunctionAccessChangedListener
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Process
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.android.permissioncontroller.appfunctions.data.repository.AppFunctionRepository
import com.android.permissioncontroller.appfunctions.domain.usecase.GetAccessRequestStateUseCase
import com.android.permissioncontroller.appfunctions.domain.usecase.GetAppFunctionPackageInfoUseCase
import com.android.permissioncontroller.appfunctions.domain.usecase.GetDeviceSettingsTargetIconUseCase
import com.android.permissioncontroller.appfunctions.domain.usecase.UpdateAccessUseCase
import com.android.permissioncontroller.common.model.Stateful
import com.android.permissioncontroller.data.repository.v31.PackageChangeListener
import com.android.permissioncontroller.pm.data.repository.v31.PackageRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ManageAccessViewModel(
    application: Application,
    private val agentPackageName: String,
    private val targetPackageName: String,
    private val appFunctionRepository: AppFunctionRepository,
    private val getAppFunctionPackageInfoUseCase: GetAppFunctionPackageInfoUseCase,
    private val getDeviceSettingsTargetIconUseCase: GetDeviceSettingsTargetIconUseCase,
    private val getAccessRequestStateUseCase: GetAccessRequestStateUseCase,
    private val updateAccessUseCase: UpdateAccessUseCase,
    scope: CoroutineScope? = null,
    val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : AndroidViewModel(application) {
    private val coroutineScope = scope ?: viewModelScope

    private val accessChangedListener = OnAppFunctionAccessChangedListener { refresh() }
    private val packageChangeListener = PackageChangeListener(::refresh)

    private val _uiStateFlow = MutableStateFlow<Stateful<ManageAccessUiState>>(Stateful.Loading())
    val uiStateFlow: StateFlow<Stateful<ManageAccessUiState>> = _uiStateFlow

    init {
        appFunctionRepository.addAccessChangedListener(
            ContextCompat.getMainExecutor(application.applicationContext),
            accessChangedListener,
        )
        packageChangeListener.register()
        refresh()
    }

    override fun onCleared() {
        appFunctionRepository.removeAccessChangedListener(accessChangedListener)
        packageChangeListener.unregister()
    }

    // TODO(b/432096594): refresh on app function manager change listener
    private fun refresh() {
        coroutineScope.launch(dispatcher) {
            try {
                val grantState = getAccessRequestStateUseCase(agentPackageName, targetPackageName)
                val agentPackageInfo =
                    getAppFunctionPackageInfoUseCase(agentPackageName, Process.myUserHandle())

                val targetLabel: CharSequence
                val targetIcon: Drawable?
                if (
                    targetPackageName == AppFunctionRepository.DEVICE_SETTINGS_TARGET_PACKAGE_NAME
                ) {
                    targetLabel = targetPackageName
                    targetIcon = getDeviceSettingsTargetIconUseCase(Process.myUserHandle())
                } else {
                    val targetPackageInfo =
                        getAppFunctionPackageInfoUseCase(targetPackageName, Process.myUserHandle())
                    targetLabel = targetPackageInfo.label
                    targetIcon = targetPackageInfo.icon
                }
                _uiStateFlow.value =
                    if (grantState != AppFunctionManager.ACCESS_REQUEST_STATE_UNREQUESTABLE) {
                        Stateful.Success(
                            ManageAccessUiState(
                                agentPackageInfo.label,
                                targetLabel,
                                agentPackageInfo.icon,
                                targetIcon,
                                grantState == AppFunctionManager.ACCESS_REQUEST_STATE_GRANTED,
                            )
                        )
                    } else {
                        Stateful.Failure(
                            throwable =
                                IllegalArgumentException("ACCESS_REQUEST_STATE_UNREQUESTABLE")
                        )
                    }
            } catch (e: Exception) {
                _uiStateFlow.value = Stateful.Failure(throwable = e)
            }
        }
    }

    fun updateAccessState(granted: Boolean) {
        coroutineScope.launch(dispatcher) {
            updateAccessUseCase(agentPackageName, targetPackageName, granted)
        }
    }
}

/** The data class for UI state of ManageAccess screen. */
data class ManageAccessUiState(
    val agentLabel: String,
    val targetLabel: String,
    val agentIcon: Drawable?,
    val targetIcon: Drawable?,
    val accessGranted: Boolean,
)

@RequiresApi(Build.VERSION_CODES.BAKLAVA)
class ManageAccessViewModelFactory(
    private val application: Application,
    private val agentPackageName: String,
    private val targetPackageName: String,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        val packageRepository = PackageRepository.getInstance(application)
        val appFunctionRepository = AppFunctionRepository.getInstance(application)
        val getAppFunctionPackageInfoUseCase = GetAppFunctionPackageInfoUseCase(packageRepository)
        val getDeviceSettingsTargetIconUseCase =
            GetDeviceSettingsTargetIconUseCase(packageRepository)
        val getAccessRequestStateUseCase = GetAccessRequestStateUseCase(appFunctionRepository)
        val updateAccessUseCase = UpdateAccessUseCase(appFunctionRepository)
        return ManageAccessViewModel(
            application,
            agentPackageName,
            targetPackageName,
            appFunctionRepository,
            getAppFunctionPackageInfoUseCase,
            getDeviceSettingsTargetIconUseCase,
            getAccessRequestStateUseCase,
            updateAccessUseCase,
        )
            as T
    }
}
