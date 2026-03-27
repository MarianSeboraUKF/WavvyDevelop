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
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;
import sk.ukf.wavvy.adapter.SongAdapter;
import sk.ukf.wavvy.model.Song;

public class SearchFragment extends Fragment implements PlaybackManager.Listener{
    private ArrayList<Song> allSongs;
    private ArrayList<Song> topSongs;
    private PlaybackManager pm;
    private int lastPlayingId = -1;
    private ArrayList<Song> filteredSongs;
    private ArrayList<sk.ukf.wavvy.model.Album> allAlbums;
    private SongAdapter adapter;
    private TextView tvSectionTitle;
    private TextView tvSectionSubtitle;
    private TextView tvEmpty;
    private TextView tvRecentSearches;
    private TextView tvRecentSearchTitle;
    private SearchView searchView;
    private RecyclerView recyclerView;
    private String lastSearch = "";
    private TextView tvAlbumsLabel;
    private TextView tvSongsLabel;
    private RecyclerView rvAlbums;
    private ArrayList<sk.ukf.wavvy.model.Album> filteredAlbums;
    private sk.ukf.wavvy.adapter.AlbumAdapter albumAdapter;
    private android.os.Handler handler = new android.os.Handler();
    private Runnable searchRunnable;

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
        tvAlbumsLabel = view.findViewById(R.id.tvAlbumsLabel);
        tvSongsLabel = view.findViewById(R.id.tvSongsLabel);
        allAlbums = AlbumRepository.getAlbums();
        pm = PlaybackManager.get(requireContext());

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setItemViewCacheSize(12);
        recyclerView.setHasFixedSize(true);

        recyclerView.setItemAnimator(null);

        rvAlbums = view.findViewById(R.id.rvAlbums);
        rvAlbums.setLayoutManager(
                new LinearLayoutManager(
                        requireContext(),
                        LinearLayoutManager.HORIZONTAL,
                        false
                )
        );
        filteredAlbums = new ArrayList<>();

        albumAdapter = new sk.ukf.wavvy.adapter.AlbumAdapter(
                filteredAlbums,
                album -> {
                    android.content.Intent intent =
                            new android.content.Intent(requireContext(), AlbumDetailActivity.class);

                    intent.putExtra("album_title", album.getTitle());

                    startActivity(intent);
                }
        );
        rvAlbums.setAdapter(albumAdapter);
        rvAlbums.setNestedScrollingEnabled(false);
        allSongs = SongRepository.getSongs();

        for (Song s : allSongs) {
            GradientPreloader.preload(requireContext(), s.getCoverResId());
        }

        topSongs = new ArrayList<>();
        recomputeTopSongs();

        filteredSongs = new ArrayList<>(topSongs);

        adapter = new SongAdapter(
                filteredSongs,
                false,
                song -> PlayerLauncher.openQueue(
                        requireContext(),
                        filteredSongs,
                        song
                )
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
                if (searchRunnable != null) {
                    handler.removeCallbacks(searchRunnable);
                }
                searchRunnable = () -> filter(newText);
                handler.postDelayed(searchRunnable, 300);
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
    public void onStart() {
        super.onStart();
        if (pm != null) pm.addListener(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (pm != null) pm.removeListener(this);
    }
    @Override
    public void onNowPlayingChanged(int audioResId, int[] queueIds, int queueIndex) {

        if (adapter == null) return;

        int newIndex = -1;
        int oldIndex = -1;

        for (int i = 0; i < filteredSongs.size(); i++) {

            int id = filteredSongs.get(i).getAudioResId();

            if (id == audioResId) newIndex = i;
            if (id == lastPlayingId) oldIndex = i;
        }

        if (oldIndex >= 0) adapter.notifyItemChanged(oldIndex);
        if (newIndex >= 0) adapter.notifyItemChanged(newIndex);

        lastPlayingId = audioResId;
    }
    @Override
    public void onIsPlayingChanged(boolean isPlaying) {}
    @Override
    public void onProgress(long positionMs, long durationMs) {}

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
        filteredAlbums.clear();

        if (TextUtils.isEmpty(q)) {
            showTopState();
            filteredSongs.addAll(topSongs);
            rvAlbums.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);

            adapter.setHighlightQuery("");
            albumAdapter.setHighlightQuery("");

            adapter.notifyDataSetChanged();
            albumAdapter.notifyDataSetChanged();
            return;
        }

        tvSectionSubtitle.setVisibility(View.GONE);
        tvRecentSearchTitle.setVisibility(View.GONE);
        tvRecentSearches.setVisibility(View.GONE);
        tvEmpty.setVisibility(View.GONE);

        ArrayList<Song> titleMatches = new ArrayList<>();
        ArrayList<Song> artistMatches = new ArrayList<>();
        ArrayList<Song> albumMatches = new ArrayList<>();

        for (Song s : allSongs) {

            String title = norm(s.getTitle());
            String artist = norm(s.getArtist());
            String album = norm(s.getAlbum());

            if (title.contains(q)) {
                titleMatches.add(s);
            } else if (artist.contains(q)) {
                artistMatches.add(s);
            } else if (album.contains(q)) {
                albumMatches.add(s);
            }
        }

        filteredSongs.addAll(titleMatches);
        filteredSongs.addAll(artistMatches);
        filteredSongs.addAll(albumMatches);

        for (sk.ukf.wavvy.model.Album a : allAlbums) {

            String albumName = norm(a.getTitle());
            String artist = norm(a.getArtist());

            if (albumName.contains(q) || artist.contains(q)) {
                filteredAlbums.add(a);
            }
        }

        if (!filteredAlbums.isEmpty()) {
            tvAlbumsLabel.setVisibility(View.VISIBLE);
            rvAlbums.setVisibility(View.VISIBLE);
            tvSongsLabel.setVisibility(View.VISIBLE);
        } else {
            tvAlbumsLabel.setVisibility(View.GONE);
            rvAlbums.setVisibility(View.GONE);
            tvSongsLabel.setVisibility(View.GONE);
        }

        tvSectionTitle.setText("Results (" + filteredSongs.size() + ")");

        if (filteredSongs.isEmpty() && filteredAlbums.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
            rvAlbums.setVisibility(View.GONE);
            tvAlbumsLabel.setVisibility(View.GONE);
            tvSongsLabel.setVisibility(View.GONE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
        }

        adapter.setHighlightQuery(q);
        albumAdapter.setHighlightQuery(q);
        albumAdapter.notifyDataSetChanged();
        adapter.notifyDataSetChanged();
    }
    private void showTopState() {
        tvSectionTitle.setText("Top songs");
        tvSectionSubtitle.setText("Your most listened tracks");

        tvEmpty.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);

        tvSectionSubtitle.setVisibility(View.VISIBLE);
        tvAlbumsLabel.setVisibility(View.GONE);
        tvSongsLabel.setVisibility(View.GONE);
        rvAlbums.setVisibility(View.GONE);
        adapter.setHighlightQuery("");
        albumAdapter.setHighlightQuery("");

        if (!lastSearch.isEmpty()) {
            tvRecentSearchTitle.setVisibility(View.VISIBLE);
            tvRecentSearches.setText("🔍 " + lastSearch);
            tvRecentSearches.setVisibility(View.VISIBLE);
        } else {
            tvRecentSearchTitle.setVisibility(View.GONE);
            tvRecentSearches.setVisibility(View.GONE);
        }
    }
}