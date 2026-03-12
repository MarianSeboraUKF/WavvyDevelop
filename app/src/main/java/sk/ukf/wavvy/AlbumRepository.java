package sk.ukf.wavvy;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Collections;
import sk.ukf.wavvy.model.Album;
import sk.ukf.wavvy.model.Song;

public class AlbumRepository {
    public static ArrayList<Album> getAlbums() {
        ArrayList<Song> songs = SongRepository.getSongs();
        LinkedHashMap<String, Album> map = new LinkedHashMap<>();

        for (Song song : songs) {
            String albumName = song.getAlbum();
            String albumArtist = song.getAlbumArtist();

            if (!map.containsKey(albumName)) {
                ArrayList<Song> albumSongs = new ArrayList<>();
                albumSongs.add(song);

                map.put(albumName, new Album(
                        albumName,
                        albumArtist,
                        song.getCoverResId(),
                        albumSongs
                ));
            } else {
                map.get(albumName).getSongs().add(song);
            }
        }
        ArrayList<Album> albums = new ArrayList<>(map.values());

        for (Album album : albums) {
            Collections.sort(album.getSongs(), (a, b) ->
                    Integer.compare(a.getTrackNumber(), b.getTrackNumber())
            );
        }
        return albums;
    }
    public static Album findByTitle(String title) {
        for (Album a : getAlbums()) {
            if (a.getTitle().equals(title)) return a;
        }
        return null;
    }
}