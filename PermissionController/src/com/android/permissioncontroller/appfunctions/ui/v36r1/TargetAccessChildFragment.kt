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
package com.android.permissioncontroller.appfunctions.ui.v36r1

import android.os.Bundle
import android.os.Process
import androidx.fragment.app.Fragment
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.android.permissioncontroller.R
import com.android.permissioncontroller.permission.utils.KotlinUtils

/**
 * Child fragment for modifying agent access of target apps.
 *
 * <p>
 * Must be added as a child fragment and its parent fragment must be a {@link
 * PreferenceFragmentCompat} that implements {@link Parent}.
 *
 * @param <PF> type of the parent fragment
 */
class TargetAccessChildFragment<PF>(val targetPackage: String) :
    Fragment(), Preference.OnPreferenceClickListener where
PF : PreferenceFragmentCompat,
PF : TargetAccessChildFragment.Parent {
    @Suppress("DEPRECATION")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val preferenceFragment = requirePreferenceFragment()
        preferenceFragment.setTitle(
            getString(
                R.string.app_function_target_access_title,
                KotlinUtils.getPackageLabel(
                    requireActivity().application,
                    targetPackage,
                    Process.myUserHandle(),
                ),
            )
        )
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

        /** Creates a new preference for a target app. */
        fun createPreference(): Preference

        /**
         * Callback when changes have been made to the {@link PreferenceScreen} of the parent {@link
         * PreferenceFragmentCompat}.
         */
        fun onPreferenceScreenChanged()
    }

    companion object {
        /**
         * Create a new instance of TargetAccessChildFragment
         *
         * @return a new instance of TargetAccessChildFragment
         */
        fun newInstance(targetPackage: String): TargetAccessChildFragment<*> =
            TargetAccessChildFragment<Nothing>(targetPackage)
    }
}
