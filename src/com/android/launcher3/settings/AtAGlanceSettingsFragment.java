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

import android.app.Activity;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;

import com.android.launcher3.LauncherFiles;
import com.android.launcher3.LauncherPrefs;
import com.android.launcher3.R;

/**
 * Settings fragment for configuring At a Glance features and providers.
 */
public class AtAGlanceSettingsFragment extends PreferenceFragmentCompat {

    private static final String KEY_CUSTOMISE_CATEGORY = "category_customise";

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        getPreferenceManager().setSharedPreferencesName(LauncherFiles.SHARED_PREFERENCES_KEY);
        setPreferencesFromResource(R.xml.at_a_glance_preferences, rootKey);
        updateActivityHeader();
        setupMasterSwitch();
    }

    private void setupMasterSwitch() {
        SwitchPreferenceCompat useAtAGlancePref =
                findPreference(LauncherPrefs.SMARTSPACE_ON_HOME_SCREEN_KEY);
        PreferenceCategory customiseCategory = findPreference(KEY_CUSTOMISE_CATEGORY);

        if (useAtAGlancePref != null && customiseCategory != null) {
            boolean isEnabled = useAtAGlancePref.isChecked();
            setCategoryEnabled(customiseCategory, isEnabled);

            useAtAGlancePref.setOnPreferenceChangeListener((preference, newValue) -> {
                boolean enabled = (Boolean) newValue;
                setCategoryEnabled(customiseCategory, enabled);
                return true;
            });
        }
    }

    private void setCategoryEnabled(PreferenceCategory category, boolean enabled) {
        category.setEnabled(enabled);
        for (int i = 0; i < category.getPreferenceCount(); i++) {
            category.getPreference(i).setEnabled(enabled);
        }
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        View listView = getListView();
        final int bottomPadding = listView.getPaddingBottom();
        listView.setOnApplyWindowInsetsListener((v, insets) -> {
            v.setPadding(
                    v.getPaddingLeft(),
                    v.getPaddingTop(),
                    v.getPaddingRight(),
                    bottomPadding + insets.getSystemWindowInsetBottom());
            return insets.consumeSystemWindowInsets();
        });
        view.setTextDirection(View.TEXT_DIRECTION_LOCALE);
    }

    @Override
    public void onResume() {
        super.onResume();
        updateActivityHeader();

        SwitchPreferenceCompat useAtAGlancePref =
                findPreference(LauncherPrefs.SMARTSPACE_ON_HOME_SCREEN_KEY);
        PreferenceCategory customiseCategory = findPreference(KEY_CUSTOMISE_CATEGORY);
        if (useAtAGlancePref != null && getContext() != null) {
            boolean isEnabled = LauncherPrefs.get(getContext()).get(LauncherPrefs.SMARTSPACE_ON_HOME_SCREEN);
            useAtAGlancePref.setChecked(isEnabled);
            if (customiseCategory != null) {
                setCategoryEnabled(customiseCategory, isEnabled);
            }
        }
    }

    private void updateActivityHeader() {
        Activity activity = getActivity();
        if (activity != null) {
            activity.setTitle(R.string.at_a_glance_title);
            if (activity.getActionBar() != null) {
                activity.getActionBar().setDisplayHomeAsUpEnabled(true);
            }
        }
    }
}
