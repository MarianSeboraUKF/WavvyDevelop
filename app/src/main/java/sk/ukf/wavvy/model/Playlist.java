package sk.ukf.wavvy.model;

import java.util.ArrayList;

public class Playlist {
    private String id;
    private String name;
    private boolean isSystem;
    private ArrayList<Integer> songAudioResIds;

    public Playlist(String id, String name) {
        this(id, name, false);
    }

    public Playlist(String id, String name, boolean isSystem) {
        this.id = id;
        this.name = name;
        this.isSystem = isSystem;
        this.songAudioResIds = new ArrayList<>();
    }

    public String getId() { return id; }

    public String getName() { return name; }
    public boolean isSystem() { return isSystem;}
    public ArrayList<Integer> getSongAudioResIds() {
        if (songAudioResIds == null) songAudioResIds = new ArrayList<>();
        return songAudioResIds;
    }
    public void addSong(int audioResId) {
        if (!getSongAudioResIds().contains(audioResId)) {
            getSongAudioResIds().add(audioResId);
        }
    }
}