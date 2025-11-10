/*
 * Copyright (C) 2021 The Android Open Source Project
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
package com.android.permissioncontroller.permission.ui.auto.dashboard

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModelProvider
import androidx.preference.Preference
import com.android.car.ui.preference.CarUiPreference
import com.android.permissioncontroller.Constants
import com.android.permissioncontroller.PermissionControllerStatsLog
import com.android.permissioncontroller.PermissionControllerStatsLog.PERMISSION_USAGE_FRAGMENT_INTERACTION
import com.android.permissioncontroller.PermissionControllerStatsLog.PERMISSION_USAGE_FRAGMENT_INTERACTION__ACTION__SHOW_SYSTEM_CLICKED
import com.android.permissioncontroller.R
import com.android.permissioncontroller.auto.AutoSettingsFrameFragment
import com.android.permissioncontroller.flags.Flags
import com.android.permissioncontroller.permission.ui.model.v31.PermissionUsageControlPreferenceUtils
import com.android.permissioncontroller.permission.ui.viewmodel.v31.PermissionUsageViewModel
import com.android.permissioncontroller.permission.ui.viewmodel.v31.PermissionUsageViewModelFactory
import com.android.permissioncontroller.permission.ui.viewmodel.v31.PermissionUsagesUiState

@RequiresApi(Build.VERSION_CODES.S)
class AutoPermissionUsageFragment : AutoSettingsFrameFragment() {

    companion object {
        private val LOG_TAG = AutoPermissionUsageFragment::class.simpleName
        private const val KEY_SESSION_ID = "_session_id"
        private val PERMISSION_GROUP_ORDER: Map<String, Int> =
            java.util.Map.of(
                Manifest.permission_group.LOCATION,
                0,
                Manifest.permission_group.CAMERA,
                1,
                Manifest.permission_group.MICROPHONE,
                2,
            )
        private const val DEFAULT_ORDER: Int = 3
        private const val COLLAPSED_ITEM_COUNT = 3
    }

    private val SESSION_ID_KEY = (AutoPermissionUsageFragment::class.java.name + KEY_SESSION_ID)

    private var showSystem = false
    private var hasSystemApps = false
    private var isExpanded = false

    /** Unique Id of a request */
    private var sessionId: Long = 0
    private lateinit var mViewModel: PermissionUsageViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        headerLabel = getString(R.string.permission_usage_title)
        sessionId =
            savedInstanceState?.getLong(SESSION_ID_KEY)
                ?: (arguments?.getLong(Constants.EXTRA_SESSION_ID, Constants.INVALID_SESSION_ID)
                    ?: Constants.INVALID_SESSION_ID)

        val factory = PermissionUsageViewModelFactory(requireActivity().application)
        mViewModel = ViewModelProvider(this, factory)[PermissionUsageViewModel::class.java]
        mViewModel.permissionUsagesUiLiveData.observe(this, this::updateAllUI)
        setLoading(true)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong(SESSION_ID_KEY, sessionId)
    }

    override fun onCreatePreferences(bundlle: Bundle?, s: String?) {
        preferenceScreen = preferenceManager.createPreferenceScreen(requireContext())
    }

    private fun updateSystemToggle() {
        if (!showSystem) {
            PermissionControllerStatsLog.write(
                PERMISSION_USAGE_FRAGMENT_INTERACTION,
                sessionId,
                PERMISSION_USAGE_FRAGMENT_INTERACTION__ACTION__SHOW_SYSTEM_CLICKED,
            )
        }
        showSystem = !showSystem
        updateAction()
    }

    /** Creates Show/Hide system button if necessary. */
    private fun updateAction() {
        if (!hasSystemApps) {
            setAction(null, null)
            return
        }
        val label =
            if (showSystem) {
                getString(R.string.menu_hide_system)
            } else {
                getString(R.string.menu_show_system)
            }
        setAction(label) {
            mViewModel.updateShowSystem(!showSystem)
            updateSystemToggle()
        }
    }

    private fun updateAllUI(uiData: PermissionUsagesUiState) {
        Log.v(LOG_TAG, "Privacy dashboard data = $uiData")
        if (activity == null || uiData is PermissionUsagesUiState.Loading) {
            return
        }
        val successData = uiData as? PermissionUsagesUiState.Success
        if (successData == null) {
            Log.w(LOG_TAG, "UI state is not Success, returning")
            setLoading(false)
            return
        }

        getPreferenceScreen().removeAll()

        if (hasSystemApps != successData.containsSystemAppUsage) {
            hasSystemApps = successData.containsSystemAppUsage
            updateAction()
        }

        val permissionGroupWithUsageCounts = successData.permissionGroupUsageCount
        val permissionGroupWithUsageCountsEntries = permissionGroupWithUsageCounts.entries.toList()

        val sortedEntries = permissionGroupWithUsageCountsEntries.sortedWith(
            Comparator.comparingInt { permissionGroupWithUsageCount: Map.Entry<String, Int> ->
                PERMISSION_GROUP_ORDER.getOrDefault(
                    permissionGroupWithUsageCount.key,
                    DEFAULT_ORDER,
                )
            }.thenComparing { permissionGroupWithUsageCount: Map.Entry<String, Int> ->
                mViewModel.getPermissionGroupLabel(
                    requireContext(),
                    permissionGroupWithUsageCount.key,
                )
            }
        )

        val shouldCollapse = !isExpanded && sortedEntries.size > COLLAPSED_ITEM_COUNT
        if (shouldCollapse && Flags.automotivePrivacyDashboardAgentActivityEnabled()) {
            for (i in 0 until COLLAPSED_ITEM_COUNT) {
                addPermissionPreference(sortedEntries[i])
            }
            addExpandPreference()
        } else {
            for (entry in sortedEntries) {
                addPermissionPreference(entry)
            }
        }
        setLoading(false)
    }

    private fun addPermissionPreference(entry: Map.Entry<String, Int>) {
        val permissionUsagePreference = CarUiPreference(requireContext())
        PermissionUsageControlPreferenceUtils.initPreference(
            permissionUsagePreference,
            requireContext(),
            entry.key,
            entry.value,
            showSystem,
            sessionId,
            false,
        )
        getPreferenceScreen().addPreference(permissionUsagePreference)
    }

    private fun addExpandPreference() {
        val expandPreference = CarUiPreference(requireContext())
        expandPreference.title = getString(R.string.perm_usage_adv_info_title)
        expandPreference.setIcon(R.drawable.ic_expand_more)
        expandPreference.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            isExpanded = true
            mViewModel.permissionUsagesUiLiveData.value?.let { updateAllUI(it) }
            true
        }
        getPreferenceScreen().addPreference(expandPreference)
    }
}
