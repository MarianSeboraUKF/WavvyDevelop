package sk.ukf.wavvy.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import sk.ukf.wavvy.R;
import sk.ukf.wavvy.SongRepository;
import sk.ukf.wavvy.model.Song;

public class SmallSongAdapter extends RecyclerView.Adapter<SmallSongAdapter.ViewHolder> {
    public interface OnSongClickListener {
        void onSongClick(Song song);
    }
    private final List<Song> songs;
    private final OnSongClickListener listener;
    public SmallSongAdapter(List<Song> songs, OnSongClickListener listener) {
        this.songs = songs;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_song_small, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Song song = songs.get(position);
        Song fresh = SongRepository.findByAudioResId(holder.itemView.getContext(), song.getAudioResId());

        final Song finalSong = (fresh != null) ? fresh : song;
        if (finalSong.getCoverUri() != null) {
            holder.ivCover.setImageURI(android.net.Uri.parse(finalSong.getCoverUri()));
        } else {
            holder.ivCover.setImageResource(finalSong.getCoverResId());
        }
        holder.tvTitle.setText(finalSong.getTitle());

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onSongClick(finalSong);
        });
    }

    @Override
    public int getItemCount() {
        return songs.size();
    }
    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivCover;
        TextView tvTitle;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivCover = itemView.findViewById(R.id.ivCover);
            tvTitle = itemView.findViewById(R.id.tvTitle);
        }
    }
    public void updateData(List<Song> newData) {
        this.songs.clear();
        this.songs.addAll(newData);
        notifyDataSetChanged();
    }
}