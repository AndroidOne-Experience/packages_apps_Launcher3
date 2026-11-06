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

package com.android.launcher3.settings;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.util.AttributeSet;
import android.view.View;

import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.android.launcher3.LauncherPrefChangeListener;
import com.android.launcher3.LauncherPrefs;
import com.android.launcher3.R;

/**
 * Preference for controlling At a Glance on the Home screen and accessing its customization options.
 */
public class AtAGlancePreference extends Preference {

    private final LauncherPrefChangeListener mPrefListener = key -> updateSummary();

    public AtAGlancePreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    public AtAGlancePreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public AtAGlancePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public AtAGlancePreference(Context context) {
        super(context);
        init();
    }

    private void init() {
        setWidgetLayoutResource(R.layout.preference_widget_gear);
        setPersistent(true);
    }

    @Override
    public void onAttached() {
        super.onAttached();
        LauncherPrefs.get(getContext()).addListener(mPrefListener, LauncherPrefs.SMARTSPACE_ON_HOME_SCREEN);
        updateSummary();
    }

    @Override
    public void onDetached() {
        super.onDetached();
        LauncherPrefs.get(getContext()).removeListener(mPrefListener, LauncherPrefs.SMARTSPACE_ON_HOME_SCREEN);
    }

    public void updateSummary() {
        boolean isEnabled = isSmartspaceEnabled();
        setSummary(isEnabled ? R.string.at_a_glance_summary_on : R.string.at_a_glance_summary_off);
    }

    private boolean isSmartspaceEnabled() {
        return LauncherPrefs.get(getContext()).get(LauncherPrefs.SMARTSPACE_ON_HOME_SCREEN);
    }

    private void setSmartspaceEnabled(boolean enabled) {
        LauncherPrefs.get(getContext()).put(LauncherPrefs.SMARTSPACE_ON_HOME_SCREEN, enabled);
        persistBoolean(enabled);
        updateSummary();
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);

        View widgetFrame = holder.findViewById(android.R.id.widget_frame);
        if (widgetFrame != null) {
            widgetFrame.setVisibility(View.VISIBLE);
            widgetFrame.setOnClickListener(v -> openCustomizationScreen());
        }

        View gearButton = holder.findViewById(R.id.gear_button);
        if (gearButton != null) {
            gearButton.setOnClickListener(v -> openCustomizationScreen());
        }

        updateSummary();
    }

    @Override
    protected void onClick() {
        if (isSmartspaceEnabled()) {
            showTurnOffDialog();
        } else {
            setSmartspaceEnabled(true);
        }
    }

    private void showTurnOffDialog() {
        new AlertDialog.Builder(getContext())
                .setTitle(R.string.at_a_glance_dialog_title)
                .setMessage(R.string.at_a_glance_dialog_message)
                .setNeutralButton(R.string.at_a_glance_customise, (dialog, which) -> {
                    openCustomizationScreen();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.at_a_glance_turn_off, (dialog, which) -> {
                    setSmartspaceEnabled(false);
                })
                .show();
    }

    public static void startAtAGlanceSettings(Context context) {
        Intent intent = new Intent(context, SettingsActivity.class);
        intent.putExtra(SettingsActivity.EXTRA_FRAGMENT, AtAGlanceSettingsFragment.class.getName());
        context.startActivity(intent);
    }

    public void openCustomizationScreen() {
        startAtAGlanceSettings(getContext());
    }
}
