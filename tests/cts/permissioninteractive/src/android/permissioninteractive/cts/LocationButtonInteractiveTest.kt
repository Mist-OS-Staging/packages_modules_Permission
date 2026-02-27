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

package android.permissioninteractive.cts

import android.content.pm.PackageManager
import android.permission.flags.Flags
import android.platform.test.annotations.RequiresFlagsEnabled
import android.platform.test.flag.junit.CheckFlagsRule
import android.platform.test.flag.junit.DeviceFlagsValueProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.android.compatibility.common.util.CddTest
import com.android.interactive.annotations.Interactive
import org.junit.Assume.assumeFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/** Interactive CTS tests for the Location Button. */
@RequiresFlagsEnabled(Flags.FLAG_LOCATION_BUTTON_ENABLED)
class LocationButtonInteractiveTest {

    @get:Rule val checkFlagsRule: CheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule()

    @Before
    fun setup() {
        assumeFalse(isAutomotive)
        assumeFalse(isTv)
        assumeFalse(isWatch)
    }

    @Interactive
    @Test
    @CddTest(requirement = "3.8.18/C-0-1")
    fun verifyLocationButtonCustomizations() {
        // TODO: Verify Location Button respects customizations set by clients.
    }

    companion object {
        private val instrumentation = InstrumentationRegistry.getInstrumentation()
        private val context = instrumentation.context
        private val packageManager = context.packageManager

        private val isTv = packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)
        private val isWatch = packageManager.hasSystemFeature(PackageManager.FEATURE_WATCH)
        private val isAutomotive =
            packageManager.hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE)
    }
}
