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
import android.os.UserHandle
import android.platform.test.annotations.RequiresFlagsDisabled
import android.platform.test.annotations.RequiresFlagsEnabled
import android.platform.test.flag.junit.CheckFlagsRule
import android.platform.test.flag.junit.DeviceFlagsValueProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.android.permissioncontroller.appfunctions.domain.usecase.v37.GetAgentUsageDetailsUseCase
import com.android.permissioncontroller.appfunctions.domain.usecase.v37.GetAgentUsageDetailsUseCase.Companion.KEY_PAST_24_HOURS
import com.android.permissioncontroller.appfunctions.domain.usecase.v37.GetAgentUsageDetailsUseCase.Companion.KEY_PAST_7_DAYS
import com.android.permissioncontroller.appinteraction.domain.model.v37.AccessHistory
import com.android.permissioncontroller.appinteraction.domain.model.v37.AgentTimelineItem
import com.android.permissioncontroller.flags.Flags
import com.android.permissioncontroller.tests.mocking.appinteraction.data.repository.FakeAppInteractionRepository
import com.android.permissioncontroller.tests.mocking.pm.data.repository.FakePackageRepository
import com.android.permissioncontroller.tests.mocking.pm.data.repository.FakePackageRepository.Companion.TEST_UID
import com.google.common.truth.Truth.assertWithMessage
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.test.runTest
import org.junit.Assume.assumeFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when` as whenever
import org.mockito.MockitoAnnotations

/** Unit tests for [GetAgentUsageDetailsUseCase]. */
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.BAKLAVA)
class GetAgentUsageDetailsUseCaseTest {
    @get:Rule val checkFlagsRule: CheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule()
    @Mock private lateinit var mockContext: Context
    @Mock private lateinit var packageManager: PackageManager
    @Mock private lateinit var userHandle: UserHandle

    private lateinit var useCase: GetAgentUsageDetailsUseCase
    private val instrumentation = InstrumentationRegistry.getInstrumentation()!!
    private val instrumentationContext = instrumentation.targetContext!!

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        whenever(mockContext.packageManager).thenReturn(packageManager)
        whenever(packageManager.hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE))
            .thenReturn(false)
        whenever(packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)).thenReturn(false)
        whenever(packageManager.hasSystemFeature(PackageManager.FEATURE_WATCH)).thenReturn(false)
    }

    @Test
    @RequiresFlagsEnabled(
        Flags.FLAG_PRIVACY_DASHBOARD_AGENT_ACTIVITY_ENABLED,
        FLAG_ENABLE_APP_INTERACTION_API,
    )
    fun getAgentUsages_success() = runTest {
        assumeFalse(isAutomotive() || isTv() || isWatch())
        val now = System.currentTimeMillis()
        val accessHistory =
            listOf(
                // Agent 1: 2 accesses in last 24 hours, 1 in last 7 days
                createAccessHistory(
                    agentPackageName = AGENT_NAME_1,
                    targetPackageName = TARGET_NAME_1,
                    interactionUri = INTERACTION_URI_1,
                    accessTime = now - TimeUnit.HOURS.toMillis(1),
                ),
                createAccessHistory(
                    agentPackageName = AGENT_NAME_1,
                    targetPackageName = TARGET_NAME_2,
                    interactionUri = INTERACTION_URI_2,
                    accessTime = now - TimeUnit.HOURS.toMillis(2),
                ),
                createAccessHistory(
                    agentPackageName = AGENT_NAME_1,
                    targetPackageName = TARGET_NAME_3,
                    interactionUri = INTERACTION_URI_3,
                    accessTime = now - TimeUnit.DAYS.toMillis(3),
                ),
                // Agent 2: 1 access in last 24 hours, 0 in last 7 days
                createAccessHistory(
                    agentPackageName = AGENT_NAME_2,
                    targetPackageName = TARGET_NAME_1,
                    interactionUri = INTERACTION_URI_1,
                    accessTime = now - TimeUnit.HOURS.toMillis(2),
                ),
            )
        val appInteractionRepository = FakeAppInteractionRepository(accessHistory)
        val packageRepository = FakePackageRepository()
        useCase = GetAgentUsageDetailsUseCase(appInteractionRepository, packageRepository)

        val result = useCase(mockContext, AGENT_NAME_1, userHandle)
        assertWithMessage("Agent 1 should have 2 accesses in the past 24 hours")
            .that(result[KEY_PAST_24_HOURS]!!.size)
            .isEqualTo(2)
        assertWithMessage("Agent 1 should have 2 accesses in the past 7 days")
            .that(result[KEY_PAST_7_DAYS]!!.size)
            .isEqualTo(3)
        result[KEY_PAST_7_DAYS]!!.forEach { accessHistory ->
            assertWithMessage(
                    "The result should only contain access history for agent 1, but" +
                        " instead found: $accessHistory"
                )
                .that(accessHistory.agentPackageName)
                .isEqualTo(AGENT_NAME_1)
        }
    }

    @Test
    @RequiresFlagsEnabled(
        Flags.FLAG_PRIVACY_DASHBOARD_AGENT_ACTIVITY_ENABLED,
        FLAG_ENABLE_APP_INTERACTION_API,
    )
    fun getAgentUsages_showsOnlyLastAccess() = runTest {
        assumeFalse(isAutomotive() || isTv() || isWatch())
        val now = System.currentTimeMillis()
        val accessHistory =
            listOf(
                createAccessHistory(
                    agentPackageName = AGENT_NAME_1,
                    targetPackageName = TARGET_NAME_1,
                    interactionUri = INTERACTION_URI_1,
                    accessTime = now - TimeUnit.HOURS.toMillis(1),
                ),
                createAccessHistory(
                    agentPackageName = AGENT_NAME_1,
                    targetPackageName = TARGET_NAME_1,
                    interactionUri = INTERACTION_URI_1,
                    accessTime = now - TimeUnit.HOURS.toMillis(2),
                ),
            )
        val appInteractionRepository = FakeAppInteractionRepository(accessHistory)
        val packageRepository = FakePackageRepository()
        useCase = GetAgentUsageDetailsUseCase(appInteractionRepository, packageRepository)

        val result = useCase(mockContext, AGENT_NAME_1, userHandle)
        assertWithMessage("There should only be 1 access history in the past 24 hours")
            .that(result[KEY_PAST_24_HOURS]!!.size)
            .isEqualTo(1)
        assertWithMessage("Only the last access should be returned")
            .that(result[KEY_PAST_24_HOURS]!!)
            .containsExactly(
                AgentTimelineItem(
                    TEST_UID,
                    AGENT_NAME_1,
                    TARGET_NAME_1,
                    userHandle,
                    now - TimeUnit.HOURS.toMillis(1),
                    INTERACTION_URI_1,
                    false,
                )
            )
    }

    @Test
    @RequiresFlagsEnabled(
        Flags.FLAG_PRIVACY_DASHBOARD_AGENT_ACTIVITY_ENABLED,
        FLAG_ENABLE_APP_INTERACTION_API,
    )
    fun getAgentUsages_deviceAssistanceAccessesAreGrouped() = runTest {
        assumeFalse(isAutomotive() || isTv() || isWatch())
        val now = System.currentTimeMillis()
        val accessHistory =
            listOf(
                // Agent 1: 1 normal app function access, 2 device assistance accesses in last 24
                // hours
                createAccessHistory(
                    agentPackageName = AGENT_NAME_1,
                    targetPackageName = TARGET_NAME_1,
                    interactionUri = INTERACTION_URI_1,
                    accessTime = now - TimeUnit.HOURS.toMillis(1),
                ),
                createAccessHistory(
                    agentPackageName = AGENT_NAME_1,
                    targetPackageName = TARGET_NAME_2,
                    interactionUri = INTERACTION_URI_2,
                    accessTime = now - TimeUnit.HOURS.toMillis(2),
                ),
                createAccessHistory(
                    agentPackageName = AGENT_NAME_1,
                    targetPackageName = TARGET_NAME_3,
                    interactionUri = INTERACTION_URI_3,
                    accessTime = now - TimeUnit.HOURS.toMillis(3),
                ),
            )
        val deviceAssistancePackageNames = listOf(TARGET_NAME_2, TARGET_NAME_3)
        val appInteractionRepository =
            FakeAppInteractionRepository(accessHistory, deviceAssistancePackageNames)
        val packageRepository = FakePackageRepository()
        useCase = GetAgentUsageDetailsUseCase(appInteractionRepository, packageRepository)

        val result = useCase(mockContext, AGENT_NAME_1, userHandle)
        assertWithMessage("Agent 1 should have 2 accesses in the past 24 hours")
            .that(result[KEY_PAST_24_HOURS]!!.size)
            .isEqualTo(2)
        assertWithMessage("Agent 1 should have 2 accesses in the past 7 days")
            .that(result[KEY_PAST_7_DAYS]!!.size)
            .isEqualTo(2)
        result[KEY_PAST_7_DAYS]!!.forEach { accessHistory ->
            assertWithMessage(
                    "target3 is a device assistance target app and should be grouped with the other" +
                        " device assistance target app target2, but instead found: $accessHistory"
                )
                .that(accessHistory.targetPackageName)
                .isNotEqualTo(TARGET_NAME_3)
        }
    }

    @Test
    @RequiresFlagsDisabled(Flags.FLAG_PRIVACY_DASHBOARD_AGENT_ACTIVITY_ENABLED)
    fun getAgentUsages_flagOff_emptyResult() = runTest {
        assumeFalse(isAutomotive() || isTv() || isWatch())
        val now = System.currentTimeMillis()
        val accessHistory =
            listOf(
                createAccessHistory(
                    agentPackageName = AGENT_NAME_1,
                    targetPackageName = TARGET_NAME_1,
                    interactionUri = INTERACTION_URI_1,
                    accessTime = now - TimeUnit.HOURS.toMillis(1),
                )
            )
        val appInteractionRepository = FakeAppInteractionRepository(accessHistory)
        val packageRepository = FakePackageRepository()
        useCase = GetAgentUsageDetailsUseCase(appInteractionRepository, packageRepository)

        val result = useCase(mockContext, AGENT_NAME_1, userHandle)
        assertWithMessage("Result should be empty when the feature flag is off")
            .that(result)
            .isEmpty()
    }

    private fun createAccessHistory(
        agentPackageName: String,
        targetPackageName: String,
        interactionUri: String?,
        accessTime: Long,
    ) = AccessHistory(agentPackageName, targetPackageName, null, null, interactionUri, accessTime)

    private fun isTv(): Boolean =
        instrumentationContext.packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)

    private fun isAutomotive(): Boolean =
        instrumentationContext.packageManager.hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE)

    private fun isWatch(): Boolean =
        instrumentationContext.packageManager.hasSystemFeature(PackageManager.FEATURE_WATCH)

    companion object {
        const val AGENT_NAME_1 = "agent1"
        const val AGENT_NAME_2 = "agent2"
        const val AGENT_NAME_3 = "agent3"
        const val TARGET_NAME_1 = "target1"
        const val TARGET_NAME_2 = "target2"
        const val TARGET_NAME_3 = "target3"
        const val INTERACTION_URI_1 = "uri1"
        const val INTERACTION_URI_2 = "uri2"
        const val INTERACTION_URI_3 = "uri3"

        const val FLAG_ENABLE_APP_INTERACTION_API =
            "com.android.permissioncontroller.jarjar.${android.app.appfunctions.flags.Flags.FLAG_ENABLE_APP_INTERACTION_API}"
    }
}
