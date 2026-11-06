package com.google.android.systemui.smartspace;

import android.app.smartspace.SmartspaceTarget;
import android.app.smartspace.SmartspaceTargetEvent;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Debug;
import android.os.Process;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.android.systemui.bcsmartspace.R;

import com.android.systemui.plugins.BcSmartspaceConfigPlugin;
import com.android.systemui.plugins.BcSmartspaceDataPlugin;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;

public final class BcSmartspaceDataProvider implements BcSmartspaceDataPlugin {
    public static final boolean DEBUG = Log.isLoggable("BcSmartspaceDataPlugin", 3);

    private static final String PREFS_NAME = "com.android.launcher3.prefs";
    private static final String KEY_SMARTSPACE_ENABLED = "pref_smartspace_home_screen";

    private Context mContext;
    private SharedPreferences mPrefs;
    private List<SmartspaceTarget> mRawTargets = new ArrayList<>();

    private final SharedPreferences.OnSharedPreferenceChangeListener mPrefsListener =
            (prefs, key) -> {
                if (key != null && (key.startsWith("pref_smartspace_") || key.equals(KEY_SMARTSPACE_ENABLED))) {
                    filterAndNotifyTargets();
                }
            };

    public final View.OnAttachStateChangeListener mStateChangeListener;
    public final Set<BcSmartspaceDataPlugin.SmartspaceTargetListener> mSmartspaceTargetListeners = new CopyOnWriteArraySet<>();
    public final Set<String> mDismissedTargetIds = new HashSet<>();
    public List<SmartspaceTarget> mSmartspaceTargets = new ArrayList<>();
    public final Set<View> mViews = new HashSet<>();
    public final Set<View.OnAttachStateChangeListener> mAttachListeners = new HashSet<>();
    public final EventNotifierProxy mEventNotifier = new EventNotifierProxy();
    public BcSmartspaceConfigPlugin mConfigProvider = new DefaultBcSmartspaceConfigProvider();

    public final class StateChangeListener implements View.OnAttachStateChangeListener {
        @Override
        public void onViewAttachedToWindow(View view) {
            mViews.add(view);
            for (View.OnAttachStateChangeListener listener : mAttachListeners) {
                listener.onViewAttachedToWindow(view);
            }
            if (view instanceof BcSmartspaceView) {
                ((BcSmartspaceView) view).registerDataProvider(BcSmartspaceDataProvider.this);
            }
        }

        @Override
        public void onViewDetachedFromWindow(View view) {
            mViews.remove(view);
            for (View.OnAttachStateChangeListener listener : mAttachListeners) {
                listener.onViewDetachedFromWindow(view);
            }
        }
    }

    public BcSmartspaceDataProvider() {
        mStateChangeListener = new StateChangeListener();
    }

    public void setContext(Context context) {
        if (context == null) {
            if (mPrefs != null) {
                mPrefs.unregisterOnSharedPreferenceChangeListener(mPrefsListener);
                mPrefs = null;
            }
            mContext = null;
            return;
        }

        Context appContext = context.getApplicationContext();
        if (mContext != appContext) {
            if (mPrefs != null) {
                mPrefs.unregisterOnSharedPreferenceChangeListener(mPrefsListener);
            }
            mContext = appContext;
            mPrefs = mContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            if (mPrefs != null) {
                mPrefs.registerOnSharedPreferenceChangeListener(mPrefsListener);
            }
            filterAndNotifyTargets();
        }
    }

    @Override
    public void addOnAttachStateChangeListener(View.OnAttachStateChangeListener listener) {
        mAttachListeners.add(listener);
        for (View view : mViews) {
            listener.onViewAttachedToWindow(view);
        }
    }

    @Override
    public BcSmartspaceDataPlugin.SmartspaceEventNotifier getEventNotifier() {
        return mEventNotifier;
    }

    @Override
    public BcSmartspaceDataPlugin.SmartspaceView getView(Context context) {
        if (mContext == null && context != null) {
            setContext(context);
        }

        int layoutId = mConfigProvider.isViewPager2Enabled()
                ? R.layout.smartspace_enhanced2
                : R.layout.smartspace_enhanced;

        View view = LayoutInflater.from(context).inflate(layoutId, (ViewGroup) null, false);
        view.addOnAttachStateChangeListener(mStateChangeListener);

        // Explicitly register data provider.
        if (view instanceof BcSmartspaceView) {
            ((BcSmartspaceView) view).registerDataProvider(this);
        }

        return (BcSmartspaceDataPlugin.SmartspaceView) view;
    }

    @Override
    public void onTargetsAvailable(List<SmartspaceTarget> list) {
        if (DEBUG) {
            Log.d("BcSmartspaceDataPlugin", this + " onTargetsAvailable called. Callers = " + Debug.getCallers(3));
            Log.d("BcSmartspaceDataPlugin", "    targets.size() = " + list.size());
        }

        mRawTargets = list != null ? new ArrayList<>(list) : new ArrayList<>();
        filterAndNotifyTargets();
    }

    public void dismissTarget(SmartspaceTarget target) {
        if (target == null) {
            return;
        }
        mDismissedTargetIds.add(target.getSmartspaceTargetId());
        filterAndNotifyTargets();
    }

    private void filterAndNotifyTargets() {
        if (mRawTargets != null) {
            mSmartspaceTargets = mRawTargets.stream()
                    .filter(this::isTargetAllowed)
                    .collect(Collectors.toList());
        } else {
            mSmartspaceTargets = Collections.emptyList();
        }

        mSmartspaceTargetListeners.forEach(listener -> listener.onSmartspaceTargetsUpdated(mSmartspaceTargets));
    }

    private boolean isTargetAllowed(SmartspaceTarget target) {
        if (target == null) {
            return false;
        }

        if (mDismissedTargetIds.contains(target.getSmartspaceTargetId())) {
            return false;
        }

        int featureType = target.getFeatureType();
        if (featureType == 15 /* MEDIA */) {
            return false;
        }

        if (mPrefs == null) {
            return true;
        }

        // Global smartspace toggle
        if (!mPrefs.getBoolean(KEY_SMARTSPACE_ENABLED, true)) {
            return false;
        }

        // Work profile filter
        if (target.getUserHandle() != null && !target.getUserHandle().equals(Process.myUserHandle())) {
            if (!mPrefs.getBoolean("pref_smartspace_work_profile", true)) {
                return false;
            }
        }

        String prefKey = getPreferenceKeyForFeatureType(featureType);
        if (prefKey != null && !mPrefs.getBoolean(prefKey, true)) {
            return false;
        }

        return true;
    }

    private String getPreferenceKeyForFeatureType(int featureType) {
        switch (featureType) {
            case 1: /* FEATURE_WEATHER */
                return "pref_smartspace_weather";
            case 2: /* FEATURE_CALENDAR */
            case 6: /* FEATURE_REMINDER */
            case 26: /* FEATURE_UPCOMING_REMINDER */
                return "pref_smartspace_upcoming";
            case 3: /* FEATURE_COMMUTE_TIME */
            case 32: /* FEATURE_COMMUTE_WEATHER */
                return "pref_smartspace_commute";
            case 4: /* FEATURE_FLIGHT */
                return "pref_smartspace_travel";
            case 9: /* FEATURE_SPORTS */
                return "pref_smartspace_sports";
            case 10: /* FEATURE_WEATHER_ALERT */
            case 40: /* FEATURE_SEVERE_WEATHER */
                return "pref_smartspace_alerts";
            case 12: /* FEATURE_STOCK_PRICE_CHANGE */
                return "pref_smartspace_finance";
            case 13: /* FEATURE_SHOPPING_LIST */
                return "pref_smartspace_food_orders";
            case 14: /* FEATURE_LOYALTY_CARD */
            case 43: /* FEATURE_WALLET_PASS */
                return "pref_smartspace_passes_nearby";
            case 16: /* FEATURE_BEDTIME_ROUTINE */
                return "pref_smartspace_bedtime";
            case 17: /* FEATURE_FITNESS_TRACKING */
            case 23: /* FEATURE_FITNESS_GOAL */
            case 35: /* FEATURE_STEP_COUNT */
            case 41: /* FEATURE_HEALTH_CONNECT */
                return "pref_smartspace_fitness";
            case 18: /* FEATURE_ETA_MONITORING */
            case 38: /* FEATURE_TIME_TO_LEAVE */
                return "pref_smartspace_time_to_leave";
            case 20: /* FEATURE_PACKAGE_TRACKING */
                return "pref_smartspace_parcel_delivery";
            case 21: /* FEATURE_TIMER */
            case 22: /* FEATURE_STOPWATCH */
                return "pref_smartspace_timer_stopwatch";
            case 24: /* FEATURE_BLUETOOTH_DEVICE */
            case 28: /* FEATURE_MEDIA_HEADSET */
            case 34: /* FEATURE_BLUETOOTH_LOW_BATTERY */
                return "pref_smartspace_connected_devices";
            case 25: /* FEATURE_CROSS_DEVICE_TIMER */
                return "pref_smartspace_cross_device_timer";
            case 29: /* FEATURE_EARTHQUAKE_ALERT */
                return "pref_smartspace_earthquake";
            case 30: /* FEATURE_DOORBELL */
                return "pref_smartspace_doorbell";
            case 33: /* FEATURE_AIR_QUALITY */
                return "pref_smartspace_air_quality";
            case 36: /* FEATURE_SAFETY_CHECK */
                return "pref_smartspace_safety_check";
            case 37: /* FEATURE_FLASHLIGHT */
                return "pref_smartspace_torch";
            case 39: /* FEATURE_RIDESHARING */
                return "pref_smartspace_ridesharing";
            case 42: /* FEATURE_LOUD_NOISE_ALERTS */
                return "pref_smartspace_loud_sound";
            default:
                return null;
        }
    }

    @Override
    public void registerConfigProvider(BcSmartspaceConfigPlugin configPlugin) {
        mConfigProvider = configPlugin;
    }

    @Override
    public void registerListener(BcSmartspaceDataPlugin.SmartspaceTargetListener listener) {
        mSmartspaceTargetListeners.add(listener);
        listener.onSmartspaceTargetsUpdated(mSmartspaceTargets);
    }

    @Override
    public void unregisterListener(BcSmartspaceDataPlugin.SmartspaceTargetListener listener) {
        mSmartspaceTargetListeners.remove(listener);
    }

    @Override
    public void setEventDispatcher(BcSmartspaceDataPlugin.SmartspaceEventDispatcher dispatcher) {
        mEventNotifier.eventDispatcher = dispatcher;
    }

    @Override
    public void setIntentStarter(BcSmartspaceDataPlugin.IntentStarter intentStarter) {
        mEventNotifier.intentStarterRef = intentStarter;
    }

    public void registerSmartspaceEventNotifier(
            BcSmartspaceDataPlugin.SmartspaceEventNotifier notifier) {
        if (notifier != null) {
            setEventDispatcher(notifier::notifySmartspaceEvent);
            setIntentStarter(notifier.getIntentStarter());
        }
    }

    public void notifySmartspaceEvent(SmartspaceTargetEvent event) {
        mEventNotifier.notifySmartspaceEvent(event);
    }
}
