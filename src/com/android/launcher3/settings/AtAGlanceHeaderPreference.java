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

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.android.launcher3.R;

/**
 * Top header banner in At a Glance settings explaining how content is personalized,
 * with a clickable "Learn more about how At a Glance personalises your content" link.
 */
public class AtAGlanceHeaderPreference extends Preference {

    public static final String LEARN_MORE_URL =
            "https://support.google.com/pixelphone/answer/14884067";

    public AtAGlanceHeaderPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    public AtAGlanceHeaderPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public AtAGlanceHeaderPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public AtAGlanceHeaderPreference(Context context) {
        super(context);
        init();
    }

    private void init() {
        setLayoutResource(R.layout.preference_at_a_glance_header);
        setSelectable(false);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        TextView textView = (TextView) holder.findViewById(R.id.at_a_glance_info_text);
        if (textView == null) {
            return;
        }

        Context context = getContext();
        String desc = context.getString(R.string.at_a_glance_info_description);
        String learnMore = context.getString(R.string.at_a_glance_info_learn_more);
        String fullText = desc + " " + learnMore;

        SpannableString spannable = new SpannableString(fullText);
        int linkStart = desc.length() + 1;
        int linkEnd = fullText.length();

        spannable.setSpan(new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(LEARN_MORE_URL))
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intent);
                } catch (Exception ignored) {}
            }

            @Override
            public void updateDrawState(@NonNull TextPaint ds) {
                super.updateDrawState(ds);
                ds.setColor(textView.getCurrentTextColor());
                ds.setUnderlineText(true);
            }
        }, linkStart, linkEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        textView.setText(spannable);
        textView.setMovementMethod(LinkMovementMethod.getInstance());
        textView.setHighlightColor(Color.TRANSPARENT);
    }
}
