package sk.ukf.wavvy.model;

public class Song {
    private final String title;
    private final String mainArtist;
    private final String featArtist;
    private final String album;
    private final String albumArtist;
    private final int trackNumber;
    private final int coverResId;
    private final int audioResId;

    public Song(String title,
                String mainArtist,
                String featArtist,
                String album,
                String albumArtist,
                int trackNumber,
                int coverResId,
                int audioResId) {

        this.title = title;
        this.mainArtist = mainArtist;
        this.featArtist = featArtist;
        this.album = album;
        this.albumArtist = albumArtist;
        this.trackNumber = trackNumber;
        this.coverResId = coverResId;
        this.audioResId = audioResId;
    }

    public String getTitle() {
        return title;
    }

    public String getMainArtist() {
        return mainArtist;
    }

    public String getFeatArtist() {
        return featArtist;
    }

    public String getArtist() {
        if (featArtist == null || featArtist.isEmpty()) {
            return mainArtist;
        }
        return mainArtist + ", " + featArtist;
    }

    public String getAlbum() {
        return album;
    }

    public String getAlbumArtist() {
        return albumArtist;
    }

    public int getTrackNumber() {
        return trackNumber;
    }

    public int getCoverResId() {
        return coverResId;
    }

    public int getAudioResId() {
        return audioResId;
    }
}