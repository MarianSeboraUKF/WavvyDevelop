package sk.ukf.wavvy.model;

import java.util.ArrayList;

public class Album {
    private final String title;
    private final String artist;
    private final int coverResId;
    private final ArrayList<Song> songs;
    public Album(String title, String artist, int coverResId, ArrayList<Song> songs) {
        this.title = title;
        this.artist = artist;
        this.coverResId = coverResId;
        this.songs = songs;
    }
    public String getTitle() {
        return title;
    }
    public String getArtist() {
        return artist;
    }
    public int getCoverResId() {
        return coverResId;
    }
    public ArrayList<Song> getSongs() {
        return songs;
    }
}