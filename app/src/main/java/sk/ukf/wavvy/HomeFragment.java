package sk.ukf.wavvy;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.Calendar;
import sk.ukf.wavvy.adapter.SongAdapter;
import sk.ukf.wavvy.adapter.SmallSongAdapter;
import sk.ukf.wavvy.model.Song;
import android.content.Intent;
import sk.ukf.wavvy.adapter.AlbumAdapter;
import sk.ukf.wavvy.model.Album;

public class HomeFragment extends Fragment implements PlaybackManager.Listener {
    private SongAdapter adapter;
    private PlaybackManager pm;
    private RecyclerView rvMostPlayed;
    private RecyclerView rvRecent;
    private ArrayList<Song> mostPlayed;
    private ArrayList<Song> recentSongs;
    private SmallSongAdapter mostPlayedAdapter;
    private SmallSongAdapter recentAdapter;
    private int lastPlayingAudioId = -1;
    private View cardContinue;
    private ImageView ivContinueCover;
    private TextView tvContinueTitle;
    private TextView tvContinueArtist;
    private BroadcastReceiver songsUpdatedReceiver;
    private ArrayList<Song> allSongs;
    private AlbumAdapter albumAdapter;
    private RecyclerView rvAlbums;
    private boolean isExpanded = false;
    private RecyclerView rvSongs;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        TextView tvGreeting = view.findViewById(R.id.tvGreeting);
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);

        if (hour < 12) {
            tvGreeting.setText("Good morning ☀️");
        } else if (hour < 18) {
            tvGreeting.setText("Good afternoon 🌤️");
        } else {
            tvGreeting.setText("Good evening 🌙");
        }

        cardContinue = view.findViewById(R.id.cardContinue);
        ivContinueCover = view.findViewById(R.id.ivContinueCover);
        tvContinueTitle = view.findViewById(R.id.tvContinueTitle);
        tvContinueArtist = view.findViewById(R.id.tvContinueArtist);
        TextView btnPlayAll = view.findViewById(R.id.btnPlayAll);
        TextView btnShuffleAll = view.findViewById(R.id.btnShuffleAll);
        updateContinueCard();

        cardContinue.setOnClickListener(v -> {
            PlaybackManager pm = PlaybackManager.get(requireContext());

            if (pm.getCurrentAudioResId() == 0) return;
            if (!pm.isPlaying()) {
                pm.togglePlayPause();
            }
            PlayerLauncher.openExisting(requireContext());
        });

        pm = PlaybackManager.get(requireContext());
        rvMostPlayed = view.findViewById(R.id.rvMostPlayed);
        rvSongs = view.findViewById(R.id.rvSongs);
        rvMostPlayed.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        rvSongs.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvRecent = view.findViewById(R.id.rvRecent);
        rvRecent.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        recentSongs = SongRepository.getRecentlyPlayedSongs(requireContext());
        recentAdapter = new SmallSongAdapter(recentSongs, song -> PlayerLauncher.openQueue(requireContext(), recentSongs, song));
        rvRecent.setAdapter(recentAdapter);

        SongRepository.loadLocalSongs(requireContext());
        allSongs = SongRepository.getSongs();
        SongRepository.preloadDurations(requireContext());

        btnPlayAll.setOnClickListener(v -> {
            ArrayList<Song> currentSongs = SongRepository.getSongs();
            int[] ids = new int[currentSongs.size()];
            for (int i = 0; i < currentSongs.size(); i++) {
                ids[i] = currentSongs.get(i).getAudioResId();
            }
            pm.setShuffle(false);
            pm.playQueue(ids, 0, true);
        });

        btnShuffleAll.setOnClickListener(v -> {
            ArrayList<Song> currentSongs = SongRepository.getSongs();
            int[] ids = new int[currentSongs.size()];
            for (int i = 0; i < currentSongs.size(); i++) {
                ids[i] = currentSongs.get(i).getAudioResId();
            }
            int randomIndex = new java.util.Random().nextInt(ids.length);
            pm.setShuffle(true);
            pm.playQueue(ids, randomIndex, true);
        });
        TextView btnViewMore = view.findViewById(R.id.btnViewMore);
        rvSongs.setItemAnimator(new androidx.recyclerview.widget.DefaultItemAnimator());

        btnViewMore.setOnClickListener(v -> {
            if (!isExpanded) {
                isExpanded = true;
                adapter.updateData(new ArrayList<>(allSongs));
                rvSongs.scheduleLayoutAnimation();
                btnViewMore.setText("Show less");

            } else {
                isExpanded = false;
                adapter.updateData(getDisplayedSongs());
                rvSongs.scheduleLayoutAnimation();
                btnViewMore.setText("View more");
                rvSongs.scrollToPosition(0);
            }
        });

        rvSongs.setHasFixedSize(true);
        rvSongs.setItemViewCacheSize(20);
        rvSongs.setItemAnimator(new androidx.recyclerview.widget.DefaultItemAnimator());

        mostPlayed = SongRepository.getMostPlayedSongs(requireContext());
        mostPlayedAdapter = new SmallSongAdapter(mostPlayed, song -> PlayerLauncher.openQueue(requireContext(), mostPlayed, song));

        ArrayList<Song> allSongsFull = SongRepository.getSongs();
        ArrayList<Song> previewSongs = new ArrayList<>();

        for (int i = 0; i < Math.min(10, allSongsFull.size()); i++) {
            previewSongs.add(allSongsFull.get(i));
        }

        adapter = new SongAdapter(getDisplayedSongs(), false, false, song -> PlayerLauncher.openQueue(requireContext(), allSongs, song));
        rvSongs.setAdapter(adapter);
        rvMostPlayed.setAdapter(mostPlayedAdapter);
        rvAlbums = view.findViewById(R.id.rvAlbums);
        rvAlbums.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));

        ArrayList<Album> albums = AlbumRepository.getAlbums();
        albumAdapter = new AlbumAdapter(albums, album -> {
            Intent i = new Intent(requireContext(), AlbumDetailActivity.class);
            i.putExtra("album_title", album.getTitle());
            startActivity(i);
            requireActivity().overridePendingTransition(R.anim.slide_in_right_fast, R.anim.slide_out_left_fast);
        });
        rvAlbums.setAdapter(albumAdapter);

        for (Song s : allSongs) {
            GradientPreloader.preload(requireContext(), s.getCoverResId());
        }

        songsUpdatedReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                refreshHome();
            }
        };
        return view;
    }
    private void updateContinueCard() {
        int audioResId = NowPlayingRepository.getAudioResId(requireContext());

        if (audioResId == 0) {
            cardContinue.setVisibility(View.GONE);
            return;
        }

        Song s = SongRepository.findByAudioResId(audioResId);

        if (s == null) {
            cardContinue.setVisibility(View.GONE);
            return;
        }
        cardContinue.setVisibility(View.VISIBLE);
        if (s.getCoverUri() != null) {
            ivContinueCover.setImageURI(android.net.Uri.parse(s.getCoverUri()));
        } else {
            ivContinueCover.setImageResource(s.getCoverResId());
        }
        tvContinueTitle.setText(s.getTitle());
        tvContinueArtist.setText(s.getArtist());
    }
    private ArrayList<Song> getDisplayedSongs() {
        if (isExpanded) {
            return new ArrayList<>(allSongs);
        } else {
            ArrayList<Song> preview = new ArrayList<>();
            for (int i = 0; i < Math.min(10, allSongs.size()); i++) {
                preview.add(allSongs.get(i));
            }
            return preview;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        updateContinueCard();
        refreshHome();
        mostPlayedAdapter.updateData(SongRepository.getMostPlayedSongs(requireContext()));
        recentSongs.clear();
        recentSongs.addAll(SongRepository.getRecentlyPlayedSongs(requireContext()));
        recentAdapter.notifyDataSetChanged();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            requireContext().unregisterReceiver(songsUpdatedReceiver);
        } catch (Exception ignored) {}
    }

    @Override
    public void onStart() {
        super.onStart();
        requireContext().registerReceiver(songsUpdatedReceiver, new IntentFilter("songs_updated"), Context.RECEIVER_NOT_EXPORTED);
        if (pm != null) {
            pm.addListener(this);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        requireContext().unregisterReceiver(songsUpdatedReceiver);
        if (pm != null) {
            pm.removeListener(this);
        }
    }
    @Override
    public void onNowPlayingChanged(int audioResId, int[] queueIds, int queueIndex) {
        updateContinueCard();
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
            if (oldIndex >= 0) adapter.notifyItemChanged(oldIndex);
            if (newIndex >= 0) adapter.notifyItemChanged(newIndex);
            lastPlayingAudioId = audioResId;
        }
        recentSongs.clear();
        recentSongs.addAll(SongRepository.getRecentlyPlayedSongs(requireContext()));
        recentAdapter.notifyDataSetChanged();
        mostPlayedAdapter.updateData(SongRepository.getMostPlayedSongs(requireContext()));
    }
    private void refreshHome() {
        updateContinueCard();
        SongRepository.loadLocalSongs(requireContext());
        ArrayList<Song> updatedSongs = SongRepository.getSongs();

        allSongs = new ArrayList<>(updatedSongs);
        if (adapter != null) {
            adapter.updateData(getDisplayedSongs());
        }

        if (albumAdapter != null) {
            albumAdapter.updateData(AlbumRepository.getAlbums());
        }
        recentSongs.clear();
        recentSongs.addAll(SongRepository.getRecentlyPlayedSongs(requireContext()));
        recentAdapter.notifyDataSetChanged();
        mostPlayedAdapter.updateData(SongRepository.getMostPlayedSongs(requireContext()));
    }
    @Override
    public void onIsPlayingChanged(boolean isPlaying) {
        mostPlayedAdapter.updateData(
                SongRepository.getMostPlayedSongs(requireContext())
        );
    }
    @Override
    public void onProgress(long positionMs, long durationMs) {}
}