package sk.ukf.wavvy;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import android.widget.ProgressBar;
import sk.ukf.wavvy.model.Song;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

public class MainActivity extends AppCompatActivity implements PlaybackManager.Listener {
    private BottomNavigationView bottomNav;
    private ConstraintLayout miniPlayer;
    private ImageView ivMiniCover;
    private TextView tvMiniTitle;
    private TextView tvMiniArtist;
    private ImageButton btnMiniPrev;
    private ImageButton btnMiniPlay;
    private ImageButton btnMiniNext;
    private ProgressBar miniProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        WindowInsetsControllerCompat insets =
                new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        insets.setAppearanceLightStatusBars(false);

        bottomNav = findViewById(R.id.bottomNav);

        if (savedInstanceState == null) {
            loadFragment(new HomeFragment());
        }

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                loadFragment(new HomeFragment());
                return true;
            } else if (id == R.id.nav_search) {
                loadFragment(new SearchFragment());
                return true;
            } else if (id == R.id.nav_playlists) {
                loadFragment(new PlaylistsFragment());
                return true;
            }
            return false;
        });

        miniPlayer = findViewById(R.id.miniPlayer);
        ivMiniCover = findViewById(R.id.ivMiniCover);
        tvMiniTitle = findViewById(R.id.tvMiniTitle);
        tvMiniArtist = findViewById(R.id.tvMiniArtist);

        btnMiniPrev = findViewById(R.id.btnMiniPrev);
        btnMiniPlay = findViewById(R.id.btnMiniPlay);
        btnMiniNext = findViewById(R.id.btnMiniNext);
        miniProgress = findViewById(R.id.miniProgress);

        miniPlayer.setOnClickListener(v -> openPlayerFromNowPlaying());
        btnMiniPlay.setOnClickListener(v -> PlaybackManager.get(this).togglePlayPause());

        btnMiniPrev.setOnClickListener(v -> {
            PlaybackManager pm = PlaybackManager.get(this);
            if (pm.getPlayer() != null && pm.getPlayer().getCurrentPosition() > 3000) {
                pm.getPlayer().seekTo(0);
                return;
            }
            pm.playPrev(true);
        });
        btnMiniNext.setOnClickListener(v -> PlaybackManager.get(this).playNext(true));
    }
    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.navHost, fragment)
                .commit();
    }

    @Override
    protected void onStart() {
        super.onStart();
        PlaybackManager.get(this).addListener(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        PlaybackManager.get(this).removeListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateMiniPlayer();
    }
    @Override
    public void onNowPlayingChanged(int audioResId, int[] queueIds, int queueIndex) {
        updateMiniPlayer();
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

        if (durationMs > 0) {
            miniProgress.setMax((int) durationMs);
            miniProgress.setProgress((int) positionMs);
        } else {
            miniProgress.setProgress(0);
        }
    }
    private void updateMiniPlayer() {
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

        PlaybackManager pm = PlaybackManager.get(this);
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
        PlaybackManager pm = PlaybackManager.get(this);
        int[] q = pm.getQueueIds();
        int idx = pm.getQueueIndex();

        if (q == null || q.length == 0) return;

        Intent intent = new Intent(this, PlayerActivity.class);
        intent.putExtra(PlayerActivity.EXTRA_QUEUE_AUDIO_IDS, q);
        intent.putExtra(PlayerActivity.EXTRA_QUEUE_INDEX, idx);
        intent.putExtra(PlayerActivity.EXTRA_OPEN_EXISTING, true);
        intent.putExtra(PlayerActivity.EXTRA_AUTOPLAY, false);
        startActivity(intent);
    }
}