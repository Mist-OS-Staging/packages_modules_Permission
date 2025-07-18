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

import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import com.android.dx.mockito.inline.extended.ExtendedMockito
import com.android.permissioncontroller.PermissionControllerApplication
import com.android.permissioncontroller.appfunctions.domain.model.AppFunctionPackageInfo
import com.android.permissioncontroller.appfunctions.domain.usecase.GetAppFunctionPackageInfoUseCase
import com.android.permissioncontroller.appfunctions.ui.viewmodel.AgentAccessUiState
import com.android.permissioncontroller.appfunctions.ui.viewmodel.AgentAccessViewModel
import com.android.permissioncontroller.common.model.Stateful
import com.android.permissioncontroller.pm.data.repository.v31.PackageRepository
import com.android.permissioncontroller.tests.mocking.coroutines.collectLastValue
import com.android.permissioncontroller.tests.mocking.pm.data.repository.FakePackageRepository
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.MockitoSession
import org.mockito.quality.Strictness

// TODO(b/424004217): Update this to the correct version code
/**
 * These unit tests are for app function agent access implementation, the view model class is
 * [AgentAccessViewModel]
 */
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.BAKLAVA)
@RunWith(AndroidJUnit4::class)
class AgentAccessViewModelTest {
    @Mock private lateinit var application: PermissionControllerApplication
    private var mockitoSession: MockitoSession? = null

    private lateinit var getAppFunctionPackageInfoUseCase: GetAppFunctionPackageInfoUseCase
    private lateinit var packageRepository: PackageRepository

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)
        mockitoSession =
            ExtendedMockito.mockitoSession()
                .mockStatic(PermissionControllerApplication::class.java)
                .strictness(Strictness.LENIENT)
                .startMocking()

        packageRepository = FakePackageRepository(packagesAndLabels = packagesToLabelMap)
        getAppFunctionPackageInfoUseCase = GetAppFunctionPackageInfoUseCase(packageRepository)
    }

    @After
    fun finish() {
        mockitoSession?.finishMocking()
    }

    @Test
    fun validAgentShown_emptySystemTargets_emptyAppTargets() = runTest {
        val testAgent = TEST_AGENT_PACKAGE_NAME

        val viewModel = getViewModel(testAgent)
        val uiState = getAgentAccessUiState(viewModel)

        assertTrue(uiState is Stateful.Success)
        // Correct Agent AppFunctionPackageInfo returned
        assertThat(uiState.value!!.agent)
            .isEqualTo(AppFunctionPackageInfo(testAgent, packagesToLabelMap[testAgent]!!, null))

        // Correct Device Settings Target returned
        assertThat(uiState.value!!.deviceSettings).isNull()

        // Correct Targets returned
        assertThat(uiState.value!!.targets).isEmpty()
    }

    private fun TestScope.getViewModel(agentPackageName: String): AgentAccessViewModel {
        return AgentAccessViewModel(
            application,
            agentPackageName,
            getAppFunctionPackageInfoUseCase,
            backgroundScope,
            StandardTestDispatcher(testScheduler),
        )
    }

    private fun TestScope.getAgentAccessUiState(
        viewModel: AgentAccessViewModel
    ): Stateful<AgentAccessUiState> {
        val result by collectLastValue(viewModel.uiStateFlow)
        return result!!
    }

    companion object {
        private const val TEST_AGENT_PACKAGE_NAME = "test.agent.package"
        private const val TEST_AGENT_PACKAGE_NAME2 = "test.agent.package2"
        private const val TEST_AGENT_LABEL = "Test Agent"
        private const val TEST_AGENT_LABEL2 = "Test Agent 2"
        private val packagesToLabelMap =
            mapOf(
                TEST_AGENT_PACKAGE_NAME to TEST_AGENT_LABEL,
                TEST_AGENT_PACKAGE_NAME2 to TEST_AGENT_LABEL2,
            )
    }
}
