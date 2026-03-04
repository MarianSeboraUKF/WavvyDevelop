package sk.ukf.wavvy;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.snackbar.Snackbar;
import java.util.ArrayList;
import sk.ukf.wavvy.adapter.PickPlaylistAdapter;
import sk.ukf.wavvy.adapter.SongAdapter;
import sk.ukf.wavvy.model.Playlist;
import sk.ukf.wavvy.model.Song;
import sk.ukf.wavvy.adapter.SmallSongAdapter;

public class HomeFragment extends Fragment implements PlaybackManager.Listener {
    private SongAdapter adapter;
    private PlaybackManager pm;
    private RecyclerView rvMostPlayed;
    private ArrayList<Song> mostPlayed;
    private RecyclerView rvRecent;
    private ArrayList<Song> recentSongs;
    private SmallSongAdapter mostPlayedAdapter;
    private SmallSongAdapter recentAdapter;
    private int lastPlayingAudioId = -1;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_home, container, false);

        pm = PlaybackManager.get(requireContext());

        rvMostPlayed = view.findViewById(R.id.rvMostPlayed);
        RecyclerView rvSongs = view.findViewById(R.id.rvSongs);

        rvMostPlayed.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        rvSongs.setLayoutManager(new LinearLayoutManager(requireContext()));

        rvRecent = view.findViewById(R.id.rvRecent);

        rvRecent.setLayoutManager(
                new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        );

        recentSongs = SongRepository.getRecentlyPlayedSongs(requireContext());

        recentAdapter = new SmallSongAdapter(
                recentSongs,
                song -> PlayerLauncher.openQueue(requireContext(), recentSongs, song)
        );

        rvRecent.setAdapter(recentAdapter);

        ArrayList<Song> allSongs = SongRepository.getSongs();
        mostPlayed = SongRepository.getMostPlayedSongs(requireContext());

        mostPlayedAdapter = new SmallSongAdapter(
                mostPlayed,
                song -> PlayerLauncher.openQueue(requireContext(), mostPlayed, song)
        );

        adapter = new SongAdapter(
                allSongs,
                song -> PlayerLauncher.openQueue(requireContext(), allSongs, song),
                this::showAddToPlaylistDialog
        );

        rvMostPlayed.setAdapter(mostPlayedAdapter);
        rvSongs.setAdapter(adapter);
        rvSongs.setItemAnimator(new androidx.recyclerview.widget.DefaultItemAnimator());

        for (Song s : allSongs) {
            GradientPreloader.preload(requireContext(), s.getCoverResId());
        }

        return view;
    }
    private void showAddToPlaylistDialog(Song song) {
        ArrayList<Playlist> playlists = PlaylistRepository.getPlaylists(requireContext());

        if (playlists.isEmpty()) {
            Toast.makeText(requireContext(), "You have to create playlist first", Toast.LENGTH_SHORT).show();
            return;
        }

        View card = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_pick_playlist, null);

        RecyclerView rv = card.findViewById(R.id.rvPickPlaylists);
        View btnCancel = card.findViewById(R.id.btnCancel);

        rv.setLayoutManager(new LinearLayoutManager(requireContext()));

        android.app.Dialog dialog =
                WavvyDialogs.showCenteredCardDialog(requireContext(), requireActivity(), card);

        PickPlaylistAdapter pickAdapter = new PickPlaylistAdapter(playlists, selected -> {
            PlaylistRepository.addSongToPlaylist(requireContext(), selected.getId(), song.getAudioResId());
            showSnack(requireView(), "Added to playlist: " + selected.getName());
            dialog.dismiss();
        });

        rv.setAdapter(pickAdapter);
        btnCancel.setOnClickListener(x -> dialog.dismiss());
    }
    private void showSnack(View anchorView, String text) {
        Snackbar sb = Snackbar.make(anchorView, text, Snackbar.LENGTH_SHORT);

        sb.setBackgroundTint(ContextCompat.getColor(requireContext(), R.color.surface));
        sb.setTextColor(ContextCompat.getColor(requireContext(), R.color.textPrimary));
        sb.setAnimationMode(Snackbar.ANIMATION_MODE_SLIDE);

        View snackView = sb.getView();
        snackView.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.bg_snackbar));

        TextView tv = snackView.findViewById(com.google.android.material.R.id.snackbar_text);
        if (tv != null) {
            tv.setTextColor(ContextCompat.getColor(requireContext(), R.color.textPrimary));
            tv.setMaxLines(2);
        }
        sb.show();
    }
    @Override
    public void onResume() {
        super.onResume();

        if (mostPlayedAdapter != null) {

            mostPlayed.clear();
            mostPlayed.addAll(
                    SongRepository.getMostPlayedSongs(requireContext())
            );
            mostPlayedAdapter.notifyDataSetChanged();
        }
        recentSongs.clear();
        recentSongs.addAll(
                SongRepository.getRecentlyPlayedSongs(requireContext())
        );
        recentAdapter.notifyDataSetChanged();
    }
    @Override
    public void onStart() {
        super.onStart();
        if (pm != null) {
            pm.addListener(this);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (pm != null) {
            pm.removeListener(this);
        }
    }
    @Override
    public void onNowPlayingChanged(int audioResId, int[] queueIds, int queueIndex) {
        if (adapter != null) {
            ArrayList<Song> songs = SongRepository.getSongs();

            int newIndex = -1;
            int oldIndex = -1;

            for (int i = 0; i < songs.size(); i++) {

                if (songs.get(i).getAudioResId() == audioResId) {
                    newIndex = i;
                }

                if (songs.get(i).getAudioResId() == lastPlayingAudioId) {
                    oldIndex = i;
                }
            }

            if (oldIndex >= 0) {
                adapter.notifyItemChanged(oldIndex);
            }

            if (newIndex >= 0) {
                adapter.notifyItemChanged(newIndex);
            }
            lastPlayingAudioId = audioResId;
        }

        if (mostPlayedAdapter != null) {
            mostPlayedAdapter.notifyDataSetChanged();
        }

        if (recentAdapter != null) {
            recentAdapter.notifyDataSetChanged();
        }
    }
    @Override
    public void onIsPlayingChanged(boolean isPlaying) {}

    @Override
    public void onProgress(long positionMs, long durationMs) {}
}