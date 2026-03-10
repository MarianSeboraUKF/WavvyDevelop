package sk.ukf.wavvy;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;

public class WavvyDialogs {
    private static int dialogWidthPx(Activity activity) {
        DisplayMetrics dm = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(dm);
        return (int) (dm.widthPixels * 0.90f);
    }
    public static Dialog showCenteredCardDialog(Context ctx, Activity activity, View cardView) {

        FrameLayout host = new FrameLayout(ctx);
        host.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                dialogWidthPx(activity),
                FrameLayout.LayoutParams.WRAP_CONTENT
        );
        lp.gravity = Gravity.CENTER;

        host.addView(cardView, lp);

        Dialog dialog = new Dialog(ctx, R.style.WavvyFullscreenDialog);

        host.setOnClickListener(v -> dialog.dismiss());

        cardView.setOnClickListener(v -> {
        });

        dialog.setContentView(host);
        dialog.setCancelable(true);
        dialog.setCanceledOnTouchOutside(true);
        dialog.show();

        Window w = dialog.getWindow();
        if (w != null) {
            w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            w.setLayout(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT
            );

            w.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);

            w.getDecorView().post(() -> {
                WindowManager.LayoutParams p = w.getAttributes();
                p.gravity = Gravity.CENTER;
                p.y = 0;
                w.setAttributes(p);
                host.requestLayout();
            });
        }
        return dialog;
    }
}