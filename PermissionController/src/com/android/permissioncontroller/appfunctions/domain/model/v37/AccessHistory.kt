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

package com.android.permissioncontroller.appfunctions.domain.model.v37

/**
 * Model for App Function access history
 *
 * @param agentPackageName The package name of the agent app
 * @param targetPackageName The package name of the target app
 * @param interactionType The type of interaction that triggered the function call
 * @param customInteractionType The custom interaction type
 * @param interactionUri A URI linking to the original interaction context
 * @param threadId An identifier to group related function calls
 * @param accessTime The timestamp (in milliseconds) when the app function was accessed
 * @param duration The duration (in milliseconds) of the app function execution
 */
data class AccessHistory(
    val agentPackageName: String,
    val targetPackageName: String,
    val interactionType: Int?,
    val customInteractionType: String?,
    val interactionUri: String?,
    val threadId: String?,
    val accessTime: Long,
    val duration: Long,
)
