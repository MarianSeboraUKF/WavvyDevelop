package sk.ukf.wavvy;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
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
        for (String id : LikedSongsRepository.getLikedSongs(requireContext())) {
            try {
                liked.addSong(Integer.parseInt(id));
            } catch (Exception ignored) {}
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
        for (String id : LikedSongsRepository.getLikedSongs(requireContext())) {
            try {
                liked.addSong(Integer.parseInt(id));
            } catch (Exception ignored) {}
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
        View card = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_playlist_menu, null);

        View btnRename = card.findViewById(R.id.btnRename);
        View btnDelete = card.findViewById(R.id.btnDelete);
        View btnInfo = card.findViewById(R.id.btnInfo);

        android.app.Dialog dialog =
                WavvyDialogs.showCenteredCardDialog(requireContext(), requireActivity(), card);

        btnRename.setOnClickListener(v -> {
            dialog.dismiss();
            showRenameDialog(playlist);
        });

        btnDelete.setOnClickListener(v -> {
            dialog.dismiss();
            showDeletePlaylistDialog(playlist);
        });

        btnInfo.setOnClickListener(v -> {
            dialog.dismiss();
            showPlaylistInfoDialog(playlist);
        });
    }
    private void showPlaylistInfoDialog(Playlist playlist) {
        View card = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_playlist_info, null);

        TextView tvInfo = card.findViewById(R.id.tvInfoContent);

        int count = playlist.getSongAudioResIds().size();

        String info =
                "Name: " + playlist.getName() +
                        "\n\nTracks: " + count +
                        "\n\nCreated: available locally" +
                        "\n\nUpdated: last saved locally";

        tvInfo.setText(info);
        WavvyDialogs.showCenteredCardDialog(requireContext(), requireActivity(), card);
    }
}