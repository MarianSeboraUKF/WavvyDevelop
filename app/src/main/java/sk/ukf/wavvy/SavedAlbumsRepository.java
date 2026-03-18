package sk.ukf.wavvy;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.HashSet;
import java.util.Set;

public class SavedAlbumsRepository {
    private static final String PREFS = "saved_albums";
    private static final String KEY = "album_titles";

    public static void add(Context context, String albumTitle) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);

        Set<String> albums = new HashSet<>(
                prefs.getStringSet(KEY, new HashSet<>())
        );
        albums.add(albumTitle);
        prefs.edit().putStringSet(KEY, albums).apply();
    }
    public static void remove(Context context, String albumTitle) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);

        Set<String> albums = new HashSet<>(
                prefs.getStringSet(KEY, new HashSet<>())
        );

        albums.remove(albumTitle);

        prefs.edit().putStringSet(KEY, albums).apply();
    }
    public static boolean isSaved(Context context, String albumTitle) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        Set<String> albums = prefs.getStringSet(KEY, new HashSet<>());
        return albums.contains(albumTitle);
    }
}