package sk.ukf.wavvy.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import sk.ukf.wavvy.PlaybackManager;
import sk.ukf.wavvy.R;
import sk.ukf.wavvy.model.Song;

public class AlbumSongAdapter extends RecyclerView.Adapter<AlbumSongAdapter.ViewHolder> {
    public interface OnSongClickListener {
        void onSongClick(Song song);
    }
    private final List<Song> songs;
    private final OnSongClickListener listener;
    public AlbumSongAdapter(List<Song> songs, OnSongClickListener listener) {
        this.songs = songs;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_album_song, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Song song = songs.get(position);

        int currentId = PlaybackManager.get(holder.itemView.getContext()).getCurrentAudioResId();

        int track = song.getTrackNumber();
        if (track > 0) {
            holder.tvTrackNumber.setText(String.valueOf(track));
        } else {
            holder.tvTrackNumber.setText(String.valueOf(position + 1));
        }
        holder.tvSongTitle.setText(song.getTitle());
        holder.tvSongArtist.setText(song.getArtist());
        holder.tvSongDuration.setText(formatDuration(song.getDurationMs()));

        if (song.getAudioResId() == currentId) {
            holder.tvSongTitle.setTextColor(holder.itemView.getContext().getColor(R.color.accent));
        } else {
            holder.tvSongTitle.setTextColor(holder.itemView.getContext().getColor(R.color.textPrimary));
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onSongClick(song);
        });
    }

    @Override
    public int getItemCount() { return songs.size(); }
    private String formatDuration(long ms) {
        long totalSeconds = ms / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return minutes + ":" + String.format("%02d", seconds);
    }
    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTrackNumber;
        TextView tvSongTitle;
        TextView tvSongArtist;
        TextView tvSongDuration;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            tvTrackNumber = itemView.findViewById(R.id.tvTrackNumber);
            tvSongTitle = itemView.findViewById(R.id.tvSongTitle);
            tvSongArtist = itemView.findViewById(R.id.tvSongArtist);
            tvSongDuration = itemView.findViewById(R.id.tvSongDuration);
        }
    }
}