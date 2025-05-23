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

package com.android.permissioncontroller.appfunctions

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.net.Uri
import android.os.UserHandle
import android.provider.Settings
import com.android.permissioncontroller.R
import com.android.permissioncontroller.permission.model.livedatatypes.AppPermGroupUiInfo.PermGrantState
import com.android.permissioncontroller.permission.ui.model.UnusedAppsViewModel.UnusedPeriod
import com.android.permissioncontroller.permission.utils.KotlinUtils.getPermGroupLabel
import com.android.permissioncontroller.permission.utils.Utils
import com.google.android.appfunctions.schema.common.v1.devicestate.DeviceStateItem
import com.google.android.appfunctions.schema.common.v1.devicestate.LocalizedString
import com.google.android.appfunctions.schema.common.v1.devicestate.PerScreenDeviceStates

abstract class PerScreenDeviceState {
    /** A unique key that identifies a screen. Used only internally */
    abstract val key: String

    /** Description of the screen. */
    abstract val description: String

    /** Path to the screen from Settings page, represented by a list of each page along the way */
    abstract val paths: List<String>

    /** Intent Uri of the screen */
    abstract val intentUri: String

    open fun getDeviceStateItems(): List<DeviceStateItem> {
        return emptyList()
    }

    fun toPerScreenDeviceStates(): PerScreenDeviceStates {
        val localizedPaths = paths.map { LocalizedString(english = it) }

        return PerScreenDeviceStates(
            description = description,
            paths = localizedPaths,
            intentUri = intentUri,
            deviceStateItems = getDeviceStateItems(),
        )
    }

    companion object {
        const val DEFAULT_PACKAGE_LABEL = "Unknown"
    }
}

class PermissionManagerScreen : PerScreenDeviceState() {
    override val key: String
        get() = KEY

    override val description: String
        get() = DESCRIPTION

    // TODO b/411229443 - Make paths customizable for OEMs
    override val paths: List<String>
        get() = listOf("Security & privacy", "Privacy controls", "Permission manager")

    override val intentUri: String
        get() = Intent(Intent.ACTION_MANAGE_PERMISSIONS).toUri(Intent.URI_INTENT_SCHEME)

    companion object {
        const val KEY = "permission_manager"
        const val DESCRIPTION = "Permission Manager"
    }
}

class PermissionAppsScreen(val context: Context, val permissionGroup: String) :
    PerScreenDeviceState() {
    private val permissionGroupLabel: String =
        getPermGroupLabel(context, permissionGroup).toString()

    override val key: String
        get() = KEY

    override val description: String
        get() = "Permission Manager: $permissionGroupLabel"

    override val paths: List<String>
        get() =
            listOf(
                "Security & privacy",
                "Privacy controls",
                "Permission manager",
                permissionGroupLabel,
            )

    override val intentUri: String
        get() =
            Intent(Intent.ACTION_MANAGE_PERMISSION_APPS)
                .apply { putExtra(Intent.EXTRA_PERMISSION_GROUP_NAME, permissionGroup) }
                .toUri(Intent.URI_INTENT_SCHEME)

    companion object {
        const val KEY = "permission_apps"
    }
}

class AppPermissionScreen(
    val context: Context,
    val permissionGroup: String,
    val packageName: String,
    val userHandle: UserHandle,
    val permissionGrantState: PermGrantState,
    val lastAccessTime: Long,
    val usePreciseLocation: Boolean?,
) : PerScreenDeviceState() {
    private val permissionGroupLabel: String =
        getPermGroupLabel(context, permissionGroup).toString()

    private var packageLabel: String

    init {
        try {
            val appInfo = context.packageManager.getApplicationInfo(packageName, 0)
            packageLabel = Utils.getFullAppLabel(appInfo, context)
        } catch (e: PackageManager.NameNotFoundException) {
            packageLabel = DEFAULT_PACKAGE_LABEL
        }
    }

    override val key: String
        get() = KEY

    override val description: String
        get() = "$permissionGroupLabel Permission: $packageLabel"

    override val paths: List<String>
        get() =
            listOf(
                "Security & privacy",
                "Privacy controls",
                "Permission manager",
                permissionGroupLabel,
                packageLabel,
            )

    override val intentUri: String
        get() =
            Intent(Intent.ACTION_MANAGE_APP_PERMISSION)
                .apply {
                    putExtra(Intent.EXTRA_PERMISSION_GROUP_NAME, permissionGroup)
                    putExtra(Intent.EXTRA_PACKAGE_NAME, packageName)
                    putExtra(Intent.EXTRA_USER, userHandle)
                }
                .toUri(Intent.URI_INTENT_SCHEME)

    override fun getDeviceStateItems(): List<DeviceStateItem> {
        val result: MutableList<DeviceStateItem> = mutableListOf()
        val permissionGroupLabel: String = getPermGroupLabel(context, permissionGroup).toString()
        val permissionStateItem =
            DeviceStateItem(
                key = "${permissionGroupLabel.lowercase()}_permission_state",
                name = LocalizedString(english = "$permissionGroupLabel access for this app"),
                jsonValue = translatePermissionGrantState(),
            )
        result.add(permissionStateItem)

        if (lastAccessTime > 0) {
            val summaryTimestamp =
                Utils.getPermissionLastAccessSummaryTimestamp(
                    lastAccessTime,
                    context,
                    permissionGroup,
                )
            val recentAccessItem =
                DeviceStateItem(
                    key = "recent_access",
                    name = LocalizedString(english = "Recent access"),
                    jsonValue = getRecentAccessSummary(summaryTimestamp),
                )
            result.add(recentAccessItem)
        }

        if (usePreciseLocation != null) {
            val usePreciseLocation =
                DeviceStateItem(
                    key = "use_precise_location",
                    name = LocalizedString(english = "Use precise location"),
                    jsonValue = usePreciseLocation.toString(),
                )
            result.add(usePreciseLocation)
        }

        return result
    }

    private fun translatePermissionGrantState(): String {
        return when (permissionGrantState) {
            PermGrantState.PERMS_DENIED -> "Not Allowed"
            PermGrantState.PERMS_ALLOWED -> "Allowed"
            PermGrantState.PERMS_ALLOWED_FOREGROUND_ONLY -> "Allowed while using the app"
            PermGrantState.PERMS_ALLOWED_ALWAYS -> "Always Allowed"
            PermGrantState.PERMS_ASK -> "Ask every time"
        }
    }

    private fun getRecentAccessSummary(summaryTimestamp: Triple<String, Int, String>): String {
        val res: Resources = context.resources

        return when (summaryTimestamp.second) {
            Utils.LAST_24H_CONTENT_PROVIDER ->
                res.getString(R.string.app_perms_content_provider_24h)
            Utils.LAST_7D_CONTENT_PROVIDER -> res.getString(R.string.app_perms_content_provider_7d)
            Utils.LAST_24H_SENSOR_TODAY ->
                res.getString(R.string.app_perms_24h_access, summaryTimestamp.first)
            Utils.LAST_24H_SENSOR_YESTERDAY ->
                res.getString(R.string.app_perms_24h_access_yest, summaryTimestamp.first)
            Utils.LAST_7D_SENSOR ->
                res.getString(
                    R.string.app_perms_7d_access,
                    summaryTimestamp.third,
                    summaryTimestamp.first,
                )
            else -> ""
        }
    }

    companion object {
        const val KEY = "app_permission"
    }
}

class UnusedAppsScreen : PerScreenDeviceState() {
    override val key: String
        get() = KEY

    override val description: String
        get() = DESCRIPTION

    override val paths: List<String>
        get() = listOf("Apps", "Unused apps")

    override val intentUri: String
        get() = Intent(Intent.ACTION_MANAGE_UNUSED_APPS).toUri(Intent.URI_INTENT_SCHEME)

    companion object {
        const val KEY = "unused_apps"
        const val DESCRIPTION = "Unused apps"
    }
}

class UnusedAppLastUsageScreen(
    val context: Context,
    val packageName: String,
    private val lastUsageTime: Long,
) : PerScreenDeviceState() {
    private var packageLabel: String

    init {
        try {
            val appInfo = context.packageManager.getApplicationInfo(packageName, 0)
            packageLabel = Utils.getFullAppLabel(appInfo, context)
        } catch (e: PackageManager.NameNotFoundException) {
            packageLabel = DEFAULT_PACKAGE_LABEL
        }
    }

    override val key: String
        get() = KEY

    override val description: String
        get() = "Unused app details: $packageLabel"

    override val paths: List<String>
        get() = listOf("Apps", "Unused apps", packageLabel)

    override val intentUri: String
        get() =
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                .apply { setData(Uri.fromParts("package", packageName, null)) }
                .toUri(Intent.URI_INTENT_SCHEME)

    override fun getDeviceStateItems(): List<DeviceStateItem> {
        val usagePeriod = UnusedPeriod.findLongestValidPeriod(lastUsageTime)

        val lastUsed =
            DeviceStateItem(
                key = "last_used",
                name = LocalizedString(english = "Last used"),
                jsonValue = translateUnusedPeriod(usagePeriod),
            )
        return listOf(lastUsed)
    }

    private fun translateUnusedPeriod(usagePeriod: UnusedPeriod): String {
        return when (usagePeriod) {
            UnusedPeriod.ONE_MONTH -> "1 month ago"
            UnusedPeriod.THREE_MONTHS -> "3 months ago"
            UnusedPeriod.SIX_MONTHS -> "6 months ago"
        }
    }

    companion object {
        const val KEY = "unused_app_last_usage"
    }
}

val deviceStateScreenKeys: List<String> =
    listOf(
        PermissionManagerScreen.KEY,
        PermissionAppsScreen.KEY,
        AppPermissionScreen.KEY,
        UnusedAppsScreen.KEY,
        UnusedAppLastUsageScreen.KEY,
    )
