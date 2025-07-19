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
import android.graphics.drawable.Drawable
import android.os.Process
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.android.permissioncontroller.appfunctions.domain.model.AppFunctionPackageInfo
import com.android.permissioncontroller.appfunctions.domain.usecase.GetAppFunctionPackageInfoUseCase
import com.android.permissioncontroller.common.model.Stateful
import com.android.permissioncontroller.pm.data.repository.v31.PackageRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AgentAccessViewModel(
    private val application: Application,
    private val agentPackageName: String,
    private val getAppFunctionPackageInfoUseCase: GetAppFunctionPackageInfoUseCase,
    scope: CoroutineScope? = null,
    val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : AndroidViewModel(application) {
    private val coroutineScope = scope ?: viewModelScope
    private var agentPackageInfo: AppFunctionPackageInfo? = null

    // Backing property to avoid state updates from other classes
    private val _uiStateFlow = MutableStateFlow<Stateful<AgentAccessUiState>>(Stateful.Loading())
    val uiStateFlow: StateFlow<Stateful<AgentAccessUiState>> = _uiStateFlow

    init {
        refresh()
    }

    // TODO(b/432096594): refresh on app function manager change listener
    private fun refresh() {
        coroutineScope.launch(dispatcher) {
            try {
                _uiStateFlow.value = Stateful.Success(AgentAccessUiState(getAgentPackageInfo()))
            } catch (e: Exception) {
                _uiStateFlow.value = Stateful.Failure(throwable = e)
            }
        }
    }

    private fun getAgentPackageInfo(): AppFunctionPackageInfo =
        agentPackageInfo
            ?: getAppFunctionPackageInfoUseCase(agentPackageName, Process.myUserHandle())

    @Suppress("UNUSED_PARAMETER")
    fun updateDeviceSettingsAccessState(granted: Boolean) {
        // TODO(b/419326395): Implement
    }

    @Suppress("UNUSED_PARAMETER")
    fun updateAccessState(targetPackageName: String, granted: Boolean) {
        // TODO(b/419326395): Implement updateAccessState
    }
}

data class AgentAccessUiState(
    val agent: AppFunctionPackageInfo,
    val deviceSettings: SystemTargetItem? = null,
    val targets: List<TargetItem> = emptyList(),
)

data class SystemTargetItem(val icon: Drawable?, val accessGranted: Boolean)

data class TargetItem(val packageInfo: AppFunctionPackageInfo, val accessGranted: Boolean)

/** Factory for [AgentAccessViewModel]. */
class AgentAccessViewModelFactory(
    private val agentPackageName: String,
    private val application: Application,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        val packageRepository = PackageRepository.getInstance(application)
        val getAppFunctionPackageInfoUseCase = GetAppFunctionPackageInfoUseCase(packageRepository)
        return AgentAccessViewModel(application, agentPackageName, getAppFunctionPackageInfoUseCase)
            as T
    }
}
