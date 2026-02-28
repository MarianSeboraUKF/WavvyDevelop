package sk.ukf.wavvy;

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

        return cached;
    }
    public static Song findByAudioResId(int audioResId) {
        ArrayList<Song> songs = getSongs();
        for (Song s : songs) {
            if (s.getAudioResId() == audioResId) return s;
        }
        return null;
    }
}