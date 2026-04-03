package sk.ukf.wavvy;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
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
    private PlaybackManager.Listener playbackListener;

    public LibraryFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_library, container, false);
        RecyclerView rvSystem = view.findViewById(R.id.rvSystemPlaylists);
        RecyclerView rv = view.findViewById(R.id.rvPlaylists);
        rvSystem.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        rv.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        rv.setNestedScrollingEnabled(false);

        systemPlaylists = new ArrayList<>();
        Playlist liked = new Playlist("liked", "Liked songs", true);
        Playlist local = new Playlist("local", "Local songs", true);
        systemAdapter = new SystemPlaylistAdapter(systemPlaylists, this::openPlaylist);
        rvSystem.setAdapter(systemAdapter);

        SongRepository.getAllSongs(requireContext(), songs -> {
            local.getSongAudioResIds().clear();
            for (Song s : songs) {
                if (!s.isOnline()) {
                    local.addSong(s.getAudioResId());
                }
            }
            systemPlaylists.clear();
            systemPlaylists.add(liked);
            systemPlaylists.add(local);
            systemAdapter.notifyDataSetChanged();
        });

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

        songPagerAdapter = new SongHorizontalAdapter(new ArrayList<>(),
                song -> {
                    SongRepository.getAllSongs(requireContext(), allSongs -> {
                        ArrayList<Song> likedSongs = new ArrayList<>();
                        for (Song s : allSongs) {
                            if (s.isOnline()) continue;
                            String id = String.valueOf(s.getAudioResId());
                            if (LikedSongsRepository.isLiked(requireContext(), id)) {
                                likedSongs.add(s);
                            }
                        }
                        PlayerLauncher.openQueue(requireContext(), likedSongs, song);
                    });
                }
        );
        rvSongsPager.setAdapter(songPagerAdapter);
        setupSongs();

        rvSongsPager.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(@NonNull android.graphics.Rect outRect, @NonNull View view, RecyclerView parent, RecyclerView.State state) {
                outRect.right = 24;
            }
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
                    requireActivity().overridePendingTransition(R.anim.slide_in_right_fast, R.anim.slide_out_left_fast);
                },
                (playlist, anchor) -> showPopupMenu(playlist, anchor)
        );
        rv.setAdapter(playlistAdapter);
        view.findViewById(R.id.btnCreatePlaylist).setOnClickListener(v -> showCreateDialog());

        albumsList = new ArrayList<>();

        Set<String> savedAlbums = requireContext()
                .getSharedPreferences("saved_albums", Context.MODE_PRIVATE)
                .getStringSet("album_titles", new HashSet<>());

        if (savedAlbums == null) savedAlbums = new HashSet<>();

        for (String albumTitle : savedAlbums) {
            Album album = AlbumRepository.findByTitle(albumTitle);
            if (album != null) {
                albumsList.add(album);
            }
        }

        Collections.sort(albumsList, (a, b) -> {
            String t1 = Normalizer.normalize(a.getTitle(), Normalizer.Form.NFD)
                    .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
            String t2 = Normalizer.normalize(b.getTitle(), Normalizer.Form.NFD)
                    .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");

            return t1.compareToIgnoreCase(t2);
        });

        albumAdapter = new AlbumAdapter(
                albumsList,
                album -> {
                    Intent intent = new Intent(requireContext(), AlbumDetailActivity.class);
                    intent.putExtra("album_title", album.getTitle());
                    startActivity(intent);
                    requireActivity().overridePendingTransition(R.anim.slide_in_right_fast, R.anim.slide_out_left_fast);
                }
        );
        rvAlbums.setAdapter(albumAdapter);
        libraryReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                reload();
            }
        };
        return view;
    }
    private void setupSongs() {
        SongRepository.getAllSongs(requireContext(), allSongs -> {
            ArrayList<Song> likedSongs = new ArrayList<>();

            for (Song s : allSongs) {
                if (s.isOnline()) continue;
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
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        IntentFilter filter = new IntentFilter();
        filter.addAction(LikedSongsRepository.ACTION_LIKED_CHANGED);
        filter.addAction("playlist_updated");
        requireContext().registerReceiver(libraryReceiver, filter, Context.RECEIVER_NOT_EXPORTED);

        playbackListener = new PlaybackManager.Listener() {
            @Override
            public void onNowPlayingChanged(int audioResId, int[] queueIds, int queueIndex) {
                if (songPagerAdapter != null) {
                    songPagerAdapter.notifyDataSetChanged();
                }
            }
            @Override
            public void onIsPlayingChanged(boolean isPlaying) {}
            @Override
            public void onProgress(long positionMs, long durationMs) {}
        };
        PlaybackManager.get(requireContext()).addListener(playbackListener);
    }

    @Override
    public void onStop() {
        super.onStop();
        try {
            requireContext().unregisterReceiver(libraryReceiver);
        } catch (Exception ignored) {}

        if (playbackListener != null) {
            PlaybackManager.get(requireContext()).removeListener(playbackListener);
        }
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

        SongRepository.getAllSongs(requireContext(), songs -> {
            for (Song s : songs) {
                if (!s.isOnline()) {
                    local.addSong(s.getAudioResId());
                }
            }
            systemPlaylists.clear();
            systemPlaylists.add(liked);
            systemPlaylists.add(local);
            systemAdapter.notifyDataSetChanged();
        });
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
        Set<String> savedAlbums = requireContext()
                .getSharedPreferences("saved_albums", Context.MODE_PRIVATE)
                .getStringSet("album_titles", new HashSet<>());

        if (savedAlbums == null) savedAlbums = new HashSet<>();

        for (String albumTitle : savedAlbums) {
            Album album = AlbumRepository.findByTitle(albumTitle);
            if (album != null) {
                albumsList.add(album);
            }
        }

        Collections.sort(albumsList, (a, b) -> {
            String t1 = Normalizer.normalize(a.getTitle(), Normalizer.Form.NFD)
                    .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
            String t2 = Normalizer.normalize(b.getTitle(), Normalizer.Form.NFD)
                    .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
            return t1.compareToIgnoreCase(t2);
        });

        albumAdapter.notifyDataSetChanged();
    }
    private void openPlaylist(Playlist playlist) {
        Intent intent = new Intent(requireContext(), PlaylistDetailActivity.class);
        intent.putExtra(PlaylistDetailActivity.EXTRA_PLAYLIST_ID, playlist.getId());
        intent.putExtra(PlaylistDetailActivity.EXTRA_PLAYLIST_NAME, playlist.getName());
        startActivity(intent);
        requireActivity().overridePendingTransition(R.anim.slide_in_right_fast, R.anim.slide_out_left_fast);
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
        View popupView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_playlist_menu, null);

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
            actionImport.setOnClickListener(v -> {popup.dismiss();});
        } else {
            actionEdit.setVisibility(View.VISIBLE);
            actionDelete.setVisibility(View.VISIBLE);
            actionEdit.setOnClickListener(v -> {popup.dismiss();
                showRenameDialog(playlist);
            });
            actionDelete.setOnClickListener(v -> {
                popup.dismiss();
                showDeleteDialog(playlist);
            });
        }
        actionInfo.setOnClickListener(v -> {popup.dismiss();showPlaylistInfoDialog(playlist);});
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
        }
        else if (playlist.getId().equals("local")) {
            songs.addAll(SongRepository.getSongs());
        }
        else {
            for (Integer id : playlist.getSongAudioResIds()) {
                Song s = SongRepository.findByAudioResId(id);
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
            cover.setImageResource(R.drawable.ic_liked);
            cover.setBackgroundResource(R.drawable.bg_liked_gradient);
        }
        else if (playlist.getId().equals("local")) {
            cover.setImageResource(R.drawable.icon_local);
            cover.setBackgroundResource(R.drawable.bg_local_gradient);
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