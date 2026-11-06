package com.android.launcher3.qsb;

import android.app.smartspace.SmartspaceAction;
import android.app.smartspace.SmartspaceTarget;
import android.app.smartspace.SmartspaceTargetEvent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;

import com.android.launcher3.DeviceProfile.OnDeviceProfileChangeListener;
import com.android.launcher3.uioverrides.QuickstepLauncher;
import com.android.launcher3.CustomLauncherModelDelegate;

import com.android.launcher3.celllayout.CellLayoutLayoutParams;
import com.android.launcher3.DeviceProfile;
import com.android.launcher3.R;
import com.android.launcher3.icons.GraphicsUtils;
import com.android.launcher3.util.Executors;
import com.android.launcher3.util.PluginManagerWrapper;
import com.android.launcher3.views.ActivityContext;
import com.android.quickstep.SystemUiProxy;

import com.android.systemui.plugins.BcSmartspaceDataPlugin;
import com.android.systemui.plugins.PluginListener;

import com.google.android.systemui.smartspace.BcSmartspaceDataProvider;
import com.google.android.systemui.smartspace.BcSmartspaceView;

import java.util.ArrayList;
import java.util.List;

public class SmartspaceViewContainer extends FrameLayout implements
        PluginListener<BcSmartspaceDataPlugin>, OnDeviceProfileChangeListener {

    public BcSmartspaceView mView;
    private final QuickstepLauncher mLauncher;

    public SmartspaceViewContainer(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        setClipChildren(false);
        setClipToPadding(false);

        mView = (BcSmartspaceView) inflate(context, R.layout.smartspace_enhanced, null);
        mView.setClipChildren(false);
        mView.setClipToPadding(false);
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

        if (needsLayoutParamsUpdate) {
            mView.setLayoutParams(layoutParams);
        }
    }

    @Override
    public void onPluginConnected(BcSmartspaceDataPlugin plugin, Context context) {
        mView.registerDataProvider(plugin);
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
}
