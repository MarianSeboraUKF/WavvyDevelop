package sk.ukf.wavvy;

import android.content.Context;
import android.content.SharedPreferences;

public class NowPlayingRepository {
    private static final String PREFS = "wavvy_prefs";
    private static final String KEY_AUDIO_ID = "now_audio_id";
    private static final String KEY_QUEUE_IDS = "now_queue_ids";
    private static final String KEY_QUEUE_INDEX = "now_queue_index";
    private static final String KEY_POSITION = "now_position";
    private static SharedPreferences sp(Context ctx) {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
    public static void saveNowPlaying(Context ctx, int audioResId, int[] queueIds, int queueIndex) {
        StringBuilder csv = new StringBuilder();
        if (queueIds != null) {
            for (int i = 0; i < queueIds.length; i++) {
                if (i > 0) csv.append(",");
                csv.append(queueIds[i]);
            }
        }
        sp(ctx).edit()
                .putInt(KEY_AUDIO_ID, audioResId)
                .putString(KEY_QUEUE_IDS, csv.toString())
                .putInt(KEY_QUEUE_INDEX, queueIndex)
                .apply();
    }
    public static int getAudioResId(Context ctx) {
        return sp(ctx).getInt(KEY_AUDIO_ID, 0);
    }
    public static int[] getQueueIds(Context ctx) {
        String csv = sp(ctx).getString(KEY_QUEUE_IDS, "");
        if (csv == null || csv.trim().isEmpty()) return null;

        String[] parts = csv.split(",");
        int[] out = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            try {
                out[i] = Integer.parseInt(parts[i].trim());
            } catch (Exception e) {
                out[i] = 0;
            }
        }
        return out;
    }
    public static int getQueueIndex(Context ctx) {
        return sp(ctx).getInt(KEY_QUEUE_INDEX, 0);
    }
    public static boolean hasNowPlaying(Context ctx) {
        return getAudioResId(ctx) != 0;
    }
    public static void clear(Context ctx) {
        sp(ctx).edit()
                .remove(KEY_AUDIO_ID)
                .remove(KEY_QUEUE_IDS)
                .remove(KEY_QUEUE_INDEX)
                .apply();
    }
    public static void savePosition(Context ctx, long positionMs) {
        sp(ctx).edit()
                .putLong(KEY_POSITION, positionMs)
                .apply();
    }
    public static long getPosition(Context ctx) {
        return sp(ctx).getLong(KEY_POSITION, 0);
    }
}