package sk.ukf.wavvy.adapter;

import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
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
    private String highlightQuery = "";
    private boolean showTrackNumbers = false;
    public SongAdapter(List<Song> songs,
                       OnSongClickListener clickListener,
                       OnSongLongClickListener longClickListener) {
        this.songs = songs;
        this.clickListener = clickListener;
        this.longClickListener = longClickListener;
    }
    public void setHighlightQuery(String query) {
        this.highlightQuery = query != null ? query.toLowerCase() : "";
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
        String title;

        if (showTrackNumbers) {
            title = song.getTrackNumber() + "  " + song.getTitle();
        } else {
            title = song.getTitle();
        }

        if (!highlightQuery.isEmpty()) {
            String lowerTitle = title.toLowerCase();
            int start = lowerTitle.indexOf(highlightQuery);

            if (start >= 0) {
                SpannableString spannable = new SpannableString(title);

                spannable.setSpan(
                        new ForegroundColorSpan(
                                ContextCompat.getColor(holder.itemView.getContext(), R.color.accent)
                        ),
                        start,
                        start + highlightQuery.length(),
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                );
                holder.tvTitle.setText(spannable);
            } else {
                holder.tvTitle.setText(title);
            }
        } else {
            holder.tvTitle.setText(title);
        }
        String artist = song.getArtist();

        if (!highlightQuery.isEmpty()) {
            String lowerArtist = artist.toLowerCase();
            int start = lowerArtist.indexOf(highlightQuery);

            if (start >= 0) {
                SpannableString spannable = new SpannableString(artist);

                spannable.setSpan(
                        new ForegroundColorSpan(
                                ContextCompat.getColor(holder.itemView.getContext(), R.color.accent)
                        ),
                        start,
                        start + highlightQuery.length(),
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                );
                holder.tvArtist.setText(spannable);
            } else {
                holder.tvArtist.setText(artist);
            }
        } else {
            holder.tvArtist.setText(artist);
        }
        holder.ivCover.setImageResource(song.getCoverResId());

        String album = song.getAlbum();

        if (album == null || album.trim().isEmpty()) {
            holder.tvAlbum.setVisibility(View.GONE);
        } else {
            if (!highlightQuery.isEmpty()) {
                String lowerAlbum = album.toLowerCase();
                int start = lowerAlbum.indexOf(highlightQuery);

                if (start >= 0) {
                    SpannableString spannable = new SpannableString(album);

                    spannable.setSpan(
                            new ForegroundColorSpan(
                                    ContextCompat.getColor(holder.itemView.getContext(), R.color.accent)
                            ),
                            start,
                            start + highlightQuery.length(),
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    );
                    holder.tvAlbum.setText(spannable);
                } else {
                    holder.tvAlbum.setText(album);
                }
            } else {
                holder.tvAlbum.setText(album);
            }
            holder.tvAlbum.setVisibility(View.VISIBLE);
        }

        int currentId = PlaybackManager
                .get(holder.itemView.getContext())
                .getCurrentAudioResId();

        holder.itemView.setBackground(
                ContextCompat.getDrawable(
                        holder.itemView.getContext(),
                        R.drawable.bg_card_selector
                )
        );

        if (song.getAudioResId() == currentId) {

            holder.tvTitle.setTextColor(
                    ContextCompat.getColor(
                            holder.itemView.getContext(),
                            R.color.accent
                    )
            );

            holder.itemView.setForeground(
                    ContextCompat.getDrawable(
                            holder.itemView.getContext(),
                            R.drawable.bg_item_playing_overlay
                    )
            );
            holder.viewNowPlayingStripe.setVisibility(View.VISIBLE);

        } else {

            holder.tvTitle.setTextColor(
                    ContextCompat.getColor(
                            holder.itemView.getContext(),
                            R.color.textPrimary
                    )
            );
            holder.itemView.setForeground(null);
            holder.viewNowPlayingStripe.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onSongClick(song);
            }
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
        View viewNowPlayingStripe;
        public SongViewHolder(@NonNull View itemView) {
            super(itemView);

            ivCover = itemView.findViewById(R.id.ivItemCover);
            tvTitle = itemView.findViewById(R.id.tvItemTitle);
            tvArtist = itemView.findViewById(R.id.tvItemArtist);
            tvAlbum = itemView.findViewById(R.id.tvItemAlbum);
            viewNowPlayingStripe = itemView.findViewById(R.id.viewNowPlayingStripe);
        }
    }
}