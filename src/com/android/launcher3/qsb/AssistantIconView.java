package com.android.launcher3.qsb;

import android.content.Context;
import android.content.Intent;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.Toast;

import com.android.launcher3.R;

public class AssistantIconView extends ImageView {

    public AssistantIconView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public AssistantIconView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    private void init(Context context) {
        setScaleType(ScaleType.CENTER);

        setOnClickListener(view -> {
            Intent intent = new Intent(Intent.ACTION_VOICE_COMMAND)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    .setPackage(QsbContainerView.getSearchWidgetPackageName(context));

            try {
                context.startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(
                        context,
                        "Google Assistant not available",
                        Toast.LENGTH_SHORT
                ).show();
            }
        });
    }
}