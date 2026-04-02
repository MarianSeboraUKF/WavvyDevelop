package sk.ukf.wavvy;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class NotificationReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        PlaybackManager pm = PlaybackManager.get(context);
        if (intent.getAction() == null) return;

        switch (intent.getAction()) {
            case "ACTION_PLAY_PAUSE":
                pm.togglePlayPause();
                break;

            case "ACTION_NEXT":
                pm.playNext(true);
                break;

            case "ACTION_PREV":
                pm.playPrev(true);
                break;
        }
    }
}