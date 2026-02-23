package sk.ukf.wavvy;

import android.content.Context;
import android.content.SharedPreferences;

public class GradientPrefs {
    private static final String PREF = "gradient_cache";
    public static void save(Context ctx, int coverResId, int vibrant, int dark) {
        SharedPreferences sp = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        sp.edit()
                .putInt(coverResId + "_v", vibrant)
                .putInt(coverResId + "_d", dark)
                .apply();
    }
    public static boolean has(Context ctx, int coverResId) {
        SharedPreferences sp = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        return sp.contains(coverResId + "_v") && sp.contains(coverResId + "_d");
    }
    public static int getVibrant(Context ctx, int coverResId) {
        return ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
                .getInt(coverResId + "_v", 0);
    }
    public static int getDark(Context ctx, int coverResId) {
        return ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
                .getInt(coverResId + "_d", 0);
    }
}