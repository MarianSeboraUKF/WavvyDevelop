package sk.ukf.wavvy;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import androidx.core.content.ContextCompat;
import androidx.palette.graphics.Palette;

public class GradientPreloader {
    public static void preload(Context ctx, int coverResId) {
        if (GradientPrefs.has(ctx, coverResId)) return;

        new Thread(() -> {

            Bitmap bitmap = BitmapFactory.decodeResource(ctx.getResources(), coverResId);
            if (bitmap == null) return;

            Palette palette = Palette.from(bitmap).generate();

            int dominant = palette.getDominantColor(
                    ContextCompat.getColor(ctx, R.color.bg)
            );
            int dark = palette.getDarkMutedColor(dominant);
            int vibrant = palette.getVibrantColor(dominant);
            GradientPrefs.save(ctx, coverResId, vibrant, dark);
        }).start();
    }
}