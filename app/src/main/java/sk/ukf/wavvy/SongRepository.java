package sk.ukf.wavvy;

import android.content.Context;
import java.util.ArrayList;
import sk.ukf.wavvy.model.Song;

public class SongRepository {
    private static ArrayList<Song> cached;

    public static ArrayList<Song> getSongs() {
        if (cached != null) return new ArrayList<>(cached);

        cached = new ArrayList<>();

        cached.add(new Song("BERI 3", "RAYYY P", "Vašo Patejdl, Majkyyy", "kto.som.?", "RAYYY P", "Majkyyy", 3, R.drawable.kto_som_cover, R.raw.beri_3));
        cached.add(new Song("NEPÝTAM SA", "RAYYY P", "Majkyyy", "kto.som.?", "RAYYY P", "Majkyyy", 10, R.drawable.kto_som_cover, R.raw.nepytam_sa));
        cached.add(new Song("Bouřka", "Lboy Bsc", "", "Bouřka", "Lboy Bsc", "Black Eagle Beats", 1, R.drawable.bourka_cover, R.raw.bourka));
        cached.add(new Song("DO OČÍ", "RAYYY P", "Majkyyy", "kto.som.?", "RAYYY P", "Majkyyy",1, R.drawable.kto_som_cover, R.raw.do_oci));
        cached.add(new Song("DIG ON(A)", "Frayer Flexking", "Separ", "DIG ON(A)", "Frayer Flexking", "Maiky Beatz, prodbyslope",1, R.drawable.dig_ona_cover, R.raw.dig_ona));
        cached.add(new Song("Streets", "guapanova", "Luca Brassi10x", "Deluzia", "guapanova", "Tristan. Hoodrich",4, R.drawable.deluzia_cover, R.raw.streets));
        cached.add(new Song("INTRO", "RAYYY P", "Majkyyy", "kto.som.?", "RAYYY P", "Majkyyy", 2, R.drawable.kto_som_cover, R.raw.intro));
        cached.add(new Song("Parabola", "Chawo", "Erik Tresor", "Glitch ve systému", "Chawo", "Mikaelbeatz", 8, R.drawable.glitch_ve_systemu_cover, R.raw.parabola));
        cached.add(new Song("E85", "Don Toliver", "", "OCTANE", "Don Toliver", "206derek, Aaron Paris, Dillon Brophy, Jess Jackson, Jaasu, Travis Scott", 1, R.drawable.octane_cover, R.raw.e85));
        cached.add(new Song("HALLE LUYAH FREESTYLE", "RAYYY P", "", "kto.som.?", "RAYYY P", "RAYYY P", 4, R.drawable.kto_som_cover, R.raw.halle_luyah_freestyle));
        cached.add(new Song("23", "Raphael", "", "23", "Raphael", "-",1, R.drawable.raphael_23_cover, R.raw.raphael_23));
        cached.add(new Song("ROZMÝŠLAM", "RAYYY P", "Majkyyy", "kto.som.?", "RAYYY P", "Majkyyy", 5, R.drawable.kto_som_cover, R.raw.rozmyslam));
        cached.add(new Song("NEBOJÍM SA", "RAYYY P", "Relon", "kto.som.?", "RAYYY P", "-", 6, R.drawable.kto_som_cover, R.raw.nebojim_sa));
        cached.add(new Song("Tsunami", "DJ Snake", "Future, Travis Scott", "Nomad", "DJ Snake", "DJ Snake", 8, R.drawable.nomad_cover, R.raw.tsunami));
        cached.add(new Song("ROZMÝŠLAM - Majkyyy Remix DnB Version", "RAYYY P", "Majkyyy", "kto.som.?", "RAYYY P", "Majkyyy", 8, R.drawable.kto_som_cover, R.raw.rozmyslam_dnb_version));
        cached.add(new Song("BODY ON BODY", "Danisen", "", "ANJELI A DEMONI", "Danisen", "Slavyyy",1, R.drawable.anjeli_a_demoni_cover, R.raw.body_on_body));
        cached.add(new Song("VÝZNAM? - skit", "RAYYY P", "", "kto.som.?", "RAYYY P","-",7, R.drawable.kto_som_cover, R.raw.vyznam_skit));
        cached.add(new Song("BBL", "P T K", "", "KARAKORAM", "P T K", "Rigas Beats, Anyvibe",16, R.drawable.karakoram_cover, R.raw.bbl));
        cached.add(new Song("TAK DOSŤ!", "RAYYY P", "", "kto.som.?", "RAYYY P", "-",9, R.drawable.kto_som_cover, R.raw.tak_dost));
        cached.add(new Song("12 FREESTYLE", "RAYYY P", "", "kto.som.?", "RAYYY P", "-",11, R.drawable.kto_som_cover, R.raw.freestyle_1_2));
        cached.add(new Song("Zub za zub", "Separ", "Raphael", "Zub za zub", "Separ", "Die For",1, R.drawable.zub_za_zub_cover, R.raw.zub_za_zub));
        cached.add(new Song("CHODÍM SPAŤ RÁNO", "RAYYY P", "", "kto.som.?", "RAYYY P", "-",12, R.drawable.kto_som_cover, R.raw.chodim_spat_rano));
        cached.add(new Song("Tempo za 2", "Sara Rikas", "Raphael", "Ja, Sára", "Sara Rikas", "Lou Xtwo, Decky",2, R.drawable.ja_sara_cover, R.raw.tempo_za_2));
        cached.add(new Song("MOON", "RAYYY P", "", "kto.som.?", "RAYYY P", "-",13, R.drawable.kto_som_cover, R.raw.moon));
        cached.add(new Song("DOKORÁN", "RAYYY P", "", "kto.som.?", "RAYYY P", "-",14, R.drawable.kto_som_cover, R.raw.dokoran));
        cached.add(new Song("ALLRIGHT", "RAYYY P", "ICOиO, Patez", "kto.som.?", "RAYYY P", "-",15, R.drawable.kto_som_cover, R.raw.allright));
        cached.add(new Song("OUTRO", "RAYYY P", "", "kto.som.?", "RAYYY P", "-",16, R.drawable.kto_som_cover, R.raw.outro));
        cached.add(new Song("Nebylo souzený", "Robin Zoot", "Chawo", "Majitel", "Robin Zoot", "Spack DS",4, R.drawable.majitel_cover, R.raw.nebylo_souzeny));
        cached.add(new Song("Good Energy", "Beachcrimes", "", "Good Energy", "Beachcrimes", "Ryan McMahon", 1, R.drawable.good_energy_cover, R.raw.good_energy));

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
        ArrayList<Song> allSongs = new ArrayList<>(getSongs());
        int limit = 10;

        allSongs.sort((a, b) -> {
            int countA = PlayCountRepository.getCount(ctx, a.getAudioResId());
            int countB = PlayCountRepository.getCount(ctx, b.getAudioResId());

            return Integer.compare(countB, countA);
        });

        if (allSongs.size() > limit) {
            return new ArrayList<>(allSongs.subList(0, limit));
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
    public static void preloadDurations(Context ctx) {
        for (Song s : getSongs()) {
            if (s.getDurationMs() > 0) continue;
            android.media.MediaMetadataRetriever mmr = new android.media.MediaMetadataRetriever();

            try {
                android.content.res.AssetFileDescriptor afd =
                        ctx.getResources().openRawResourceFd(s.getAudioResId());

                if (afd == null) continue;

                mmr.setDataSource(
                        afd.getFileDescriptor(),
                        afd.getStartOffset(),
                        afd.getLength()
                );

                String dur = mmr.extractMetadata(
                        android.media.MediaMetadataRetriever.METADATA_KEY_DURATION
                );

                afd.close();

                if (dur != null) {
                    s.setDurationMs(Long.parseLong(dur));
                }

            } catch (Exception ignored) {}
            finally {
                try {
                    mmr.release();
                } catch (Exception ignored) {}
            }
        }
    }
}