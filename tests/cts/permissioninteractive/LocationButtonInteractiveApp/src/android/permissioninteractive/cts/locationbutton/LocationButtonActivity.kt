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

package android.permissioninteractive.cts.locationbutton

import android.app.Activity
import android.app.permissionui.LocationButtonRequest
import android.os.Bundle
import android.view.ViewGroup
import android.widget.FrameLayout

class LocationButtonActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val width = intent.getIntExtra(EXTRA_WIDTH, ViewGroup.LayoutParams.WRAP_CONTENT)
        val height = intent.getIntExtra(EXTRA_HEIGHT, ViewGroup.LayoutParams.WRAP_CONTENT)

        val locationButton = LocationButton(this)
        val buttonParams =
            FrameLayout.LayoutParams(width, height).apply {
                gravity = android.view.Gravity.BOTTOM or android.view.Gravity.CENTER_HORIZONTAL
                bottomMargin = (resources.displayMetrics.density * 16).toInt()
            }
        locationButton.layoutParams = buttonParams

        val config =
            android.content.res.Configuration(resources.configuration).apply {
                intent.getStringExtra(EXTRA_LANGUAGE_TAG)?.let { languageTag ->
                    setLocales(android.os.LocaleList.forLanguageTags(languageTag))
                }
            }
        val requestBuilder = LocationButtonRequest.Builder(width, height, config)

        val extras = intent.extras ?: Bundle.EMPTY
        for (key in extras.keySet()) {
            when (key) {
                EXTRA_BACKGROUND_COLOR -> requestBuilder.setBackgroundColor(extras.getInt(key))
                EXTRA_TEXT_COLOR -> requestBuilder.setTextColor(extras.getInt(key))
                EXTRA_ICON_TINT -> requestBuilder.setIconTint(extras.getInt(key))
                EXTRA_STROKE_COLOR -> requestBuilder.setStrokeColor(extras.getInt(key))
                EXTRA_STROKE_WIDTH -> requestBuilder.setStrokeWidth(extras.getInt(key))
                EXTRA_CORNER_RADIUS -> requestBuilder.setCornerRadius(extras.getFloat(key))
                EXTRA_PRESSED_CORNER_RADIUS ->
                    requestBuilder.setPressedCornerRadius(extras.getFloat(key))
                EXTRA_TEXT_TYPE -> requestBuilder.setTextType(extras.getInt(key))
            }
        }
        locationButton.setRequest(requestBuilder.build())

        val container =
            FrameLayout(this).apply {
                layoutParams =
                    ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                fitsSystemWindows = true
            }
        container.addView(locationButton)
        setContentView(container)
    }

    companion object {
        const val EXTRA_WIDTH = "width"
        const val EXTRA_HEIGHT = "height"
        const val EXTRA_BACKGROUND_COLOR = "background_color"
        const val EXTRA_TEXT_COLOR = "text_color"
        const val EXTRA_ICON_TINT = "icon_tint"
        const val EXTRA_STROKE_COLOR = "stroke_color"
        const val EXTRA_STROKE_WIDTH = "stroke_width"
        const val EXTRA_CORNER_RADIUS = "corner_radius"
        const val EXTRA_PRESSED_CORNER_RADIUS = "pressed_corner_radius"
        const val EXTRA_TEXT_TYPE = "text_type"
        const val EXTRA_LANGUAGE_TAG = "language_tag"
        const val LOCATION_BUTTON_CONTENT_DESCRIPTION = "location_button"
    }
}
