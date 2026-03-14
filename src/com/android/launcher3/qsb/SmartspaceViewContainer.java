package com.android.launcher3.qsb;

import android.app.smartspace.SmartspaceAction;
import android.app.smartspace.SmartspaceTarget;
import android.app.smartspace.SmartspaceTargetEvent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
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

        mView = (BcSmartspaceView) inflate(context, R.layout.smartspace_enhanced, null);
        mView.setPrimaryTextColor(GraphicsUtils.getAttrColor(context, R.attr.workspaceTextColor));
        mLauncher = (QuickstepLauncher) ActivityContext.lookupContext(context);
        addView(mView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        updateSmartspaceLayout(mLauncher.getDeviceProfile());
        mLauncher.getLauncherUnlockAnimationController().setSmartspaceView(mView);

        CustomLauncherModelDelegate delegate =
                (CustomLauncherModelDelegate) mLauncher.getModel().getModelDelegate();
        BcSmartspaceDataProvider plugin = mLauncher.getSmartspacePlugin();
        plugin.registerSmartspaceEventNotifier(event -> delegate.notifySmartspaceEvent(event));
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

        int marginStart = getResources().getDimensionPixelSize(
                R.dimen.enhanced_smartspace_margin_start_launcher);
        int defaultWorkspaceMargin =
                getResources().getDimensionPixelSize(R.dimen.dynamic_grid_left_right_margin);
        int extraWorkspaceStartInset =
                Math.max(0, deviceProfile.desiredWorkspaceHorizontalMarginPx
                        - defaultWorkspaceMargin);
        marginStart = Math.max(0, marginStart - extraWorkspaceStartInset);

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
