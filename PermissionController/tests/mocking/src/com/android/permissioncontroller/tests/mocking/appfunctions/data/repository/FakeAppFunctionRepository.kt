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

package com.android.permissioncontroller.tests.mocking.appfunctions.data.repository

import com.android.permissioncontroller.appfunctions.data.repository.v36r1.AppFunctionRepository

/** Fake implementation of [AppFunctionRepository] for testing. */
class FakeAppFunctionRepository(
    private val agents: List<String> = emptyList(),
    private val targets: List<String> = emptyList(),
    private val accessFlags: Map<Pair<String, String>, Int> = emptyMap(),
) : AppFunctionRepository {
    override suspend fun getValidAgents(): List<String> = agents

    override suspend fun getValidTargets(): List<String> = targets

    // ACCESS_REQUEST_STATE_GRANTED is 0, so returning default value 2 which is
    // ACCESS_REQUEST_STATE_UNREQUESTABLE
    override suspend fun getAppFunctionAccessRequestState(
        agentPackageName: String,
        targetPackageName: String,
    ): Int = accessFlags.getOrDefault(agentPackageName to targetPackageName, 2)

    override suspend fun getAppFunctionAccessFlags(
        agentPackageName: String,
        targetPackageName: String,
    ): Int = accessFlags.getOrDefault(agentPackageName to targetPackageName, 0)
}
