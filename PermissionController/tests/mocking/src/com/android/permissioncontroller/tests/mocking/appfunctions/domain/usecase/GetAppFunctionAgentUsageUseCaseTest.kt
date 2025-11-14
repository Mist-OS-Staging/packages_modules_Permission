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
package com.android.permissioncontroller.tests.mocking.appfunctions.domain.usecase

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.platform.test.annotations.RequiresFlagsDisabled
import android.platform.test.annotations.RequiresFlagsEnabled
import android.platform.test.flag.junit.CheckFlagsRule
import android.platform.test.flag.junit.DeviceFlagsValueProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import com.android.permissioncontroller.appfunctions.domain.model.v31.AccessCount
import com.android.permissioncontroller.appfunctions.domain.model.v37.AccessHistory
import com.android.permissioncontroller.appfunctions.domain.usecase.v31.GetAppFunctionAgentUsageUseCase
import com.android.permissioncontroller.appfunctions.domain.usecase.v37.GetAppFunctionAgentUsageUseCaseImpl
import com.android.permissioncontroller.flags.Flags
import com.android.permissioncontroller.tests.mocking.appfunctions.data.repository.FakeAppFunctionRepository
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when` as whenever
import org.mockito.MockitoAnnotations

/** Unit tests for [GetAppFunctionAgentUsageUseCaseImpl]. */
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.BAKLAVA)
class GetAppFunctionAgentUsageUseCaseTest {
    @get:Rule val checkFlagsRule: CheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule()
    @Mock private lateinit var mockContext: Context
    @Mock private lateinit var packageManager: PackageManager

    private lateinit var useCase: GetAppFunctionAgentUsageUseCase

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        whenever(mockContext.packageManager).thenReturn(packageManager)
        whenever(packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)).thenReturn(false)
        whenever(packageManager.hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE))
            .thenReturn(false)
        whenever(packageManager.hasSystemFeature(PackageManager.FEATURE_WATCH)).thenReturn(false)
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_PRIVACY_DASHBOARD_AGENT_ACTIVITY_ENABLED)
    fun getAgentUsages_success() = runTest {
        val now = System.currentTimeMillis()
        val accessHistory =
            listOf(
                // Agent 1: 1 access in last 24 hours, 2 in last 7 days
                createAccessHistory(
                    agentPackageName = AGENT_NAME_1,
                    targetPackageName = TARGET_NAME_1,
                    accessTime = now - TimeUnit.HOURS.toMillis(1),
                ),
                createAccessHistory(
                    agentPackageName = AGENT_NAME_1,
                    targetPackageName = TARGET_NAME_2,
                    accessTime = now - TimeUnit.DAYS.toMillis(3),
                ),
                // Agent 2: 0 accesses in last 24 hours, 1 in last 7 days
                createAccessHistory(
                    agentPackageName = AGENT_NAME_2,
                    targetPackageName = TARGET_NAME_1,
                    accessTime = now - TimeUnit.DAYS.toMillis(3),
                ),
                // Agent 3: 0 accesses in last 24 hours, 0 in last 7 days
                createAccessHistory(
                    agentPackageName = AGENT_NAME_3,
                    targetPackageName = TARGET_NAME_1,
                    accessTime = now - TimeUnit.DAYS.toMillis(8),
                ),
            )
        val repository = FakeAppFunctionRepository(accessHistory = accessHistory)
        useCase = GetAppFunctionAgentUsageUseCaseImpl(repository)

        val result = useCase(mockContext)
        assertThat(result).hasSize(3)
        assertThat(result[AGENT_NAME_1]).isEqualTo(AccessCount(1, 2))
        assertThat(result[AGENT_NAME_2]).isEqualTo(AccessCount(0, 1))
        assertThat(result[AGENT_NAME_3]).isEqualTo(AccessCount(0, 0))
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_PRIVACY_DASHBOARD_AGENT_ACTIVITY_ENABLED)
    fun getAgentUsages_resultIsDistinct() = runTest {
        val now = System.currentTimeMillis()
        val accessHistory =
            listOf(
                // Agent 1: 2 accesses in last 24 hours, 2 in last 7 days
                createAccessHistory(
                    agentPackageName = AGENT_NAME_1,
                    targetPackageName = TARGET_NAME_1,
                    accessTime = now - TimeUnit.HOURS.toMillis(1),
                ),
                createAccessHistory(
                    agentPackageName = AGENT_NAME_1,
                    targetPackageName = TARGET_NAME_2,
                    accessTime = now - TimeUnit.HOURS.toMillis(2),
                ),
                createAccessHistory(
                    agentPackageName = AGENT_NAME_1,
                    targetPackageName = TARGET_NAME_2,
                    accessTime = now - TimeUnit.HOURS.toMillis(3),
                ),
            )
        val repository = FakeAppFunctionRepository(accessHistory = accessHistory)
        useCase = GetAppFunctionAgentUsageUseCaseImpl(repository)

        val result = useCase(mockContext)
        assertThat(result).hasSize(1)
        assertThat(result[AGENT_NAME_1]).isEqualTo(AccessCount(2, 2))
    }

    @Test
    @RequiresFlagsDisabled(Flags.FLAG_PRIVACY_DASHBOARD_AGENT_ACTIVITY_ENABLED)
    fun getAgentUsages_flagOff_emptyResult() = runTest {
        val now = System.currentTimeMillis()
        val accessHistory =
            listOf(
                // Agent 1: 1 accesses in last 24 hours, 1 in last 7 days
                createAccessHistory(
                    agentPackageName = AGENT_NAME_1,
                    targetPackageName = TARGET_NAME_1,
                    accessTime = now - TimeUnit.HOURS.toMillis(1),
                )
            )
        val repository = FakeAppFunctionRepository(accessHistory = accessHistory)
        useCase = GetAppFunctionAgentUsageUseCaseImpl(repository)

        val result = useCase(mockContext)
        assertThat(result).hasSize(0)
    }

    private fun createAccessHistory(
        agentPackageName: String,
        targetPackageName: String,
        accessTime: Long,
    ) =
        AccessHistory(
            agentPackageName,
            targetPackageName,
            null,
            null,
            null,
            accessTime,
            ACCESS_DURATION,
        )

    companion object {
        const val AGENT_NAME_1 = "agent1"
        const val AGENT_NAME_2 = "agent2"
        const val AGENT_NAME_3 = "agent3"
        const val TARGET_NAME_1 = "target1"
        const val TARGET_NAME_2 = "target2"
        const val ACCESS_DURATION = 1000L
    }
}
