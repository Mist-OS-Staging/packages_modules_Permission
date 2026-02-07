/*
 * Copyright (C) 2026 The Android Open Source Project
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
package com.android.permissioncontroller.permission.utils.v37

import android.app.Application
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Parcel
import android.os.UserHandle
import android.os.UserManager
import android.util.Log
import com.android.modules.utils.build.SdkLevel
import com.android.permissioncontroller.permission.model.livedatatypes.LightPackageInfo
import com.android.permissioncontroller.permission.utils.ContextCompat
import com.android.permissioncontroller.permission.utils.Utils
import com.android.permissioncontroller.permission.utils.v35.MultiDeviceUtils

/**
 * Utility methods for creating [LightPackageInfo] synchronously. Live data are tricky to use when
 * we simply want to read the information one time.
 */
object LightPackageInfoUtils {
    private val LOG_TAG = "LightPackageInfoUtils"

    fun getLightPackageInfo(
        app: Application,
        packageName: String,
        deviceId: Int,
        user: UserHandle,
    ): LightPackageInfo? {
        val packageInfo = getPackageInfo(app, packageName, user) ?: return null

        // PackageInfo#requestedPermissionsFlags is not device aware. Hence for device aware
        // permissions if the deviceId is not the primary device we need to separately check
        // permission for that device and update requestedPermissionsFlags.
        return if (SdkLevel.isAtLeastV() && deviceId != ContextCompat.DEVICE_ID_DEFAULT) {
            val requestedPermissionsFlagsForDevice =
                MultiDeviceUtils.getPermissionsFlagsForDevice(
                    app,
                    packageInfo.requestedPermissions?.toList() ?: emptyList(),
                    packageInfo.requestedPermissionsFlags?.toList() ?: emptyList(),
                    packageInfo.applicationInfo!!.uid,
                    deviceId,
                )

            LightPackageInfo(packageInfo, deviceId, requestedPermissionsFlagsForDevice)
        } else {
            LightPackageInfo(packageInfo)
        }
    }

    fun getPackageInfo(app: Application, packageName: String, user: UserHandle): PackageInfo? {
        try {
            val packageManager = Utils.getUserContext(app, user).packageManager
            var packageInfo = getPackageInfo(packageManager, packageName)
            if (packageInfo.sharedUserId != null && packageInfo.applicationInfo != null) {
                val sharedPackageNames =
                    packageManager.getPackagesForUid(packageInfo.applicationInfo!!.uid)
                val sharedPackages =
                    sharedPackageNames?.mapNotNull { otherPackageName ->
                        try {
                            val otherPi = getPackageInfo(packageManager, otherPackageName)
                            otherPi
                        } catch (_: PackageManager.NameNotFoundException) {
                            null
                        }
                    }
                packageInfo = mergePermissionsInSharedUid(packageInfo, sharedPackages)
            }
            return packageInfo
        } catch (e: Exception) {
            if (e is PackageManager.NameNotFoundException) {
                Log.w(LOG_TAG, "Package \"$packageName\" not found for user $user")
            } else {
                val profiles = app.getSystemService(UserManager::class.java)!!.userProfiles
                Log.e(
                    LOG_TAG,
                    "Failed to create context for user $user. " +
                        "User exists : ${user in profiles}",
                    e,
                )
            }
            return null
        }
    }

    @Suppress("DEPRECATION")
    private fun getPackageInfo(pm: PackageManager, pkgName: String): PackageInfo {
        if (SdkLevel.isAtLeastU()) {
            val flags =
                PackageManager.PackageInfoFlags.of(
                    PackageManager.GET_ATTRIBUTIONS_LONG or PackageManager.GET_PERMISSIONS.toLong()
                )
            return pm.getPackageInfo(pkgName, flags)
        }
        var flags = PackageManager.GET_PERMISSIONS
        if (SdkLevel.isAtLeastS()) {
            flags = flags or PackageManager.GET_ATTRIBUTIONS
        }
        return pm.getPackageInfo(pkgName, flags)
    }

    @JvmStatic
    fun mergePermissionsInSharedUid(
        base: PackageInfo,
        flags: Int,
        packageManager: PackageManager,
    ): PackageInfo {
        if (base.sharedUserId != null && base.applicationInfo != null) {
            val sharedPackageNames = packageManager.getPackagesForUid(base.applicationInfo!!.uid)
            val sharedPackages =
                sharedPackageNames?.mapNotNull { otherPackageName ->
                    try {
                        val otherPi = packageManager.getPackageInfo(otherPackageName, flags)
                        otherPi
                    } catch (_: PackageManager.NameNotFoundException) {
                        null
                    }
                }
            return mergePermissionsInSharedUid(base, sharedPackages)
        }
        return base
    }

    fun mergePermissionsInSharedUid(
        base: PackageInfo,
        otherPackages: List<PackageInfo>?,
    ): PackageInfo {
        if (otherPackages == null || base.applicationInfo == null || base.sharedUserId == null) {
            return base
        }

        val allPackages =
            if (base in otherPackages) otherPackages
            else otherPackages.toMutableList().apply { add(base) }
        val permissionStates = mutableMapOf<String, Int>()
        allPackages.forEach { packageInfo ->
            if (
                packageInfo.applicationInfo?.uid != base.applicationInfo?.uid ||
                    packageInfo.requestedPermissions == null
            ) {
                return@forEach
            }
            packageInfo.requestedPermissions!!.forEachIndexed { index, permission ->
                val existingFlags = permissionStates.getOrDefault(permission, 0)
                permissionStates[permission] =
                    existingFlags or packageInfo.requestedPermissionsFlags!![index]
            }
        }
        val requestedPermissions = permissionStates.keys.toTypedArray()
        val requestedPermissionsFlags = IntArray(requestedPermissions.size)
        requestedPermissions.forEachIndexed { index, permission ->
            requestedPermissionsFlags[index] = permissionStates[permission]!!
        }
        val packageCopy = copyPackageInfo(base)
        packageCopy.requestedPermissions = requestedPermissions
        packageCopy.requestedPermissionsFlags = requestedPermissionsFlags
        return packageCopy
    }

    fun copyPackageInfo(original: PackageInfo): PackageInfo {
        val parcel = Parcel.obtain()
        try {
            original.writeToParcel(parcel, 0)
            parcel.setDataPosition(0)
            val copy = PackageInfo.CREATOR.createFromParcel(parcel)
            return copy
        } finally {
            parcel.recycle()
        }
    }
}
