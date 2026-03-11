package sk.ukf.wavvy;

import android.content.Context;
import android.content.Intent;
import java.util.ArrayList;
import sk.ukf.wavvy.model.Song;

public class PlayerLauncher {
    public static void openQueue(Context ctx, ArrayList<Song> songs, Song clicked) {

        if (songs == null || songs.isEmpty()) return;

        int[] ids = new int[songs.size()];
        int index = 0;

        for (int i = 0; i < songs.size(); i++) {
            ids[i] = songs.get(i).getAudioResId();

            if (songs.get(i).getAudioResId() == clicked.getAudioResId()) {
                index = i;
            }
        }

        PlaybackManager pm = PlaybackManager.get(ctx);
        pm.playQueue(ids, index, true);
    }
    public static void openExisting(Context ctx) {
        Intent intent = new Intent(ctx, PlayerActivity.class);
        intent.putExtra(PlayerActivity.EXTRA_OPEN_EXISTING, true);
        ctx.startActivity(intent);
    }
}