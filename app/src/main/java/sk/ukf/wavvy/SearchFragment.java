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
    private TextView tvSectionSubtitle;
    private TextView tvEmpty;
    private TextView tvRecentSearches;
    private TextView tvRecentSearchTitle;
    private SearchView searchView;
    private RecyclerView recyclerView;
    private String lastSearch = "";

    public SearchFragment() {}
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_search, container, false);

        tvSectionTitle = view.findViewById(R.id.tvSectionTitle);
        tvSectionSubtitle = view.findViewById(R.id.tvSectionSubtitle);
        tvEmpty = view.findViewById(R.id.tvEmpty);
        tvRecentSearches = view.findViewById(R.id.tvRecentSearches);
        tvRecentSearchTitle = view.findViewById(R.id.tvRecentSearchTitle);
        recyclerView = view.findViewById(R.id.rvSearchSongs);

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setItemViewCacheSize(12);
        recyclerView.setHasFixedSize(true);

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

        recyclerView.setAdapter(adapter);

        view.post(this::prewarmRecycler);

        searchView = view.findViewById(R.id.searchView);
        searchView.setQueryHint("Search songs, artists or albums...");

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

        tvRecentSearches.setOnClickListener(v -> {
            if (!lastSearch.isEmpty()) {
                searchView.setQuery(lastSearch, true);
            }
        });

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filter(query);
                return true;
            }
            @Override
            public boolean onQueryTextChange(String newText) {
                filter(newText);
                return true;
            }
        });
        return view;
    }
    private void prewarmRecycler() {
        if (recyclerView == null) return;
        if (recyclerView.getAdapter() == null) return;

        RecyclerView.Adapter adapter = recyclerView.getAdapter();

        int warmCount = Math.min(4, adapter.getItemCount());

        for (int i = 0; i < warmCount; i++) {
            RecyclerView.ViewHolder vh =
                    adapter.createViewHolder(
                            recyclerView,
                            adapter.getItemViewType(i)
                    );

            adapter.bindViewHolder(vh, i);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        recomputeTopSongs();
        String currentQuery = "";

        if (searchView != null && searchView.getQuery() != null) {
            currentQuery = searchView.getQuery().toString();
        }

        if (!currentQuery.isEmpty()) {
            lastSearch = currentQuery;
            filter(currentQuery);
        } else {
            showTopState();
            filteredSongs.clear();
            filteredSongs.addAll(topSongs);
            adapter.notifyDataSetChanged();
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

        Collections.sort(copy, (a, b) -> {
            int ca = PlayCountRepository.getCount(requireContext(), a.getAudioResId());
            int cb = PlayCountRepository.getCount(requireContext(), b.getAudioResId());
            return Integer.compare(cb, ca);
        });

        topSongs.clear();

        for (int i = 0; i < copy.size() && i < 3; i++) {
            topSongs.add(copy.get(i));
        }
    }
    private void filter(String text) {

        if (!text.isEmpty()) {
            lastSearch = text;
        }

        String q = norm(text);

        filteredSongs.clear();

        if (TextUtils.isEmpty(q)) {
            showTopState();
            filteredSongs.addAll(topSongs);
            recyclerView.setVisibility(View.VISIBLE);

        } else {
            tvSectionSubtitle.setText("Matching songs, artists or albums");
            tvSectionSubtitle.setVisibility(View.VISIBLE);
            tvRecentSearchTitle.setVisibility(View.GONE);
            tvRecentSearches.setVisibility(View.GONE);
            tvEmpty.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);

            for (Song s : allSongs) {

                String title = norm(s.getTitle());
                String artist = norm(s.getArtist());
                String album = norm(s.getAlbum());

                if (title.contains(q) ||
                        artist.contains(q) ||
                        album.contains(q)) {
                    filteredSongs.add(s);
                }
            }

            tvSectionTitle.setText("Results (" + filteredSongs.size() + ")");

            if (filteredSongs.isEmpty()) {
                tvEmpty.setText("No songs found\n\nTry another title, artist or album");
                tvEmpty.setVisibility(View.VISIBLE);
                tvSectionSubtitle.setVisibility(View.GONE);
                recyclerView.setVisibility(View.GONE);
            }
        }
        adapter.setHighlightQuery(q);
        adapter.notifyDataSetChanged();
        recyclerView.setAlpha(0f);
        recyclerView.animate().alpha(1f).setDuration(180).start();
    }
    private void showTopState() {
        tvSectionTitle.setText("Top songs");
        tvEmpty.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);
        tvSectionSubtitle.setText("Your most listened tracks");
        tvSectionSubtitle.setVisibility(View.VISIBLE);

        if (!lastSearch.isEmpty()) {
            tvRecentSearchTitle.setVisibility(View.VISIBLE);
            tvRecentSearches.setText("🔍 " + lastSearch);
            tvRecentSearches.setVisibility(View.VISIBLE);
        } else {
            tvRecentSearchTitle.setVisibility(View.GONE);
            tvRecentSearches.setVisibility(View.GONE);
        }
    }
    private void showAddToPlaylistDialog(Song song) {

        ArrayList<Playlist> playlists =
                PlaylistRepository.getPlaylists(requireContext());

        if (playlists.isEmpty()) {
            android.widget.Toast.makeText(
                    requireContext(),
                    "You have to create playlist first",
                    android.widget.Toast.LENGTH_SHORT
            ).show();
            return;
        }

        View card = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_pick_playlist, null);

        RecyclerView rv = card.findViewById(R.id.rvPickPlaylists);
        View btnCancel = card.findViewById(R.id.btnCancel);

        rv.setLayoutManager(new LinearLayoutManager(requireContext()));

        android.app.Dialog dialog =
                WavvyDialogs.showCenteredCardDialog(
                        requireContext(),
                        requireActivity(),
                        card
                );

        PickPlaylistAdapter pickAdapter =
                new PickPlaylistAdapter(playlists, selected -> {

                    PlaylistRepository.addSongToPlaylist(
                            requireContext(),
                            selected.getId(),
                            song.getAudioResId()
                    );

                    showSnack(requireView(),
                            "Added to playlist: " + selected.getName());
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

        snackView.setBackground(
                ContextCompat.getDrawable(requireContext(), R.drawable.bg_snackbar)
        );

        TextView tv =
                snackView.findViewById(
                        com.google.android.material.R.id.snackbar_text
                );

        if (tv != null) {
            tv.setMaxLines(2);
        }

        sb.show();
    }
}