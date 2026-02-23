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

public class HomeFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_home, container, false);

        RecyclerView rvSongs = view.findViewById(R.id.rvSongs);
        rvSongs.setLayoutManager(new LinearLayoutManager(requireContext()));

        ArrayList<Song> songs = SongRepository.getSongs();
        for (Song s : songs) {
            GradientPreloader.preload(requireContext(), s.getCoverResId());
        }

        SongAdapter adapter = new SongAdapter(
                songs,
                song -> PlayerLauncher.openQueue(requireContext(), songs, song),
                this::showAddToPlaylistDialog
        );
        rvSongs.setAdapter(adapter);
        return view;
    }
    private void showAddToPlaylistDialog(Song song) {
        ArrayList<Playlist> playlists = PlaylistRepository.getPlaylists(requireContext());

        if (playlists.isEmpty()) {
            Toast.makeText(requireContext(), "Najprv si vytvor playlist", Toast.LENGTH_SHORT).show();
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
            showSnack(requireView(), "Pridané do playlistu: " + selected.getName());
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
}