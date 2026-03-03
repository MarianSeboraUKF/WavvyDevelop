package sk.ukf.wavvy.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import sk.ukf.wavvy.PlaybackManager;
import sk.ukf.wavvy.R;
import sk.ukf.wavvy.model.Song;

public class SongAdapter extends RecyclerView.Adapter<SongAdapter.SongViewHolder> {
    public interface OnSongClickListener {
        void onSongClick(Song song);
    }
    public interface OnSongLongClickListener {
        void onSongLongClick(Song song);
    }
    private final List<Song> songs;
    private final OnSongClickListener clickListener;
    private final OnSongLongClickListener longClickListener;
    public SongAdapter(List<Song> songs,
                       OnSongClickListener clickListener,
                       OnSongLongClickListener longClickListener) {
        this.songs = songs;
        this.clickListener = clickListener;
        this.longClickListener = longClickListener;
    }

    @NonNull
    @Override
    public SongViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_song, parent, false);
        return new SongViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SongViewHolder holder, int position) {
        Song song = songs.get(position);

        holder.tvTitle.setText(song.getTitle());
        holder.tvArtist.setText(song.getArtist());
        holder.ivCover.setImageResource(song.getCoverResId());

        String album = song.getAlbum();
        if (album == null || album.trim().isEmpty()) {
            holder.tvAlbum.setVisibility(View.GONE);
        } else {
            holder.tvAlbum.setText(album);
            holder.tvAlbum.setVisibility(View.VISIBLE);
        }

        int currentId = PlaybackManager
                .get(holder.itemView.getContext())
                .getCurrentAudioResId();

        holder.itemView.setBackground(
                ContextCompat.getDrawable(holder.itemView.getContext(), R.drawable.bg_card_selector)
        );

        if (song.getAudioResId() == currentId) {
            holder.tvTitle.setTextColor(
                    ContextCompat.getColor(holder.itemView.getContext(), R.color.accent)
            );

            holder.itemView.setForeground(
                    ContextCompat.getDrawable(holder.itemView.getContext(), R.drawable.bg_item_playing_overlay)
            );

            holder.viewNowPlayingDot.setVisibility(View.VISIBLE);
            holder.viewNowPlayingStripe.setVisibility(View.VISIBLE);
        } else {
            holder.tvTitle.setTextColor(
                    ContextCompat.getColor(holder.itemView.getContext(), R.color.textPrimary)
            );

            holder.itemView.setForeground(null);
            holder.viewNowPlayingDot.setVisibility(View.GONE);
            holder.viewNowPlayingStripe.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) clickListener.onSongClick(song);
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (longClickListener != null) {
                longClickListener.onSongLongClick(song);
                return true;
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return songs.size();
    }
    static class SongViewHolder extends RecyclerView.ViewHolder {

        ImageView ivCover;
        TextView tvTitle;
        TextView tvArtist;
        TextView tvAlbum;
        View viewNowPlayingDot;
        View viewNowPlayingStripe;
        public SongViewHolder(@NonNull View itemView) {
            super(itemView);

            ivCover = itemView.findViewById(R.id.ivItemCover);
            tvTitle = itemView.findViewById(R.id.tvItemTitle);
            tvArtist = itemView.findViewById(R.id.tvItemArtist);
            tvAlbum = itemView.findViewById(R.id.tvItemAlbum);
            viewNowPlayingDot = itemView.findViewById(R.id.viewNowPlayingDot);
            viewNowPlayingStripe = itemView.findViewById(R.id.viewNowPlayingStripe);
        }
    }
}