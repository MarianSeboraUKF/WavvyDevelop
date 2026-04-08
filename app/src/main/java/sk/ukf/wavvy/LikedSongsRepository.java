package sk.ukf.wavvy;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import java.util.ArrayList;

public class LikedSongsRepository {
    private static final String PREFS = "liked_songs";
    private static final String KEY = "songs";
    public static final String ACTION_LIKED_CHANGED = "wavvy_liked_changed";
    public static ArrayList<String> getLikedSongs(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        ArrayList<String> list = new ArrayList<>();

        try {
            String json = prefs.getString(KEY, null);
            if (json != null) {
                JSONArray arr = new JSONArray(json);
                for (int i = 0; i < arr.length(); i++) {
                    list.add(arr.getString(i));
                }
                return list;
            }

        } catch (ClassCastException e) {
            try {
                java.util.Set<String> oldSet = prefs.getStringSet(KEY, new java.util.HashSet<>());
                list.addAll(oldSet);
                JSONArray arr = new JSONArray(list);
                prefs.edit().putString(KEY, arr.toString()).apply();
                return list;
            } catch (Exception ignored) {}
        } catch (Exception ignored) {}
        return list;
    }
    public static boolean isLiked(Context context, String songId) { return getLikedSongs(context).contains(songId); }
    public static void toggleLike(Context context, String songId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        ArrayList<String> liked = getLikedSongs(context);

        if (liked.contains(songId)) {
            liked.remove(songId);
        } else {
            liked.add(songId);
        }

        JSONArray arr = new JSONArray(liked);
        prefs.edit().putString(KEY, arr.toString()).commit();
        android.content.Intent intent = new android.content.Intent(ACTION_LIKED_CHANGED);
        context.sendBroadcast(intent);
    }
}