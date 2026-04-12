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
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v4.media.session.MediaSessionCompat;
import sk.ukf.wavvy.model.Song;
import android.support.v4.media.MediaMetadataCompat;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

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
    private int lastCountedAudioId = -1;
    private boolean shuffleEnabled = false;
    private RepeatMode repeatMode = RepeatMode.OFF;
    private final List<WeakReference<Listener>> listeners = new ArrayList<>();
    private final android.os.Handler progressHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private static final String PREFS = "wavvy_playback_prefs";
    private static final String KEY_SHUFFLE = "shuffle";
    private static final String KEY_REPEAT = "repeat";
    private long lastSave = 0;
    private MediaNotificationManager notificationManager;
    private MediaSessionCompat mediaSession;
    private final Runnable progressTick = new Runnable() {
        @Override public void run() {
            if (player != null) {
                long pos = player.getCurrentPosition();
                long dur = player.getDuration();
                if (dur < 0) dur = 0;

                notifyProgress(pos, dur);
                long now = System.currentTimeMillis();

                if (player.isPlaying() && now - lastSave > 3000) {
                    NowPlayingRepository.savePosition(appContext, pos);
                    lastSave = now;
                }
            }
            progressHandler.postDelayed(this, 250);
        }
    };
    public void saveCurrentPositionNow() {
        if (player == null) return;
        long pos = player.getCurrentPosition();
        NowPlayingRepository.savePosition(appContext, pos);
    }
    private PlaybackManager(Context appContext) {
        this.appContext = appContext;
        this.player = new ExoPlayer.Builder(appContext).build();

        notificationManager = new MediaNotificationManager(appContext);
        mediaSession = new MediaSessionCompat(appContext, "WavvySession");
        mediaSession.setActive(true);

        restoreFromNowPlayingRepository();
        loadPlaybackSettings();

        player.addListener(new Player.Listener() {
            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                notifyIsPlaying(isPlaying);
                updateNotification();

                if (isPlaying && currentAudioResId != 0 && currentAudioResId != lastCountedAudioId) {
                    PlayCountRepository.increment(appContext, currentAudioResId);
                    lastCountedAudioId = currentAudioResId;
                }
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
    private void updateNotification() {
        if (currentAudioResId == 0) return;

        Song song = SongRepository.findByAudioResId(appContext, currentAudioResId);
        if (song == null) return;
        long duration = player.getDuration();
        if (duration < 0) duration = 0;

        Bitmap cover;
        if (song.getCoverUri() != null) {
            try {
                Uri uri = Uri.parse(song.getCoverUri());
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                    cover = ImageDecoder.decodeBitmap(ImageDecoder.createSource(appContext.getContentResolver(), uri));
                } else {
                    cover = MediaStore.Images.Media.getBitmap(appContext.getContentResolver(), uri);
                }
            } catch (Exception e) {
                cover = BitmapFactory.decodeResource(appContext.getResources(), song.getCoverResId());
            }
        } else {
            cover = BitmapFactory.decodeResource(appContext.getResources(), song.getCoverResId());
        }

        MediaMetadataCompat metadata = new MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, song.getTitle())
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, song.getArtist())
                .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, cover)
                .build();
        mediaSession.setMetadata(metadata);
        notificationManager.showNotification(song, player.isPlaying(), mediaSession, player.getCurrentPosition(), duration);
    }
    public static void loadOnlineMetadata(Context ctx, Song song, Runnable onDone) {
        new Thread(() -> {
            android.media.MediaMetadataRetriever mmr = new android.media.MediaMetadataRetriever();
            try {
                mmr.setDataSource(song.getDownloadUrl(), new java.util.HashMap<>());

                String title = mmr.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_TITLE);
                String artist = mmr.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_ARTIST);
                String album = mmr.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_ALBUM);
                String duration = mmr.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION);

                byte[] art = mmr.getEmbeddedPicture();

                if (title != null) song.setTitle(title);
                if (artist != null) song.setMainArtist(artist);
                if (album != null) song.setAlbum(album);
                if (duration != null) song.setDurationMs(Long.parseLong(duration));

                if (art != null) {
                    java.io.File file = new java.io.File(ctx.getCacheDir(), "cover_" + song.getAudioResId() + ".png");
                    java.io.FileOutputStream fos = new java.io.FileOutputStream(file);
                    fos.write(art);
                    fos.close();
                    song.setCoverUri(file.toURI().toString());
                }

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try { mmr.release(); } catch (Exception ignored) {}
            }

            new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                PlaybackManager pm = PlaybackManager.get(ctx);
                pm.refreshCurrentSong();
                if (onDone != null) {
                    onDone.run();
                }
            });
        }).start();
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

        Song s = SongRepository.findByAudioResId(appContext, currentAudioResId);
        MediaItem item;
        if (s != null && s.getUriString() != null) {
            item = MediaItem.fromUri(s.getUriString());
        } else {
            item = MediaItem.fromUri("android.resource://" + appContext.getPackageName() + "/" + currentAudioResId);
        }

        player.setMediaItem(item);
        player.prepare();

        long savedPos = NowPlayingRepository.getPosition(appContext);
        if (savedPos > 0) {
            player.seekTo(savedPos);
        }
        notifyProgress(savedPos, player.getDuration());
        notifyIsPlaying(player.isPlaying());
        notifyNowPlaying();
    }
    private void savePlaybackSettings() {
        appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_SHUFFLE, shuffleEnabled)
                .putString(KEY_REPEAT, repeatMode.name())
                .apply();
    }
    private void loadPlaybackSettings() {
        android.content.SharedPreferences sp = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
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
        Song s = SongRepository.findByAudioResId(appContext, currentAudioResId);
        if (s != null) {
            notificationManager.showNotification(s, player.isPlaying(), mediaSession, player.getCurrentPosition(), player.getDuration());
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
        if (player.isPlaying()) {
            player.pause();
        } else {
            player.play();
        }
        updateNotification();
    }
    public boolean playPrev(boolean autoPlay) {
        ensureQueueLoadedIfPossible();
        if (activeQueue == null || activeQueue.length == 0) return false;

        if (queueIndex > 0) {
            queueIndex--;
            loadCurrent(autoPlay);
            updateNotification();
            return true;
        }

        if (repeatMode == RepeatMode.ALL && activeQueue.length > 1) {
            queueIndex = activeQueue.length - 1;
            loadCurrent(autoPlay);
            updateNotification();
            return true;
        }
        return false;
    }
    public boolean playNext(boolean autoPlay) {
        ensureQueueLoadedIfPossible();
        if (activeQueue == null || activeQueue.length == 0) return false;
        if (queueIndex < activeQueue.length - 1) {
            queueIndex++;
            loadCurrent(autoPlay);
            updateNotification();
            return true;
        }
        if (repeatMode == RepeatMode.ALL && activeQueue.length > 1) {
            queueIndex = 0;
            loadCurrent(autoPlay);
            updateNotification();
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
    public void setShuffle(boolean enabled) {
        ensureQueueLoadedIfPossible();

        if (shuffleEnabled == enabled) return;
        shuffleEnabled = enabled;

        if (originalQueue == null || originalQueue.length <= 1) {
            savePlaybackSettings();
            notifyNowPlaying();
            return;
        }

        int keepId = currentAudioResId;

        if (shuffleEnabled) {
            activeQueue = buildShuffledQueueKeepingCurrent(originalQueue, keepId);
            queueIndex = 0;
        } else {
            activeQueue = originalQueue.clone();
            queueIndex = indexOf(activeQueue, keepId);
        }
        savePlaybackSettings();
        notifyNowPlaying();
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
        lastCountedAudioId = -1;

        Song s = SongRepository.findByAudioResId(appContext, currentAudioResId);
        MediaItem item;

        if (s != null && s.getDownloadUrl() != null && !s.getDownloadUrl().isEmpty()) {
            if (!hasInternet()) {
                android.widget.Toast.makeText(appContext, "No internet", android.widget.Toast.LENGTH_SHORT).show();
                return;
            }

            if (s.getDurationMs() == 0) {
                PlaybackManager.loadOnlineMetadata(appContext, s, null);
            }
            MediaItem track = MediaItem.fromUri(s.getDownloadUrl());
            player.setMediaItem(track);
            player.prepare();

            NowPlayingRepository.saveNowPlaying(appContext, currentAudioResId, activeQueue, queueIndex);

            if (autoPlay) {
                RecentlyPlayedRepository.add(appContext, currentAudioResId);
                player.play();
            }
            notifyNowPlaying();
            updateNotification();
            return;
        }
        else if (s != null && s.getUriString() != null) {
            item = MediaItem.fromUri(s.getUriString());
        } else {
            item = MediaItem.fromUri("android.resource://" + appContext.getPackageName() + "/" + currentAudioResId);
        }
        player.setMediaItem(item);
        player.prepare();

        NowPlayingRepository.saveNowPlaying(appContext, currentAudioResId, activeQueue, queueIndex);
        if (autoPlay) {
            RecentlyPlayedRepository.add(appContext, currentAudioResId);
        }
        notifyNowPlaying();

        if (autoPlay) {
            player.play();
        }
        updateNotification();
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
                notificationManager.cancel();
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
    public void removeFromQueue(int position) {
        if (activeQueue == null || position < 0 || position >= activeQueue.length) return;

        int[] newQueue = new int[activeQueue.length - 1];
        int j = 0;
        for (int i = 0; i < activeQueue.length; i++) {
            if (i == position) continue;
            newQueue[j++] = activeQueue[i];
        }
        activeQueue = newQueue;

        if (queueIndex > position) {
            queueIndex--;
        } else if (queueIndex == position) {
            if (activeQueue.length > 0) {
                queueIndex = Math.min(queueIndex, activeQueue.length - 1);
                loadCurrent(true);
            } else {
                player.stop();
                notificationManager.cancel();
                mediaSession.setActive(false);
            }
        }
        NowPlayingRepository.saveNowPlaying(appContext, currentAudioResId, activeQueue, queueIndex);
        notifyNowPlaying();
    }
    public void playFromQueue(int position) {
        ensureQueueLoadedIfPossible();
        if (activeQueue == null || position < 0 || position >= activeQueue.length) return;
        queueIndex = position;
        loadCurrent(true);
    }
    public void refreshCurrentSong() {
        if (currentAudioResId == 0) return;
        notifyNowPlaying();
        updateNotification();
    }
    public void moveQueueItem(int from, int to) {
        if (activeQueue == null) return;
        if (from < 0 || to < 0 || from >= activeQueue.length || to >= activeQueue.length) return;

        int moved = activeQueue[from];

        if (from < to) {
            for (int i = from; i < to; i++) {
                activeQueue[i] = activeQueue[i + 1];
            }
        } else {
            for (int i = from; i > to; i--) {
                activeQueue[i] = activeQueue[i - 1];
            }
        }
        activeQueue[to] = moved;

        if (queueIndex == from) {
            queueIndex = to;
        } else if (from < queueIndex && to >= queueIndex) {
            queueIndex--;
        } else if (from > queueIndex && to <= queueIndex) {
            queueIndex++;
        }
        NowPlayingRepository.saveNowPlaying(appContext, currentAudioResId, activeQueue, queueIndex);
        notifyNowPlaying();
    }
    private boolean hasInternet() {
        android.net.ConnectivityManager cm = (android.net.ConnectivityManager) appContext.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (cm == null) return false;

        android.net.Network network = cm.getActiveNetwork();
        if (network == null) return false;

        android.net.NetworkCapabilities caps = cm.getNetworkCapabilities(network);
        return caps != null && (caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) || caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR));
    }
    public void insertNext(int audioResId) {
        ensureQueueLoadedIfPossible();

        if (activeQueue == null || activeQueue.length == 0) {
            playQueue(new int[]{audioResId}, 0, false);
            return;
        }

        ArrayList<Integer> queue = new ArrayList<>();

        for (int id : activeQueue) {
            if (id != audioResId) {
                queue.add(id);
            }
        }

        int insertPos = queueIndex + 1;
        queue.add(insertPos, audioResId);
        activeQueue = new int[queue.size()];

        for (int i = 0; i < queue.size(); i++) {
            activeQueue[i] = queue.get(i);
        }
        NowPlayingRepository.saveNowPlaying(appContext, currentAudioResId, activeQueue, queueIndex);
        notifyNowPlaying();
    }
}