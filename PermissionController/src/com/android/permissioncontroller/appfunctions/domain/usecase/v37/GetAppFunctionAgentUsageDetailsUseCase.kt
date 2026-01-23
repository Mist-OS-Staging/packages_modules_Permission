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

package com.android.permissioncontroller.appfunctions.domain.usecase.v37

import android.content.Context
import android.os.UserManager
import com.android.permissioncontroller.appfunctions.AppFunctionsUtil
import com.android.permissioncontroller.appinteraction.data.repository.AppInteractionRepository
import com.android.permissioncontroller.appinteraction.domain.model.v37.AccessHistory
import java.time.Instant
import java.util.concurrent.TimeUnit
import kotlin.math.max

/**
 * The use case gets the agent usage details and returns the last [AccessHistory] in a map of its
 * past 24 hours usages and past 7 days usages for each target package.
 *
 * @param appInteractionRepository The repository to use to get the agent usages
 */
class GetAppFunctionAgentUsageDetailsUseCase(
    private val appInteractionRepository: AppInteractionRepository
) {
    suspend operator fun invoke(
        context: Context,
        agentPackageName: String,
    ): Map<String, List<AccessHistory>> {
        if (!AppFunctionsUtil.isPrivacyDashboardAgentActivityEnabled(context)) {
            return emptyMap()
        }

        val profiles = context.getSystemService(UserManager::class.java).userProfiles
        val accessHistories =
            profiles.flatMap { appInteractionRepository.getAccessHistory(context, it) }
        val now = System.currentTimeMillis()
        val timeStamp24Hours = max(now - TimeUnit.DAYS.toMillis(1), Instant.EPOCH.toEpochMilli())
        val timeStamp7Days = max(now - TimeUnit.DAYS.toMillis(7), Instant.EPOCH.toEpochMilli())
        return accessHistories
            .filter { it.agentPackageName == agentPackageName }
            .sortedBy { it.accessTime }
            .fold(
                mapOf(
                    KEY_PAST_24_HOURS to mutableMapOf<String, AccessHistory>(),
                    KEY_PAST_7_DAYS to mutableMapOf<String, AccessHistory>(),
                )
            ) { map, accessHistory ->
                val targetPackageName = accessHistory.targetPackageName
                if (accessHistory.accessTime > timeStamp24Hours) {
                    map[KEY_PAST_24_HOURS]!![targetPackageName] = accessHistory
                }
                if (accessHistory.accessTime > timeStamp7Days) {
                    map[KEY_PAST_7_DAYS]!![targetPackageName] = accessHistory
                }
                map
            }
            .mapValues { it.value.values.toList() }
    }

    companion object {
        const val KEY_PAST_24_HOURS = "PAST_24_HOURS"
        const val KEY_PAST_7_DAYS = "PAST_7_DAYS"
    }
}
