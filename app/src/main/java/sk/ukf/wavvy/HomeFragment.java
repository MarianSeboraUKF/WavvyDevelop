package sk.ukf.wavvy;

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

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

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
        RecyclerView rvSongs = view.findViewById(R.id.rvSongs);

        rvMostPlayed.setLayoutManager(
                new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        );

        rvSongs.setLayoutManager(
                new LinearLayoutManager(requireContext())
        );

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
        SongRepository.preloadDurations(requireContext());

        mostPlayed = SongRepository.getMostPlayedSongs(requireContext());

        mostPlayedAdapter = new SmallSongAdapter(
                mostPlayed,
                song -> PlayerLauncher.openQueue(requireContext(), mostPlayed, song)
        );

        adapter = new SongAdapter(
                allSongs,
                false,
                false,
                song -> PlayerLauncher.openQueue(requireContext(), allSongs, song)
        );

        rvMostPlayed.setAdapter(mostPlayedAdapter);
        rvSongs.setAdapter(adapter);

        rvSongs.setItemAnimator(null);

        RecyclerView rvAlbums = view.findViewById(R.id.rvAlbums);

        rvAlbums.setLayoutManager(
                new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        );

        ArrayList<Album> albums = AlbumRepository.getAlbums();

        AlbumAdapter albumAdapter = new AlbumAdapter(albums, album -> {
            Intent i = new Intent(requireContext(), AlbumDetailActivity.class);
            i.putExtra("album_title", album.getTitle());
            startActivity(i);
        });

        rvAlbums.setAdapter(albumAdapter);

        for (Song s : allSongs) {
            GradientPreloader.preload(requireContext(), s.getCoverResId());
        }
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
        ivContinueCover.setImageResource(s.getCoverResId());
        tvContinueTitle.setText(s.getTitle());
        tvContinueArtist.setText(s.getArtist());
    }

    @Override
    public void onResume() {
        super.onResume();

        updateContinueCard();

        mostPlayedAdapter.updateData(
                SongRepository.getMostPlayedSongs(requireContext())
        );

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
        recentSongs.addAll(
                SongRepository.getRecentlyPlayedSongs(requireContext())
        );
        recentAdapter.notifyDataSetChanged();

        mostPlayedAdapter.updateData(
                SongRepository.getMostPlayedSongs(requireContext())
        );
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