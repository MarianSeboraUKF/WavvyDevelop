package sk.ukf.wavvy.model;

public class Song implements java.io.Serializable {
    private String title;
    private String mainArtist;
    private String featArtist;
    private String album;
    private String albumArtist;
    private String producedBy;
    private int trackNumber;
    private final int coverResId;
    private String coverUri;
    private final int audioResId;
    private long durationMs;
    private String uriString;
    private String downloadUrl;

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
        this.uriString = null;
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

    public void setTitle(String title) { this.title = title; }
    public void setMainArtist(String mainArtist) { this.mainArtist = mainArtist; }
    public void setFeatArtist(String featArtist) { this.featArtist = featArtist; }
    public void setAlbumArtist(String albumArtist) { this.albumArtist = albumArtist; }

    public void setAlbum(String album) { this.album = album; }
    public void setProducedBy(String producedBy) { this.producedBy = producedBy; }
    public void setTrackNumber(int trackNumber) { this.trackNumber = trackNumber; }

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
    public String getUriString() { return uriString; }
    public void setUriString(String uriString) {
        this.uriString = uriString;
    }

    public String getCoverUri() { return coverUri; }
    public void setCoverUri(String coverUri) { this.coverUri = coverUri; }
    public String getDownloadUrl() { return downloadUrl; }
    public void setDownloadUrl(String downloadUrl) { this.downloadUrl = downloadUrl; }
}