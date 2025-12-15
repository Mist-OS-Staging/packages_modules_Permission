/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.role.controller.behavior.v33;

import android.app.admin.DevicePolicyManager;
import android.app.role.RoleManager;
import android.content.Context;
import android.os.Build;
import android.os.UserHandle;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.android.role.controller.model.Role;
import com.android.role.controller.model.RoleBehavior;
import com.android.role.controller.util.RoleManagerCompat;
import com.android.role.controller.util.UserUtils;

/**
 * Class for behavior of the device policy management role.
 */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
public class DevicePolicyManagementRoleBehavior implements RoleBehavior {

    @Override
    public Boolean shouldAllowBypassingQualification(@NonNull Role role,
            @NonNull Context context) {
        if (android.app.admin.flags.Flags.secureAdbRoleBypassing()) {
            return null;
        }
        DevicePolicyManager devicePolicyManager =
                context.getSystemService(DevicePolicyManager.class);
        return devicePolicyManager.shouldAllowBypassingDevicePolicyManagementRoleQualification();
    }

    /**
     * Checks if the package having {@code packageName} is qualified for the Device Policy
     * Management Role.
     *
     * @return {@code false} if the package having {@code packageName} does not pass all the checks
     * for becoming a Device Policy Management Role Holder. {@code true} if the package passes the
     * checks and the bypassing role qualification flags is enabled. {@code null} otherwise.
     */
    @Override
    public Boolean isPackageQualifiedAsUser(@NonNull Role role, @NonNull String packageName,
            @NonNull UserHandle user, @NonNull Context context) {
        if (android.app.admin.flags.Flags.secureAdbRoleBypassing()) {
            Context userContext = UserUtils.getUserContext(context, user);
            DevicePolicyManager userDevicePolicyManager =
                    userContext.getSystemService(DevicePolicyManager.class);
            boolean isQualified = userDevicePolicyManager
                    .isPackageQualifiedForDevicePolicyManagementRole(packageName);
            if (!isQualified) {
                return false;
            }
            RoleManager userRoleManager = userContext.getSystemService(RoleManager.class);
            if (RoleManagerCompat.isBypassingRoleQualification(userRoleManager)) {
                return true;
            }
            // If the role bypassing is not enabled, the standard qualification checks are performed
            return null;
        }
        return null;
    }
}
