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
package com.android.permissioncontroller.appfunctions.ui

import android.content.Intent
import android.os.Bundle
import android.os.Process
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle.State
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceGroup
import androidx.preference.TwoStatePreference
import com.android.permissioncontroller.R
import com.android.permissioncontroller.permission.utils.KotlinUtils
import kotlinx.coroutines.launch

/**
 * Child fragment for modifying agent access of target apps.
 *
 * <p>
 * Must be added as a child fragment and its parent fragment must be a {@link
 * PreferenceFragmentCompat} that implements {@link Parent}.
 *
 * @param <PF> type of the parent fragment
 */
class TargetAccessChildFragment<PF>() : Fragment(), Preference.OnPreferenceClickListener where
PF : PreferenceFragmentCompat,
PF : TargetAccessChildFragment.Parent {
    private lateinit var targetPackageName: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        targetPackageName = arguments!!.getString(Intent.EXTRA_PACKAGE_NAME)!!

        val preferenceFragment = requirePreferenceFragment()
        preferenceFragment.setTitle(
            getString(
                R.string.app_function_target_access_title,
                KotlinUtils.getPackageLabel(
                    requireActivity().application,
                    targetPackageName,
                    Process.myUserHandle(),
                ),
            )
        )

        preferenceFragment.lifecycleScope.launch {
            preferenceFragment.lifecycle.repeatOnLifecycle(State.STARTED) { onUiStateChanged() }
        }
    }

    private fun onUiStateChanged() {
        val preferenceFragment = requirePreferenceFragment()
        val preferenceManager = preferenceFragment.preferenceManager
        var preferenceScreen = preferenceFragment.preferenceScreen
        val context = preferenceManager.context
        val oldPreferences = mutableMapOf<String, Preference>()
        if (preferenceScreen == null) {
            preferenceScreen = preferenceManager.createPreferenceScreen(context)
            preferenceFragment.preferenceScreen = preferenceScreen
        } else {
            clearPreferences(preferenceScreen, oldPreferences)
        }

        addHeaderPreference(preferenceScreen, oldPreferences)
        addEmptyStatePreference(preferenceScreen, oldPreferences)

        preferenceFragment.onPreferenceScreenChanged()
    }

    private fun clearPreferences(
        preferenceGroup: PreferenceGroup,
        oldPreferences: MutableMap<String, Preference>,
    ) {
        for (i in preferenceGroup.preferenceCount - 1 downTo 0) {
            val preference = preferenceGroup.getPreference(i)
            preferenceGroup.removePreference(preference)
            preference.order = Preference.DEFAULT_ORDER
            oldPreferences[preference.key] = preference
        }
    }

    private fun addHeaderPreference(
        preferenceGroup: PreferenceGroup,
        oldPreferences: Map<String, Preference>,
    ) {
        val targetIcon =
            KotlinUtils.getBadgedPackageIcon(
                requireActivity().application,
                targetPackageName,
                Process.myUserHandle(),
            )
        val label =
            KotlinUtils.getPackageLabel(
                requireActivity().application,
                targetPackageName,
                Process.myUserHandle(),
            )
        val preference =
            oldPreferences[PREFERENCE_KEY_INTRO]
                ?: requirePreferenceFragment().createHeaderPreference().apply {
                    key = PREFERENCE_KEY_INTRO
                    icon = targetIcon
                    title = label
                    summary = getString(R.string.app_function_target_access_summary, label)
                }
        preferenceGroup.addPreference(preference)
    }

    private fun addEmptyStatePreference(
        preferenceGroup: PreferenceGroup,
        oldPreferences: Map<String, Preference>,
    ) {
        val preference =
            oldPreferences[PREFERENCE_KEY_ZERO_STATE]
                ?: requirePreferenceFragment().createEmptyStatePreference().apply {
                    key = PREFERENCE_KEY_ZERO_STATE
                    setTitle(R.string.app_function_agent_list_empty_title)
                    setSummary(R.string.app_function_agent_list_empty_summary)
                }
        preferenceGroup.addPreference(preference)
    }

    override fun onPreferenceClick(preference: Preference): Boolean {
        // TODO(rmacgregor): Implement onPreferenceClick
        return true
    }

    private fun requirePreferenceFragment(): PF {
        @Suppress("UNCHECKED_CAST")
        return requireParentFragment() as PF
    }

    /** Interface that the parent fragment must implement. */
    interface Parent {
        /**
         * Set the title of the current settings page.
         *
         * @param title the title of the current settings page
         */
        fun setTitle(title: CharSequence)

        /** Creates a new header preference for the screen */
        fun createHeaderPreference(): Preference

        /** Creates a new empty state preference for the screen */
        fun createEmptyStatePreference(): Preference

        /** Creates a new preference for a target app. */
        fun createPreference(): TwoStatePreference

        /**
         * Callback when changes have been made to the {@link PreferenceScreen} of the parent {@link
         * PreferenceFragmentCompat}.
         */
        fun onPreferenceScreenChanged()
    }

    companion object {
        private val PREFERENCE_KEY_INTRO =
            TargetAccessChildFragment::class.java.name + ".preference.INTRO"
        private val PREFERENCE_KEY_ZERO_STATE =
            TargetAccessChildFragment::class.java.name + ".preference.ZERO_STATE"

        /**
         * Create a new instance of TargetAccessChildFragment
         *
         * @param targetPackageName target package to modify access for
         * @return a new instance of TargetAccessChildFragment
         */
        fun newInstance(targetPackageName: String): TargetAccessChildFragment<*> {
            val arguments =
                Bundle().apply { putString(Intent.EXTRA_PACKAGE_NAME, targetPackageName) }
            return TargetAccessChildFragment<Nothing>().apply { setArguments(arguments) }
        }
    }
}
