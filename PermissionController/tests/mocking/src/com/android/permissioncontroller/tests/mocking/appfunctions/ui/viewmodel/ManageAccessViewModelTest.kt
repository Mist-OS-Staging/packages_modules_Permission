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
package com.android.permissioncontroller.tests.mocking.appfunctions.ui.viewmodel

import android.app.appfunctions.AppFunctionManager.ACCESS_FLAG_USER_DENIED
import android.app.appfunctions.AppFunctionManager.ACCESS_FLAG_USER_GRANTED
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import com.android.dx.mockito.inline.extended.ExtendedMockito
import com.android.permissioncontroller.PermissionControllerApplication
import com.android.permissioncontroller.R
import com.android.permissioncontroller.appfunctions.data.repository.AppFunctionRepository
import com.android.permissioncontroller.appfunctions.domain.usecase.GetAccessRequestStateUseCase
import com.android.permissioncontroller.appfunctions.domain.usecase.GetAppFunctionPackageInfoUseCase
import com.android.permissioncontroller.appfunctions.domain.usecase.GetDeviceSettingsTargetIconUseCase
import com.android.permissioncontroller.appfunctions.domain.usecase.UpdateAccessUseCase
import com.android.permissioncontroller.appfunctions.ui.viewmodel.ManageAccessUiState
import com.android.permissioncontroller.appfunctions.ui.viewmodel.ManageAccessViewModel
import com.android.permissioncontroller.common.model.Stateful
import com.android.permissioncontroller.pm.data.repository.v31.PackageRepository
import com.android.permissioncontroller.tests.mocking.appfunctions.data.repository.FakeAppFunctionRepository
import com.android.permissioncontroller.tests.mocking.coroutines.collectLastValue
import com.android.permissioncontroller.tests.mocking.pm.data.repository.FakePackageRepository
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.Executor
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.`when` as whenever
import org.mockito.MockitoAnnotations
import org.mockito.MockitoSession
import org.mockito.quality.Strictness

// TODO(b/424004217): Update this to the correct version code
/**
 * These unit tests are for app function agent access implementation, the view model class is
 * [ManageAccessViewModel]
 */
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.BAKLAVA)
@RunWith(AndroidJUnit4::class)
class ManageAccessViewModelTest {
    @Mock private lateinit var application: PermissionControllerApplication

    private lateinit var appFunctionRepository: AppFunctionRepository
    private lateinit var packageRepository: PackageRepository

    private var mockitoSession: MockitoSession? = null

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)
        mockitoSession =
            ExtendedMockito.mockitoSession()
                .mockStatic(PermissionControllerApplication::class.java)
                .strictness(Strictness.LENIENT)
                .startMocking()
        ExtendedMockito.doReturn(application).`when` { PermissionControllerApplication.get() }

        whenever(application.applicationContext).thenReturn(application)
        whenever(application.mainExecutor).thenReturn(Mockito.mock(Executor::class.java))
        whenever(application.registerReceiverForAllUsers(any(), any(), any(), any()))
            .thenReturn(null)
        whenever(application.getString(R.string.app_function_device_settings_target_title))
            .thenReturn(TEST_DEVICE_SETTINGS_LABEL)

        appFunctionRepository =
            FakeAppFunctionRepository(
                agents = agentPackageNames,
                targets = systemTargetPackageNames + targetPackageNames,
                accessFlags = accessFlags,
            )
        packageRepository = FakePackageRepository(packagesAndLabels = packagesToLabelMap)
    }

    @After
    fun finish() {
        mockitoSession?.finishMocking()
    }

    @Test
    fun expectedAgentAndTarget() = runTest {
        val viewModel = getViewModel(TEST_AGENT_PACKAGE_NAME, TEST_TARGET_PACKAGE_NAME)
        val uiState = getManageAccessUiState(viewModel)

        assertTrue(uiState is Stateful.Success)

        // Correct Agent label returned
        assertThat(uiState.value!!.agentLabel).isEqualTo(TEST_AGENT_LABEL)
        // Correct Target label returned
        assertThat(uiState.value!!.targetLabel).isEqualTo(TEST_TARGET_LABEL)
        // Correct expected access state
        assertThat(uiState.value!!.accessGranted).isTrue()
    }

    @Test
    fun expectedAgentAndTarget_deviceSettings() = runTest {
        val viewModel = getViewModel(TEST_AGENT_PACKAGE_NAME, DEVICE_SETTINGS_TARGET_PACKAGE_NAME)
        val uiState = getManageAccessUiState(viewModel)

        assertTrue(uiState is Stateful.Success)

        // Correct Agent label returned
        assertThat(uiState.value!!.agentLabel).isEqualTo(TEST_AGENT_LABEL)
        // Correct Target label returned, for device settings it should be the package name.
        assertThat(uiState.value!!.targetLabel).isEqualTo(DEVICE_SETTINGS_TARGET_PACKAGE_NAME)
        // Correct expected access state
        assertThat(uiState.value!!.accessGranted).isFalse()
    }

    @Test
    fun updateAccessState() = runTest {
        val viewModel = getViewModel(TEST_AGENT_PACKAGE_NAME, TEST_TARGET_PACKAGE_NAME)

        viewModel.updateAccessState(false)
        assertThat(getManageAccessUiState(viewModel).value?.accessGranted ?: true).isFalse()

        viewModel.updateAccessState(true)
        assertThat(getManageAccessUiState(viewModel).value?.accessGranted ?: false).isTrue()
    }

    private fun TestScope.getViewModel(
        agentPackageName: String,
        targetPackageName: String,
    ): ManageAccessViewModel {
        return ManageAccessViewModel(
            application,
            agentPackageName,
            targetPackageName,
            appFunctionRepository,
            GetAppFunctionPackageInfoUseCase(packageRepository),
            GetDeviceSettingsTargetIconUseCase(packageRepository),
            GetAccessRequestStateUseCase(appFunctionRepository),
            UpdateAccessUseCase(appFunctionRepository),
            backgroundScope,
            StandardTestDispatcher(testScheduler),
        )
    }

    private fun TestScope.getManageAccessUiState(
        viewModel: ManageAccessViewModel
    ): Stateful<ManageAccessUiState> {
        val result by collectLastValue(viewModel.uiStateFlow)
        return result!!
    }

    companion object {
        private const val TEST_AGENT_PACKAGE_NAME = "test.agent.package"
        private const val TEST_TARGET_PACKAGE_NAME = "test.target.package"
        private const val DEVICE_SETTINGS_TARGET_PACKAGE_NAME = "android"
        private const val TEST_AGENT_LABEL = "Test Agent"
        private const val TEST_TARGET_LABEL = "Test Target"
        private const val TEST_DEVICE_SETTINGS_LABEL = "Device Settings"

        private val agentPackageNames = listOf(TEST_AGENT_PACKAGE_NAME)
        private val systemTargetPackageNames = listOf(DEVICE_SETTINGS_TARGET_PACKAGE_NAME)
        private val targetPackageNames = listOf(TEST_TARGET_PACKAGE_NAME)
        private val packagesToLabelMap =
            mapOf(
                TEST_AGENT_PACKAGE_NAME to TEST_AGENT_LABEL,
                TEST_TARGET_PACKAGE_NAME to TEST_TARGET_LABEL,
            )
        private val accessFlags =
            mapOf(
                (TEST_AGENT_PACKAGE_NAME to DEVICE_SETTINGS_TARGET_PACKAGE_NAME) to
                    ACCESS_FLAG_USER_DENIED,
                (TEST_AGENT_PACKAGE_NAME to TEST_TARGET_PACKAGE_NAME) to ACCESS_FLAG_USER_GRANTED,
            )
    }
}
