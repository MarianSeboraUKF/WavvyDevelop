package sk.ukf.wavvy;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import sk.ukf.wavvy.adapter.PlaylistAdapter;
import sk.ukf.wavvy.adapter.SystemPlaylistAdapter;
import sk.ukf.wavvy.model.Playlist;
import sk.ukf.wavvy.model.Song;

public class LibraryFragment extends Fragment {
    private ArrayList<Playlist> playlists;
    private PlaylistAdapter adapter;
    private SystemPlaylistAdapter systemAdapter;
    private ArrayList<Playlist> systemPlaylists;

    public LibraryFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_library, container, false);

        RecyclerView rvSystem = view.findViewById(R.id.rvSystemPlaylists);
        RecyclerView rv = view.findViewById(R.id.rvPlaylists);
        rvSystem.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        rv.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        rv.setNestedScrollingEnabled(false);
        rv.setHasFixedSize(false);

        systemPlaylists = new ArrayList<>();
        Playlist liked = new Playlist("liked", "Liked songs", true);
        ArrayList<Song> likedSongs = SongRepository.getLikedSongs(requireContext());

        for (Song s : likedSongs) {
            liked.addSong(s.getAudioResId());
        }

        Playlist local = new Playlist("local", "Local songs", true);
        for (Song s : SongRepository.getSongs()) {
            local.addSong(s.getAudioResId());
        }

        systemPlaylists.add(liked);
        systemPlaylists.add(local);
        systemAdapter = new SystemPlaylistAdapter(systemPlaylists, this::openPlaylist);
        rvSystem.setAdapter(systemAdapter);

        playlists = new ArrayList<>();
        playlists.addAll(PlaylistRepository.getPlaylists(requireContext()));
        adapter = new PlaylistAdapter(
                playlists,
                playlist -> {
                    Intent intent = new Intent(requireContext(), PlaylistDetailActivity.class);
                    intent.putExtra(PlaylistDetailActivity.EXTRA_PLAYLIST_ID, playlist.getId());
                    intent.putExtra(PlaylistDetailActivity.EXTRA_PLAYLIST_NAME, playlist.getName());
                    startActivity(intent);
                },
                (playlist, anchor) -> showPopupMenu(playlist, anchor)
        );
        rv.setAdapter(adapter);
        View btnCreate = view.findViewById(R.id.btnCreatePlaylist);
        btnCreate.setOnClickListener(v -> showCreateDialog());
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        reload();
    }
    private void openPlaylist(Playlist playlist) {
        Intent intent = new Intent(requireContext(), PlaylistDetailActivity.class);
        intent.putExtra(PlaylistDetailActivity.EXTRA_PLAYLIST_ID, playlist.getId());
        intent.putExtra(PlaylistDetailActivity.EXTRA_PLAYLIST_NAME, playlist.getName());
        startActivity(intent);
    }
    private void reload() {
        systemPlaylists.clear();
        Playlist liked = new Playlist("liked", "Liked songs", true);
        ArrayList<Song> likedSongs = SongRepository.getLikedSongs(requireContext());
        for (Song s : likedSongs) {
            liked.addSong(s.getAudioResId());
        }

        Playlist local = new Playlist("local", "Local songs", true);
        for (Song s : SongRepository.getSongs()) {
            local.addSong(s.getAudioResId());
        }

        systemPlaylists.add(liked);
        systemPlaylists.add(local);
        systemAdapter.notifyDataSetChanged();
        playlists.clear();
        playlists.addAll(PlaylistRepository.getPlaylists(requireContext()));
        adapter.notifyDataSetChanged();
    }
    private void showCreateDialog() {
        View card = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_create_playlist, null);

        EditText etName = card.findViewById(R.id.etName);
        View btnCreate = card.findViewById(R.id.btnCreate);
        View btnCancel = card.findViewById(R.id.btnCancel);

        android.app.Dialog dialog =
                WavvyDialogs.showCenteredCardDialog(requireContext(), requireActivity(), card);

        btnCreate.setOnClickListener(x -> {
            String name = etName.getText().toString().trim();
            if (!name.isEmpty()) {
                PlaylistRepository.createPlaylist(requireContext(), name);
                reload();
                dialog.dismiss();
            }
        });
        btnCancel.setOnClickListener(x -> dialog.dismiss());
    }
    private void showDeletePlaylistDialog(Playlist playlist) {
        View card = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_delete_playlist, null);
        TextView tvMsg = card.findViewById(R.id.tvMsg);
        tvMsg.setText("Do you really want to delete „" + playlist.getName() + "“?");

        View btnDelete = card.findViewById(R.id.btnDelete);
        View btnCancel = card.findViewById(R.id.btnCancel);

        android.app.Dialog dialog = WavvyDialogs.showCenteredCardDialog(requireContext(), requireActivity(), card);

        btnDelete.setOnClickListener(x -> {
            PlaylistRepository.deletePlaylist(requireContext(), playlist.getId());
            dialog.dismiss();
            reload();
        });
        btnCancel.setOnClickListener(x -> dialog.dismiss());
    }
    private void showRenameDialog(Playlist playlist) {
        View card = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_rename_playlist, null);

        EditText etName = card.findViewById(R.id.etName);
        View btnRename = card.findViewById(R.id.btnRename);
        View btnCancel = card.findViewById(R.id.btnCancel);

        etName.setText(playlist.getName());

        android.app.Dialog dialog =
                WavvyDialogs.showCenteredCardDialog(requireContext(), requireActivity(), card);

        btnRename.setOnClickListener(x -> {
            String newName = etName.getText().toString().trim();

            if (!newName.isEmpty()) {
                PlaylistRepository.renamePlaylist(requireContext(), playlist.getId(), newName);
                dialog.dismiss();
                reload();
            }
        });
        btnCancel.setOnClickListener(x -> dialog.dismiss());
    }
    private void showPopupMenu(Playlist playlist, View anchor) {
        View popupView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_playlist_menu, null);

        PopupWindow popup = new PopupWindow(
                popupView,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true
        );

        popup.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        popup.setElevation(12f);

        View actionInfo = popupView.findViewById(R.id.actionInfo);
        View actionImport = popupView.findViewById(R.id.actionImport);
        View actionEdit = popupView.findViewById(R.id.actionEdit);
        View actionDelete = popupView.findViewById(R.id.actionDelete);

        actionImport.setVisibility(View.GONE);
        actionEdit.setVisibility(View.GONE);
        actionDelete.setVisibility(View.GONE);

        if (!playlist.isSystem()) {
            actionEdit.setVisibility(View.VISIBLE);
            actionDelete.setVisibility(View.VISIBLE);
            actionEdit.setOnClickListener(v -> {
                popup.dismiss();
                showRenameDialog(playlist);
            });

            actionDelete.setOnClickListener(v -> {
                popup.dismiss();
                showDeletePlaylistDialog(playlist);
            });
        }

        actionInfo.setOnClickListener(v -> {
            popup.dismiss();
            showPlaylistInfoDialog(playlist);
        });
        popup.showAsDropDown(anchor, -200, 20);
    }
    private void showPlaylistInfoDialog(Playlist playlist) {
        View card = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_playlist_info, null);

        View coverBg = card.findViewById(R.id.playlistCoverBg);
        ImageView coverIcon = card.findViewById(R.id.playlistCoverIcon);
        TextView tvTitle = card.findViewById(R.id.tvPlaylistTitle);
        TextView tvCount = card.findViewById(R.id.tvPlaylistCount);
        TextView tvLength = card.findViewById(R.id.tvPlaylistLength);
        View btnClose = card.findViewById(R.id.btnClose);
        tvTitle.setText(playlist.getName());

        int count = playlist.getSongAudioResIds().size();
        long totalMs = 0;
        for (Integer id : playlist.getSongAudioResIds()) {
            Song s = SongRepository.findByAudioResId(id);
            if (s != null) totalMs += s.getDurationMs();
        }

        tvCount.setText(count == 1 ? "1 song" : count + " songs");
        tvLength.setText(formatDuration(totalMs));

        if (playlist.getId().equals("liked")) {
            coverBg.setBackgroundResource(R.drawable.bg_liked_gradient);
            coverIcon.setImageResource(R.drawable.ic_liked);
        } else if (playlist.getId().equals("local")) {
            coverBg.setBackgroundResource(R.drawable.bg_local_gradient);
            coverIcon.setImageResource(R.drawable.icon_local);
        } else {
            coverBg.setBackgroundColor(android.graphics.Color.TRANSPARENT);
            if (!playlist.getSongAudioResIds().isEmpty()) {
                Song s = SongRepository.findByAudioResId(
                        playlist.getSongAudioResIds().get(0)
                );
                if (s != null) {
                    coverIcon.setImageResource(s.getCoverResId());
                }
            }
        }
        android.app.Dialog dialog = WavvyDialogs.showCenteredCardDialog(requireContext(), requireActivity(), card);
        btnClose.setOnClickListener(v -> dialog.dismiss());
    }
    private String formatDuration(long ms) {
        long totalSeconds = ms / 1000;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        if (hours > 0) {
            return hours + " hr " + minutes + " min";
        } else {
            return minutes + " min " + seconds + " sec";
        }
    }
}