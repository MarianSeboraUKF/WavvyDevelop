package sk.ukf.wavvy;

import java.util.Collections;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaMetadataRetriever;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.palette.graphics.Palette;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import sk.ukf.wavvy.adapter.AlbumSongAdapter;
import sk.ukf.wavvy.model.Album;
import sk.ukf.wavvy.model.Song;

public class AlbumDetailActivity extends AppCompatActivity implements PlaybackManager.Listener {
    private TextView tvAlbumTitle;
    private TextView tvAlbumMeta;
    private ImageView ivAlbumCover;
    private RecyclerView rvAlbumSongs;
    private TextView tvAlbumArtist;
    private ArrayList<Song> songsInAlbum;
    private TextView btnShuffleAlbum;
    private View miniPlayer;
    private ImageView ivMiniCover;
    private TextView tvMiniTitle;
    private TextView tvMiniArtist;
    private ImageButton btnMiniPrev;
    private ImageButton btnMiniPlay;
    private ImageButton btnMiniNext;
    private ProgressBar miniProgress;
    private TextView btnPlayAlbum;
    private PlaybackManager pm;
    private AlbumSongAdapter adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_album_detail);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        View root = findViewById(R.id.albumRoot);

        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            int top = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            v.setPadding(0, top, 0, 0);
            return insets;
        });

        pm = PlaybackManager.get(this);

        tvAlbumTitle = findViewById(R.id.tvAlbumTitle);
        tvAlbumMeta = findViewById(R.id.tvAlbumMeta);
        ivAlbumCover = findViewById(R.id.ivAlbumCover);
        rvAlbumSongs = findViewById(R.id.rvAlbumSongs);
        tvAlbumArtist = findViewById(R.id.tvAlbumArtist);
        btnPlayAlbum = findViewById(R.id.btnPlayAlbum);
        btnShuffleAlbum = findViewById(R.id.btnShuffleAlbum);
        miniPlayer = findViewById(R.id.miniPlayer);
        ivMiniCover = findViewById(R.id.ivMiniCover);
        tvMiniTitle = findViewById(R.id.tvMiniTitle);
        tvMiniArtist = findViewById(R.id.tvMiniArtist);
        btnMiniPrev = findViewById(R.id.btnMiniPrev);
        btnMiniPlay = findViewById(R.id.btnMiniPlay);
        btnMiniNext = findViewById(R.id.btnMiniNext);
        miniProgress = findViewById(R.id.miniProgress);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        miniPlayer.setOnClickListener(v -> PlayerLauncher.openExisting(this));
        btnMiniPlay.setOnClickListener(v -> pm.togglePlayPause());
        btnMiniPrev.setOnClickListener(v -> pm.playPrev(true));
        btnMiniNext.setOnClickListener(v -> pm.playNext(true));

        String albumTitle = getIntent().getStringExtra("album_title");

        Album album = AlbumRepository.findByTitle(albumTitle);

        if (album == null) {
            finish();
            return;
        }

        tvAlbumTitle.setText(album.getTitle());
        tvAlbumArtist.setText(album.getArtist());
        ivAlbumCover.setImageResource(album.getCoverResId());
        songsInAlbum = album.getSongs();

        btnPlayAlbum.setOnClickListener(v -> {
            if (!songsInAlbum.isEmpty()) {
                PlayerLauncher.openQueue(this, songsInAlbum, songsInAlbum.get(0));
            }
        });

        btnShuffleAlbum.setOnClickListener(v -> {
            if (!songsInAlbum.isEmpty()) {
                ArrayList<Song> shuffled = new ArrayList<>(songsInAlbum);
                Collections.shuffle(shuffled);

                PlayerLauncher.openQueue(this, shuffled, shuffled.get(0));
            }
        });

        long totalMs = 0;
        for (Song s : songsInAlbum) {
            totalMs += getDurationMsFromRaw(s.getAudioResId());
        }

        String songLabel;

        if (songsInAlbum.size() == 1) {
            songLabel = "1 song";
        } else {
            songLabel = songsInAlbum.size() + " songs";
        }

        tvAlbumMeta.setText(songLabel + " • " + formatDuration(totalMs));
        rvAlbumSongs.setLayoutManager(new LinearLayoutManager(this));

        adapter = new AlbumSongAdapter(
                songsInAlbum,
                song -> PlayerLauncher.openQueue(this, songsInAlbum, song)
        );
        rvAlbumSongs.setAdapter(adapter);
        applyGradient(album.getCoverResId());
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

        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }
    @Override
    public void onIsPlayingChanged(boolean isPlaying) {
        updateMiniPlayerUi();
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
        if (!NowPlayingRepository.hasNowPlaying(this)) {
            miniPlayer.setVisibility(View.GONE);
            return;
        }

        Song s = SongRepository.findByAudioResId(
                NowPlayingRepository.getAudioResId(this)
        );

        if (s == null) {
            miniPlayer.setVisibility(View.GONE);
            return;
        }

        miniPlayer.setVisibility(View.VISIBLE);
        ivMiniCover.setImageResource(s.getCoverResId());
        tvMiniTitle.setText(s.getTitle());
        tvMiniArtist.setText(s.getArtist());

        btnMiniPlay.setImageResource(
                pm.isPlaying() ? R.drawable.ic_pause : R.drawable.ic_play
        );
    }
    private void applyGradient(int coverResId) {
        View root = findViewById(R.id.albumRoot);

        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), coverResId);

        if (bitmap == null) return;

        Palette.from(bitmap).generate(palette -> {
            int dominant = palette.getDominantColor(
                    ContextCompat.getColor(this, R.color.bg)
            );

            int dark = palette.getDarkMutedColor(dominant);
            int vibrant = palette.getVibrantColor(dominant);

            GradientDrawable gd = new GradientDrawable(
                    GradientDrawable.Orientation.TOP_BOTTOM,
                    new int[]{
                            vibrant,
                            dark,
                            ContextCompat.getColor(this, R.color.bg)
                    }
            );

            root.setBackground(gd);
        });
    }
    private long getDurationMsFromRaw(int rawResId) {
        MediaMetadataRetriever mmr = new MediaMetadataRetriever();

        try {
            android.content.res.AssetFileDescriptor afd =
                    getResources().openRawResourceFd(rawResId);

            if (afd == null) return 0;

            mmr.setDataSource(
                    afd.getFileDescriptor(),
                    afd.getStartOffset(),
                    afd.getLength()
            );

            String dur =
                    mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);

            afd.close();

            if (dur == null) return 0;
            return Long.parseLong(dur);

        } catch (Exception e) {
            return 0;
        } finally {
            try {
                mmr.release();
            } catch (Exception ignored) {}
        }
    }
    private String formatDuration(long ms) {
        long totalSeconds = ms / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;

        if (seconds == 0) {
            return minutes + " min";
        }
        return minutes + " min " + String.format("%02d", seconds) + " sec";
    }
}