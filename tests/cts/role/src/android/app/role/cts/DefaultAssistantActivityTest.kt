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
package android.app.role.cts

import android.app.AppOpsManager
import android.app.role.RoleManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import android.os.UserHandle
import android.permission.flags.Flags.FLAG_ASSIST_SETTINGS_PRIVACY_IMPROVEMENTS_ENABLED
import android.platform.test.annotations.RequiresFlagsEnabled
import android.platform.test.flag.junit.DeviceFlagsValueProvider
import android.provider.Settings
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiObject2
import com.android.compatibility.common.util.DisableAnimationRule
import com.android.compatibility.common.util.FreezeRotationRule
import com.android.compatibility.common.util.SystemUtil
import com.android.compatibility.common.util.UiAutomatorUtils2
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Assume
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Tests for the assist structure setting on the Default Assistant page */
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.VANILLA_ICE_CREAM)
@RunWith(AndroidJUnit4::class)
@RequiresFlagsEnabled(FLAG_ASSIST_SETTINGS_PRIVACY_IMPROVEMENTS_ENABLED)
class DefaultAssistantActivityTest {
    @get:Rule val checkFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule()
    @get:Rule val disableAnimationRule = DisableAnimationRule()
    @get:Rule val freezeRotationRule = FreezeRotationRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val uiDevice = UiAutomatorUtils2.getUiDevice()
    private val appOpsManager = context.getSystemService(AppOpsManager::class.java)!!
    private val roleManager = context.getSystemService(RoleManager::class.java)!!

    private var assistantRoleHolderPackageUid: Int = Process.INVALID_UID
    private var originalRoleHolder: String? = null

    @Before
    fun setup() {
        val packageManager = context.packageManager
        Assume.assumeFalse(packageManager.hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE))
        Assume.assumeFalse(packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK))
        Assume.assumeFalse(packageManager.hasSystemFeature(PackageManager.FEATURE_WATCH))
        Assume.assumeTrue(RoleManagerUtil.isCddCompliantScreenSize())
        saveRoleHolder()
        installPackage(APP_APK_PATH)
        assistantRoleHolderPackageUid = context.packageManager.getPackageUid(APP_PACKAGE_NAME, 0)
        wakeUpScreen()
        closeNotificationShade()
    }

    @After
    fun tearDown() {
        // Close activity, if a test failed it might be left open
        pressBack()
        uninstallPackage(APP_PACKAGE_NAME)
        restoreRoleHolder()
    }

    @Test
    fun defaultAssistantActivity_canBeOpened_viaVoiceInputSettings() {
        launchDefaultAssistantActivity(useVoiceInputSettingsAction = true)

        // If we've reached here, it means we've found the default assistant activity and the test
        // can pass.
    }

    @Test
    fun assistStructureToggle_whenNoneSelected_isDisabled() {
        setAppOpMode(AppOpsManager.MODE_ALLOWED)

        launchDefaultAssistantActivity()

        selectNoneAsRoleHolder()

        // Verify toggle is disabled and unchecked
        assertAssistToggleState(isEnabled = false, isChecked = false)
    }

    @Test
    fun assistStructureToggle_whenAppSelected_isCorrectlySet() {
        setAppOpMode(AppOpsManager.MODE_ALLOWED)

        launchDefaultAssistantActivity()

        selectAppAsRoleHolder()

        // Verify toggle is enabled and checked
        assertAssistToggleState(isEnabled = true, isChecked = true)
    }

    @Test
    fun assistStructureToggle_whenAppSelected_isCorrectlySet_modeIgnored() {
        setAppOpMode(AppOpsManager.MODE_IGNORED)

        launchDefaultAssistantActivity()

        selectAppAsRoleHolder()

        // Verify toggle is enabled and checked
        assertAssistToggleState(isEnabled = true, isChecked = false)
    }

    @Test
    fun assistStructureToggle_whenClicked_changesAppOp() {
        addRoleHolder(RoleManager.ROLE_ASSISTANT, APP_PACKAGE_NAME)
        setAppOpMode(AppOpsManager.MODE_IGNORED)

        launchDefaultAssistantActivity()

        assertAssistToggleState(isEnabled = true, isChecked = false)

        // Click to allow
        findAssistStructureToggle().click()
        uiDevice.waitForIdle()
        assertAssistToggleState(isEnabled = true, isChecked = true)
        assertThat(getAppOpMode()).isEqualTo(AppOpsManager.MODE_ALLOWED)

        // Click to deny
        findAssistStructureToggle().click()
        uiDevice.waitForIdle()
        assertAssistToggleState(isEnabled = true, isChecked = false)
        assertThat(getAppOpMode()).isEqualTo(AppOpsManager.MODE_IGNORED)
    }

    @Test
    fun assistStructureToggle_whenAppOpUpdate_toggleChanges() {
        addRoleHolder(RoleManager.ROLE_ASSISTANT, APP_PACKAGE_NAME)

        launchDefaultAssistantActivity()

        setAppOpMode(AppOpsManager.MODE_IGNORED)
        uiDevice.waitForIdle()
        assertAssistToggleState(isEnabled = true, isChecked = false)

        setAppOpMode(AppOpsManager.MODE_ALLOWED)
        uiDevice.waitForIdle()
        assertAssistToggleState(isEnabled = true, isChecked = true)

        setAppOpMode(AppOpsManager.MODE_IGNORED)
        uiDevice.waitForIdle()
        assertAssistToggleState(isEnabled = true, isChecked = false)

        setAppOpMode(AppOpsManager.MODE_DEFAULT)
        uiDevice.waitForIdle()
        assertAssistToggleState(isEnabled = true, isChecked = true)
    }

    @Test
    fun assistStructureToggle_whenNoneSelected_resetsAppOpToIgnored() {
        addRoleHolder(RoleManager.ROLE_ASSISTANT, APP_PACKAGE_NAME)
        setAppOpMode(AppOpsManager.MODE_DEFAULT)

        launchDefaultAssistantActivity()

        // Verify mode_default is assistToggleState enabled
        assertAssistToggleState(isEnabled = true, isChecked = true)

        selectNoneAsRoleHolder()

        assertAssistToggleState(isEnabled = false, isChecked = false)

        // Ensure app op mode is now ignored
        assertThat(getAppOpMode()).isEqualTo(AppOpsManager.MODE_IGNORED)

        selectAppAsRoleHolder()

        assertAssistToggleState(isEnabled = true, isChecked = false)

        // Double check/ensure that app op is still ignored
        assertThat(getAppOpMode()).isEqualTo(AppOpsManager.MODE_IGNORED)
    }

    private fun launchDefaultAssistantActivity(useVoiceInputSettingsAction: Boolean = false) {
        SystemUtil.runWithShellPermissionIdentity {
            val intent =
                if (useVoiceInputSettingsAction) {
                    Intent(Settings.ACTION_VOICE_INPUT_SETTINGS)
                } else {
                    Intent(Intent.ACTION_MANAGE_DEFAULT_APP)
                        .putExtra(Intent.EXTRA_ROLE_NAME, RoleManager.ROLE_ASSISTANT)
                }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            context.startActivity(intent)
        }
        UiAutomatorUtils2.waitFindObject(
            By.descContains(DEFAULT_ASSISTANT_APP_LABEL).pkg(PERMISSION_CONTROLLER_PACKAGE_NAME)
        )
    }

    private fun selectNoneAsRoleHolder() {
        UiAutomatorUtils2.waitFindObject(
                By.clickable(true)
                    .hasDescendant(By.checkable(true))
                    .hasDescendant(By.text(NONE_LABEL))
            )
            .click()

        UiAutomatorUtils2.waitFindObject(
            By.clickable(true)
                .hasDescendant(By.checkable(true).checked(true))
                .hasDescendant(By.text(NONE_LABEL))
        )
    }

    private fun selectAppAsRoleHolder() {
        UiAutomatorUtils2.waitFindObject(
                By.clickable(true)
                    .hasDescendant(By.checkable(true))
                    .hasDescendant(By.text(APP_LABEL))
            )
            .click()

        // Dismiss the confirmation dialog if it appears
        val positiveButton =
            UiAutomatorUtils2.waitFindObjectOrNull(By.text("Change").clazz("android.widget.Button"))
                ?: UiAutomatorUtils2.waitFindObjectOrNull(By.text("OK"))
        if (positiveButton != null) {
            positiveButton.click()
            uiDevice.waitForIdle()
        }

        UiAutomatorUtils2.waitFindObject(
            By.clickable(true)
                .hasDescendant(By.checkable(true).checked(true))
                .hasDescendant(By.text(APP_LABEL))
        )
    }

    private fun findAssistStructureToggle(): UiObject2 =
        UiAutomatorUtils2.waitFindObject(By.text(ASSIST_STRUCTURE_SWITCH_LABEL))

    private fun assertAssistToggleState(isEnabled: Boolean, isChecked: Boolean) {
        if (isEnabled) {
            UiAutomatorUtils2.waitFindObject(
                By.clickable(true)
                    .hasDescendant(By.checkable(true).checked(isChecked))
                    .hasDescendant(By.text(ASSIST_STRUCTURE_SWITCH_LABEL))
            )
        } else {
            UiAutomatorUtils2.waitFindObject(
                By.enabled(false)
                    .hasDescendant(By.checkable(true).checked(isChecked))
                    .hasDescendant(By.text(ASSIST_STRUCTURE_SWITCH_LABEL))
            )
        }
    }

    private fun getAppOpMode(): Int =
        SystemUtil.runWithShellPermissionIdentity<Int> {
            appOpsManager.checkOpNoThrow(
                AppOpsManager.OPSTR_READ_SCREEN_CONTEXT,
                assistantRoleHolderPackageUid,
                APP_PACKAGE_NAME,
            )
        }

    private fun setAppOpMode(mode: Int) {
        SystemUtil.runWithShellPermissionIdentity {
            appOpsManager.setUidMode(
                AppOpsManager.OPSTR_READ_SCREEN_CONTEXT,
                assistantRoleHolderPackageUid,
                mode,
            )
        }
    }

    private fun saveRoleHolder() {
        val roleHolders = RoleManagerUtil.getRoleHolders(roleManager, RoleManager.ROLE_ASSISTANT)
        originalRoleHolder = roleHolders.firstOrNull()

        if (originalRoleHolder == APP_PACKAGE_NAME) {
            RoleManagerUtil.removeRoleHolder(
                roleManager,
                context,
                RoleManager.ROLE_ASSISTANT,
                APP_PACKAGE_NAME,
            )
            originalRoleHolder = null
        }
    }

    private fun restoreRoleHolder() {
        RoleManagerUtil.removeRoleHolder(
            roleManager,
            context,
            RoleManager.ROLE_ASSISTANT,
            APP_PACKAGE_NAME,
        )
        originalRoleHolder?.let {
            RoleManagerUtil.addRoleHolder(roleManager, context, RoleManager.ROLE_ASSISTANT, it)
        }
    }

    private fun addRoleHolder(roleName: String, packageName: String) {
        RoleManagerUtil.addRoleHolder(roleManager, context, roleName, packageName)
    }

    private fun removeRoleHolder(roleName: String, packageName: String) {
        RoleManagerUtil.removeRoleHolder(roleManager, context, roleName, packageName)
    }

    private fun installPackage(apkPath: String, user: UserHandle = Process.myUserHandle()) =
        SystemUtil.runShellCommandOrThrow("pm install -r --user ${user.identifier} $apkPath")

    private fun uninstallPackage(packageName: String, user: UserHandle = Process.myUserHandle()) =
        SystemUtil.runShellCommand("pm uninstall --user ${user.identifier} $packageName")

    private fun wakeUpScreen() =
        SystemUtil.runShellCommand(
            InstrumentationRegistry.getInstrumentation(),
            "input keyevent KEYCODE_WAKEUP",
        )

    private fun closeNotificationShade() =
        context.sendBroadcast(Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS))

    private fun pressBack() {
        uiDevice.pressBack()
        uiDevice.waitForIdle()
    }

    companion object {
        private const val TIMEOUT_MILLIS = 15_000L
        private const val APP_APK_PATH = "/data/local/tmp/cts-role/CtsRoleTestApp.apk"
        private const val APP_PACKAGE_NAME = "android.app.role.cts.app"
        private const val APP_LABEL = "CtsRoleTestApp"
        private const val NONE_LABEL = "None"
        private const val ASSIST_STRUCTURE_SWITCH_LABEL = "Use screen and app context"
        private const val DEFAULT_ASSISTANT_APP_LABEL = "Default digital assistant app"
        private val PERMISSION_CONTROLLER_PACKAGE_NAME =
            InstrumentationRegistry.getInstrumentation()
                .targetContext
                .packageManager
                .permissionControllerPackageName
    }
}
