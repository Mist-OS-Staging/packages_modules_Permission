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

import android.app.AppOpsManager
import android.app.AppOpsManager.MODE_ALLOWED
import android.app.Application
import android.os.Build
import android.os.Process
import androidx.annotation.RequiresApi
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.android.permissioncontroller.appops.data.repository.v31.AppOpRepository
import com.android.permissioncontroller.common.model.Stateful
import com.android.permissioncontroller.permission.data.repository.v31.PermissionRepository
import com.android.permissioncontroller.pm.data.repository.v31.PackageRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** Viewmodel to support assist structure request screen */
class RequestAssistStructureViewModel(
    application: Application,
    private val packageName: String,
    private val packageUid: Int,
    private val appOpRepository: AppOpRepository,
    scope: CoroutineScope? = null,
    val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : AndroidViewModel(application) {
    private val coroutineScope = scope ?: viewModelScope

    @VisibleForTesting
    private val _uiStateFlow = MutableStateFlow<Stateful<RequestAssistState>>(Stateful.Loading())
    val uiStateFlow: StateFlow<Stateful<RequestAssistState>> = _uiStateFlow

    init {
        coroutineScope.launch(dispatcher) {
            try {
                // TODO: finish if already enabled or request denied multiple times
                val assistStructureEnabled =
                    appOpRepository.checkOpNoThrow(
                        AppOpsManager.OPSTR_ASSIST_STRUCTURE,
                        packageUid,
                        packageName,
                    )
                val state =
                    if (assistStructureEnabled == MODE_ALLOWED) {
                        RequestAssistState.ALLOWED
                    } else {
                        RequestAssistState.REQUESTABLE
                    }
                _uiStateFlow.value = Stateful.Success(state)
            } catch (e: Exception) {
                _uiStateFlow.value = Stateful.Failure(throwable = e)
            }
        }
    }

    fun setAllowed() {
        // TODO: Clear previous record of denials
        coroutineScope.launch(dispatcher) {
            appOpRepository.setUidMode(
                AppOpsManager.OPSTR_ASSIST_STRUCTURE,
                packageUid,
                MODE_ALLOWED,
            )
        }
    }

    fun markRequestDenied() {
        // TODO: mark request denied
    }
}

enum class RequestAssistState {
    ALLOWED,
    REQUESTABLE,
    UNREQUESTABLE,
}

@RequiresApi(Build.VERSION_CODES.CINNAMON_BUN)
class RequestAssistStructureViewModelFactory(
    private val application: Application,
    private val packageName: String,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        val permissionRepository = PermissionRepository.getInstance(application)
        val appOpsRepository = AppOpRepository.getInstance(application, permissionRepository)
        val packageRepository = PackageRepository.getInstance(application)
        return RequestAssistStructureViewModel(
            application,
            packageName,
            packageRepository.getPackageUid(packageName, Process.myUserHandle()),
            appOpsRepository,
        )
            as T
    }
}
