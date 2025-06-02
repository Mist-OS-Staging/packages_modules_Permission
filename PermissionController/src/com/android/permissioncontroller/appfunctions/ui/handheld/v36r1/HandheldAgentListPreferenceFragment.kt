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
package com.android.permissioncontroller.appfunctions.ui.handheld.v36r1

import android.os.Bundle
import androidx.preference.Preference
import com.android.permissioncontroller.appfunctions.ui.handheld.v36r1.HandheldAgentListPreferenceFragment.Parent
import com.android.permissioncontroller.appfunctions.ui.v36r1.AgentListChildFragment
import com.android.settingslib.widget.SettingsBasePreferenceFragment

/**
 * Handheld preference fragment for the list of app function agents.
 *
 * Must be added as a child fragment and its parent fragment must implement [Parent].
 */
class HandheldAgentListPreferenceFragment :
    SettingsBasePreferenceFragment(), AgentListChildFragment.Parent {
    @Suppress("DEPRECATION")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        if (savedInstanceState == null) {
            val fragment = AgentListChildFragment.newInstance()
            childFragmentManager.beginTransaction().add(fragment, null).commit()
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        // Preferences will be added by the child fragment later.
    }

    override fun setTitle(title: CharSequence) {
        requireParent().setTitle(title)
    }

    override fun createPreference(): Preference {
        return Preference(requireContext())
    }

    override fun onPreferenceScreenChanged() {
        requireParent().onPreferenceScreenChanged()
    }

    private fun requireParent(): Parent {
        return requireParentFragment() as Parent
    }

    /** Interface that the parent fragment must implement. */
    interface Parent {
        /**
         * Set the title of the current settings page.
         *
         * @param title the title of the current settings page
         */
        fun setTitle(title: CharSequence)

        /**
         * Callback when changes have been made to the {@link androidx.preference.PreferenceScreen}
         * of this {@link PreferenceFragmentCompat}.
         */
        fun onPreferenceScreenChanged()
    }

    companion object {
        /**
         * Create a new instance of this fragment.
         *
         * @return a new instance of this fragment
         */
        @JvmStatic
        fun newInstance(): HandheldAgentListPreferenceFragment {
            return HandheldAgentListPreferenceFragment()
        }
    }
}
