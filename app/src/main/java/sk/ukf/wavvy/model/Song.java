package sk.ukf.wavvy.model;

public class Song implements java.io.Serializable {
    private final String title;
    private final String mainArtist;
    private final String featArtist;
    private final String album;
    private final String albumArtist;
    private final String producedBy;
    private final int trackNumber;
    private final int coverResId;
    private final int audioResId;
    private long durationMs;

    public Song(String title, String mainArtist, String featArtist, String album, String albumArtist, String producedBy, int trackNumber, int coverResId, int audioResId) {
        this.title = title;
        this.mainArtist = mainArtist;
        this.featArtist = featArtist;
        this.album = album;
        this.albumArtist = albumArtist;
        this.producedBy = producedBy;
        this.trackNumber = trackNumber;
        this.coverResId = coverResId;
        this.audioResId = audioResId;
        this.durationMs = 0;
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
    public String getProducedBy() {
        return producedBy;
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
    public long getDurationMs() {
        return durationMs;
    }
    public void setDurationMs(long durationMs) {
        this.durationMs = durationMs;
    }
}