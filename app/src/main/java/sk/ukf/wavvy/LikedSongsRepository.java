package sk.ukf.wavvy;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.HashSet;
import java.util.Set;

public class LikedSongsRepository {
    private static final String PREFS = "liked_songs";
    private static final String KEY = "songs";
    public static Set<String> getLikedSongs(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return new HashSet<>(prefs.getStringSet(KEY, new HashSet<>()));
    }
    public static boolean isLiked(Context context, String songId) {
        return getLikedSongs(context).contains(songId);
    }
    public static void toggleLike(Context context, String songId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        Set<String> liked = getLikedSongs(context);

        if (liked.contains(songId)) {
            liked.remove(songId);
        } else {
            liked.add(songId);
        }

        prefs.edit().putStringSet(KEY, liked).apply();
    }
}