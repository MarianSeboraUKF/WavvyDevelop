package sk.ukf.wavvy;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.MotionEvent;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.GradientDrawable;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.palette.graphics.Palette;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.media3.exoplayer.ExoPlayer;
import com.google.android.material.imageview.ShapeableImageView;
import sk.ukf.wavvy.adapter.PickPlaylistAdapter;
import sk.ukf.wavvy.model.Playlist;
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
    private TextView tvSongAlbum;
    private TextView tvPlaybackStatus;
    private ImageView ivCover;
    private boolean isUserSeeking = false;
    private float startY;
    private long lastTrackChangeTime = 0;
    private static final int SWIPE_THRESHOLD = 180;
    private TextView tvBottomLabel, tvBottomTrack;
    private ImageView btnFavourite;
    private float startX;
    private Boolean lastIsPlaying = null;
    private int lastAnimatedAudioId = -1;
    private boolean ignorePlaybackChanges = false;
    private String lastTitle = "";
    private String lastArtist = "";

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);
        overridePendingTransition(R.anim.slide_in_right_fast, R.anim.slide_out_left_fast);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
        getWindow().setNavigationBarColor(android.graphics.Color.TRANSPARENT);

        View root = findViewById(R.id.playerRoot);
        root.setAlpha(0f);
        root.animate().alpha(1f).setDuration(350).start();

        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            int bottom = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom;
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), bottom);
            return insets;
        });

        WindowInsetsControllerCompat insets = new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        insets.setAppearanceLightStatusBars(false);

        pm = PlaybackManager.get(this);
        player = pm.getPlayer();
        btnBack = findViewById(R.id.btnBack);
        btnMore = findViewById(R.id.btnMore);
        tvSongTitle = findViewById(R.id.tvSongTitle);
        tvSongArtist = findViewById(R.id.tvSongArtist);
        tvSongAlbum = findViewById(R.id.tvSongAlbum);
        ivCover = findViewById(R.id.ivCover);

        ivCover.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    startY = event.getY();
                    startX = event.getX();
                    return true;

                case MotionEvent.ACTION_UP:

                    float endY = event.getY();
                    float endX = event.getX();

                    float deltaY = endY - startY;
                    float deltaX = endX - startX;

                    if (Math.abs(deltaX) > 180) {
                        if (deltaX > 0) {
                            pm.playPrev(true);
                        } else {
                            pm.playNext(true);
                        }
                        return true;
                    }
                    if (deltaY > SWIPE_THRESHOLD) {
                        finish();
                        overridePendingTransition(R.anim.slide_in_left_fast, R.anim.slide_out_right_fast);
                    }
                    v.performClick();
                    return true;
            }
            return false;
        });

        btnShuffle = findViewById(R.id.btnShuffle);
        btnPrev = findViewById(R.id.btnPrev);
        btnPlayPause = findViewById(R.id.btnPlayPause);
        btnNext = findViewById(R.id.btnNext);
        btnRepeat = findViewById(R.id.btnRepeat);
        tvPlaybackStatus = findViewById(R.id.tvPlaybackStatus);
        seekBar = findViewById(R.id.seekBar);
        tvCurrentTime = findViewById(R.id.tvCurrentTime);
        tvTotalTime = findViewById(R.id.tvTotalTime);
        tvBottomLabel = findViewById(R.id.tvBottomLabel);
        tvBottomTrack = findViewById(R.id.tvBottomTrack);
        btnFavourite = findViewById(R.id.btnFavourite);
        tvSongTitle.setSelected(true);
        tvBottomTrack.setSelected(true);

        btnFavourite.setOnClickListener(v -> {
            Song song = SongRepository.findByAudioResId(pm.getCurrentAudioResId());

            if (song == null) return;

            String songId = String.valueOf(song.getAudioResId());
            LikedSongsRepository.toggleLike(this, songId);
            LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent("LIKED_UPDATED"));
            boolean liked = LikedSongsRepository.isLiked(this, songId);

            if (liked) {
                btnFavourite.setImageResource(R.drawable.ic_liked);

                android.widget.Toast.makeText(
                        this,
                        "Added to favorites",
                        android.widget.Toast.LENGTH_SHORT
                ).show();
            } else {
                btnFavourite.setImageResource(R.drawable.ic_like);

                android.widget.Toast.makeText(
                        this,
                        "Removed from favorites",
                        android.widget.Toast.LENGTH_SHORT
                ).show();
            }
        });

        btnBack.setOnClickListener(v -> {
            finish();
            overridePendingTransition(R.anim.slide_in_left_fast, R.anim.slide_out_right_fast);
        });

        btnMore.setOnClickListener(v -> {
            Song song = SongRepository.findByAudioResId(pm.getCurrentAudioResId());

            if (song == null) return;
            View popupView = getLayoutInflater()
                    .inflate(R.layout.dialog_player_menu, null);

            android.widget.PopupWindow popupWindow = new android.widget.PopupWindow(
                    popupView,
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                    true
            );

            popupWindow.setBackgroundDrawable(
                    new android.graphics.drawable.ColorDrawable(
                            android.graphics.Color.TRANSPARENT
                    )
            );

            popupWindow.setElevation(16f);

            LinearLayout favAction = popupView.findViewById(R.id.actionAddToFavorites);
            ImageView favIcon = (ImageView) favAction.getChildAt(0);
            TextView favText = (TextView) favAction.getChildAt(1);

            String songId = String.valueOf(song.getAudioResId());
            boolean liked = LikedSongsRepository.isLiked(this, songId);

            if (liked) {
                favIcon.setImageResource(R.drawable.ic_liked);
                favIcon.setColorFilter(ContextCompat.getColor(this, R.color.accent));
                favText.setText("Remove from favorites");
            } else {
                favIcon.setImageResource(R.drawable.ic_like);
                favIcon.setColorFilter(ContextCompat.getColor(this, R.color.textPrimary));
                favText.setText("Add to favorites");
            }

            favAction.setOnClickListener(v1 -> {
                LikedSongsRepository.toggleLike(this, songId);
                LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent("LIKED_UPDATED"));

                boolean newLiked = LikedSongsRepository.isLiked(this, songId);

                if (newLiked) {
                    btnFavourite.setImageResource(R.drawable.ic_liked);

                    android.widget.Toast.makeText(
                            this,
                            "Added to favorites",
                            android.widget.Toast.LENGTH_SHORT
                    ).show();
                } else {
                    btnFavourite.setImageResource(R.drawable.ic_like);

                    android.widget.Toast.makeText(
                            this,
                            "Removed from favorites",
                            android.widget.Toast.LENGTH_SHORT
                    ).show();
                }
                popupWindow.dismiss();
            });

            popupView.findViewById(R.id.actionAddPlaylist).setOnClickListener(v1 -> {
                popupWindow.dismiss();

                java.util.ArrayList<Playlist> playlists =
                        PlaylistRepository.getPlaylists(this);

                if (playlists.isEmpty()) {
                    android.widget.Toast.makeText(
                            this,
                            "No playlists yet",
                            android.widget.Toast.LENGTH_SHORT
                    ).show();
                    return;
                }

                View dialogView = getLayoutInflater().inflate(R.layout.dialog_pick_playlist, null);
                androidx.recyclerview.widget.RecyclerView rv = dialogView.findViewById(R.id.rvPickPlaylists);
                android.widget.Button btnCancel = dialogView.findViewById(R.id.btnCancel);
                androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(this).setView(dialogView).create();

                if (dialog.getWindow() != null) {
                    dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
                }
                rv.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this));

                PickPlaylistAdapter pickAdapter = new PickPlaylistAdapter(
                        playlists,
                        playlist -> {
                            PlaylistRepository.addSongToPlaylist(this, playlist.getId(), song.getAudioResId());
                            android.widget.Toast.makeText(this, "Added to " + playlist.getName(), android.widget.Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                        }
                );
                rv.setAdapter(pickAdapter);
                btnCancel.setOnClickListener(v2 -> dialog.dismiss());
                dialog.show();
            });

            popupView.findViewById(R.id.actionGoAlbum).setOnClickListener(v1 -> {
                android.content.Intent intent = new android.content.Intent(this, AlbumDetailActivity.class);
                intent.putExtra("album_title", song.getAlbum());
                startActivity(intent);
                popupWindow.dismiss();
            });

            popupView.findViewById(R.id.actionInfo).setOnClickListener(v1 -> {
                popupWindow.dismiss();
                showSongInfo();
            });
            popupWindow.showAsDropDown(btnMore, -245, 0);
            LinearLayout actionDelete = popupView.findViewById(R.id.actionDeleteSong);

            if (song.getUriString() != null) {
                actionDelete.setVisibility(View.VISIBLE);
            } else {
                actionDelete.setVisibility(View.GONE);
            }

            actionDelete.setOnClickListener(v1 -> {
                View dialogView = getLayoutInflater().inflate(R.layout.dialog_delete_song, null);
                TextView tvMsg = dialogView.findViewById(R.id.tvMsg);
                TextView btnDelete = dialogView.findViewById(R.id.btnDelete);
                TextView btnCancel = dialogView.findViewById(R.id.btnCancel);
                tvMsg.setText("Do you really want to delete „" + song.getTitle() + "“?");
                android.app.Dialog dialog = WavvyDialogs.showCenteredCardDialog(this, this, dialogView);

                btnDelete.setOnClickListener(v2 -> {
                    SongRepository.deleteLocalSong(this, song.getAudioResId());
                    sendBroadcast(new Intent("songs_updated"));
                    pm.playNext(true);
                    android.widget.Toast.makeText(this, "Deleted", android.widget.Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                });
                btnCancel.setOnClickListener(v2 -> dialog.dismiss());
                popupWindow.dismiss();
            });
        });

        btnPlayPause.setOnClickListener(v -> {
            haptic(v);
            v.animate().scaleX(0.85f).scaleY(0.85f).setDuration(80).withEndAction(() -> v.animate().scaleX(1f).scaleY(1f).setDuration(120).start()).start();
            pm.togglePlayPause();
        });

        btnNext.setOnClickListener(v -> {
            haptic(v);
            if (pm.getRepeatMode() == PlaybackManager.RepeatMode.ONE) {
                if (player != null) {
                    player.seekTo(0);
                }
                return;
            }
            pm.playNext(true);
        });

        btnPrev.setOnClickListener(v -> {
            haptic(v);
            if (player != null && player.getCurrentPosition() > 3000) {
                player.seekTo(0);
                return;
            }
            pm.playPrev(true);
        });

        btnShuffle.setOnClickListener(v -> {
            haptic(v);
            pm.toggleShuffle();
            updateShuffleUi();
            updatePlaybackStatusText();
            updateNavButtons();
        });

        btnRepeat.setOnClickListener(v -> {
            haptic(v);
            pm.cycleRepeatMode();
            updateRepeatUi();
            updatePlaybackStatusText();
            updateNavButtons();
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
                if (fromUser) tvCurrentTime.setText(formatTime(progress));
            }
            @Override
            public void onStartTrackingTouch(SeekBar sb) {
                isUserSeeking = true;
                ignorePlaybackChanges = true;
            }
            @Override
            public void onStopTrackingTouch(SeekBar sb) {
                isUserSeeking = false;
                if (player != null) player.seekTo(sb.getProgress());
                sb.postDelayed(() -> {
                    ignorePlaybackChanges = false;
                    boolean playing = player != null && player.isPlaying();
                    lastIsPlaying = playing;
                    animateCoverState(playing);
                }, 200);
            }
        });
        handleIntentPlayback();
        updateShuffleUi();
        updateRepeatUi();
        updatePlaybackStatusText();
        updateNowPlayingUiFromRepo();
        updatePlayPauseIcon();
        animateCoverState(player != null && player.isPlaying());
        updateNavButtons();
        LinearLayout bottomArea = findViewById(R.id.bottomNowPlaying);

        bottomArea.setOnClickListener(v -> {
            if (getSupportFragmentManager().isStateSaved()) return;
            if (getSupportFragmentManager().findFragmentByTag("queue") != null) {
                return;
            }
            QueueBottomSheet sheet = new QueueBottomSheet();
            sheet.show(getSupportFragmentManager(), "queue");
        });
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
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
    protected void onPause() {
        super.onPause();
        PlaybackManager.get(this).saveCurrentPositionNow();
    }
    private void handleIntentPlayback() {
        boolean openExisting = getIntent().getBooleanExtra(EXTRA_OPEN_EXISTING, false);
        boolean autoPlay = getIntent().getBooleanExtra(EXTRA_AUTOPLAY, true);
        int[] ids = getIntent().getIntArrayExtra(EXTRA_QUEUE_AUDIO_IDS);
        int idx = getIntent().getIntExtra(EXTRA_QUEUE_INDEX, 0);

        if (openExisting && ids == null && pm.getCurrentAudioResId() != 0 && pm.getQueueIds() != null) { return; }
        if (openExisting) { return; }
        if (ids == null || ids.length == 0) return;

        int[] currentQueue = pm.getQueueIds();

        if (currentQueue != null && currentQueue.length > 0) {

            if (java.util.Arrays.equals(currentQueue, ids)) {
                pm.playFromQueue(idx);
                return;
            }
        }
        pm.playQueue(ids, idx, autoPlay);
    }
    @Override
    public void onNowPlayingChanged(int audioResId, int[] queueIds, int queueIndex) {
        lastTrackChangeTime = System.currentTimeMillis();
        updateNowPlayingUiFromRepo();
        updateNavButtons();
        updateShuffleUi();
        updateRepeatUi();
        updatePlaybackStatusText();
    }
    @Override
    public void onIsPlayingChanged(boolean isPlaying) {
        updatePlayPauseIcon();
        if (ignorePlaybackChanges) return;
        if (isUserSeeking) return;

        if (lastIsPlaying != null && lastIsPlaying == isPlaying) {
            float currentScale = ivCover.getScaleX();
            if ((isPlaying && currentScale == 1f) ||
                    (!isPlaying && currentScale == 0.75f)) {
                return;
            }
        }
        lastIsPlaying = isPlaying;
        animateCoverState(isPlaying);
    }
    private void animateCoverState(boolean isPlaying) {
        if (isUserSeeking) return;
        ivCover.animate().cancel();
        float targetScale = isPlaying ? 1f : 0.75f;
        ivCover.animate().scaleX(targetScale).scaleY(targetScale).setDuration(260).setInterpolator(new DecelerateInterpolator()).start();
        animateCornerRadius(isPlaying);
    }
    private void animateCornerRadius(boolean isPlaying) {
        if (isUserSeeking) return;
        ShapeableImageView image = findViewById(R.id.ivCover);

        float start = image.getShapeAppearanceModel().getTopLeftCornerSize().getCornerSize(null);
        float end = isPlaying ? dp(20) : dp(40);

        ValueAnimator animator = ValueAnimator.ofFloat(start, end);
        animator.setDuration(320);
        animator.setInterpolator(new android.view.animation.PathInterpolator(0.4f, 0f, 0.2f, 1f));

        animator.addUpdateListener(animation -> {
            float value = (float) animation.getAnimatedValue();
            image.setShapeAppearanceModel(image.getShapeAppearanceModel().toBuilder().setAllCornerSizes(value).build());
        });
        animator.start();
    }
    private float dp(int value) {
        return value * getResources().getDisplayMetrics().density;
    }
    @Override
    public void onProgress(long positionMs, long durationMs) {
        seekBar.setSecondaryProgress((int) durationMs);
        if (isUserSeeking) return;

        if (durationMs > 0) {
            seekBar.setMax((int) durationMs);
            tvTotalTime.setText(formatTime(durationMs));
        }

        seekBar.setProgress((int) positionMs);
        tvCurrentTime.setText(formatTime(positionMs));

        long remaining = durationMs - positionMs;
        long elapsedSinceTrackChange = System.currentTimeMillis() - lastTrackChangeTime;

        if (elapsedSinceTrackChange < 1200) {
            Song current = SongRepository.findByAudioResId(pm.getCurrentAudioResId());

            if (current != null) {
                animateBottomText("Now playing", current.getTitle());
            }
            return;
        }

        if (remaining <= 10000) {
            int[] q = pm.getQueueIds();
            int idx = pm.getQueueIndex();

            if (q != null && idx < q.length - 1) {
                Song nextSong = SongRepository.findByAudioResId(q[idx + 1]);

                if (nextSong != null) {
                    animateBottomText("Next up", nextSong.getTitle());
                }
            }
        } else {
            Song current = SongRepository.findByAudioResId(pm.getCurrentAudioResId());

            if (current != null) {
                animateBottomText("Now playing", current.getTitle());
            }
        }
    }
    private void showSongInfo() {
        Song song = SongRepository.findByAudioResId(pm.getCurrentAudioResId());

        if (song == null) return;

        View dialogView = getLayoutInflater()
                .inflate(R.layout.dialog_song_info, null);

        ImageView ivCover = dialogView.findViewById(R.id.ivSongInfoCover);
        TextView tvTitle = dialogView.findViewById(R.id.tvSongInfoTitle);
        TextView tvArtist = dialogView.findViewById(R.id.tvSongInfoArtist);
        TextView tvAlbum = dialogView.findViewById(R.id.tvSongInfoAlbum);
        TextView tvProducer = dialogView.findViewById(R.id.tvSongInfoProducer);
        TextView tvLength = dialogView.findViewById(R.id.tvSongInfoLength);
        android.widget.Button btnClose = dialogView.findViewById(R.id.btnCloseSongInfo);
        ivCover.setImageDrawable(null);
        ivCover.post(() -> ivCover.setImageResource(song.getCoverResId()));
        tvTitle.setText(song.getTitle());
        tvArtist.setText(song.getArtist());
        tvAlbum.setText(song.getAlbum());
        tvProducer.setText(song.getProducedBy());
        tvLength.setText(formatTime(song.getDurationMs()));
        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(this).setView(dialogView).create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(
                    android.R.color.transparent
            );
        }
        btnClose.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }
    private void updateNowPlayingUiFromRepo() {
        int audioResId = pm.getCurrentAudioResId();
        Song s = SongRepository.findByAudioResId(audioResId);

        if (s != null) {
            if (!lastTitle.equals(s.getTitle())) {
                fadeText(tvSongTitle, s.getTitle());
                lastTitle = s.getTitle();
            }

            if (!lastArtist.equals(s.getArtist())) {
                fadeText(tvSongArtist, s.getArtist());
                lastArtist = s.getArtist();
            }
            animateBottomText("Now playing", s.getTitle());

            String album = s.getAlbum();

            if (album != null && !album.trim().isEmpty()) {
                tvSongAlbum.setText(album);
                tvSongAlbum.setVisibility(View.VISIBLE);
            } else {
                tvSongAlbum.setVisibility(View.GONE);
            }
            boolean isNewSong = audioResId != lastAnimatedAudioId;

            if (isNewSong) {
                lastAnimatedAudioId = audioResId;

                ivCover.animate().cancel();
                ivCover.setImageResource(s.getCoverResId());
                ivCover.setAlpha(1f);

            } else {
                ivCover.setImageResource(s.getCoverResId());
            }
            applyDynamicGradient(s.getCoverResId());
        }
        if (s != null) {
            String songId = String.valueOf(s.getAudioResId());

            if (LikedSongsRepository.isLiked(this, songId)) {
                btnFavourite.setImageResource(R.drawable.ic_liked);
            } else {
                btnFavourite.setImageResource(R.drawable.ic_like);
            }
        }
    }
    private void fadeText(TextView tv, String newText) {
        tv.animate().cancel();
        tv.animate().alpha(0.6f).setDuration(80).withEndAction(() -> {tv.setText(newText);tv.animate().alpha(1f).setDuration(140).setInterpolator(new DecelerateInterpolator()).start();}).start();
    }
    private void updatePlayPauseIcon() {
        if (player == null) return;
        btnPlayPause.setImageResource(player.isPlaying() ? R.drawable.ic_pause : R.drawable.ic_play);
    }
    private void updateNavButtons() {
        int[] q = pm.getQueueIds();
        int idx = pm.getQueueIndex();

        boolean hasQueue = q != null && q.length > 1;
        boolean isRepeatOne = pm.getRepeatMode() == PlaybackManager.RepeatMode.ONE;
        boolean prevEnabled = hasQueue && (idx > 0 || isRepeatOne || pm.getRepeatMode() == PlaybackManager.RepeatMode.ALL);
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
        PlaybackManager.RepeatMode rm = pm.getRepeatMode();
        String repeat;

        if (rm == PlaybackManager.RepeatMode.OFF) {
            repeat = "Repeat OFF";
        } else if (rm == PlaybackManager.RepeatMode.ONE) {
            repeat = "Repeat ONE";
        } else {
            repeat = "Repeat ALL";
        }

        String fullText = shuffle + " • " + repeat;
        android.text.SpannableString spannable = new android.text.SpannableString(fullText);

        int gray = ContextCompat.getColor(this, R.color.textSecondary);
        int white = ContextCompat.getColor(this, R.color.textPrimary);

        spannable.setSpan(
                new android.text.style.ForegroundColorSpan(
                        pm.isShuffleEnabled() ? white : gray
                ),
                0,
                shuffle.length(),
                android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        );

        spannable.setSpan(
                new android.text.style.ForegroundColorSpan(
                        rm == PlaybackManager.RepeatMode.OFF ? gray : white
                ),
                shuffle.length() + 3,
                fullText.length(),
                android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        );
        tvPlaybackStatus.setText(spannable);
    }
    private void applyDynamicGradient(int coverResId) {
        View root = findViewById(R.id.playerRoot);
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), coverResId);
        if (bitmap == null) return;

        Palette.from(bitmap).generate(palette -> {
            int base = palette.getDarkMutedColor(
                    palette.getMutedColor(
                            ContextCompat.getColor(this, R.color.bg)
                    )
            );

            int red = android.graphics.Color.red(base);
            int green = android.graphics.Color.green(base);
            int blue = android.graphics.Color.blue(base);

            boolean tooGray =
                    Math.abs(red - green) < 18 &&
                            Math.abs(green - blue) < 18;

            if (tooGray) {
                base = palette.getVibrantColor(
                        palette.getMutedColor(base)
                );
            }

            int vivid = android.graphics.Color.argb(
                    255,
                    (int)(android.graphics.Color.red(base) * 0.88),
                    (int)(android.graphics.Color.green(base) * 0.88),
                    (int)(android.graphics.Color.blue(base) * 0.88)
            );

            int almostBlack = android.graphics.Color.argb(
                    255,
                    20,
                    22,
                    28
            );

            GradientDrawable gd = new GradientDrawable(
                    GradientDrawable.Orientation.TOP_BOTTOM,
                    new int[]{
                            vivid,
                            almostBlack,
                            android.graphics.Color.BLACK
                    }
            );
            root.setBackground(gd);
        });
    }
    private void tintOn(ImageButton btn) {
        btn.setColorFilter(ContextCompat.getColor(this, R.color.accent));
        btn.setAlpha(1f);
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
    private void haptic(View v) {
        v.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);
    }
    private void animateBottomText(String label, String track) {
        String currentLabel = tvBottomLabel.getText().toString();
        String currentTrack = tvBottomTrack.getText().toString();

        boolean labelChanged = !currentLabel.equals(label);
        boolean trackChanged = !currentTrack.equals(track);

        if (!labelChanged && !trackChanged) {
            return;
        }
        if (labelChanged) {
            tvBottomLabel.animate()
                    .alpha(0f)
                    .setDuration(180)
                    .withEndAction(() -> {
                        tvBottomLabel.setText(label);
                        tvBottomLabel.animate()
                                .alpha(1f)
                                .setDuration(180)
                                .start();
                    })
                    .start();
        }
        if (trackChanged) {
            tvBottomTrack.animate()
                    .alpha(0f)
                    .setDuration(180)
                    .withEndAction(() -> {
                        tvBottomTrack.setText(track);
                        tvBottomTrack.animate()
                                .alpha(1f)
                                .setDuration(180)
                                .start();
                    })
                    .start();
        }
    }
}