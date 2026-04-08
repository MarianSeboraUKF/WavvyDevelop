package sk.ukf.wavvy;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.widget.ProgressBar;
import android.widget.Toast;
import java.util.ArrayList;
import sk.ukf.wavvy.adapter.SongAdapter;
import sk.ukf.wavvy.model.Playlist;
import sk.ukf.wavvy.model.Song;
import java.text.Collator;
import java.util.Locale;

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
    private View coverBg;
    private ImageView coverIcon;
    private TextView btnPlay;
    private TextView btnShuffle;
    private TextView btnSort;
    private String currentSort = "TITLE_AZ";
    private Collator collator;
    private BroadcastReceiver songsUpdatedReceiver;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playlist_detail);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
        getWindow().setNavigationBarColor(android.graphics.Color.TRANSPARENT);

        View root = findViewById(R.id.playlistRoot);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            int topInset = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            v.setPadding(0, topInset + 8, 0, 0);
            return insets;
        });

        pm = PlaybackManager.get(this);
        collator = Collator.getInstance(new Locale("sk", "SK"));
        collator.setStrength(Collator.PRIMARY);

        Intent intent = getIntent();
        playlistId = intent.getStringExtra(EXTRA_PLAYLIST_ID);
        String nameFromIntent = intent.getStringExtra(EXTRA_PLAYLIST_NAME);
        if (nameFromIntent != null && !nameFromIntent.trim().isEmpty()) {
            playlistName = nameFromIntent.trim();
        }

        tvPlaylistTitle = findViewById(R.id.tvPlaylistTitle);
        tvPlaylistMeta = findViewById(R.id.tvPlaylistMeta);
        rvPlaylistSongs = findViewById(R.id.rvPlaylistSongs);
        coverBg = findViewById(R.id.playlistCoverBg);
        coverIcon = findViewById(R.id.playlistCoverIcon);
        btnPlay = findViewById(R.id.btnPlay);
        btnShuffle = findViewById(R.id.btnShuffle);
        btnSort = findViewById(R.id.btnSort);
        ImageButton btnBack = findViewById(R.id.btnBack);
        ImageButton btnMenu = findViewById(R.id.btnMenu);
        TextView tvTopTitle = findViewById(R.id.tvTopTitle);

        btnBack.setOnClickListener(v -> {
            finish();
            overridePendingTransition(0, 0);
        });

        findViewById(R.id.btnBack).setOnClickListener(v -> {
            finish();
            overridePendingTransition(R.anim.slide_in_left_animation, R.anim.slide_out_right_animation);
        });

        if (tvTopTitle != null) {
            tvTopTitle.setText("Playlist");
        }

        btnMenu.setOnClickListener(v -> showPlaylistMenu(v));

        tvPlaylistTitle.setText(playlistName);
        boolean isSystem = "liked".equals(playlistId) || "local".equals(playlistId);
        btnSort.setVisibility(isSystem ? View.GONE : View.VISIBLE);
        btnPlay.setOnClickListener(v -> {
            if (songsInPlaylist.isEmpty()) return;
            PlayerLauncher.openQueue(PlaylistDetailActivity.this, songsInPlaylist, songsInPlaylist.get(0));
            pm.setShuffle(false);
        });

        btnShuffle.setOnClickListener(v -> {
            if (songsInPlaylist.isEmpty()) return;

            int randomIndex = new java.util.Random().nextInt(songsInPlaylist.size());
            Song randomSong = songsInPlaylist.get(randomIndex);
            PlayerLauncher.openQueue(PlaylistDetailActivity.this, songsInPlaylist, randomSong);
            pm.setShuffle(true);
        });
        btnSort.setOnClickListener(v -> showSortPopup(v));

        if (playlistId != null) {
            ViewGroup.LayoutParams params = coverIcon.getLayoutParams();
            if ("liked".equals(playlistId)) {
                coverBg.setBackgroundResource(R.drawable.background_liked_gradient);
                coverIcon.setImageResource(R.drawable.icon_liked);
                coverIcon.setColorFilter(android.graphics.Color.BLACK);

                params.width = 350;
                params.height = 350;
            } else if ("local".equals(playlistId)) {
                coverBg.setBackgroundResource(R.drawable.background_local_gradient);
                coverIcon.setImageResource(R.drawable.icon_local);
                coverIcon.setColorFilter(android.graphics.Color.BLACK);

                params.width = 350;
                params.height = 350;
            } else {
                coverIcon.clearColorFilter();
                params.width = ViewGroup.LayoutParams.MATCH_PARENT;
                params.height = ViewGroup.LayoutParams.MATCH_PARENT;
                Playlist p = PlaylistRepository.findById(this, playlistId);

                if (p != null && !p.getSongAudioResIds().isEmpty()) {
                    Song s = SongRepository.findByAudioResId(p.getSongAudioResIds().get(0));
                    if (s != null) {
                        coverBg.setBackgroundColor(android.graphics.Color.TRANSPARENT);
                        if (s.getCoverUri() != null && !s.getCoverUri().isEmpty()) {
                            coverIcon.setImageURI(android.net.Uri.parse(s.getCoverUri()));
                        } else {
                            coverIcon.setImageResource(s.getCoverResId());
                        }
                    }
                } else {
                    coverBg.setBackgroundColor(android.graphics.Color.TRANSPARENT);
                    coverIcon.setImageResource(R.drawable.default_cover);
                }
            }
            coverIcon.setLayoutParams(params);
            ((FrameLayout.LayoutParams) coverIcon.getLayoutParams()).gravity = android.view.Gravity.CENTER;
        }
        rvPlaylistSongs.setLayoutManager(new LinearLayoutManager(this));
        songsInPlaylist = new ArrayList<>();

        adapter = new SongAdapter(songsInPlaylist, false, isSystem, song -> PlayerLauncher.openQueue(PlaylistDetailActivity.this, songsInPlaylist, song));
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
                pm.getPlayer().play();
            } else {
                pm.playPrev(true);
            }
        });
        btnMiniNext.setOnClickListener(v -> pm.playNext(true));

        songsUpdatedReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {loadSongs();
            }
        };
        loadSongs();
        updateMiniPlayerUi();
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateMiniPlayerUi();
    }

    @Override
    protected void onPause() {
        super.onPause();
        PlaybackManager.get(this).saveCurrentPositionNow();
    }

    @Override
    protected void onStart() {
        super.onStart();
        pm.addListener(this);
        registerReceiver(songsUpdatedReceiver, new IntentFilter("songs_updated"), Context.RECEIVER_NOT_EXPORTED);
    }

    @Override
    protected void onStop() {
        super.onStop();
        pm.removeListener(this);
        try {
            unregisterReceiver(songsUpdatedReceiver);
        } catch (Exception ignored) {}
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
        if (btnMiniPlay != null) {
            btnMiniPlay.setImageResource(isPlaying ? R.drawable.icon_pause : R.drawable.icon_play);
        }
        if (adapter != null) {
            adapter.notifyDataSetChanged();
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

        if (s.getCoverUri() != null && !s.getCoverUri().isEmpty()) {
            ivMiniCover.setImageURI(android.net.Uri.parse(s.getCoverUri()));
        } else {
            ivMiniCover.setImageResource(s.getCoverResId());
        }
        tvMiniTitle.setText(s.getTitle());
        tvMiniArtist.setText(s.getArtist());
        btnMiniPlay.setImageResource(pm.isPlaying() ? R.drawable.icon_pause : R.drawable.icon_play);

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
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1001 && resultCode == RESULT_OK) {
            if (data != null && data.getData() != null) {
                try {
                    getContentResolver().takePersistableUriPermission(data.getData(), Intent.FLAG_GRANT_READ_URI_PERMISSION);
                } catch (Exception ignored) {}
                importSong(data.getData());
            }
        }
    }
    private void importSong(android.net.Uri uri) {
        android.media.MediaMetadataRetriever mmr = new android.media.MediaMetadataRetriever();
        try {
            mmr.setDataSource(this, uri);
            String title = mmr.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_TITLE);
            String artist = mmr.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_ARTIST);
            String album = mmr.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_ALBUM);

            if (title == null || title.isEmpty()) {
                Cursor cursor = getContentResolver().query(uri, null, null, null, null);
                if (cursor != null) {
                    int nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                    if (nameIndex != -1 && cursor.moveToFirst()) {
                        title = cursor.getString(nameIndex);
                    }
                    cursor.close();
                }
            }
            if (title == null || title.isEmpty()) {
                title = "Unknown";
            }
            if (title.endsWith(".mp3")) {
                title = title.replace(".mp3", "");
            }

            if (artist == null) artist = "Unknown";
            if (album == null) album = "Unknown";
            int id = uri.toString().hashCode();

            Song newSong = new Song(title, artist, "", album, artist, "-", 0, R.drawable.default_cover, id);
            newSong.setUriString(uri.toString());
            SongRepository.addLocalSong(newSong);

            String dur = mmr.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION);
            if (dur != null) { newSong.setDurationMs(Long.parseLong(dur)); }

            SongRepository.saveLocalSongs(this);
            Intent intent = new Intent("songs_updated");
            intent.setPackage(getPackageName());
            sendBroadcast(intent);
            loadSongs();
            Toast.makeText(this, "Imported: " + title, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Import failed", Toast.LENGTH_SHORT).show();
        } finally {
            try { mmr.release(); } catch (Exception ignored) {}
        }
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
    private void showSortPopup(View anchor) {
        View popupView = getLayoutInflater().inflate(R.layout.popup_sort_playlist, null);
        PopupWindow popup = new PopupWindow(popupView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);
        popup.setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        popup.setElevation(12f);

        View actionTitleAZ = popupView.findViewById(R.id.actionTitleAZ);
        View actionTitleZA = popupView.findViewById(R.id.actionTitleZA);
        View actionArtistAZ = popupView.findViewById(R.id.actionArtistAZ);
        View actionArtistZA = popupView.findViewById(R.id.actionArtistZA);

        actionTitleAZ.setOnClickListener(v -> {
            currentSort = "TITLE_AZ";
            songsInPlaylist.sort((a, b) -> collator.compare(a.getTitle(), b.getTitle()));
            afterSort(popup);
        });

        actionTitleZA.setOnClickListener(v -> {
            currentSort = "TITLE_ZA";
            songsInPlaylist.sort((a, b) -> collator.compare(b.getTitle(), a.getTitle()));
            afterSort(popup);
        });

        actionArtistAZ.setOnClickListener(v -> {
            currentSort = "ARTIST_AZ";
            songsInPlaylist.sort((a, b) -> collator.compare(a.getArtist(), b.getArtist()));
            afterSort(popup);
        });

        actionArtistZA.setOnClickListener(v -> {
            currentSort = "ARTIST_ZA";
            songsInPlaylist.sort((a, b) -> collator.compare(b.getArtist(), a.getArtist()));
            afterSort(popup);
        });
        popupView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);

        int popupWidth = popupView.getMeasuredWidth();
        int anchorWidth = anchor.getWidth();
        int offsetX = (anchorWidth / 2) - (popupWidth / 2);
        int offsetY = 8;

        updateSortUI(popupView);
        popup.showAsDropDown(anchor, offsetX, offsetY);
    }
    private void updateSortUI(View popupView) {
        View actionTitleAZ = popupView.findViewById(R.id.actionTitleAZ);
        View actionTitleZA = popupView.findViewById(R.id.actionTitleZA);
        View actionArtistAZ = popupView.findViewById(R.id.actionArtistAZ);
        View actionArtistZA = popupView.findViewById(R.id.actionArtistZA);

        actionTitleAZ.setSelected(false);
        actionTitleZA.setSelected(false);
        actionArtistAZ.setSelected(false);
        actionArtistZA.setSelected(false);

        switch (currentSort) {
            case "TITLE_AZ":
                actionTitleAZ.setSelected(true);
                break;
            case "TITLE_ZA":
                actionTitleZA.setSelected(true);
                break;
            case "ARTIST_AZ":
                actionArtistAZ.setSelected(true);
                break;
            case "ARTIST_ZA":
                actionArtistZA.setSelected(true);
                break;
        }
    }
    private void afterSort(PopupWindow popup) {
        adapter.notifyDataSetChanged();
        updateMeta();
        popup.dismiss();
        Toast.makeText(this, "Sorted", Toast.LENGTH_SHORT).show();
    }
    private void loadSongs() {
        songsInPlaylist.clear();
        if ("liked".equals(playlistId)) {
            songsInPlaylist.addAll(SongRepository.getLikedSongs(this));
        } else if ("local".equals(playlistId)) {
            songsInPlaylist.addAll(SongRepository.getSongs());
        } else {
            Playlist p = PlaylistRepository.findById(this, playlistId);
            if (p != null) {
                for (Integer id : p.getSongAudioResIds()) {
                    Song s = SongRepository.findByAudioResId(id);
                    if (s != null) {
                        songsInPlaylist.add(s);
                    }
                }
            }
        }
        updateMeta();
        updateCover();
        adapter.notifyDataSetChanged();
    }
    public void updateMeta() {
        long totalMs = 0;
        for (Song s : songsInPlaylist) {
            totalMs += s.getDurationMs();
        }
        String label = songsInPlaylist.size() == 1 ? "song" : "songs";
        tvPlaylistMeta.setText(songsInPlaylist.size() + " " + label + " • " + formatDuration(totalMs));
    }
    private void showDeletePlaylistDialog() {
        View card = getLayoutInflater().inflate(R.layout.dialog_delete_playlist, null);
        TextView tvMsg = card.findViewById(R.id.tvMsg);
        tvMsg.setText("Do you really want to delete „" + playlistName + "“?");

        View btnDelete = card.findViewById(R.id.btnDelete);
        View btnCancel = card.findViewById(R.id.btnCancel);
        android.app.Dialog dialog = WavvyDialogs.showCenteredCardDialog(this, this, card);

        btnDelete.setOnClickListener(v -> {
            PlaylistRepository.deletePlaylist(this, playlistId);
            dialog.dismiss();
            finish();
        });
        btnCancel.setOnClickListener(v -> dialog.dismiss());
    }
    private void showRenameDialog() {
        View card = getLayoutInflater().inflate(R.layout.dialog_rename_playlist, null);

        EditText etName = card.findViewById(R.id.etName);
        View btnRename = card.findViewById(R.id.btnRename);
        View btnCancel = card.findViewById(R.id.btnCancel);
        etName.setText(playlistName);

        android.app.Dialog dialog = WavvyDialogs.showCenteredCardDialog(this, this, card);

        btnRename.setOnClickListener(v -> {
            String newName = etName.getText().toString().trim();
            if (!newName.isEmpty()) {
                PlaylistRepository.renamePlaylist(this, playlistId, newName);
                playlistName = newName;
                tvPlaylistTitle.setText(newName);
                dialog.dismiss();
            }
        });
        btnCancel.setOnClickListener(v -> dialog.dismiss());
    }
    private void showPlaylistMenu(View anchor) {
        View popupView = getLayoutInflater().inflate(R.layout.popup_playlist_menu, null);

        final PopupWindow popup = new PopupWindow(popupView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);
        popup.setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        popup.setElevation(12f);

        View actionInfo = popupView.findViewById(R.id.actionInfo);
        View actionImport = popupView.findViewById(R.id.actionImport);
        View actionEdit = popupView.findViewById(R.id.actionEdit);
        View actionDelete = popupView.findViewById(R.id.actionDelete);

        actionImport.setVisibility(View.GONE);
        actionEdit.setVisibility(View.GONE);
        actionDelete.setVisibility(View.GONE);

        if ("liked".equals(playlistId)) {}
        else if ("local".equals(playlistId)) {
            actionImport.setVisibility(View.VISIBLE);
            actionImport.setOnClickListener(v -> {
                popup.dismiss();
                openFilePicker();
            });
        } else {
            actionEdit.setVisibility(View.VISIBLE);
            actionDelete.setVisibility(View.VISIBLE);
            actionEdit.setOnClickListener(v -> {
                popup.dismiss();
                showRenameDialog();
            });
            actionDelete.setOnClickListener(v -> {
                popup.dismiss();
                showDeletePlaylistDialog();
            });
        }
        actionInfo.setOnClickListener(v -> {
            popup.dismiss();
            showPlaylistInfo();
        });
        popup.showAsDropDown(anchor, -200, 20);
    }
    private void showPlaylistInfo() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_playlist_info, null);
        View coverBg = dialogView.findViewById(R.id.playlistCoverBg);

        ImageView coverIcon = dialogView.findViewById(R.id.playlistCoverIcon);

        TextView tvTitle = dialogView.findViewById(R.id.tvPlaylistTitle);
        TextView tvCount = dialogView.findViewById(R.id.tvPlaylistCount);
        TextView tvLength = dialogView.findViewById(R.id.tvPlaylistLength);
        TextView btnClose = dialogView.findViewById(R.id.btnClose);
        tvTitle.setText(playlistName);

        if ("liked".equals(playlistId)) {
            ViewGroup.LayoutParams params = coverIcon.getLayoutParams();
            params.width = 350;
            params.height = 350;
            coverIcon.setLayoutParams(params);

            ((FrameLayout.LayoutParams) coverIcon.getLayoutParams()).gravity = android.view.Gravity.CENTER;

            coverIcon.setScaleType(ImageView.ScaleType.CENTER_CROP);
            coverBg.setBackgroundResource(R.drawable.background_liked_gradient);
            coverIcon.setImageResource(R.drawable.icon_liked);
            coverIcon.setColorFilter(android.graphics.Color.BLACK);
            coverIcon.setAlpha(1f);
        } else if ("local".equals(playlistId)) {
            ViewGroup.LayoutParams params = coverIcon.getLayoutParams();
            params.width = 350;
            params.height = 350;
            coverIcon.setLayoutParams(params);

            ((FrameLayout.LayoutParams) coverIcon.getLayoutParams()).gravity = android.view.Gravity.CENTER;

            coverIcon.setScaleType(ImageView.ScaleType.CENTER_CROP);
            coverBg.setBackgroundResource(R.drawable.background_local_gradient);
            coverIcon.setImageResource(R.drawable.icon_local);
            coverIcon.setColorFilter(android.graphics.Color.BLACK);
            coverIcon.setAlpha(1f);
        } else {
            coverBg.setBackgroundColor(android.graphics.Color.TRANSPARENT);
            ViewGroup.LayoutParams params = coverIcon.getLayoutParams();
            params.width = ViewGroup.LayoutParams.MATCH_PARENT;
            params.height = ViewGroup.LayoutParams.MATCH_PARENT;
            coverIcon.setLayoutParams(params);

            ((FrameLayout.LayoutParams) coverIcon.getLayoutParams()).gravity = android.view.Gravity.CENTER;

            coverIcon.setScaleType(ImageView.ScaleType.CENTER_CROP);
            coverIcon.clearColorFilter();

            if (!songsInPlaylist.isEmpty()) {
                Song s = songsInPlaylist.get(0);
                coverIcon.setImageResource(s.getCoverResId());
            } else {
                coverIcon.setImageResource(R.drawable.default_cover);
            }
        }
        int count = songsInPlaylist.size();
        long totalMs = 0;
        for (Song s : songsInPlaylist) {
            totalMs += s.getDurationMs();
        }
        tvCount.setText(count == 1 ? "1 song" : count + " songs");
        tvLength.setText(formatDuration(totalMs));
        android.app.Dialog dialog = WavvyDialogs.showCenteredCardDialog(this, this, dialogView);
        btnClose.setOnClickListener(v -> dialog.dismiss());
    }
    private void openFilePicker() {
        Intent pickIntent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        pickIntent.setType("audio/*");
        pickIntent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(pickIntent, 1001);
    }
    public void updateCover() {
        if ("liked".equals(playlistId)) return;
        if ("local".equals(playlistId)) return;

        if (songsInPlaylist.isEmpty()) {
            coverBg.setBackgroundColor(android.graphics.Color.TRANSPARENT);
            coverIcon.setImageResource(R.drawable.default_cover);
            return;
        }
        Song first = songsInPlaylist.get(0);
        if (first.getCoverUri() != null && !first.getCoverUri().isEmpty()) {
            coverIcon.setImageURI(android.net.Uri.parse(first.getCoverUri()));
        } else {
            coverIcon.setImageResource(first.getCoverResId());
        }
    }
    private String formatDuration(long ms) {
        long totalSeconds = ms / 1000;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        if (hours > 0) {
            if (minutes == 0) {
                return hours + " h";
            }
            return hours + " h " + minutes + " min";
        } else {
            if (minutes == 0) {
                return seconds + " sec";
            }
            return minutes + " min " + seconds + " sec";
        }
    }
}