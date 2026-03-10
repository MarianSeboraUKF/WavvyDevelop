package sk.ukf.wavvy;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.UUID;
import sk.ukf.wavvy.model.Playlist;

public class PlaylistRepository {
    private static final String PREFS = "wavvy_prefs";
    private static final String KEY_PLAYLISTS = "playlists_json";
    private static final Gson gson = new Gson();
    public static ArrayList<Playlist> getPlaylists(Context ctx) {
        SharedPreferences sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String json = sp.getString(KEY_PLAYLISTS, "[]");

        Type type = new TypeToken<ArrayList<Playlist>>(){}.getType();
        ArrayList<Playlist> playlists = gson.fromJson(json, type);
        if (playlists == null) playlists = new ArrayList<>();
        return playlists;
    }
    public static void savePlaylists(Context ctx, ArrayList<Playlist> playlists) {
        SharedPreferences sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        sp.edit().putString(KEY_PLAYLISTS, gson.toJson(playlists)).apply();
    }
    public static Playlist createPlaylist(Context ctx, String name) {
        ArrayList<Playlist> playlists = getPlaylists(ctx);

        String id = UUID.randomUUID().toString();
        Playlist p = new Playlist(id, name);
        playlists.add(p);

        savePlaylists(ctx, playlists);
        return p;
    }
    public static Playlist findById(Context ctx, String playlistId) {
        ArrayList<Playlist> playlists = getPlaylists(ctx);
        for (Playlist p : playlists) {
            if (p.getId().equals(playlistId)) return p;
        }
        return null;
    }
    public static void addSongToPlaylist(Context ctx, String playlistId, int audioResId) {
        ArrayList<Playlist> playlists = getPlaylists(ctx);

        for (Playlist p : playlists) {
            if (p.getId().equals(playlistId)) {
                p.addSong(audioResId);
                break;
            }
        }
        savePlaylists(ctx, playlists);
    }
    public static void removeSongFromPlaylist(Context ctx, String playlistId, int audioResId) {
        ArrayList<Playlist> playlists = getPlaylists(ctx);

        for (Playlist p : playlists) {
            if (p.getId().equals(playlistId)) {
                p.removeSong(audioResId);
                break;
            }
        }
        savePlaylists(ctx, playlists);
    }
    public static void deletePlaylist(Context ctx, String playlistId) {
        ArrayList<Playlist> playlists = getPlaylists(ctx);

        for (int i = 0; i < playlists.size(); i++) {
            if (playlists.get(i).getId().equals(playlistId)) {
                playlists.remove(i);
                break;
            }
        }
        savePlaylists(ctx, playlists);
    }
    public static void renamePlaylist(Context ctx, String playlistId, String newName) {
        ArrayList<Playlist> playlists = getPlaylists(ctx);

        for (Playlist p : playlists) {
            if (p.getId().equals(playlistId)) {
                playlists.remove(p);
                Playlist updated = new Playlist(playlistId, newName);

                for (Integer id : p.getSongAudioResIds()) {
                    updated.addSong(id);
                }

                playlists.add(updated);
                break;
            }
        }

        savePlaylists(ctx, playlists);
    }
}