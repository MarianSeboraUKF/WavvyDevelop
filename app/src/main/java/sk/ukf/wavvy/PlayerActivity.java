package sk.ukf.wavvy;

import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.GradientDrawable;
import androidx.palette.graphics.Palette;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.media3.exoplayer.ExoPlayer;
import sk.ukf.wavvy.model.Song;

public class PlayerActivity extends AppCompatActivity implements PlaybackManager.Listener {
    public static final String EXTRA_QUEUE_AUDIO_IDS = "queue_audio_ids";
    public static final String EXTRA_QUEUE_INDEX = "queue_index";
    public static final String EXTRA_OPEN_EXISTING = "open_existing";
    public static final String EXTRA_AUTOPLAY = "autoplay";
    private ExoPlayer player;
    private PlaybackManager pm;
    private ImageButton btnBack, btnMore;
    private ImageButton btnShuffle, btnPrev, btnPlayPause, btnNext, btnRepeat;
    private SeekBar seekBar;
    private TextView tvCurrentTime, tvTotalTime;
    private TextView tvSongTitle, tvSongArtist;
    private ImageView ivCover;
    private TextView tvPlaybackStatus;
    private boolean isUserSeeking = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        WindowInsetsControllerCompat insets =
                new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        insets.setAppearanceLightStatusBars(false);

        pm = PlaybackManager.get(this);
        player = pm.getPlayer();

        btnBack = findViewById(R.id.btnBack);
        btnMore = findViewById(R.id.btnMore);

        tvSongTitle = findViewById(R.id.tvSongTitle);
        tvSongArtist = findViewById(R.id.tvSongArtist);
        ivCover = findViewById(R.id.ivCover);

        btnShuffle = findViewById(R.id.btnShuffle);
        btnPrev = findViewById(R.id.btnPrev);
        btnPlayPause = findViewById(R.id.btnPlayPause);
        btnNext = findViewById(R.id.btnNext);
        btnRepeat = findViewById(R.id.btnRepeat);
        tvPlaybackStatus = findViewById(R.id.tvPlaybackStatus);

        seekBar = findViewById(R.id.seekBar);
        tvCurrentTime = findViewById(R.id.tvCurrentTime);
        tvTotalTime = findViewById(R.id.tvTotalTime);

        btnBack.setOnClickListener(v -> finish());
        btnMore.setOnClickListener(v -> android.widget.Toast.makeText(this, "More Soon", android.widget.Toast.LENGTH_SHORT).show());

        btnPlayPause.setOnClickListener(v -> pm.togglePlayPause());
        btnNext.setOnClickListener(v -> pm.playNext(true));
        btnPrev.setOnClickListener(v -> {
            if (player != null && player.getCurrentPosition() > 3000) {
                player.seekTo(0);
                return;
            }
            pm.playPrev(true);
        });

        btnShuffle.setOnClickListener(v -> {
            pm.toggleShuffle();
            updateShuffleUi();
            updatePlaybackStatusText();
            updateNavButtons();
        });

        btnRepeat.setOnClickListener(v -> {
            pm.cycleRepeatMode();
            updateRepeatUi();
            updatePlaybackStatusText();
            updateNavButtons();
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
                if (fromUser) tvCurrentTime.setText(formatTime(progress));
            }
            @Override public void onStartTrackingTouch(SeekBar sb) { isUserSeeking = true; }
            @Override public void onStopTrackingTouch(SeekBar sb) {
                isUserSeeking = false;
                if (player != null) player.seekTo(sb.getProgress());
            }
        });
        handleIntentPlayback();
        updateShuffleUi();
        updateRepeatUi();
        updatePlaybackStatusText();
        updateNowPlayingUiFromRepo();
        updatePlayPauseIcon();
        updateNavButtons();
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
    private void handleIntentPlayback() {

        boolean openExisting = getIntent().getBooleanExtra(EXTRA_OPEN_EXISTING, false);
        boolean autoPlay = getIntent().getBooleanExtra(EXTRA_AUTOPLAY, true);

        int[] ids = getIntent().getIntArrayExtra(EXTRA_QUEUE_AUDIO_IDS);
        int idx = getIntent().getIntExtra(EXTRA_QUEUE_INDEX, 0);

        if (openExisting
                && ids == null
                && pm.getCurrentAudioResId() != 0
                && pm.getQueueIds() != null) {
            return;
        }

        if (openExisting) {
            return;
        }

        if (ids == null || ids.length == 0) return;
        pm.playQueue(ids, idx, autoPlay);
    }
    @Override
    public void onNowPlayingChanged(int audioResId, int[] queueIds, int queueIndex) {
        updateNowPlayingUiFromRepo();
        updateNavButtons();
    }
    @Override
    public void onIsPlayingChanged(boolean isPlaying) {
        updatePlayPauseIcon();
    }
    @Override
    public void onProgress(long positionMs, long durationMs) {
        if (isUserSeeking) return;
        if (durationMs > 0) {
            seekBar.setMax((int) durationMs);
            tvTotalTime.setText(formatTime(durationMs));
        }
        seekBar.setProgress((int) positionMs);
        tvCurrentTime.setText(formatTime(positionMs));
    }
    private void updateNowPlayingUiFromRepo() {
        int audioResId = NowPlayingRepository.getAudioResId(this);
        Song s = SongRepository.findByAudioResId(audioResId);

        if (s != null) {
            tvSongTitle.setText(s.getTitle());
            tvSongArtist.setText(s.getArtist());
            ivCover.setImageResource(s.getCoverResId());

            applyDynamicGradient(s.getCoverResId());
        }
    }
    private void updatePlayPauseIcon() {
        if (player == null) return;
        btnPlayPause.setImageResource(player.isPlaying() ? R.drawable.ic_pause : R.drawable.ic_play);
    }
    private void updateNavButtons() {
        int[] q = pm.getQueueIds();
        int idx = pm.getQueueIndex();
        boolean hasQueue = q != null && q.length > 1;

        boolean prevEnabled = hasQueue && (idx > 0 || pm.getRepeatMode() == PlaybackManager.RepeatMode.ALL);
        boolean nextEnabled = hasQueue && (idx < q.length - 1 || pm.getRepeatMode() == PlaybackManager.RepeatMode.ALL);

        btnPrev.setEnabled(prevEnabled);
        btnNext.setEnabled(nextEnabled);

        float disabledAlpha = 0.35f;
        btnPrev.setAlpha(prevEnabled ? 1f : disabledAlpha);
        btnNext.setAlpha(nextEnabled ? 1f : disabledAlpha);
    }
    private void updateShuffleUi() {
        if (pm.isShuffleEnabled()) tintOn(btnShuffle);
        else tintOff(btnShuffle);
    }
    private void updateRepeatUi() {
        PlaybackManager.RepeatMode rm = pm.getRepeatMode();

        if (rm == PlaybackManager.RepeatMode.OFF) {
            btnRepeat.setImageResource(R.drawable.ic_repeat);
            tintOff(btnRepeat);
            return;
        }

        if (rm == PlaybackManager.RepeatMode.ONE) {
            btnRepeat.setImageResource(R.drawable.ic_repeat_one);
        } else {
            btnRepeat.setImageResource(R.drawable.ic_repeat);
        }
        tintOn(btnRepeat);
    }
    private void updatePlaybackStatusText() {
        if (tvPlaybackStatus == null) return;

        String shuffle = pm.isShuffleEnabled() ? "Shuffle ON" : "Shuffle OFF";

        String repeat;
        PlaybackManager.RepeatMode rm = pm.getRepeatMode();
        if (rm == PlaybackManager.RepeatMode.OFF) repeat = "Repeat OFF";
        else if (rm == PlaybackManager.RepeatMode.ONE) repeat = "Repeat ONE";
        else repeat = "Repeat ALL";

        tvPlaybackStatus.setText(shuffle + " • " + repeat);

        boolean anyOn = pm.isShuffleEnabled() || rm != PlaybackManager.RepeatMode.OFF;
        tvPlaybackStatus.setTextColor(ContextCompat.getColor(
                this,
                anyOn ? R.color.accent : R.color.textSecondary
        ));
    }
    private void applyDynamicGradient(int coverResId) {
        if (GradientPrefs.has(this, coverResId)) {

            int vibrant = GradientPrefs.getVibrant(this, coverResId);
            int dark = GradientPrefs.getDark(this, coverResId);

            GradientDrawable gradient = new GradientDrawable(
                    GradientDrawable.Orientation.TOP_BOTTOM,
                    new int[]{vibrant, dark, ContextCompat.getColor(this, R.color.bg)}
            );

            findViewById(R.id.playerRoot).setBackground(gradient);
            return;
        }

        new Thread(() -> {
            Bitmap bitmap = BitmapFactory.decodeResource(getResources(), coverResId);
            if (bitmap == null) return;

            Palette.from(bitmap).generate(palette -> {

                int dominant = palette.getDominantColor(
                        ContextCompat.getColor(this, R.color.bg)
                );

                int dark = palette.getDarkMutedColor(dominant);
                int vibrant = palette.getVibrantColor(dominant);

                GradientPrefs.save(this, coverResId, vibrant, dark);

                GradientDrawable gradient = new GradientDrawable(
                        GradientDrawable.Orientation.TOP_BOTTOM,
                        new int[]{
                                vibrant,
                                dark,
                                ContextCompat.getColor(this, R.color.bg)
                        }
                );
                runOnUiThread(() ->
                        findViewById(R.id.playerRoot).setBackground(gradient)
                );
            });
        }).start();
    }
    private void tintOn(ImageButton btn) {
        btn.setColorFilter(ContextCompat.getColor(this, R.color.accent));
        btn.setAlpha(1.0f);
    }
    private void tintOff(ImageButton btn) {
        btn.setColorFilter(ContextCompat.getColor(this, R.color.textSecondary));
        btn.setAlpha(0.55f);
    }
    private String formatTime(long ms) {
        long totalSeconds = ms / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }
}