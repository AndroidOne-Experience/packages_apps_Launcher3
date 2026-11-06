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

package com.android.launcher3.qsb;

import android.app.smartspace.SmartspaceTarget;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Rect;
import android.net.Uri;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.UnderlineSpan;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.android.launcher3.AbstractFloatingView;
import com.android.launcher3.Insettable;
import com.android.launcher3.Launcher;
import com.android.launcher3.R;
import com.android.launcher3.settings.AtAGlancePreference;
import com.android.launcher3.views.AbstractSlideInView;

/**
 * Bottom sheet dialog displaying information about Smartspace content sources,
 * matching the Pixel Launcher "About this content" experience.
 */
public class AboutSmartspaceBottomSheet extends AbstractSlideInView<Launcher> implements Insettable {

    private static final String LEARN_MORE_URL =
            "https://support.google.com/pixelphone/answer/14884067";
    private static final int DEFAULT_CLOSE_DURATION = 200;

    private final Rect mInsets = new Rect();
    private ImageView mFeatureIcon;
    private ImageView mSourceIcon;
    private TextView mSourceTitle;
    private TextView mSourceLearnMore;
    private TextView mNoticeText;
    private View mCustomiseButton;
    private View mCloseButton;
    private int mDefaultPaddingBottom;

    public AboutSmartspaceBottomSheet(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AboutSmartspaceBottomSheet(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContent = this;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mContent = findViewById(R.id.about_smartspace_content);
        mDefaultPaddingBottom = mContent.getPaddingBottom();

        mFeatureIcon = findViewById(R.id.about_feature_icon);
        mSourceIcon = findViewById(R.id.about_source_icon);
        mSourceTitle = findViewById(R.id.about_source_title);
        mSourceLearnMore = findViewById(R.id.about_source_learn_more);
        mNoticeText = findViewById(R.id.about_customise_notice);
        mCustomiseButton = findViewById(R.id.about_customise_button);
        mCloseButton = findViewById(R.id.about_close_button);

        mCloseButton.setOnClickListener(v -> handleClose(true));
        mCustomiseButton.setOnClickListener(v -> {
            handleClose(true);
            AtAGlancePreference.startAtAGlanceSettings(mActivityContext);
        });
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        setTranslationShift(mTranslationShift);
    }

    public void populateContent(SmartspaceTarget target) {
        int featureType = target != null ? target.getFeatureType() : 1;

        switch (featureType) {
            case 2: /* FEATURE_CALENDAR */
            case 6: /* FEATURE_REMINDER */
            case 26: /* FEATURE_UPCOMING_REMINDER */
                mFeatureIcon.setImageResource(R.drawable.ic_smartspace_upcoming);
                mSourceIcon.setImageResource(R.drawable.ic_smartspace_upcoming);
                mSourceTitle.setText(R.string.about_smartspace_source_calendar);
                break;
            case 3: /* FEATURE_COMMUTE_TIME */
            case 18: /* FEATURE_ETA_MONITORING */
            case 32: /* FEATURE_COMMUTE_WEATHER */
            case 38: /* FEATURE_TIME_TO_LEAVE */
                mFeatureIcon.setImageResource(R.drawable.ic_smartspace_commute);
                mSourceIcon.setImageResource(R.drawable.ic_location_crosshair);
                mSourceTitle.setText(R.string.about_smartspace_source_maps);
                break;
            case 4: /* FEATURE_FLIGHT */
                mFeatureIcon.setImageResource(R.drawable.ic_smartspace_travel);
                mSourceIcon.setImageResource(R.drawable.ic_smartspace_travel);
                mSourceTitle.setText(R.string.about_smartspace_source_gmail);
                break;
            case 9: /* FEATURE_SPORTS */
                mFeatureIcon.setImageResource(R.drawable.ic_smartspace_sports);
                mSourceIcon.setImageResource(R.drawable.ic_smartspace_sports);
                mSourceTitle.setText(R.string.about_smartspace_source_search);
                break;
            case 12: /* FEATURE_STOCK_PRICE_CHANGE */
                mFeatureIcon.setImageResource(R.drawable.ic_smartspace_finance);
                mSourceIcon.setImageResource(R.drawable.ic_smartspace_finance);
                mSourceTitle.setText(R.string.about_smartspace_source_finance);
                break;
            default: /* Weather and other default items */
                mFeatureIcon.setImageResource(R.drawable.ic_smartspace_weather);
                mSourceIcon.setImageResource(R.drawable.ic_location_crosshair);
                mSourceTitle.setText(R.string.about_smartspace_location_for_weather);
                break;
        }

        // Setup source "Learn more" clickable link
        String learnMoreStr = getContext().getString(R.string.about_smartspace_learn_more);
        SpannableString sourceLearnMoreSpan = new SpannableString(learnMoreStr);
        sourceLearnMoreSpan.setSpan(new UnderlineSpan(), 0, learnMoreStr.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        mSourceLearnMore.setText(sourceLearnMoreSpan);
        mSourceLearnMore.setOnClickListener(v -> openLearnMore());

        // Setup bottom notice with inline "Learn more" link
        String noticePrefix = getContext().getString(R.string.about_smartspace_customise_notice_prefix);
        String fullNotice = noticePrefix + " " + learnMoreStr;
        SpannableString noticeSpan = new SpannableString(fullNotice);
        int linkStart = noticePrefix.length() + 1;
        int linkEnd = fullNotice.length();
        noticeSpan.setSpan(new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {
                openLearnMore();
            }

            @Override
            public void updateDrawState(@NonNull TextPaint ds) {
                super.updateDrawState(ds);
                ds.setColor(Color.parseColor("#C4C7C5"));
                ds.setUnderlineText(true);
            }
        }, linkStart, linkEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        mNoticeText.setText(noticeSpan);
        mNoticeText.setMovementMethod(LinkMovementMethod.getInstance());
        mNoticeText.setHighlightColor(Color.TRANSPARENT);
    }

    private void openLearnMore() {
        try {
            Intent webIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(LEARN_MORE_URL))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            mActivityContext.startActivity(webIntent);
        } catch (Exception ignored) {
            handleClose(true);
            AtAGlancePreference.startAtAGlanceSettings(mActivityContext);
        }
    }

    public void animateOpen() {
        if (mIsOpen || mOpenCloseAnimation.getAnimationPlayer().isRunning()) {
            return;
        }
        mIsOpen = true;
        setUpDefaultOpenAnimation().start();
    }

    @Override
    protected void handleClose(boolean animate) {
        handleClose(animate, DEFAULT_CLOSE_DURATION);
    }

    @Override
    protected boolean isOfType(int type) {
        return (type & TYPE_ON_BOARD_POPUP) != 0;
    }

    @Override
    protected int getScrimColor(Context context) {
        return 0x88000000;
    }

    @Override
    public void setInsets(Rect insets) {
        mInsets.set(insets);
        mContent.setPadding(
                mContent.getPaddingLeft(),
                mContent.getPaddingTop(),
                mContent.getPaddingRight(),
                mDefaultPaddingBottom + insets.bottom
        );
    }

    /**
     * Shows the "About this content" bottom sheet on top of the given Launcher instance.
     */
    public static void show(Launcher launcher, SmartspaceTarget target) {
        AbstractFloatingView.closeAllOpenViews(launcher);
        LayoutInflater inflater = LayoutInflater.from(launcher);
        AboutSmartspaceBottomSheet sheet = (AboutSmartspaceBottomSheet) inflater.inflate(
                R.layout.about_smartspace_bottom_sheet, launcher.getDragLayer(), false);
        sheet.setTranslationShift(TRANSLATION_SHIFT_CLOSED);
        sheet.populateContent(target);
        sheet.attachToContainer();
        sheet.animateOpen();
    }
}
