package sk.ukf.wavvy;

import java.util.Collections;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
    private ImageButton btnAlbumMore;
    private ImageView btnSaveAlbum;

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
        btnAlbumMore = findViewById(R.id.btnAlbumMore);
        btnSaveAlbum = findViewById(R.id.btnSaveAlbum);

        findViewById(R.id.btnBack).setOnClickListener(v -> {
            finish();
            overridePendingTransition(R.anim.slide_in_left_animation, R.anim.slide_out_right_animation);
        });

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

        if (album.getCoverUri() != null && !album.getCoverUri().isEmpty()) {
            ivAlbumCover.setImageURI(android.net.Uri.parse(album.getCoverUri()));
        } else {
            ivAlbumCover.setImageResource(album.getCoverResId());
        }
        songsInAlbum = album.getSongs();

        boolean isSaved = SavedAlbumsRepository.isSaved(this, album.getTitle());

        if (isSaved) {
            btnSaveAlbum.setImageResource(R.drawable.icon_bookmark_added);
            btnSaveAlbum.setTag(true);
        } else {
            btnSaveAlbum.setImageResource(R.drawable.icon_bookmark_add);
            btnSaveAlbum.setTag(false);
        }

        btnSaveAlbum.setOnClickListener(v -> {
            boolean selected = btnSaveAlbum.getTag() != null && (boolean) btnSaveAlbum.getTag();

            if (!selected) {
                btnSaveAlbum.setImageResource(R.drawable.icon_bookmark_added);
                btnSaveAlbum.setTag(true);

                SavedAlbumsRepository.add(this, album.getTitle());

                android.widget.Toast.makeText(this, "Album added to library", android.widget.Toast.LENGTH_SHORT).show();
            } else {
                btnSaveAlbum.setImageResource(R.drawable.icon_bookmark_add);
                btnSaveAlbum.setTag(false);

                SavedAlbumsRepository.remove(this, album.getTitle());

                android.widget.Toast.makeText(this, "Album removed from library", android.widget.Toast.LENGTH_SHORT).show();
            }
        });

        btnAlbumMore.setOnClickListener(v -> {
            View popupView = getLayoutInflater().inflate(R.layout.popup_album_menu, null);

            android.widget.PopupWindow popupWindow = new android.widget.PopupWindow(
                    popupView,
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                    true
            );

            popupWindow.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
            popupWindow.setElevation(16f);

            LinearLayout libraryAction = popupView.findViewById(R.id.actionAddLibrary);
            ImageView libraryIcon = (ImageView) libraryAction.getChildAt(0);
            TextView libraryText = (TextView) libraryAction.getChildAt(1);

            boolean saved = SavedAlbumsRepository.isSaved(this, album.getTitle());

            if (saved) {
                libraryIcon.setImageResource(R.drawable.icon_bookmark_remove);
                libraryIcon.setColorFilter(ContextCompat.getColor(this, R.color.danger));
                libraryText.setText("Remove from library");
            } else {
                libraryIcon.setImageResource(R.drawable.icon_add);
                libraryIcon.setColorFilter(ContextCompat.getColor(this, R.color.textPrimary));
                libraryText.setText("Add to library");
            }

            libraryAction.setOnClickListener(v1 -> {
                boolean currentlySaved = SavedAlbumsRepository.isSaved(this, album.getTitle());

                if (!currentlySaved) {
                    SavedAlbumsRepository.add(this, album.getTitle());
                    btnSaveAlbum.setImageResource(R.drawable.icon_bookmark_added);
                    btnSaveAlbum.setTag(true);
                    android.widget.Toast.makeText(this, "Added to library", android.widget.Toast.LENGTH_SHORT).show();
                } else {
                    SavedAlbumsRepository.remove(this, album.getTitle());
                    btnSaveAlbum.setImageResource(R.drawable.icon_bookmark_add);
                    btnSaveAlbum.setTag(false);
                    android.widget.Toast.makeText(this, "Removed from library", android.widget.Toast.LENGTH_SHORT).show();
                }
                popupWindow.dismiss();
            });

            popupView.findViewById(R.id.actionAlbumInfo).setOnClickListener(v1 -> {
                popupWindow.dismiss();
                showAlbumInfo(album);
            });
            popupWindow.showAsDropDown(btnAlbumMore, -220, 0);
        });

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
            totalMs += s.getDurationMs();
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
        applyGradient(album);
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

        Song s = SongRepository.findByAudioResId(this, NowPlayingRepository.getAudioResId(this));

        if (s == null) {
            miniPlayer.setVisibility(View.GONE);
            return;
        }

        miniPlayer.setVisibility(View.VISIBLE);
        if (s.getCoverUri() != null && !s.getCoverUri().isEmpty()) {
            ivMiniCover.setImageURI(android.net.Uri.parse(s.getCoverUri()));
        } else {
            ivMiniCover.setImageResource(s.getCoverResId());
        }
        tvMiniTitle.setText(s.getTitle());
        tvMiniArtist.setText(s.getArtist());

        btnMiniPlay.setImageResource(pm.isPlaying() ? R.drawable.icon_pause : R.drawable.icon_play);
    }
    private void showAlbumInfo(Album album) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_album_info, null);

        ImageView ivCover = dialogView.findViewById(R.id.ivAlbumInfoCover);
        TextView tvTitle = dialogView.findViewById(R.id.tvAlbumInfoTitle);
        TextView tvArtist = dialogView.findViewById(R.id.tvAlbumInfoArtist);
        TextView tvSongs = dialogView.findViewById(R.id.tvAlbumInfoSongs);
        TextView tvLength = dialogView.findViewById(R.id.tvAlbumInfoLength);
        android.widget.Button btnClose = dialogView.findViewById(R.id.btnCloseAlbumInfo);

        if (album.getCoverUri() != null && !album.getCoverUri().isEmpty()) {
            ivCover.setImageURI(android.net.Uri.parse(album.getCoverUri()));
        } else {
            ivCover.setImageResource(album.getCoverResId());
        }
        tvTitle.setText(album.getTitle());
        tvArtist.setText(album.getArtist());
        tvSongs.setText(album.getSongs().size() + " songs");

        long totalMs = 0;
        for (Song s : album.getSongs()) {
            totalMs += s.getDurationMs();
        }

        tvLength.setText(formatDuration(totalMs));

        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(this).setView(dialogView).create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        btnClose.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }
    private void applyGradient(Album album) {
        View root = findViewById(R.id.albumRoot);
        Bitmap bitmap = null;

        try {
            if (album.getCoverUri() != null && !album.getCoverUri().isEmpty()) {
                java.io.InputStream is = getContentResolver().openInputStream(android.net.Uri.parse(album.getCoverUri()));
                bitmap = BitmapFactory.decodeStream(is);
            } else {
                bitmap = BitmapFactory.decodeResource(getResources(), album.getCoverResId());
            }
        } catch (Exception ignored) {}

        if (bitmap == null) return;

        Palette.from(bitmap).generate(palette -> {
            int base = palette.getDarkMutedColor(palette.getMutedColor(ContextCompat.getColor(this, R.color.bg)));
            int red = android.graphics.Color.red(base);
            int green = android.graphics.Color.green(base);
            int blue = android.graphics.Color.blue(base);

            boolean tooGray = Math.abs(red - green) < 18 && Math.abs(green - blue) < 18;

            if (tooGray) {
                base = palette.getVibrantColor(palette.getMutedColor(base));
            }

            int softened = android.graphics.Color.argb(
                    255,
                    (int)(android.graphics.Color.red(base) * 0.78),
                    (int)(android.graphics.Color.green(base) * 0.78),
                    (int)(android.graphics.Color.blue(base) * 0.78)
            );

            int almostBlack = android.graphics.Color.argb(255, 20, 22, 28);

            GradientDrawable gd = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{ softened, almostBlack, android.graphics.Color.BLACK });
            root.setBackground(gd);
        });
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