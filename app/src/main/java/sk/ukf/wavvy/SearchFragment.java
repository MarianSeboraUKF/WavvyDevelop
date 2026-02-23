package sk.ukf.wavvy;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.snackbar.Snackbar;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;
import sk.ukf.wavvy.adapter.PickPlaylistAdapter;
import sk.ukf.wavvy.adapter.SongAdapter;
import sk.ukf.wavvy.model.Playlist;
import sk.ukf.wavvy.model.Song;

public class SearchFragment extends Fragment {
    private ArrayList<Song> allSongs;
    private ArrayList<Song> topSongs;
    private ArrayList<Song> filteredSongs;
    private SongAdapter adapter;
    private TextView tvSectionTitle;
    private TextView tvEmpty;
    private SearchView searchView;

    public SearchFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_search, container, false);

        tvSectionTitle = view.findViewById(R.id.tvSectionTitle);
        tvEmpty = view.findViewById(R.id.tvEmpty);

        RecyclerView rv = view.findViewById(R.id.rvSearchSongs);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));

        allSongs = SongRepository.getSongs();
        for (Song s : allSongs) {
            GradientPreloader.preload(requireContext(), s.getCoverResId());
        }

        topSongs = new ArrayList<>();
        recomputeTopSongs();

        filteredSongs = new ArrayList<>(topSongs);

        adapter = new SongAdapter(
                filteredSongs,
                song -> PlayerLauncher.openQueue(requireContext(), filteredSongs, song),
                this::showAddToPlaylistDialog
        );
        rv.setAdapter(adapter);

        searchView = view.findViewById(R.id.searchView);
        searchView.setQueryHint("Vyhľadajte skladbu, interpreta alebo album...");

        android.widget.EditText searchText =
                searchView.findViewById(androidx.appcompat.R.id.search_src_text);

        searchText.setTextColor(ContextCompat.getColor(requireContext(), R.color.textPrimary));
        searchText.setHintTextColor(ContextCompat.getColor(requireContext(), R.color.textSecondary));

        View plate = searchView.findViewById(androidx.appcompat.R.id.search_plate);
        if (plate != null) plate.setBackground(null);

        View submitArea = searchView.findViewById(androidx.appcompat.R.id.submit_area);
        if (submitArea != null) submitArea.setBackground(null);

        searchView.setIconifiedByDefault(false);
        searchView.setQuery("", false);
        searchView.clearFocus();

        showTopState();

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override public boolean onQueryTextSubmit(String query) { filter(query); return true; }
            @Override public boolean onQueryTextChange(String newText) { filter(newText); return true; }
        });
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        recomputeTopSongs();

        if (searchView != null) {
            filter(searchView.getQuery() != null ? searchView.getQuery().toString() : "");
        } else {
            filter("");
        }
    }
    private String norm(String s) {
        if (s == null) return "";
        String n = Normalizer.normalize(s, Normalizer.Form.NFD);
        n = n.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return n.toLowerCase(Locale.ROOT).trim();
    }
    private void recomputeTopSongs() {
        ArrayList<Song> copy = new ArrayList<>(allSongs);

        Collections.sort(copy, new Comparator<Song>() {
            @Override
            public int compare(Song a, Song b) {
                int ca = PlayCountRepository.getCount(requireContext(), a.getAudioResId());
                int cb = PlayCountRepository.getCount(requireContext(), b.getAudioResId());
                return Integer.compare(cb, ca);
            }
        });

        topSongs.clear();
        for (int i = 0; i < copy.size() && i < 3; i++) {
            topSongs.add(copy.get(i));
        }
    }
    private void filter(String text) {
        String q = norm(text);
        filteredSongs.clear();
        if (TextUtils.isEmpty(q)) {
            showTopState();
            filteredSongs.addAll(topSongs);
        } else {
            tvSectionTitle.setText("Výsledky");
            tvEmpty.setVisibility(View.GONE);
            for (Song s : allSongs) {
                String title = norm(s.getTitle());
                String artist = norm(s.getArtist());
                String album = norm(s.getAlbum());

                if (title.contains(q) || artist.contains(q) || album.contains(q)) {
                    filteredSongs.add(s);
                }
            }
            if (filteredSongs.isEmpty()) {
                tvEmpty.setText("Nič sa nenašlo.");
                tvEmpty.setVisibility(View.VISIBLE);
            }
        }
        adapter.notifyDataSetChanged();
    }
    private void showTopState() {
        tvSectionTitle.setText("Top skladby");
        tvEmpty.setVisibility(View.GONE);
    }
    private void showAddToPlaylistDialog(Song song) {
        ArrayList<Playlist> playlists = PlaylistRepository.getPlaylists(requireContext());

        if (playlists.isEmpty()) {
            android.widget.Toast.makeText(requireContext(),
                    "Najprv si vytvor playlist", android.widget.Toast.LENGTH_SHORT).show();
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