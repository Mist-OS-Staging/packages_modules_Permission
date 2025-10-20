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

package android.permission.cts;

import static android.Manifest.permission.QUERY_AUDIO_VOLUME;
import static android.permission.cts.PermissionUtils.install;
import static android.permission.cts.PermissionUtils.isPermissionGranted;
import static android.permission.cts.PermissionUtils.uninstallApp;

import static com.google.common.truth.Truth.assertThat;

import android.os.Build;
import android.platform.test.annotations.RequiresFlagsEnabled;
import android.platform.test.flag.junit.CheckFlagsRule;
import android.platform.test.flag.junit.DeviceFlagsValueProvider;

import androidx.test.filters.SdkSuppress;
import androidx.test.runner.AndroidJUnit4;

import com.google.common.collect.ImmutableList;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

/**
 * CTS tests for install-time purpose declaration feature targeted for Android 17 (26Q2) release.
 */
@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.CINNAMON_BUN, codeName = "CinnamonBun")
// Note that ppd_install_time_enabled flag guards the package parsing and install-time purpose
// validation logic. Hence, this flag should be enabled as well for the test to work. However,
// we cannot add a RequiresFlagEnabled constraint for that flag since it's not exported and the
// CtsPermissionTestCases module cannot depend on android.permission.flags-aconfig-java
// due to MTS issues. Therefore, we are using the enabled status of a new purpose-guarded
// permission as proxy since ppd_install_time_enabled must have the same state as the new audio
// permission flag. This is a temporary workaround until the flags are enabled for 26Q2 release.
@RequiresFlagsEnabled(android.media.audio.Flags.FLAG_GUARD_STREAM_VOLUME_APIS)
public class PurposeDeclarationNormalPermissionsTest {
    private static final String TMP_DIR = "/data/local/tmp/cts-permission/";
    private static final String PKG_PREFIX = "android.permission.cts.";

    // All test apps share the same UID for convenience to simulate shared UID scenarios as needed.
    private static final TestApp APP_REQUESTS_PERMISSION_WITH_INVALID_PURPOSE = new TestApp(
            /* packageName= */ PKG_PREFIX + "appthatrequestpermissionwithinvalidpurpose37",
            /* apkPath= */ TMP_DIR + "CtsAppThatRequestPermissionWithInvalidPurpose37.apk");

    private static final TestApp APP_REQUESTS_PERMISSION_WITH_NO_PURPOSE = new TestApp(
            /* packageName= */ PKG_PREFIX + "appthatrequestpermissionwithnopurpose37",
            /* apkPath= */ TMP_DIR + "CtsAppThatRequestPermissionWithNoPurpose37.apk");

    private static final TestApp APP_REQUESTS_PERMISSION_WITH_VALID_PURPOSE = new TestApp(
            /* packageName= */ PKG_PREFIX + "appthatrequestpermissionwithvalidpurpose37",
            /* apkPath= */ TMP_DIR + "CtsAppThatRequestPermissionWithValidPurpose37.apk");

    private static final TestApp APP_REQUESTS_PERMISSION_WITH_NO_PURPOSE_OLD_TARGET_SDK = new TestApp(
            /* packageName= */ PKG_PREFIX + "appthatrequestpermissionwithnopurposeoldtargetsdk36",
            /* apkPath= */ TMP_DIR + "CtsAppThatRequestPermissionWithNoPurposeOldTargetSdk36.apk");

    private static final TestApp APP_REQUESTS_PERMISSION_WITH_NO_PURPOSE_USING_MIN_MAX_ATTRS = new TestApp(
            /* packageName= */ PKG_PREFIX + "appthatrequestpermissionwithnopurposeusingminmaxattrs37",
            /* apkPath= */ TMP_DIR + "CtsAppThatRequestPermissionWithNoPurposeUsingMinMaxAttrs37.apk");

    private static final List<TestApp> TEST_APPS = ImmutableList.of(
            APP_REQUESTS_PERMISSION_WITH_INVALID_PURPOSE,
            APP_REQUESTS_PERMISSION_WITH_NO_PURPOSE,
            APP_REQUESTS_PERMISSION_WITH_VALID_PURPOSE,
            APP_REQUESTS_PERMISSION_WITH_NO_PURPOSE_OLD_TARGET_SDK,
            APP_REQUESTS_PERMISSION_WITH_NO_PURPOSE_USING_MIN_MAX_ATTRS);

    @Rule
    public final CheckFlagsRule mCheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule();

    @Before
    @After
    public void uninstallTestApps() {
        for (TestApp testApp : TEST_APPS) {
            uninstallApp(testApp.packageName);
        }
    }

    @Test
    public void testInstallPackageRequestingPermissionWithValidPurpose_granted()
            throws Exception {
        install(APP_REQUESTS_PERMISSION_WITH_VALID_PURPOSE.apkPath);
        assertThat(isPermissionGranted(APP_REQUESTS_PERMISSION_WITH_VALID_PURPOSE.packageName,
                QUERY_AUDIO_VOLUME)).isTrue();
    }

    @Test
    public void testInstallPackageRequestingPermissionWithNoPurpose_revoked()
            throws Exception {
        install(APP_REQUESTS_PERMISSION_WITH_NO_PURPOSE.apkPath);
        assertThat(isPermissionGranted(APP_REQUESTS_PERMISSION_WITH_NO_PURPOSE.packageName,
                QUERY_AUDIO_VOLUME)).isFalse();
    }

    @Test
    public void testInstallPackageRequestingPermissionWithInvalidPurpose_revoked()
            throws Exception {
        install(APP_REQUESTS_PERMISSION_WITH_INVALID_PURPOSE.apkPath);
        assertThat(isPermissionGranted(APP_REQUESTS_PERMISSION_WITH_INVALID_PURPOSE.packageName,
                QUERY_AUDIO_VOLUME)).isFalse();
    }

    @Test
    public void testInstallPackageRequestingPermissionWithNoPurposeUsingMinMaxAttrs_revoked()
            throws Exception {
        install(APP_REQUESTS_PERMISSION_WITH_NO_PURPOSE_USING_MIN_MAX_ATTRS.apkPath);
        assertThat(isPermissionGranted(
                APP_REQUESTS_PERMISSION_WITH_NO_PURPOSE_USING_MIN_MAX_ATTRS.packageName,
                QUERY_AUDIO_VOLUME)).isFalse();
    }

    @Test
    public void testInstallPackageRequestingPermissionWithNoPurposeTargetingOldSdk_granted()
            throws Exception {
        install(APP_REQUESTS_PERMISSION_WITH_NO_PURPOSE_OLD_TARGET_SDK.apkPath);
        assertThat(isPermissionGranted(
                APP_REQUESTS_PERMISSION_WITH_NO_PURPOSE_OLD_TARGET_SDK.packageName,
                QUERY_AUDIO_VOLUME)).isTrue();
    }

    @Test
    public void testInstallSharedUidPackageWithValidPurposeAndPackageWithNoPurpose_grantedForAll()
            throws Exception {
        install(APP_REQUESTS_PERMISSION_WITH_VALID_PURPOSE.apkPath);
        install(APP_REQUESTS_PERMISSION_WITH_NO_PURPOSE.apkPath);

        assertThat(isPermissionGranted(APP_REQUESTS_PERMISSION_WITH_VALID_PURPOSE.packageName,
                QUERY_AUDIO_VOLUME)).isTrue();
        assertThat(isPermissionGranted(APP_REQUESTS_PERMISSION_WITH_NO_PURPOSE.packageName,
                QUERY_AUDIO_VOLUME)).isTrue();
    }

    @Test
    public void testInstallSharedUidPackageOldTargetSdkAndPackageWithInvalidPurpose_grantedForAll()
            throws Exception {
        install(APP_REQUESTS_PERMISSION_WITH_INVALID_PURPOSE.apkPath);
        install(APP_REQUESTS_PERMISSION_WITH_NO_PURPOSE_OLD_TARGET_SDK.apkPath);

        assertThat(isPermissionGranted(APP_REQUESTS_PERMISSION_WITH_INVALID_PURPOSE.packageName,
                QUERY_AUDIO_VOLUME)).isTrue();
        assertThat(isPermissionGranted(
                APP_REQUESTS_PERMISSION_WITH_NO_PURPOSE_OLD_TARGET_SDK.packageName,
                QUERY_AUDIO_VOLUME)).isTrue();
    }

    @Test
    public void testUninstallSharedUidPackageWithValidPurpose_revokedForAll()
            throws Exception {
        install(APP_REQUESTS_PERMISSION_WITH_VALID_PURPOSE.apkPath);
        install(APP_REQUESTS_PERMISSION_WITH_NO_PURPOSE.apkPath);

        uninstallApp(APP_REQUESTS_PERMISSION_WITH_VALID_PURPOSE.packageName);

        assertThat(isPermissionGranted(APP_REQUESTS_PERMISSION_WITH_NO_PURPOSE.packageName,
                QUERY_AUDIO_VOLUME)).isFalse();
    }

    private static class TestApp {
        final String packageName;
        final String apkPath;

        TestApp(String packageName, String apkFileName) {
            this.packageName = packageName;
            this.apkPath = apkFileName;
        }
    }
}
