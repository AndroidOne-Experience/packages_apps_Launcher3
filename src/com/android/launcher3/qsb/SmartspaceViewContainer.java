package com.android.launcher3.qsb;

import android.app.PendingIntent;
import android.app.smartspace.SmartspaceAction;
import android.app.smartspace.SmartspaceTarget;
import android.app.smartspace.SmartspaceTargetEvent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;

import androidx.core.content.ContextCompat;

import com.android.launcher3.AbstractFloatingView;
import com.android.launcher3.DeviceProfile.OnDeviceProfileChangeListener;
import com.android.launcher3.Utilities;
import com.android.launcher3.uioverrides.QuickstepLauncher;
import com.android.launcher3.CustomLauncherModelDelegate;

import com.android.launcher3.celllayout.CellLayoutLayoutParams;
import com.android.launcher3.DeviceProfile;
import com.android.launcher3.R;
import com.android.launcher3.icons.GraphicsUtils;
import com.android.launcher3.settings.SettingsActivity;
import com.android.launcher3.util.Executors;
import com.android.launcher3.util.PluginManagerWrapper;
import com.android.launcher3.views.ActivityContext;
import com.android.launcher3.views.OptionsPopupView;
import com.android.launcher3.views.OptionsPopupView.OptionItem;
import static com.android.launcher3.logging.StatsLogManager.LauncherEvent.IGNORE;
import com.android.quickstep.SystemUiProxy;

import com.android.systemui.plugins.BcSmartspaceDataPlugin;
import com.android.systemui.plugins.PluginListener;

import com.google.android.systemui.smartspace.BcSmartspaceDataProvider;
import com.google.android.systemui.smartspace.BcSmartspaceEvent;
import com.google.android.systemui.smartspace.BcSmartspaceView;
import com.google.android.systemui.smartspace.SmartspaceCard;
import com.google.android.systemui.smartspace.logging.BcSmartspaceCardLogger;

import java.util.ArrayList;
import java.util.List;

public class SmartspaceViewContainer extends FrameLayout implements
        PluginListener<BcSmartspaceDataPlugin>, OnDeviceProfileChangeListener, View.OnLongClickListener {

    public BcSmartspaceView mView;
    private final QuickstepLauncher mLauncher;

    public SmartspaceViewContainer(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        setClipChildren(false);
        setClipToPadding(false);

        mView = (BcSmartspaceView) inflate(context, R.layout.smartspace_enhanced, null);
        mView.setHorizontalPaddings(0);
        mView.setUiSurface(BcSmartspaceDataPlugin.UI_SURFACE_HOME_SCREEN);
        mView.setBgHandler(Executors.UI_HELPER_EXECUTOR.getHandler());
        mView.setPrimaryTextColor(GraphicsUtils.getAttrColor(context, R.attr.workspaceTextColor));
        mLauncher = (QuickstepLauncher) ActivityContext.lookupContext(context);
        addView(mView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        updateSmartspaceLayout(mLauncher.getDeviceProfile());
        mLauncher.getLauncherUnlockAnimationController().setSmartspaceView(mView);

        CustomLauncherModelDelegate delegate =
                (CustomLauncherModelDelegate) mLauncher.getModel().getModelDelegate();
        BcSmartspaceDataProvider plugin = mLauncher.getSmartspacePlugin();
        plugin.setContext(context);
        plugin.registerSmartspaceEventNotifier(new BcSmartspaceDataPlugin.SmartspaceEventNotifier() {
            @Override
            public void notifySmartspaceEvent(SmartspaceTargetEvent event) {
                delegate.notifySmartspaceEvent(event);
            }

            @Override
            public BcSmartspaceDataPlugin.IntentStarter getIntentStarter() {
                return null;
            }
        });
        mView.registerDataProvider(plugin);
        mView.setOnLongClickListener(this);
        setOnLongClickListener(this);
    }

    private void updateSmartspaceLayout(DeviceProfile deviceProfile) {
        LayoutParams layoutParams = (LayoutParams) mView.getLayoutParams();
        boolean needsLayoutParamsUpdate = layoutParams == null;
        if (layoutParams == null) {
            layoutParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        }
        int gravity = Gravity.TOP | Gravity.START;
        if (layoutParams.gravity != gravity) {
            layoutParams.gravity = gravity;
            needsLayoutParamsUpdate = true;
        }

        // Align Smartspace start with the status bar clock padding
        int statusBarPaddingStart = 0;
        try {
            Resources sysUiRes = getContext().getPackageManager()
                    .getResourcesForApplication("com.android.systemui");
            int resId = sysUiRes.getIdentifier("status_bar_padding_start", "dimen", "com.android.systemui");
            if (resId > 0) {
                statusBarPaddingStart = sysUiRes.getDimensionPixelSize(resId);
            }
        } catch (Exception ignored) {}

        if (statusBarPaddingStart == 0) {
            try {
                int resId = getResources().getIdentifier("status_bar_padding_start", "dimen", "android");
                if (resId > 0) {
                    statusBarPaddingStart = getResources().getDimensionPixelSize(resId);
                }
            } catch (Exception ignored) {}
        }

        int roundedCornerPadding = 0;
        try {
            int resId = Resources.getSystem()
                    .getIdentifier("rounded_corner_content_padding", "dimen", "android");
            if (resId > 0) {
                roundedCornerPadding = Resources.getSystem().getDimensionPixelSize(resId);
            }
        } catch (Exception ignored) {}

        int insetsLeft = (deviceProfile != null && deviceProfile.getInsets() != null)
                ? deviceProfile.getInsets().left
                : 0;
        int clockStart = Math.max(insetsLeft, roundedCornerPadding) + statusBarPaddingStart;

        int workspaceStart = (deviceProfile != null && deviceProfile.getWorkspaceIconProfile() != null)
                ? deviceProfile.getWorkspaceIconProfile().getDesiredWorkspaceHorizontalMarginPx()
                : 0;

        int marginStart;
        if (clockStart > 0) {
            marginStart = clockStart - workspaceStart;
        } else {
            marginStart = getResources().getDimensionPixelSize(
                    R.dimen.enhanced_smartspace_margin_start_launcher);
        }

        if (layoutParams.getMarginStart() != marginStart) {
            layoutParams.setMarginStart(marginStart);
            needsLayoutParamsUpdate = true;
        }

        int marginEnd = marginStart;
        if (layoutParams.getMarginEnd() != marginEnd) {
            layoutParams.setMarginEnd(marginEnd);
            needsLayoutParamsUpdate = true;
        }

        if (needsLayoutParamsUpdate) {
            mView.setLayoutParams(layoutParams);
        }
    }

    @Override
    public void onPluginConnected(BcSmartspaceDataPlugin plugin, Context context) {
        mView.registerDataProvider(plugin);
        mView.setOnLongClickListener(this);
        setOnLongClickListener(this);
    }

    @Override
    public void onPluginDisconnected(BcSmartspaceDataPlugin plugin) {
        mView.registerDataProvider(mLauncher.getSmartspacePlugin());
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        mLauncher.addOnDeviceProfileChangeListener(this);
        updateSmartspaceLayout(mLauncher.getDeviceProfile());
        PluginManagerWrapper.INSTANCE.get(getContext()).addPluginListener(this, BcSmartspaceDataPlugin.class);
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mLauncher.removeOnDeviceProfileChangeListener(this);
        PluginManagerWrapper.INSTANCE.get(getContext()).removePluginListener(this);
    }

    @Override
    public void onDeviceProfileChanged(DeviceProfile dp) {
        updateSmartspaceLayout(dp);
    }

    @Override
    public void setPadding(int left, int top, int right, int bottom) {
        CellLayoutLayoutParams lp = (CellLayoutLayoutParams) getLayoutParams();
        lp.setMargins(left, top, right, bottom);
        setLayoutParams(lp);
    }

    @Override
    public boolean onLongClick(View v) {
        return handleSmartspaceLongClick(v);
    }

    private boolean handleSmartspaceLongClick(View v) {
        if (mLauncher == null || mLauncher.isWorkspaceLocked()) {
            return false;
        }

        SmartspaceTarget target = mView.getCurrentTarget();
        boolean isEvent = isEventTarget(target);

        ArrayList<OptionItem> options = new ArrayList<>();
        if (isEvent) {
            options.add(new OptionItem(
                    getContext().getText(R.string.smartspace_dismiss),
                    ContextCompat.getDrawable(getContext(), R.drawable.ic_smartspace_dismiss),
                    IGNORE,
                    view -> {
                        dismissTarget(target);
                        return true;
                    }));
        }

        options.add(new OptionItem(
                getContext().getText(R.string.smartspace_settings),
                ContextCompat.getDrawable(getContext(), R.drawable.ic_setting),
                IGNORE,
                view -> {
                    openSmartspaceSettings();
                    return true;
                }));

        options.add(new OptionItem(
                getContext().getText(R.string.smartspace_about),
                ContextCompat.getDrawable(getContext(), R.drawable.ic_smartspace_about),
                IGNORE,
                view -> {
                    openAboutContent(target);
                    return true;
                }));

        AbstractFloatingView.closeAllOpenViews(mLauncher);

        View cardView = mView.getCurrentCardView();
        View anchorView = cardView != null ? cardView : mView;
        Rect pos = new Rect();
        mLauncher.getDragLayer().getDescendantRectRelativeToSelf(anchorView, pos);
        RectF targetRect = new RectF(pos);
        float centerX = targetRect.centerX();
        targetRect.left = centerX;
        targetRect.right = centerX;

        OptionsPopupView.show(mLauncher, targetRect, options, true);
        return true;
    }

    private void dismissTarget(SmartspaceTarget target) {
        if (target == null) {
            return;
        }

        int position = mView.getSelectedPage();
        SmartspaceCard card = mView.mAdapter != null ? mView.mAdapter.getCardAtPosition(position) : null;
        if (card != null && card.getLoggingInfo() != null) {
            BcSmartspaceCardLogger.log(BcSmartspaceEvent.SMARTSPACE_CARD_DISMISS, card.getLoggingInfo());
        }

        if (target.getBaseAction() != null && target.getBaseAction().getExtras() != null) {
            Bundle extras = target.getBaseAction().getExtras();
            PendingIntent dismissPi = extras.getParcelable("dismiss_intent", PendingIntent.class);
            if (dismissPi == null) {
                dismissPi = extras.getParcelable("dismissIntent", PendingIntent.class);
            }
            if (dismissPi != null) {
                try {
                    dismissPi.send();
                } catch (Exception ignored) {}
            }
        }

        String actionId = target.getBaseAction() != null ? target.getBaseAction().getId() : target.getSmartspaceTargetId();
        SmartspaceTargetEvent event = new SmartspaceTargetEvent.Builder(
                SmartspaceTargetEvent.EVENT_TARGET_DISMISS)
                .setSmartspaceTarget(target)
                .setSmartspaceActionId(actionId)
                .build();
        BcSmartspaceDataProvider plugin = mLauncher.getSmartspacePlugin();
        if (plugin != null) {
            plugin.setContext(getContext());
            plugin.notifySmartspaceEvent(event);
            plugin.dismissTarget(target);
        }
    }

    private void openSmartspaceSettings() {
        Context context = getContext();
        String packageName = mLauncher != null ? mLauncher.getPackageName() : context.getPackageName();
        Intent launcherSettings = new Intent(Intent.ACTION_APPLICATION_PREFERENCES)
                .setPackage(packageName)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        try {
            if (mLauncher != null) {
                mLauncher.startActivity(launcherSettings);
            } else {
                context.startActivity(launcherSettings);
            }
        } catch (Exception e) {
            try {
                Intent fallback = new Intent(context, SettingsActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                context.startActivity(fallback);
            } catch (Exception ignored) {}
        }
    }

    private void openAboutContent(SmartspaceTarget target) {
        post(() -> {
            QuickstepLauncher launcher = mLauncher != null ? mLauncher : ActivityContext.lookupContext(getContext());
            if (launcher != null) {
                AboutSmartspaceBottomSheet.show(launcher, target);
            }
        });
    }

    public static boolean isEventTarget(SmartspaceTarget target) {
        if (target == null) {
            return false;
        }
        int featureType = target.getFeatureType();
        // Weather and default date card are not events and must never show dismiss
        if (featureType == SmartspaceTarget.FEATURE_WEATHER || featureType == SmartspaceTarget.FEATURE_UNDEFINED) {
            return false;
        }
        String id = target.getSmartspaceTargetId();
        if (id != null && (id.contains("date_card") || id.contains("weather"))) {
            return false;
        }
        // Explicitly check for Calendar event (FEATURE_CALENDAR = 2) or other event types (reminder, flight)
        if (featureType == SmartspaceTarget.FEATURE_CALENDAR
                || featureType == 6 /* FEATURE_REMINDER */
                || featureType == 4 /* FEATURE_FLIGHT */) {
            return true;
        }
        // Also check template data if primary item logging info indicates calendar/event
        if (target.getTemplateData() != null && target.getTemplateData().getPrimaryItem() != null
                && target.getTemplateData().getPrimaryItem().getLoggingInfo() != null) {
            int templateType = target.getTemplateData().getPrimaryItem().getLoggingInfo().getFeatureType();
            if (templateType == SmartspaceTarget.FEATURE_CALENDAR
                    || templateType == 6 /* FEATURE_REMINDER */
                    || templateType == 4 /* FEATURE_FLIGHT */) {
                return true;
            }
            if (templateType == SmartspaceTarget.FEATURE_WEATHER) {
                return false;
            }
        }
        return featureType == SmartspaceTarget.FEATURE_CALENDAR;
    }
}
