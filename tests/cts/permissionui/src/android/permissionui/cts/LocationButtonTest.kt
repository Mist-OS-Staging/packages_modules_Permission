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
package android.permissionui.cts

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.os.Build
import android.permission.flags.Flags
import android.platform.test.annotations.RequiresFlagsEnabled
import android.view.SurfaceView
import androidx.test.filters.SdkSuppress
import androidx.test.uiautomator.By
import com.android.compatibility.common.util.SystemUtil.eventually
import org.junit.After
import org.junit.Assume.assumeFalse
import org.junit.Before
import org.junit.Test

/** Tests for the Location Button UI and functionality. */
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.BAKLAVA)
@RequiresFlagsEnabled(Flags.FLAG_LOCATION_BUTTON_ENABLED)
class LocationButtonTest : BaseUsePermissionTest() {

    @Before
    fun setup() {
        assumeFalse(isAutomotive)
        assumeFalse(isTv)
        assumeFalse(isWatch)
        installPackage(TEST_APP_APK_PATH, expectSuccess = true)
    }

    @After
    fun cleanup() {
        val skipUninstall = isAutomotive || isTv || isWatch
        uninstallPackage(TEST_APP_PACKAGE_NAME, requireSuccess = !skipUninstall)
    }

    @Test
    fun testLocationButton_grantsPreciseLocation() {
        assertAppHasPermission(
            Manifest.permission.ACCESS_FINE_LOCATION,
            false,
            packageName = TEST_APP_PACKAGE_NAME,
        )
        val intent =
            Intent().apply {
                component =
                    ComponentName(
                        TEST_APP_PACKAGE_NAME,
                        "$TEST_APP_PACKAGE_NAME.LocationButtonActivity",
                    )
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                putExtra("width", dpToPx(200))
                putExtra("height", dpToPx(80))
            }
        context.startActivity(intent)

        val surfaceView = waitFindObject(By.clazz(SurfaceView::class.java.name))
        surfaceView.click()
        eventually { clickPermissionRequestAllowLocationButtonButton() }
        eventually {
            assertAppHasPermission(
                Manifest.permission.ACCESS_FINE_LOCATION,
                true,
                packageName = TEST_APP_PACKAGE_NAME,
            )
        }
    }

    @Test
    fun testLocationButton_minSize_doesGrantPermission() {
        assertAppHasPermission(
            Manifest.permission.ACCESS_FINE_LOCATION,
            false,
            packageName = TEST_APP_PACKAGE_NAME,
        )
        val intent =
            Intent().apply {
                component =
                    ComponentName(
                        TEST_APP_PACKAGE_NAME,
                        "$TEST_APP_PACKAGE_NAME.LocationButtonActivity",
                    )
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                putExtra("width", dpToPx(48))
                putExtra("height", dpToPx(48))
            }
        context.startActivity(intent)

        val surfaceView = waitFindObject(By.clazz(SurfaceView::class.java.name))
        surfaceView.click()
        eventually { clickPermissionRequestAllowLocationButtonButton() }
        eventually {
            assertAppHasPermission(
                Manifest.permission.ACCESS_FINE_LOCATION,
                true,
                packageName = TEST_APP_PACKAGE_NAME,
            )
        }
    }

    @Test
    fun testLocationButton_buttonTooSmall_doesNotOpenPermissionDialog() {
        assertAppHasPermission(
            Manifest.permission.ACCESS_FINE_LOCATION,
            false,
            packageName = TEST_APP_PACKAGE_NAME,
        )
        val intent =
            Intent().apply {
                component =
                    ComponentName(
                        TEST_APP_PACKAGE_NAME,
                        "$TEST_APP_PACKAGE_NAME.LocationButtonActivity",
                    )
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                putExtra("width", dpToPx(40))
                putExtra("height", dpToPx(40))
            }
        context.startActivity(intent)

        val surfaceView = waitFindObject(By.clazz(SurfaceView::class.java.name))
        surfaceView.click()
        findView(By.res(LOCATION_BUTTON_ALLOW_BUTTON), expected = false)
        assertAppHasPermission(
            Manifest.permission.ACCESS_FINE_LOCATION,
            false,
            packageName = TEST_APP_PACKAGE_NAME,
        )
    }

    @Test
    fun testLocationButton_buttonTooLarge_doesNotOpenPermissionDialog() {
        assertAppHasPermission(
            Manifest.permission.ACCESS_FINE_LOCATION,
            false,
            packageName = TEST_APP_PACKAGE_NAME,
        )
        val intent =
            Intent().apply {
                component =
                    ComponentName(
                        TEST_APP_PACKAGE_NAME,
                        "$TEST_APP_PACKAGE_NAME.LocationButtonActivity",
                    )
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                // Too large width cause clipping, and location button won't work.
                putExtra("width", dpToPx(2000))
                putExtra("height", dpToPx(200)) // height is clamped down to max 136 dp.
            }
        context.startActivity(intent)

        val surfaceView = waitFindObject(By.clazz(SurfaceView::class.java.name))
        surfaceView.click()
        findView(By.res(LOCATION_BUTTON_ALLOW_BUTTON), expected = false)
        assertAppHasPermission(
            Manifest.permission.ACCESS_FINE_LOCATION,
            false,
            packageName = TEST_APP_PACKAGE_NAME,
        )
    }

    private fun dpToPx(dp: Int): Int {
        val density = context.resources.displayMetrics.density
        return (dp * density).toInt()
    }

    companion object {
        private const val TEST_APP_PACKAGE_NAME = "android.permissionui.cts.locationbutton"
        private const val TEST_APP_APK_PATH = "$APK_DIRECTORY/CtsLocationButtonApp.apk"
    }
}
