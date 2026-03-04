package sk.ukf.wavvy;

import android.content.Context;
import java.util.ArrayList;
import sk.ukf.wavvy.model.Song;

public class SongRepository {
    private static ArrayList<Song> cached;
    public static ArrayList<Song> getSongs() {
        if (cached != null) return new ArrayList<>(cached);

        cached = new ArrayList<>();
        cached.add(new Song("BERI 3", "RAYYY P, Vašo Patejdl, Majkyyy", "kto.som.?", R.drawable.kto_som_cover, R.raw.beri_3));
        cached.add(new Song("NEPÝTAM SA", "RAYYY P, Majkyyy", "kto.som.?", R.drawable.kto_som_cover, R.raw.nepytam_sa));
        cached.add(new Song("DO OČÍ", "RAYYY P, Majkyyy", "kto.som.?", R.drawable.kto_som_cover, R.raw.do_oci));
        cached.add(new Song("DIG ON(A)", "Frayer Flexking, Separ", "DIG ON(A)", R.drawable.dig_ona_cover, R.raw.dig_ona));
        cached.add(new Song("Streets", "guapanova, Luca Brassi10x", "Deluzia", R.drawable.deluzia_cover, R.raw.streets));
        cached.add(new Song("Parabola", "Slatt Chawo, Erik Tresor", "Glitch ve systému", R.drawable.glitch_ve_systemu_cover, R.raw.parabola));
        cached.add(new Song("E85", "Don Toliver", "OCTANE", R.drawable.octane_cover, R.raw.e85));
        cached.add(new Song("23", "Raphael", "23", R.drawable.raphael_23_cover, R.raw.raphael_23));

        return cached;
    }
    public static Song findByAudioResId(int audioResId) {
        ArrayList<Song> songs = getSongs();
        for (Song s : songs) {
            if (s.getAudioResId() == audioResId) return s;
        }
        return null;
    }
    public static ArrayList<Song> getMostPlayedSongs(Context ctx) {
        ArrayList<Song> allSongs = getSongs();

        allSongs.sort((a, b) -> {
            int countA = PlayCountRepository.getCount(ctx, a.getAudioResId());
            int countB = PlayCountRepository.getCount(ctx, b.getAudioResId());

            return Integer.compare(countB, countA);
        });

        if (allSongs.size() > 5) {
            return new ArrayList<>(allSongs.subList(0, 5));
        }
        return allSongs;
    }
    public static ArrayList<Song> getRecentlyPlayedSongs(Context ctx) {
        ArrayList<Integer> ids = RecentlyPlayedRepository.get(ctx);

        ArrayList<Song> songs = new ArrayList<>();
        for (Integer id : ids) {
            Song s = findByAudioResId(id);

            if (s != null) {
                songs.add(s);
            }
        }
        return songs;
    }
}