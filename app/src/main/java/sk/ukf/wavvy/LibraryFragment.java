package sk.ukf.wavvy;

import android.content.Intent;
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
import java.util.List;
import sk.ukf.wavvy.adapter.PlaylistAdapter;
import sk.ukf.wavvy.adapter.SongHorizontalAdapter;
import sk.ukf.wavvy.adapter.SystemPlaylistAdapter;
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

        for (Song s : SongRepository.getSongs()) {
            local.addSong(s.getAudioResId());
        }

        systemPlaylists.add(liked);
        systemPlaylists.add(local);
        systemAdapter = new SystemPlaylistAdapter(systemPlaylists, this::openPlaylist);
        rvSystem.setAdapter(systemAdapter);

        rvSongsPager = view.findViewById(R.id.rvSongsPager);
        rvSongsPager.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        snapHelper = new PagerSnapHelper();
        snapHelper.attachToRecyclerView(rvSongsPager);
        rvSongsPager.setOverScrollMode(View.OVER_SCROLL_NEVER);
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

        libraryReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                reload();
            }
        };
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

            List<Song> page = new ArrayList<>(
                    likedSongs.subList(i, Math.min(i + 3, likedSongs.size()))
            );
            while (page.size() < 3) {
                page.add(null);
            }
            pages.add(page);
        }
        SongHorizontalAdapter songPagerAdapter = new SongHorizontalAdapter(
                pages,
                song -> PlayerLauncher.openQueue(requireContext(), likedSongs, song)
        );
        rvSongsPager.setAdapter(songPagerAdapter);
    }

    @Override
    public void onStart() {
        super.onStart();
        IntentFilter filter = new IntentFilter();
        filter.addAction(LikedSongsRepository.ACTION_LIKED_CHANGED);
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

        for (Song s : SongRepository.getSongs()) {
            local.addSong(s.getAudioResId());
        }

        systemPlaylists.add(liked);
        systemPlaylists.add(local);
        systemAdapter.notifyDataSetChanged();
        playlists.clear();
        playlists.addAll(PlaylistRepository.getPlaylists(requireContext()));
        playlistAdapter.notifyDataSetChanged();

        setupSongs();
        rvSongsPager.scrollToPosition(songsScrollPosition);
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

        PopupWindow popup = new PopupWindow(
                popupView,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true
        );
        popup.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        popup.setElevation(12f);

        popupView.findViewById(R.id.actionInfo).setOnClickListener(v -> {
            popup.dismiss();
            showPlaylistInfoDialog(playlist);
        });
        popup.showAsDropDown(anchor, -200, 20);
    }
    private void showPlaylistInfoDialog(Playlist playlist) {
        View card = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_playlist_info, null);
        ((TextView) card.findViewById(R.id.tvPlaylistTitle)).setText(playlist.getName());
        android.app.Dialog dialog = WavvyDialogs.showCenteredCardDialog(requireContext(), requireActivity(), card);
        card.findViewById(R.id.btnClose).setOnClickListener(v -> dialog.dismiss());
    }
}