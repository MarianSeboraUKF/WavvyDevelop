package sk.ukf.wavvy;

import android.content.Intent;
import android.media.MediaMetadataRetriever;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.widget.ProgressBar;
import java.util.ArrayList;
import sk.ukf.wavvy.adapter.SongAdapter;
import sk.ukf.wavvy.model.Playlist;
import sk.ukf.wavvy.model.Song;

public class PlaylistDetailActivity extends AppCompatActivity implements PlaybackManager.Listener {
    public static final String EXTRA_PLAYLIST_ID = "playlist_id";
    public static final String EXTRA_PLAYLIST_NAME = "playlist_name";
    private TextView tvPlaylistTitle;
    private TextView tvPlaylistMeta;
    private RecyclerView rvPlaylistSongs;
    private SongAdapter adapter;
    private ArrayList<Song> songsInPlaylist;
    private String playlistId;
    private String playlistName = "Playlist";
    private View miniPlayer;
    private ImageView ivMiniCover;
    private TextView tvMiniTitle;
    private TextView tvMiniArtist;
    private ImageButton btnMiniPrev;
    private ImageButton btnMiniPlay;
    private ImageButton btnMiniNext;
    private ProgressBar miniProgress;
    private PlaybackManager pm;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playlist_detail);

        pm = PlaybackManager.get(this);

        tvPlaylistTitle = findViewById(R.id.tvPlaylistTitle);
        tvPlaylistMeta = findViewById(R.id.tvPlaylistMeta);
        rvPlaylistSongs = findViewById(R.id.rvPlaylistSongs);

        Intent intent = getIntent();
        playlistId = intent.getStringExtra(EXTRA_PLAYLIST_ID);

        String nameFromIntent = intent.getStringExtra(EXTRA_PLAYLIST_NAME);
        if (nameFromIntent != null && !nameFromIntent.trim().isEmpty()) {
            playlistName = nameFromIntent.trim();
        }
        tvPlaylistTitle.setText(playlistName);

        rvPlaylistSongs.setLayoutManager(new LinearLayoutManager(this));
        songsInPlaylist = new ArrayList<>();

        adapter = new SongAdapter(
                songsInPlaylist,
                song -> PlayerLauncher.openQueue(PlaylistDetailActivity.this, songsInPlaylist, song),
                this::showRemoveFromPlaylistDialog
        );
        rvPlaylistSongs.setAdapter(adapter);

        miniPlayer = findViewById(R.id.miniPlayer);
        ivMiniCover = findViewById(R.id.ivMiniCover);
        tvMiniTitle = findViewById(R.id.tvMiniTitle);
        tvMiniArtist = findViewById(R.id.tvMiniArtist);
        btnMiniPrev = findViewById(R.id.btnMiniPrev);
        btnMiniPlay = findViewById(R.id.btnMiniPlay);
        btnMiniNext = findViewById(R.id.btnMiniNext);
        miniProgress = findViewById(R.id.miniProgress);

        miniPlayer.setOnClickListener(v -> openPlayerFromNowPlaying());

        btnMiniPlay.setOnClickListener(v -> pm.togglePlayPause());
        btnMiniPrev.setOnClickListener(v -> {
            if (pm.getPlayer() != null && pm.getPlayer().getCurrentPosition() > 3000) {
                pm.getPlayer().seekTo(0);
                return;
            }
            pm.playPrev(true);
        });
        btnMiniNext.setOnClickListener(v -> pm.playNext(true));

        loadSongs();
        for (Song s : songsInPlaylist) {
            GradientPreloader.preload(this, s.getCoverResId());
        }
        updateMiniPlayerUi();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadSongs();
        updateMiniPlayerUi();
    }

    @Override
    protected void onStart() {
        super.onStart();
        pm.addListener(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        pm.removeListener(this);
    }
    @Override
    public void onNowPlayingChanged(int audioResId, int[] queueIds, int queueIndex) {
        updateMiniPlayerUi();
    }
    @Override
    public void onIsPlayingChanged(boolean isPlaying) {
        if (btnMiniPlay != null) {
            btnMiniPlay.setImageResource(isPlaying ? R.drawable.ic_pause : R.drawable.ic_play);
        }
    }
    @Override
    public void onProgress(long positionMs, long durationMs) {
        if (miniProgress == null) return;

        if (durationMs <= 0) {
            miniProgress.setIndeterminate(true);
            return;
        }

        miniProgress.setIndeterminate(false);
        miniProgress.setMax((int) durationMs);
        miniProgress.setProgress((int) positionMs);
    }
    private void updateMiniPlayerUi() {
        if (miniPlayer == null) return;

        if (!NowPlayingRepository.hasNowPlaying(this)) {
            miniPlayer.setVisibility(View.GONE);
            return;
        }

        int audioResId = NowPlayingRepository.getAudioResId(this);
        Song s = SongRepository.findByAudioResId(audioResId);

        if (s == null) {
            miniPlayer.setVisibility(View.GONE);
            return;
        }

        miniPlayer.setVisibility(View.VISIBLE);
        ivMiniCover.setImageResource(s.getCoverResId());
        tvMiniTitle.setText(s.getTitle());
        tvMiniArtist.setText(s.getArtist());

        btnMiniPlay.setImageResource(pm.isPlaying() ? R.drawable.ic_pause : R.drawable.ic_play);

        int[] q = pm.getQueueIds();
        int idx = pm.getQueueIndex();
        boolean hasQueue = q != null && q.length > 1;

        boolean prevEnabled = hasQueue && (idx > 0 || pm.getRepeatMode() == PlaybackManager.RepeatMode.ALL);
        boolean nextEnabled = hasQueue && (idx < q.length - 1 || pm.getRepeatMode() == PlaybackManager.RepeatMode.ALL);

        btnMiniPrev.setEnabled(prevEnabled);
        btnMiniNext.setEnabled(nextEnabled);

        float disabledAlpha = 0.35f;
        btnMiniPrev.setAlpha(prevEnabled ? 1f : disabledAlpha);
        btnMiniNext.setAlpha(nextEnabled ? 1f : disabledAlpha);
    }
    private void openPlayerFromNowPlaying() {
        int[] q = NowPlayingRepository.getQueueIds(this);
        int idx = NowPlayingRepository.getQueueIndex(this);

        if (q == null || q.length == 0) return;

        Intent i = new Intent(this, PlayerActivity.class);
        i.putExtra(PlayerActivity.EXTRA_QUEUE_AUDIO_IDS, q);
        i.putExtra(PlayerActivity.EXTRA_QUEUE_INDEX, idx);
        i.putExtra(PlayerActivity.EXTRA_OPEN_EXISTING, true);
        startActivity(i);
    }
    private void showRemoveFromPlaylistDialog(Song song) {
        View card = LayoutInflater.from(this).inflate(R.layout.dialog_remove_song, null);

        TextView tvMessage = card.findViewById(R.id.tvMessage);
        View btnRemove = card.findViewById(R.id.btnRemove);
        View btnCancel = card.findViewById(R.id.btnCancel);

        tvMessage.setText("Odstrániť „" + song.getTitle() + "“ z playlistu?");

        android.app.Dialog dialog =
                WavvyDialogs.showCenteredCardDialog(this, this, card);

        btnRemove.setOnClickListener(x -> {
            if (playlistId != null) {
                PlaylistRepository.removeSongFromPlaylist(this, playlistId, song.getAudioResId());
                dialog.dismiss();
                loadSongs();
            }
        });
        btnCancel.setOnClickListener(x -> dialog.dismiss());
    }
    private void loadSongs() {
        songsInPlaylist.clear();

        if (playlistId == null) {
            tvPlaylistMeta.setText("0 skladieb • 0:00");
            adapter.notifyDataSetChanged();
            return;
        }

        Playlist p = PlaylistRepository.findById(this, playlistId);
        if (p == null) {
            tvPlaylistMeta.setText("0 skladieb • 0:00");
            adapter.notifyDataSetChanged();
            return;
        }

        for (Integer audioResId : p.getSongAudioResIds()) {
            Song s = SongRepository.findByAudioResId(audioResId);
            if (s != null) songsInPlaylist.add(s);
        }

        long totalMs = 0;
        for (Song s : songsInPlaylist) {
            totalMs += getDurationMsFromRaw(s.getAudioResId());
        }

        tvPlaylistMeta.setText(songsInPlaylist.size() + " skladieb • " + formatDuration(totalMs));
        adapter.notifyDataSetChanged();
    }
    private long getDurationMsFromRaw(int rawResId) {
        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        try {
            android.content.res.AssetFileDescriptor afd = getResources().openRawResourceFd(rawResId);
            if (afd == null) return 0;

            mmr.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            String dur = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            afd.close();

            if (dur == null) return 0;
            return Long.parseLong(dur);
        } catch (Exception e) {
            return 0;
        } finally {
            try { mmr.release(); } catch (Exception ignored) {}
        }
    }
    private String formatDuration(long ms) {
        long totalSeconds = ms / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return minutes + ":" + String.format("%02d", seconds);
    }
}