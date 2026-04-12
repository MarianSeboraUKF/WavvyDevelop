package sk.ukf.wavvy;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.PopupWindow;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.text.Normalizer;
import android.widget.ImageView;
import sk.ukf.wavvy.adapter.AlbumAdapter;
import sk.ukf.wavvy.adapter.PlaylistAdapter;
import sk.ukf.wavvy.adapter.SongHorizontalAdapter;
import sk.ukf.wavvy.adapter.SystemPlaylistAdapter;
import sk.ukf.wavvy.model.Album;
import sk.ukf.wavvy.model.Playlist;
import sk.ukf.wavvy.model.Song;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.widget.Toast;
import androidx.recyclerview.widget.PagerSnapHelper;

public class LibraryFragment extends Fragment {
    private ArrayList<Playlist> playlists;
    private PlaylistAdapter playlistAdapter;
    private SystemPlaylistAdapter systemAdapter;
    private ArrayList<Playlist> systemPlaylists;
    private int songsScrollPosition = 0;
    private PagerSnapHelper snapHelper;
    private BroadcastReceiver libraryReceiver;
    private RecyclerView rvSongsPager;
    private RecyclerView rvAlbums;
    private AlbumAdapter albumAdapter;
    private ArrayList<Album> albumsList;
    private SongHorizontalAdapter songPagerAdapter;

    public LibraryFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_library, container, false);
        RecyclerView rvSystem = view.findViewById(R.id.rvSystemPlaylists);
        RecyclerView rv = view.findViewById(R.id.rvPlaylists);

        GridLayoutManager layoutManager = new GridLayoutManager(requireContext(), 2);
        layoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                if (position == 2) return 2;
                return 1;
            }
        });
        rvSystem.setLayoutManager(layoutManager);
        rv.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        rv.setNestedScrollingEnabled(false);

        systemPlaylists = new ArrayList<>();
        Playlist liked = new Playlist("liked", "Liked songs", true);
        Playlist local = new Playlist("local", "Local songs", true);
        Playlist online = new Playlist("online", "Online songs", true);

        for (Song s : SongRepository.getSongs()) {
            local.addSong(s.getAudioResId());
        }

        ArrayList<Song> onlineSongs = SongRepository.getOnlineSongs(requireContext());
        for (Song s : onlineSongs) {
            online.addSong(s.getAudioResId());
        }

        systemPlaylists.add(liked);
        systemPlaylists.add(local);
        systemPlaylists.add(online);

        systemAdapter = new SystemPlaylistAdapter(systemPlaylists, this::openPlaylist);
        rvSystem.setAdapter(systemAdapter);

        rvAlbums = view.findViewById(R.id.rvAlbums);
        rvAlbums.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        rvAlbums.setNestedScrollingEnabled(false);
        rvAlbums.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(@NonNull Rect outRect, @NonNull View view,
                                       RecyclerView parent, RecyclerView.State state) {
                outRect.right = 24;
            }
        });
        rvAlbums.setPadding(16, 0, 16, 0);
        rvAlbums.setClipToPadding(false);

        rvSongsPager = view.findViewById(R.id.rvSongsPager);
        rvSongsPager.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        snapHelper = new PagerSnapHelper();
        snapHelper.attachToRecyclerView(rvSongsPager);
        rvSongsPager.setOverScrollMode(View.OVER_SCROLL_NEVER);

        ImageView btnAdd = view.findViewById(R.id.btnAdd);
        btnAdd.setOnClickListener(v -> {
            Intent pickIntent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            pickIntent.setType("audio/*");
            pickIntent.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(pickIntent, 1001);
        });

        songPagerAdapter = new SongHorizontalAdapter(
                new ArrayList<>(),
                song -> {
                    ArrayList<Song> likedSongs = new ArrayList<>();

                    for (Song s : SongRepository.getSongs()) {
                        String id = String.valueOf(s.getAudioResId());
                        if (LikedSongsRepository.isLiked(requireContext(), id)) {
                            likedSongs.add(s);
                        }
                    }
                    PlayerLauncher.openQueue(requireContext(), likedSongs, song);
                    rvSongsPager.post(() -> songPagerAdapter.notifyDataSetChanged());
                }
        );
        rvSongsPager.setAdapter(songPagerAdapter);
        setupSongs();

        rvSongsPager.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(@NonNull android.graphics.Rect outRect, @NonNull View view, RecyclerView parent, RecyclerView.State state) { outRect.right = 24; }
        });
        playlists = new ArrayList<>();
        playlists.addAll(PlaylistRepository.getPlaylists(requireContext()));

        playlistAdapter = new PlaylistAdapter(
                playlists,
                playlist -> {
                    Intent intent = new Intent(requireContext(), PlaylistDetailActivity.class);
                    intent.putExtra(PlaylistDetailActivity.EXTRA_PLAYLIST_ID, playlist.getId());
                    intent.putExtra(PlaylistDetailActivity.EXTRA_PLAYLIST_NAME, playlist.getName());
                    startActivity(intent);
                    requireActivity().overridePendingTransition(R.anim.slide_in_right_animation, R.anim.slide_out_left_animation);
                },
                (playlist, anchor) -> showPopupMenu(playlist, anchor)
        );
        rv.setAdapter(playlistAdapter);
        view.findViewById(R.id.btnCreatePlaylist).setOnClickListener(v -> showCreateDialog());

        albumsList = new ArrayList<>();

        Set<String> savedAlbums = requireContext().getSharedPreferences("saved_albums", Context.MODE_PRIVATE).getStringSet("album_titles", new HashSet<>());

        if (savedAlbums == null) savedAlbums = new HashSet<>();

        for (String albumTitle : savedAlbums) {
            Album album = AlbumRepository.findByTitle(albumTitle);
            if (album != null) {
                albumsList.add(album);
            }
        }

        Collections.sort(albumsList, (a, b) -> {
            String t1 = Normalizer.normalize(a.getTitle(), Normalizer.Form.NFD).replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
            String t2 = Normalizer.normalize(b.getTitle(), Normalizer.Form.NFD).replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
            return t1.compareToIgnoreCase(t2);
        });

        albumAdapter = new AlbumAdapter(
                albumsList,
                album -> {
                    Intent intent = new Intent(requireContext(), AlbumDetailActivity.class);
                    intent.putExtra("album_title", album.getTitle());
                    startActivity(intent);
                    requireActivity().overridePendingTransition(R.anim.slide_in_right_animation, R.anim.slide_out_left_animation);
                }
        );
        rvAlbums.setAdapter(albumAdapter);
        libraryReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                reload();
            }
        };

        new Thread(() -> {
            for (Song s : SongRepository.getOnlineSongs(requireContext())) {
                if (s.getDurationMs() == 0 && s.getDownloadUrl() != null) {
                    PlaybackManager.loadOnlineMetadata(requireContext(), s, null);
                }
            }
        }).start();
        return view;
    }
    private void setupSongs() {
        ArrayList<Song> likedSongs = new ArrayList<>();

        for (Song s : SongRepository.getSongs()) {
            String id = String.valueOf(s.getAudioResId());
            if (LikedSongsRepository.isLiked(requireContext(), id)) {
                likedSongs.add(s);
            }
        }

        List<List<Song>> pages = new ArrayList<>();

        for (int i = 0; i < likedSongs.size(); i += 3) {
            List<Song> page = new ArrayList<>(likedSongs.subList(i, Math.min(i + 3, likedSongs.size())));
            while (page.size() < 3) {
                page.add(null);
            }
            pages.add(page);
        }
        songPagerAdapter.updateData(pages);
    }

    @Override
    public void onStart() {
        super.onStart();
        IntentFilter filter = new IntentFilter();
        filter.addAction(LikedSongsRepository.ACTION_LIKED_CHANGED);
        filter.addAction("playlist_updated");
        requireContext().registerReceiver(libraryReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
    }

    @Override
    public void onStop() {
        super.onStop();
        try {
            requireContext().unregisterReceiver(libraryReceiver);
        } catch (Exception ignored) {}
    }

    @Override
    public void onResume() {
        super.onResume();
        reload();
    }
    private void reload() {
        systemPlaylists.clear();
        if (rvSongsPager != null && rvSongsPager.getLayoutManager() != null && snapHelper != null) {
            View snapView = snapHelper.findSnapView(rvSongsPager.getLayoutManager());
            if (snapView != null) {
                songsScrollPosition = rvSongsPager.getLayoutManager().getPosition(snapView);
            }
        }
        Playlist liked = new Playlist("liked", "Liked songs", true);
        Playlist local = new Playlist("local", "Local songs", true);
        Playlist online = new Playlist("online", "Online songs", true);

        for (Song s : SongRepository.getSongs()) {
            local.addSong(s.getAudioResId());
        }

        ArrayList<Song> onlineSongs = SongRepository.getOnlineSongs(requireContext());
        for (Song s : onlineSongs) {
            online.addSong(s.getAudioResId());
        }

        systemPlaylists.add(liked);
        systemPlaylists.add(local);
        systemPlaylists.add(online);
        systemAdapter.notifyDataSetChanged();

        ArrayList<Playlist> sorted = new ArrayList<>(PlaylistRepository.getPlaylists(requireContext()));

        Collections.sort(sorted, (a, b) -> {
            if (a.isSystem() && !b.isSystem()) return -1;
            if (!a.isSystem() && b.isSystem()) return 1;
            return a.getName().compareToIgnoreCase(b.getName());
        });
        playlistAdapter.updateData(sorted);

        setupSongs();
        rvSongsPager.scrollToPosition(songsScrollPosition);

        albumsList.clear();
        Set<String> savedAlbums = requireContext().getSharedPreferences("saved_albums", Context.MODE_PRIVATE).getStringSet("album_titles", new HashSet<>());

        if (savedAlbums == null) savedAlbums = new HashSet<>();

        for (String albumTitle : savedAlbums) {
            Album album = AlbumRepository.findByTitle(albumTitle);
            if (album != null) {
                albumsList.add(album);
            }
        }

        Collections.sort(albumsList, (a, b) -> {
            String t1 = Normalizer.normalize(a.getTitle(), Normalizer.Form.NFD).replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
            String t2 = Normalizer.normalize(b.getTitle(), Normalizer.Form.NFD).replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
            return t1.compareToIgnoreCase(t2);
        });

        albumAdapter.notifyDataSetChanged();
    }
    private void openPlaylist(Playlist playlist) {
        if (playlist.getId().equals("online")) {
            if (!hasInternet()) {
                Toast.makeText(requireContext(), "No internet connection", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(requireContext(), PlaylistDetailActivity.class);
            intent.putExtra(PlaylistDetailActivity.EXTRA_PLAYLIST_ID, "online");
            intent.putExtra(PlaylistDetailActivity.EXTRA_PLAYLIST_NAME, "Online songs");
            startActivity(intent);
            return;
        }
        Intent intent = new Intent(requireContext(), PlaylistDetailActivity.class);
        intent.putExtra(PlaylistDetailActivity.EXTRA_PLAYLIST_ID, playlist.getId());
        intent.putExtra(PlaylistDetailActivity.EXTRA_PLAYLIST_NAME, playlist.getName());
        startActivity(intent);
    }
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1001 && resultCode == requireActivity().RESULT_OK) {
            if (data != null && data.getData() != null) {
                Uri uri = data.getData();
                try {
                    requireContext().getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                } catch (Exception ignored) {}
                importSong(uri);
            }
        }
    }
    private void importSong(android.net.Uri uri) {
        android.media.MediaMetadataRetriever mmr = new android.media.MediaMetadataRetriever();
        try {
            mmr.setDataSource(requireContext(), uri);
            String title = mmr.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_TITLE);
            String artist = mmr.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_ARTIST);
            String album = mmr.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_ALBUM);

            if (title == null || title.isEmpty()) {
                Cursor cursor = requireContext().getContentResolver().query(uri, null, null, null, null);
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
            int id = ("local_" + uri.toString()).hashCode();

            Song newSong = new Song(title, artist, "", album, artist, "-", 0, R.drawable.default_cover, id);
            newSong.setUriString(uri.toString());
            SongRepository.addLocalSong(newSong);

            String dur = mmr.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION);
            if (dur != null) { newSong.setDurationMs(Long.parseLong(dur)); }

            SongRepository.saveLocalSongs(requireContext());

            Intent intent = new Intent("songs_updated");
            intent.setPackage(requireContext().getPackageName());
            requireContext().sendBroadcast(intent);

            reload();
            Toast.makeText(requireContext(), "Imported: " + title, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Import failed", Toast.LENGTH_SHORT).show();
        } finally {
            try { mmr.release(); } catch (Exception ignored) {}
        }
    }
    private void showCreateDialog() {
        View card = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_create_playlist, null);
        EditText etName = card.findViewById(R.id.etName);
        android.app.Dialog dialog = WavvyDialogs.showCenteredCardDialog(requireContext(), requireActivity(), card);

        card.findViewById(R.id.btnCreate).setOnClickListener(x -> {
            String name = etName.getText().toString().trim();
            if (!name.isEmpty()) {
                PlaylistRepository.createPlaylist(requireContext(), name);
                reload();
                dialog.dismiss();
            }
        });
        card.findViewById(R.id.btnCancel).setOnClickListener(x -> dialog.dismiss());
    }
    private void showPopupMenu(Playlist playlist, View anchor) {
        View popupView = LayoutInflater.from(requireContext()).inflate(R.layout.popup_playlist_menu, null);

        PopupWindow popup = new PopupWindow(popupView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);
        popup.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        popup.setElevation(12f);
        popup.setFocusable(true);
        popup.setOutsideTouchable(true);

        View actionImport = popupView.findViewById(R.id.actionImport);
        View actionEdit = popupView.findViewById(R.id.actionEdit);
        View actionDelete = popupView.findViewById(R.id.actionDelete);
        View actionInfo = popupView.findViewById(R.id.actionInfo);

        actionImport.setVisibility(View.GONE);
        actionEdit.setVisibility(View.GONE);
        actionDelete.setVisibility(View.GONE);

        if (playlist.getId().equals("liked")) {
        } else if (playlist.getId().equals("local")) {
            actionImport.setVisibility(View.VISIBLE);
            actionImport.setOnClickListener(v -> { popup.dismiss(); });
        } else {
            actionEdit.setVisibility(View.VISIBLE);
            actionDelete.setVisibility(View.VISIBLE);
            actionEdit.setOnClickListener(v -> {
                popup.dismiss();
                showRenameDialog(playlist);
            });
            actionDelete.setOnClickListener(v -> {
                popup.dismiss();
                showDeleteDialog(playlist);
            });
        }
        actionInfo.setOnClickListener(v -> { popup.dismiss();showPlaylistInfoDialog(playlist); });
        popup.showAsDropDown(anchor);
    }
    private void showRenameDialog(Playlist playlist) {
        View card = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_rename_playlist, null);

        EditText etName = card.findViewById(R.id.etName);
        etName.setText(playlist.getName());

        android.app.Dialog dialog = WavvyDialogs.showCenteredCardDialog(requireContext(), requireActivity(), card);

        card.findViewById(R.id.btnRename).setOnClickListener(v -> {
            String newName = etName.getText().toString().trim();
            if (!newName.isEmpty()) {
                PlaylistRepository.renamePlaylist(requireContext(), playlist.getId(), newName);
                reload();
                dialog.dismiss();
            }
        });
        card.findViewById(R.id.btnCancel).setOnClickListener(v -> dialog.dismiss());
    }
    private void showPlaylistInfoDialog(Playlist playlist) {
        View card = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_playlist_info, null);

        ImageView cover = card.findViewById(R.id.playlistCoverIcon);
        TextView tvTitle = card.findViewById(R.id.tvPlaylistTitle);
        TextView tvCount = card.findViewById(R.id.tvPlaylistCount);
        TextView tvLength = card.findViewById(R.id.tvPlaylistLength);
        tvTitle.setText(playlist.getName());

        ArrayList<Song> songs = new ArrayList<>();
        if (playlist.getId().equals("liked")) {
            songs.addAll(SongRepository.getLikedSongs(requireContext()));
        } else if (playlist.getId().equals("local")) {
            songs.addAll(SongRepository.getSongs());
        } else if (playlist.getId().equals("online")) {
            songs.addAll(SongRepository.getOnlineSongs(requireContext()));
        } else {
            for (Integer id : playlist.getSongAudioResIds()) {
                Song s = SongRepository.findByAudioResId(requireContext(), id);
                if (s != null) songs.add(s);
            }
        }

        int count = songs.size();
        tvCount.setText(count == 1 ? "1 song" : count + " songs");
        long totalMs = 0;
        for (Song s : songs) {
            totalMs += s.getDurationMs();
        }
        tvLength.setText(formatDuration(totalMs));

        if (playlist.getId().equals("liked")) {
            cover.setImageResource(R.drawable.icon_liked);
            cover.setBackgroundResource(R.drawable.background_liked_gradient);
        }
        else if (playlist.getId().equals("local")) {
            cover.setImageResource(R.drawable.icon_local);
            cover.setBackgroundResource(R.drawable.background_local_gradient);
        }
        else if (playlist.getId().equals("online")) {
            cover.setImageResource(R.drawable.icon_online);
            cover.setBackgroundResource(R.drawable.background_online_gradient);
        }
        else if (!songs.isEmpty()) {
            cover.setImageResource(songs.get(0).getCoverResId());
        }
        else {
            cover.setImageResource(R.drawable.default_cover);
        }
        android.app.Dialog dialog = WavvyDialogs.showCenteredCardDialog(requireContext(), requireActivity(), card);
        card.findViewById(R.id.btnClose).setOnClickListener(v -> dialog.dismiss());
    }
    private void showDeleteDialog(Playlist playlist) {
        View card = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_delete_playlist, null);
        TextView tvMsg = card.findViewById(R.id.tvMsg);
        tvMsg.setText("Do you really want to delete \"" + playlist.getName() + "\"?");
        android.app.Dialog dialog = WavvyDialogs.showCenteredCardDialog(requireContext(), requireActivity(), card);

        card.findViewById(R.id.btnDelete).setOnClickListener(v -> {
            PlaylistRepository.deletePlaylist(requireContext(), playlist.getId());
            reload();
            dialog.dismiss();
            android.widget.Toast.makeText(requireContext(), "Playlist deleted", android.widget.Toast.LENGTH_SHORT).show();
        });
        card.findViewById(R.id.btnCancel).setOnClickListener(v -> dialog.dismiss());
    }
    private boolean hasInternet() {
        android.net.ConnectivityManager cm = (android.net.ConnectivityManager) requireContext().getSystemService(Context.CONNECTIVITY_SERVICE);

        if (cm == null) return false;
        android.net.Network network = cm.getActiveNetwork();

        if (network == null) return false;
        android.net.NetworkCapabilities caps = cm.getNetworkCapabilities(network);
        return caps != null && (caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) || caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR));
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