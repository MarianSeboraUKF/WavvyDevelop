package sk.ukf.wavvy;

import android.content.Context;
import androidx.annotation.Nullable;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class PlaybackManager {
    public interface Listener {
        void onNowPlayingChanged(int audioResId, int[] queueIds, int queueIndex);
        void onIsPlayingChanged(boolean isPlaying);
        void onProgress(long positionMs, long durationMs);
    }
    public enum RepeatMode { OFF, ONE, ALL }
    private static PlaybackManager instance;

    public static synchronized PlaybackManager get(Context ctx) {
        if (instance == null) instance = new PlaybackManager(ctx.getApplicationContext());
        return instance;
    }
    private final Context appContext;
    private final ExoPlayer player;
    private int[] originalQueue = null;
    private int[] activeQueue = null;
    private int queueIndex = 0;
    private int currentAudioResId = 0;
    private boolean shuffleEnabled = false;
    private RepeatMode repeatMode = RepeatMode.OFF;
    private final List<WeakReference<Listener>> listeners = new ArrayList<>();
    private final android.os.Handler progressHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private static final String PREFS = "wavvy_playback_prefs";
    private static final String KEY_SHUFFLE = "shuffle";
    private static final String KEY_REPEAT = "repeat";
    private final Runnable progressTick = new Runnable() {
        @Override public void run() {
            if (player != null) {
                long pos = player.getCurrentPosition();
                long dur = player.getDuration();
                notifyProgress(pos, dur);
            }
            progressHandler.postDelayed(this, 500);
        }
    };
    private PlaybackManager(Context appContext) {
        this.appContext = appContext;
        this.player = new ExoPlayer.Builder(appContext).build();

        restoreFromNowPlayingRepository();
        loadPlaybackSettings();

        player.addListener(new Player.Listener() {
            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                notifyIsPlaying(isPlaying);
            }

            @Override
            public void onPlaybackStateChanged(int playbackState) {
                if (playbackState == Player.STATE_ENDED) {
                    onTrackEnded();
                }
            }

            @Override
            public void onMediaItemTransition(@Nullable MediaItem mediaItem, int reason) {}
        });
        progressHandler.post(progressTick);
    }
    private void restoreFromNowPlayingRepository() {
        if (!NowPlayingRepository.hasNowPlaying(appContext)) return;

        int audioResId = NowPlayingRepository.getAudioResId(appContext);
        int[] q = NowPlayingRepository.getQueueIds(appContext);
        int idx = NowPlayingRepository.getQueueIndex(appContext);

        if (audioResId == 0) return;
        if (q == null || q.length == 0) q = new int[]{ audioResId };

        if (idx < 0) idx = 0;
        if (idx >= q.length) idx = q.length - 1;

        originalQueue = q.clone();
        activeQueue = q.clone();
        queueIndex = idx;
        currentAudioResId = activeQueue[queueIndex];

        MediaItem item = MediaItem.fromUri("android.resource://" + appContext.getPackageName() + "/" + currentAudioResId);
        player.setMediaItem(item);
        player.prepare();
    }
    private void savePlaybackSettings() {
        appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_SHUFFLE, shuffleEnabled)
                .putString(KEY_REPEAT, repeatMode.name())
                .apply();
    }
    private void loadPlaybackSettings() {
        android.content.SharedPreferences sp =
                appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE);

        shuffleEnabled = sp.getBoolean(KEY_SHUFFLE, false);

        String r = sp.getString(KEY_REPEAT, RepeatMode.OFF.name());
        try {
            repeatMode = RepeatMode.valueOf(r);
        } catch (Exception e) {
            repeatMode = RepeatMode.OFF;
        }
    }
    private void ensureQueueLoadedIfPossible() {
        if (activeQueue != null && activeQueue.length > 0 && currentAudioResId != 0) return;
        restoreFromNowPlayingRepository();
    }
    public void addListener(Listener l) {
        if (l == null) return;
        listeners.add(new WeakReference<>(l));

        if (currentAudioResId != 0 && activeQueue != null) {
            l.onNowPlayingChanged(currentAudioResId, activeQueue, queueIndex);
        }
        l.onIsPlayingChanged(player.isPlaying());
        l.onProgress(player.getCurrentPosition(), player.getDuration());
        cleanupListeners();
    }
    public void removeListener(Listener l) {
        if (l == null) return;
        Iterator<WeakReference<Listener>> it = listeners.iterator();
        while (it.hasNext()) {
            Listener x = it.next().get();
            if (x == null || x == l) it.remove();
        }
    }
    private void cleanupListeners() {
        Iterator<WeakReference<Listener>> it = listeners.iterator();
        while (it.hasNext()) {
            if (it.next().get() == null) it.remove();
        }
    }
    private void notifyNowPlaying() {
        cleanupListeners();
        for (WeakReference<Listener> wr : listeners) {
            Listener l = wr.get();
            if (l != null) l.onNowPlayingChanged(currentAudioResId, activeQueue, queueIndex);
        }
    }
    private void notifyIsPlaying(boolean isPlaying) {
        cleanupListeners();
        for (WeakReference<Listener> wr : listeners) {
            Listener l = wr.get();
            if (l != null) l.onIsPlayingChanged(isPlaying);
        }
    }
    private void notifyProgress(long positionMs, long durationMs) {
        cleanupListeners();
        for (WeakReference<Listener> wr : listeners) {
            Listener l = wr.get();
            if (l != null) l.onProgress(positionMs, durationMs);
        }
    }
    public ExoPlayer getPlayer() {
        return player;
    }
    public boolean isPlaying() {
        return player.isPlaying();
    }
    public int getCurrentAudioResId() {
        return currentAudioResId;
    }
    public int[] getQueueIds() {
        return activeQueue;
    }
    public int getQueueIndex() {
        return queueIndex;
    }
    public boolean isShuffleEnabled() {
        return shuffleEnabled;
    }
    public RepeatMode getRepeatMode() {
        return repeatMode;
    }
    public void playQueue(int[] ids, int startIndex, boolean autoPlay) {
        if (ids == null || ids.length == 0) return;

        if (startIndex < 0) startIndex = 0;
        if (startIndex >= ids.length) startIndex = ids.length - 1;

        originalQueue = ids.clone();

        if (shuffleEnabled && ids.length > 1) {
            int keepId = ids[startIndex];
            activeQueue = buildShuffledQueueKeepingCurrent(originalQueue, keepId);
            queueIndex = 0;
        } else {
            activeQueue = ids.clone();
            queueIndex = startIndex;
        }
        loadCurrent(autoPlay);
    }
    public void togglePlayPause() {
        ensureQueueLoadedIfPossible();
        if (activeQueue == null || activeQueue.length == 0) return;

        if (player.isPlaying()) player.pause();
        else player.play();
    }
    public boolean playNext(boolean autoPlay) {
        ensureQueueLoadedIfPossible();
        if (activeQueue == null || activeQueue.length == 0) return false;

        if (queueIndex < activeQueue.length - 1) {
            queueIndex++;
            loadCurrent(autoPlay);
            return true;
        }

        if (repeatMode == RepeatMode.ALL && activeQueue.length > 1) {
            queueIndex = 0;
            loadCurrent(autoPlay);
            return true;
        }

        return false;
    }
    public boolean playPrev(boolean autoPlay) {
        ensureQueueLoadedIfPossible();
        if (activeQueue == null || activeQueue.length == 0) return false;

        if (queueIndex > 0) {
            queueIndex--;
            loadCurrent(autoPlay);
            return true;
        }

        if (repeatMode == RepeatMode.ALL && activeQueue.length > 1) {
            queueIndex = activeQueue.length - 1;
            loadCurrent(autoPlay);
            return true;
        }
        return false;
    }
    public void toggleShuffle() {
        ensureQueueLoadedIfPossible();

        if (originalQueue == null || originalQueue.length <= 1) {
            shuffleEnabled = !shuffleEnabled;
            notifyNowPlaying();
            return;
        }

        shuffleEnabled = !shuffleEnabled;

        int keepId = currentAudioResId;

        if (shuffleEnabled) {
            activeQueue = buildShuffledQueueKeepingCurrent(originalQueue, keepId);
            queueIndex = 0;
        } else {
            activeQueue = originalQueue.clone();
            queueIndex = indexOf(activeQueue, keepId);
        }
        notifyNowPlaying();
        savePlaybackSettings();
    }
    public void cycleRepeatMode() {
        if (repeatMode == RepeatMode.OFF) {
            repeatMode = RepeatMode.ALL;
        }
        else if (repeatMode == RepeatMode.ALL) {
            repeatMode = RepeatMode.ONE;
        }
        else {
            repeatMode = RepeatMode.OFF;
        }
        notifyNowPlaying();
        savePlaybackSettings();
    }
    private void loadCurrent(boolean autoPlay) {
        if (activeQueue == null || activeQueue.length == 0) return;

        currentAudioResId = activeQueue[queueIndex];

        MediaItem item = MediaItem.fromUri("android.resource://" + appContext.getPackageName() + "/" + currentAudioResId);
        player.setMediaItem(item);
        player.prepare();

        NowPlayingRepository.saveNowPlaying(appContext, currentAudioResId, activeQueue, queueIndex);

        notifyNowPlaying();

        if (autoPlay) player.play();
        else notifyIsPlaying(player.isPlaying());
    }
    private void onTrackEnded() {
        if (repeatMode == RepeatMode.ONE) {
            player.seekTo(0);
            player.play();
            return;
        }
        boolean moved = playNext(true);
        if (!moved) {
            if (repeatMode == RepeatMode.ALL && activeQueue != null && activeQueue.length > 0) {
                queueIndex = 0;
                loadCurrent(true);
            } else {
                player.pause();
                player.seekTo(0);
                notifyIsPlaying(false);
            }
        }
    }
    private int[] buildShuffledQueueKeepingCurrent(int[] source, int currentId) {
        if (source == null || source.length == 0) return source;

        ArrayList<Integer> rest = new ArrayList<>();
        boolean found = false;

        for (int id : source) {
            if (id == currentId && !found) found = true;
            else rest.add(id);
        }

        if (!found) {
            ArrayList<Integer> all = new ArrayList<>();
            for (int id : source) all.add(id);
            Collections.shuffle(all, new Random(System.nanoTime()));
            int[] out = new int[all.size()];
            for (int i = 0; i < all.size(); i++) out[i] = all.get(i);
            return out;
        }

        Collections.shuffle(rest, new Random(System.nanoTime()));

        int[] out = new int[source.length];
        out[0] = currentId;
        for (int i = 0; i < rest.size(); i++) out[i + 1] = rest.get(i);
        return out;
    }
    private int indexOf(int[] arr, int id) {
        if (arr == null) return 0;
        for (int i = 0; i < arr.length; i++) {
            if (arr[i] == id) return i;
        }
        return 0;
    }
}