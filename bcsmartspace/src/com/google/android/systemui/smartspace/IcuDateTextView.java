package com.google.android.systemui.smartspace;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.icu.text.DateFormat;
import android.icu.text.DisplayContext;
import android.os.Handler;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.Log;

import com.android.systemui.plugins.BcSmartspaceDataPlugin;

import com.android.systemui.bcsmartspace.R;

import java.util.Locale;
import java.util.Objects;

public class IcuDateTextView extends DoubleShadowTextView {
    private static final String TAG = "IcuDateTextView";
    public final ContentObserver mAodSettingsObserver;
    public Handler mBgHandler;
    public DateFormat mFormatter;
    public Handler mHandler;
    public final BroadcastReceiver mIntentReceiver;
    public boolean mIsAodEnabled;
    public Boolean mIsInteractive;
    public String mText;
    public final Runnable mTimeChangedCallback;
    public BcSmartspaceDataPlugin.TimeChangedDelegate mTimeChangedDelegate;
    public boolean mUpdatesOnAod;

    public final class DefaultTimeChangedDelegate implements BcSmartspaceDataPlugin.TimeChangedDelegate, Runnable {
        public Handler mHandler;
        public Runnable mTimeChangedCallback;

        @Override
        public final void register(Runnable callback) {
            if (mTimeChangedCallback != null) {
                unregister();
            }
            mTimeChangedCallback = callback;
            run();
        }

        @Override
        public final void run() {
            if (mTimeChangedCallback != null) {
                mTimeChangedCallback.run();
                if (mHandler != null) {
                    long now = SystemClock.uptimeMillis();
                    long delay = 60000 - (now % 60000);
                    mHandler.postAtTime(this, now + delay);
                }
            }
        }

        @Override
        public final void unregister() {
            mHandler.removeCallbacks(this);
            mTimeChangedCallback = null;
        }
    }

    public IcuDateTextView(Context context) {
        this(context, null);
    }

    @Override
    public final void onAttachedToWindow() {
        super.onAttachedToWindow();

        if (mUpdatesOnAod) {
            Runnable registerAod = () -> {
                try {
                    getContext().getContentResolver().registerContentObserver(
                            Settings.Secure.getUriFor("doze_always_on"), false, mAodSettingsObserver);
                } catch (Exception e) {
                    Log.w(TAG, "Unable to register DOZE_ALWAYS_ON content observer: ", e);
                }
            };
            if (mBgHandler != null) {
                mBgHandler.post(registerAod);
            } else {
                registerAod.run();
            }
            try {
                mIsAodEnabled = Settings.Secure.getIntForUser(getContext().getContentResolver(), "doze_always_on", 0, getContext().getUserId()) == 1;
            } catch (Exception ignored) {}
        }

        mHandler = new Handler();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_TIME_CHANGED);
        intentFilter.addAction(Intent.ACTION_TIMEZONE_CHANGED);

        if (mBgHandler == null) {
            Log.w(TAG, "mBgHandler is not set! Fallback to make binder calls on main thread.");
            try {
                getContext().registerReceiver(mIntentReceiver, intentFilter, Context.RECEIVER_EXPORTED);
            } catch (Exception e) {
                Log.w(TAG, "Unable to register receiver: ", e);
            }
        } else {
            mBgHandler.post(() -> {
                try {
                    getContext().registerReceiver(mIntentReceiver, intentFilter, Context.RECEIVER_EXPORTED);
                } catch (Exception e) {
                    Log.w(TAG, "Unable to register receiver: ", e);
                }
            });
        }

        if (mTimeChangedDelegate == null) {
            DefaultTimeChangedDelegate delegate = new DefaultTimeChangedDelegate();
            delegate.mHandler = mHandler;
            mTimeChangedDelegate = delegate;
        }
        onTimeChanged(true);
    }

    @Override
    public final void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mHandler != null) {
            if (mBgHandler == null) {
                Log.w(TAG, "mBgHandler is not set! Fallback to make binder calls on main thread.");
                getContext().unregisterReceiver(mIntentReceiver);
            } else {
                mBgHandler.post(() -> {
                    try {
                        getContext().unregisterReceiver(mIntentReceiver);
                    } catch (IllegalArgumentException ignored) {}
                });
            }
            mTimeChangedDelegate.unregister();
            mHandler = null;
        }
        if (mUpdatesOnAod) {
            Runnable unregisterAod = () -> {
                try {
                    getContext().getContentResolver().unregisterContentObserver(mAodSettingsObserver);
                } catch (Exception ignored) {}
            };
            if (mBgHandler != null) {
                mBgHandler.post(unregisterAod);
            } else {
                unregisterAod.run();
            }
        }
    }

    public final void onTimeChanged(boolean forceUpdateFormatter) {
        if (mFormatter == null || forceUpdateFormatter) {
            mFormatter = DateFormat.getInstanceForSkeleton(getContext().getString(R.string.smartspace_icu_date_pattern), Locale.getDefault());
            mFormatter.setContext(DisplayContext.CAPITALIZATION_FOR_BEGINNING_OF_SENTENCE);
        }
        String newText = mFormatter.format(Long.valueOf(System.currentTimeMillis()));
        if (Objects.equals(mText, newText)) {
            return;
        }
        mText = newText;
        setText(newText);
        setContentDescription(newText);
    }

    @Override
    public final void onVisibilityAggregated(boolean isVisible) {
        super.onVisibilityAggregated(isVisible);
        rescheduleTicker();
    }

    public final void rescheduleTicker() {
        if (mHandler == null) {
            return;
        }
        mTimeChangedDelegate.unregister();
        if ((mIsInteractive == null || mIsInteractive || (mUpdatesOnAod && mIsAodEnabled)) && isAggregatedVisible()) {
            mTimeChangedDelegate.register(mTimeChangedCallback);
        }
    }

    public IcuDateTextView(Context context, AttributeSet attrs) {
        super(context, attrs, 0);

        mAodSettingsObserver = new ContentObserver(new Handler()) {
            @Override
            public void onChange(boolean selfChange) {
                try {
                    boolean isAodEnabled = Settings.Secure.getIntForUser(getContext().getContentResolver(), "doze_always_on", 0, getContext().getUserId()) == 1;
                    if (mIsAodEnabled == isAodEnabled) {
                        return;
                    }
                    mIsAodEnabled = isAodEnabled;
                    rescheduleTicker();
                } catch (Exception ignored) {}
            }
        };

        mIntentReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                boolean updateFormatter = "android.intent.action.TIMEZONE_CHANGED".equals(intent.getAction()) || "android.intent.action.TIME_SET".equals(intent.getAction());
                onTimeChanged(updateFormatter);
            }
        };

        mTimeChangedCallback = () -> onTimeChanged(false);
    }
}
