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
import com.android.permissioncontroller.appfunctions.domain.usecase.v31.GetAgentUsageUseCase
import com.android.permissioncontroller.appinteraction.data.repository.AppInteractionRepository
import com.android.permissioncontroller.appinteraction.domain.model.v31.AccessCount
import java.time.Instant
import java.util.concurrent.TimeUnit
import kotlin.math.max

/**
 * The use case gets the agent usages and returns a map of agent names to their [AccessCount]'s. The
 * AccessCount contains the number of agent access to their target apps in the past 24 hours and 7
 * days.
 *
 * @param appFunctionRepository The repository to use to get the app function agents.
 */
class GetAgentUsageUseCaseImpl(private val appInteractionRepository: AppInteractionRepository) :
    GetAgentUsageUseCase {
    override suspend operator fun invoke(context: Context): Map<String, AccessCount> {
        if (!AppFunctionsUtil.isPrivacyDashboardAgentActivityEnabled(context)) {
            return emptyMap()
        }

        val profiles = context.getSystemService(UserManager::class.java).userProfiles
        val accessHistories =
            profiles.flatMap { appInteractionRepository.getAccessHistory(context, it) }
        val deviceAssistancePackageNames =
            appInteractionRepository.getDeviceAssistancePackageNames(context).toSet()
        val now = System.currentTimeMillis()
        val timeStamp24Hours = max(now - TimeUnit.DAYS.toMillis(1), Instant.EPOCH.toEpochMilli())
        val timeStamp7Days = max(now - TimeUnit.DAYS.toMillis(7), Instant.EPOCH.toEpochMilli())
        return accessHistories
            .groupBy { it.agentPackageName }
            .mapValues { (_, accessHistories) ->
                val distinctAccessCount24Hours = mutableSetOf<String>()
                val distinctAccessCount7Days = mutableSetOf<String>()
                for (accessHistory in accessHistories) {
                    val targetPackageName =
                        if (accessHistory.targetPackageName in deviceAssistancePackageNames) {
                            DEVICE_ASSISTANCE_TARGET_PACKAGE_NAME
                        } else {
                            accessHistory.targetPackageName
                        }

                    if (accessHistory.accessTime > timeStamp24Hours) {
                        distinctAccessCount24Hours.add(targetPackageName)
                    }
                    if (accessHistory.accessTime > timeStamp7Days) {
                        distinctAccessCount7Days.add(targetPackageName)
                    }
                }
                AccessCount(distinctAccessCount24Hours.size, distinctAccessCount7Days.size)
            }
    }

    companion object {
        private const val DEVICE_ASSISTANCE_TARGET_PACKAGE_NAME = "DEVICE_ASSISTANCE"
    }
}
