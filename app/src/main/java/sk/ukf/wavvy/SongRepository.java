package sk.ukf.wavvy;

import android.content.Context;
import java.util.ArrayList;
import sk.ukf.wavvy.model.Song;

public class SongRepository {
    private static ArrayList<Song> cached;

    public static ArrayList<Song> getSongs() {
        if (cached != null) return new ArrayList<>(cached);

        cached = new ArrayList<>();

        cached.add(new Song("BERI 3", "RAYYY P", "Vašo Patejdl, Majkyyy", "kto.som.?", "RAYYY P", 3, R.drawable.kto_som_cover, R.raw.beri_3));
        cached.add(new Song("NEPÝTAM SA", "RAYYY P", "Majkyyy", "kto.som.?", "RAYYY P", 10, R.drawable.kto_som_cover, R.raw.nepytam_sa));
        cached.add(new Song("DO OČÍ", "RAYYY P", "Majkyyy", "kto.som.?", "RAYYY P", 1, R.drawable.kto_som_cover, R.raw.do_oci));
        cached.add(new Song("DIG ON(A)", "Frayer Flexking", "Separ", "DIG ON(A)", "Frayer Flexking", 1, R.drawable.dig_ona_cover, R.raw.dig_ona));
        cached.add(new Song("Streets", "guapanova", "Luca Brassi10x", "Deluzia", "guapanova", 4, R.drawable.deluzia_cover, R.raw.streets));
        cached.add(new Song("INTRO", "RAYYY P", "Majkyyy", "kto.som.?", "RAYYY P", 2, R.drawable.kto_som_cover, R.raw.intro));
        cached.add(new Song("Parabola", "Chawo", "Erik Tresor", "Glitch ve systému", "Chawo", 8, R.drawable.glitch_ve_systemu_cover, R.raw.parabola));
        cached.add(new Song("E85", "Don Toliver", "", "OCTANE", "Don Toliver", 1, R.drawable.octane_cover, R.raw.e85));
        cached.add(new Song("HALLE LUYAH FREESTYLE", "RAYYY P", "", "kto.som.?", "RAYYY P", 4, R.drawable.kto_som_cover, R.raw.halle_luyah_freestyle));
        cached.add(new Song("23", "Raphael", "", "23", "Raphael", 1, R.drawable.raphael_23_cover, R.raw.raphael_23));
        cached.add(new Song("ROZMÝŠLAM", "RAYYY P", "Majkyyy", "kto.som.?", "RAYYY P", 5, R.drawable.kto_som_cover, R.raw.rozmyslam));
        cached.add(new Song("NEBOJÍM SA", "RAYYY P", "Relon", "kto.som.?", "RAYYY P", 6, R.drawable.kto_som_cover, R.raw.nebojim_sa));
        cached.add(new Song("ROZMÝŠLAM - Majkyyy Remix DnB Version", "RAYYY P", "Majkyyy", "kto.som.?", "RAYYY P", 8, R.drawable.kto_som_cover, R.raw.rozmyslam_dnb_version));
        cached.add(new Song("BODY ON BODY", "Danisen", "", "ANJELI A DEMONI", "Danisen", 1, R.drawable.anjeli_a_demoni_cover, R.raw.body_on_body));
        cached.add(new Song("VÝZNAM? - skit", "RAYYY P", "", "kto.som.?", "RAYYY P", 7, R.drawable.kto_som_cover, R.raw.vyznam_skit));
        cached.add(new Song("BBL", "P T K", "", "KARAKORAM", "P T K", 16, R.drawable.karakoram_cover, R.raw.bbl));
        cached.add(new Song("TAK DOSŤ!", "RAYYY P", "", "kto.som.?", "RAYYY P", 9, R.drawable.kto_som_cover, R.raw.tak_dost));
        cached.add(new Song("12 FREESTYLE", "RAYYY P", "", "kto.som.?", "RAYYY P", 11, R.drawable.kto_som_cover, R.raw.freestyle_1_2));
        cached.add(new Song("CHODÍM SPAŤ RÁNO", "RAYYY P", "", "kto.som.?", "RAYYY P", 12, R.drawable.kto_som_cover, R.raw.chodim_spat_rano));
        cached.add(new Song("Tempo za 2", "Sara Rikas", "Raphael", "Ja, Sára", "Sara Rikas", 2, R.drawable.ja_sara_cover, R.raw.tempo_za_2));
        cached.add(new Song("MOON", "RAYYY P", "", "kto.som.?", "RAYYY P", 13, R.drawable.kto_som_cover, R.raw.moon));
        cached.add(new Song("DOKORÁN", "RAYYY P", "", "kto.som.?", "RAYYY P", 14, R.drawable.kto_som_cover, R.raw.dokoran));
        cached.add(new Song("ALLRIGHT", "RAYYY P", "ICOиO, Patez", "kto.som.?", "RAYYY P", 15, R.drawable.kto_som_cover, R.raw.allright));
        cached.add(new Song("OUTRO", "RAYYY P", "", "kto.som.?", "RAYYY P", 16, R.drawable.kto_som_cover, R.raw.outro));
        cached.add(new Song("Nebylo souzený", "Robin Zoot", "Chawo", "Majitel", "Robin Zoot", 4, R.drawable.majitel_cover, R.raw.nebylo_souzeny));

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