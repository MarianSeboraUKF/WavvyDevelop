package sk.ukf.wavvy;

import android.content.Context;
import android.content.Intent;
import java.util.ArrayList;
import sk.ukf.wavvy.model.Song;

public class SongRepository {
    private static ArrayList<Song> cached;
    private static ArrayList<Song> localSongs = new ArrayList<>();

    public static ArrayList<Song> getSongs() {
        if (cached == null) {
            cached = new ArrayList<>();
            cached.add(new Song("BERI 3", "RAYYY P", "Vašo Patejdl, Majkyyy", "kto.som.?", "RAYYY P", "Majkyyy", 3, R.drawable.kto_som_album_cover, R.raw.beri_3));
            cached.add(new Song("NEPÝTAM SA", "RAYYY P", "Majkyyy", "kto.som.?", "RAYYY P", "Majkyyy", 10, R.drawable.kto_som_album_cover, R.raw.nepytam_sa));
            cached.add(new Song("Bouřka", "Lboy Bsc", "", "Bouřka", "Lboy Bsc", "Black Eagle Beats", 1, R.drawable.bourka_cover, R.raw.bourka));
            cached.add(new Song("Čikitas 2", "Lboy Bsc", "Freez247", "Čikitas 2", "Lboy Bsc", "Anyvibe", 1, R.drawable.cikitas_2_cover, R.raw.cikitas_2));
            cached.add(new Song("Plaza", "Lboy Bsc", "", "Plaza", "Lboy Bsc", "-", 1, R.drawable.plaza_cover, R.raw.plaza));
            cached.add(new Song("DO OČÍ", "RAYYY P", "Majkyyy", "kto.som.?", "RAYYY P", "Majkyyy", 1, R.drawable.kto_som_album_cover, R.raw.do_oci));
            cached.add(new Song("INTRO", "RAYYY P", "Majkyyy", "kto.som.?", "RAYYY P", "Majkyyy", 2, R.drawable.kto_som_album_cover, R.raw.intro));
            cached.add(new Song("Parabola", "Chawo", "Erik Tresor", "Glitch ve systému", "Chawo", "Mikaelbeatz", 8, R.drawable.glitch_ve_systemu_cover, R.raw.parabola));
            cached.add(new Song("E85", "Don Toliver", "", "OCTANE", "Don Toliver", "206derek, Aaron Paris, Dillon Brophy, Jess Jackson, Jaasu, Travis Scott", 1, R.drawable.octane_cover, R.raw.e85));
            cached.add(new Song("HALLE LUYAH FREESTYLE", "RAYYY P", "", "kto.som.?", "RAYYY P", "RAYYY P", 4, R.drawable.kto_som_album_cover, R.raw.halle_luyah_freestyle));
            cached.add(new Song("K T O . $ O M . !", "RAYYY P", "", "R A Y . $ A V E D . M E", "RAYYY P", "RAYYY P", 1, R.drawable.kto_som_ep_cover, R.raw.kto_som));
            cached.add(new Song("ROZMÝŠLAM", "RAYYY P", "Majkyyy", "kto.som.?", "RAYYY P", "Majkyyy", 5, R.drawable.kto_som_album_cover, R.raw.rozmyslam));
            cached.add(new Song("NEBOJÍM SA", "RAYYY P", "Relon", "kto.som.?", "RAYYY P", "-", 6, R.drawable.kto_som_album_cover, R.raw.nebojim_sa));
            cached.add(new Song("Tsunami", "DJ Snake", "Future, Travis Scott", "Nomad", "DJ Snake", "DJ Snake", 8, R.drawable.nomad_cover, R.raw.tsunami));
            cached.add(new Song("ROZMÝŠLAM - Majkyyy Remix DnB Version", "RAYYY P", "Majkyyy", "kto.som.?", "RAYYY P", "Majkyyy", 8, R.drawable.kto_som_album_cover, R.raw.rozmyslam_dnb_version));
            cached.add(new Song("VÝZNAM? - skit", "RAYYY P", "", "kto.som.?", "RAYYY P", "-", 7, R.drawable.kto_som_album_cover, R.raw.vyznam_skit));
            cached.add(new Song("BBL", "P T K", "", "KARAKORAM", "P T K", "Rigas Beats, Anyvibe", 16, R.drawable.karakoram_cover, R.raw.bbl));
            cached.add(new Song("TAK DOSŤ!", "RAYYY P", "", "kto.som.?", "RAYYY P", "-", 9, R.drawable.kto_som_album_cover, R.raw.tak_dost));
            cached.add(new Song("12 FREESTYLE", "RAYYY P", "", "kto.som.?", "RAYYY P", "-", 11, R.drawable.kto_som_album_cover, R.raw.freestyle_1_2));
            cached.add(new Song("Z A M E $ T N Á V A M . H O E $", "RAYYY P", "ICOиO, Relon", "R A Y . $ A V E D . M E", "RAYYY P", "RAYYY P, Relon, Majkyyy", 2, R.drawable.kto_som_ep_cover, R.raw.zamestnavam_hoes));
            cached.add(new Song("CHODÍM SPAŤ RÁNO", "RAYYY P", "", "kto.som.?", "RAYYY P", "-", 12, R.drawable.kto_som_album_cover, R.raw.chodim_spat_rano));
            cached.add(new Song("MOON", "RAYYY P", "", "kto.som.?", "RAYYY P", "-", 13, R.drawable.kto_som_album_cover, R.raw.moon));
            cached.add(new Song("DOKORÁN", "RAYYY P", "", "kto.som.?", "RAYYY P", "-", 14, R.drawable.kto_som_album_cover, R.raw.dokoran));
            cached.add(new Song("BERI 2", "RAYYY P", "", "R A Y . $ A V E D . M E", "RAYYY P", "Segway Beats, JacobD", 3, R.drawable.kto_som_ep_cover, R.raw.beri_2));
            cached.add(new Song("ALLRIGHT", "RAYYY P", "ICOиO, Patez", "kto.som.?", "RAYYY P", "-", 15, R.drawable.kto_som_album_cover, R.raw.allright));
            cached.add(new Song("OUTRO", "RAYYY P", "", "kto.som.?", "RAYYY P", "-", 16, R.drawable.kto_som_album_cover, R.raw.outro));
            cached.add(new Song("BOO - Bonus Track", "RAYYY P", "dum13o", "R A Y . $ A V E D . M E", "RAYYY P", "RAYYY P, Relon", 4, R.drawable.kto_som_ep_cover, R.raw.boo));
        }
        ArrayList<Song> result = new ArrayList<>(cached);
        result.addAll(localSongs);
        return result;
    }
    public static Song findByAudioResId(int audioResId) {
        ArrayList<Song> songs = getSongs();
        for (Song s : songs) {
            if (s.getAudioResId() == audioResId) return s;
        }
        return null;
    }
    public static void addLocalSong(Song song) {
        localSongs.add(song);
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
    public static ArrayList<Song> getLikedSongs(Context ctx) {
        ArrayList<Song> result = new ArrayList<>();
        ArrayList<String> liked = new ArrayList<>(LikedSongsRepository.getLikedSongs(ctx));
        java.util.Collections.reverse(liked);
        for (String id : liked) {
            try {
                int audioId = Integer.parseInt(id);
                Song s = findByAudioResId(audioId);
                if (s != null) {
                    result.add(s);
                }
            } catch (Exception ignored) {}
        }
        return result;
    }
    public static void loadLocalSongs(Context ctx) {
        android.content.SharedPreferences sp = ctx.getSharedPreferences("wavvy_local", Context.MODE_PRIVATE);
        String json = sp.getString("songs", null);
        if (json == null) return;

        try {
            org.json.JSONArray arr = new org.json.JSONArray(json);
            localSongs.clear();

            for (int i = 0; i < arr.length(); i++) {
                org.json.JSONObject o = arr.getJSONObject(i);

                String title = o.getString("title");
                String artist = o.getString("artist");
                String album = o.getString("album");
                String uri = o.getString("uri");
                long duration = o.getLong("duration");

                int id = uri.toString().hashCode();
                String coverUri = o.optString("coverUri", null);
                if ("null".equals(coverUri)) coverUri = null;
                String feat = o.optString("featArtist", "");
                String albumArtist = o.optString("albumArtist", artist);
                String producer = o.optString("producer", "-");
                int track = o.optInt("trackNumber", 0);

                Song s = new Song(title, artist, feat, album, albumArtist, producer, track, R.drawable.default_cover, id);

                s.setCoverUri(coverUri);
                s.setUriString(uri);
                s.setDurationMs(duration);
                localSongs.add(s);
            }
        } catch (Exception ignored) {}
    }
    public static void saveLocalSongs(Context ctx) {
        android.content.SharedPreferences sp = ctx.getSharedPreferences("wavvy_local", Context.MODE_PRIVATE);
        org.json.JSONArray arr = new org.json.JSONArray();
        try {
            for (Song s : localSongs) {
                org.json.JSONObject o = new org.json.JSONObject();
                o.put("title", s.getTitle());
                o.put("artist", s.getMainArtist());
                o.put("album", s.getAlbum());
                o.put("uri", s.getUriString());
                o.put("duration", s.getDurationMs());
                o.put("coverUri", s.getCoverUri());
                o.put("albumArtist", s.getAlbumArtist());
                o.put("featArtist", s.getFeatArtist());
                o.put("producer", s.getProducedBy());
                o.put("trackNumber", s.getTrackNumber());
                arr.put(o);
            }
        } catch (Exception ignored) {}
        sp.edit().putString("songs", arr.toString()).apply();
    }
    public static void deleteLocalSong(Context ctx, int audioResId) {
        localSongs.removeIf(s -> s.getAudioResId() == audioResId);
        saveLocalSongs(ctx);
        PlaybackManager pm = PlaybackManager.get(ctx);
        if (pm.getCurrentAudioResId() == audioResId) {
            pm.getPlayer().stop();
        }
    }
    public static void updateLocalSong(Context ctx, int audioResId, String title, String artist, String feat, String albumartist, String album, String producer, String coverUri, int trackNumber) {
        for (Song s : localSongs) {
            if (s.getAudioResId() == audioResId && s.getUriString() != null) {
                s.setTitle(title.isEmpty() ? "Unknown" : title);
                s.setMainArtist(artist.isEmpty() ? "Unknown" : artist);
                s.setFeatArtist(feat);
                s.setAlbumArtist(albumartist.isEmpty() ? s.getMainArtist() : albumartist);
                s.setAlbum(album.isEmpty() ? "Unknown" : album);
                s.setProducedBy(producer.isEmpty() ? "-" : producer);
                s.setTrackNumber(trackNumber);

                if (coverUri != null) {
                    s.setCoverUri(coverUri);
                }
                saveLocalSongs(ctx);
                ctx.sendBroadcast(new Intent("songs_updated"));
                break;
            }
        }
    }
    public static void preloadDurations(Context ctx) {
        for (Song s : getSongs()) {
            if (s.getDurationMs() > 0) continue;
            android.media.MediaMetadataRetriever mmr = new android.media.MediaMetadataRetriever();

            try {
                android.content.res.AssetFileDescriptor afd = ctx.getResources().openRawResourceFd(s.getAudioResId());
                if (afd == null) continue;

                mmr.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
                String dur = mmr.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION);
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