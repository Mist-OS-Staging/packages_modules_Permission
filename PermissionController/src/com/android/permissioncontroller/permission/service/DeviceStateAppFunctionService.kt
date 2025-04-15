/*
 * Copyright 2025 The Android Open Source Project
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

package com.android.permissioncontroller.permission.service

import android.Manifest
import android.app.appfunctions.AppFunctionException
import android.app.appfunctions.AppFunctionException.ERROR_FUNCTION_NOT_FOUND
import android.app.appfunctions.AppFunctionService
import android.app.appfunctions.ExecuteAppFunctionRequest
import android.app.appfunctions.ExecuteAppFunctionResponse
import android.app.appsearch.GenericDocument
import android.content.Context
import android.content.pm.SigningInfo
import android.content.res.Configuration
import android.os.CancellationSignal
import android.os.OutcomeReceiver
import com.android.permissioncontroller.appfunctions.AppPermissionScreen
import com.android.permissioncontroller.appfunctions.GenericDocumentToPlatformConverter
import com.android.permissioncontroller.appfunctions.PermissionAppsScreen
import com.android.permissioncontroller.appfunctions.PermissionManagerScreen
import com.android.permissioncontroller.appfunctions.deviceStateScreenKeys
import com.android.permissioncontroller.permission.data.SinglePermGroupPackagesUiInfoLiveData
import com.google.android.appfunctions.schema.common.v1.devicestate.DeviceStateResponse
import com.google.android.appfunctions.schema.common.v1.devicestate.PerScreenDeviceStates
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

// TODO b/411150350: Add CTS test for this service
class DeviceStateAppFunctionService : AppFunctionService() {
    private lateinit var englishContext: Context
    private val serviceScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        englishContext = createEnglishContext()
    }

    override fun onExecuteFunction(
        request: ExecuteAppFunctionRequest,
        callingPackage: String,
        callingPackageSigningInfo: SigningInfo,
        cancellationSignal: CancellationSignal,
        callback: OutcomeReceiver<ExecuteAppFunctionResponse, AppFunctionException>,
    ) {
        if (request.functionIdentifier != APP_FUNCTION_IDENTIFIER) {
            callback.onError(
                AppFunctionException(
                    ERROR_FUNCTION_NOT_FOUND,
                    "${request.functionIdentifier} not supported.",
                )
            )
            return
        }

        serviceScope.launch {
            val jetpackDocument =
                androidx.appsearch.app.GenericDocument.fromDocumentClass(buildDeviceStateResponse())

            val platformDocument =
                GenericDocumentToPlatformConverter.toPlatformGenericDocument(jetpackDocument)

            val resultDocument =
                GenericDocument.Builder<GenericDocument.Builder<*>>("", "", "")
                    .setPropertyDocument(
                        ExecuteAppFunctionResponse.PROPERTY_RETURN_VALUE,
                        platformDocument,
                    )
                    .build()
            val response = ExecuteAppFunctionResponse(resultDocument)
            callback.onResult(response)
        }
    }

    private fun createEnglishContext(): Context {
        val configuration = Configuration(applicationContext.resources.configuration)
        configuration.setLocale(Locale.US)
        return applicationContext.createConfigurationContext(configuration)
    }

    private suspend fun buildDeviceStateResponse(): DeviceStateResponse {
        val perScreenDeviceStatesList = deviceStateScreenKeys.map { buildPerScreenDeviceStates(it) }
        val locale = applicationContext.resources.configuration.locales[0]

        return DeviceStateResponse(
            perScreenDeviceStates = perScreenDeviceStatesList.flatten(),
            deviceLocale = locale.toString(),
        )
    }

    private suspend fun buildPerScreenDeviceStates(screenKey: String): List<PerScreenDeviceStates> {
        when (screenKey) {
            PermissionManagerScreen.KEY -> {
                return listOf(PermissionManagerScreen().toPerScreenDeviceStates())
            }
            PermissionAppsScreen.KEY -> {
                return coroutineScope {
                    SUPPORTED_PERMISSION_GROUPS.map {
                            async {
                                PermissionAppsScreen(applicationContext, it)
                                    .toPerScreenDeviceStates()
                            }
                        }
                        .awaitAll()
                }
            }
            AppPermissionScreen.KEY -> {
                return coroutineScope {
                    SUPPORTED_PERMISSION_GROUPS.map { permissionGroup ->
                            async {
                                val packagePermissionInfoMap =
                                    SinglePermGroupPackagesUiInfoLiveData[permissionGroup]
                                        .getInitializedValue(staleOk = false, forceUpdate = true)!!

                                val deviceStateScreens = mutableListOf<PerScreenDeviceStates>()
                                packagePermissionInfoMap.forEach { (packageInfo, permissionInfo) ->
                                    deviceStateScreens.add(
                                        AppPermissionScreen(
                                                context = applicationContext,
                                                permissionGroup = permissionGroup,
                                                packageName = packageInfo.first,
                                                userHandle = packageInfo.second,
                                                permissionGrantState = permissionInfo.permGrantState,
                                            )
                                            .toPerScreenDeviceStates()
                                    )
                                }
                                deviceStateScreens
                            }
                        }
                        .awaitAll()
                        .flatten()
                }
            }
        }
        throw Exception("$screenKey is not supported")
    }

    companion object {
        private const val TAG = "DeviceStateAppFunctionService"
        private const val APP_FUNCTION_IDENTIFIER = "getAppPermissionDeviceState"
        private val SUPPORTED_PERMISSION_GROUPS =
            listOf(
                Manifest.permission_group.LOCATION,
                Manifest.permission_group.CONTACTS,
                Manifest.permission_group.CALL_LOG,
            )
    }
}
