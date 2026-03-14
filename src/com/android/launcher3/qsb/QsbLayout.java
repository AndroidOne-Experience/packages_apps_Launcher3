package com.android.launcher3.qsb;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import com.android.launcher3.DeviceProfile;
import com.android.launcher3.LauncherPrefs;
import com.android.launcher3.R;
import com.android.launcher3.Utilities;
import com.android.launcher3.util.Themes;
import com.android.launcher3.views.ActivityContext;

public class QsbLayout extends FrameLayout implements
        SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = "QsbLayout";

    ImageView mAssistantIcon;
    ImageView mGeminiIcon;
    ImageView mGoogleIcon;
    ImageView mLensIcon;
    Context mContext;

    public QsbLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
    }

    public QsbLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mContext = context;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mAssistantIcon = findViewById(R.id.mic_icon);
        mGeminiIcon = findViewById(R.id.gemini_icon);
        mGoogleIcon = findViewById(R.id.g_icon);
        mLensIcon = findViewById(R.id.lens_icon);
        setIcons();

        LauncherPrefs.getPrefs(mContext).registerOnSharedPreferenceChangeListener(this);

        String searchPackage = QsbContainerView.getSearchWidgetPackageName(mContext);
        setOnClickListener(view -> {
            Intent intent = new Intent("android.search.action.GLOBAL_SEARCH")
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    .setPackage(searchPackage);

            try {
                mContext.startActivity(intent);
            } catch (Exception e) {
                android.widget.Toast.makeText(
                        mContext,
                        "Google search not available",
                        android.widget.Toast.LENGTH_SHORT
                ).show();
            }
        });
        setupGeminiIcon();
        enableLensIcon();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int requestedWidth = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);

        DeviceProfile dp = ActivityContext.lookupContext(mContext).getDeviceProfile();
        int cellWidth = DeviceProfile.calculateCellWidth(requestedWidth, dp.cellLayoutBorderSpacePx.x, dp.numShownHotseatIcons);
        int iconSize = (int)(Math.round((dp.iconSizePx * 0.92f)));
        int width = requestedWidth;
        setMeasuredDimension(width, height);

        for (int i = 0; i < getChildCount(); i++) {
            final View child = getChildAt(i);
            if (child != null) {
                measureChildWithMargins(child, widthMeasureSpec, 0, heightMeasureSpec, 0);
            }
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        if (key.equals(Themes.KEY_THEMED_ICONS)) {
            setIcons();
        }
    }

    private void setIcons() {
        if (Themes.isThemedIconEnabled(mContext)) {
            mAssistantIcon.setImageResource(R.drawable.ic_mic_themed);
            mGeminiIcon.setImageResource(R.drawable.ic_gemini_themed);
            mGoogleIcon.setImageResource(R.drawable.ic_super_g_themed);
            mLensIcon.setImageResource(R.drawable.ic_lens_themed);
        } else {
            mAssistantIcon.setImageResource(R.drawable.ic_mic_color);
            mGeminiIcon.setImageResource(R.drawable.ic_gemini_color);
            mGoogleIcon.setImageResource(R.drawable.ic_super_g_color);
            mLensIcon.setImageResource(R.drawable.ic_lens_color);
        }
    }

    private void setupGeminiIcon() {
        if (mGeminiIcon == null) {
            return;
        }

        mGeminiIcon.setVisibility(View.VISIBLE);
        mGeminiIcon.setOnClickListener(view -> {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(Utilities.GEMINI_URI))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    .setPackage(Utilities.GSA_PACKAGE);

            try {
                view.getContext().startActivity(intent);
                return;
            } catch (Exception e) {
                Log.e(TAG, "Gemini launch failed", e);
                android.widget.Toast.makeText(
                        mContext,
                        "Google Gemini not available",
                        android.widget.Toast.LENGTH_SHORT
                ).show();
            }
        });
    }

    private void enableLensIcon() {
        mLensIcon.setVisibility(View.VISIBLE);
        mLensIcon.setOnClickListener(view -> {
            Intent lensIntent = new Intent();
            lensIntent.setAction(Intent.ACTION_VIEW)
                    .setComponent(new ComponentName(Utilities.GSA_PACKAGE, Utilities.LENS_ACTIVITY))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    .setData(Uri.parse(Utilities.LENS_URI))
                    .putExtra("LensHomescreenShortcut", true);
            try {
                mContext.startActivity(lensIntent);
            } catch (Exception e) {
                android.widget.Toast.makeText(
                        mContext,
                        "Google Lens not available",
                        android.widget.Toast.LENGTH_SHORT
                ).show();
            }
        });
    }

}
