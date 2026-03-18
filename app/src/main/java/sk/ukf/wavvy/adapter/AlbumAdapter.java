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
import sk.ukf.wavvy.model.Album;

public class AlbumAdapter extends RecyclerView.Adapter<AlbumAdapter.ViewHolder> {
    public interface OnAlbumClickListener {
        void onAlbumClick(Album album);
    }
    private final List<Album> albums;
    private final OnAlbumClickListener listener;
    private String highlightQuery = "";
    public AlbumAdapter(List<Album> albums, OnAlbumClickListener listener) {
        this.albums = albums;
        this.listener = listener;
    }
    public void setHighlightQuery(String query) {
        this.highlightQuery = query == null ? "" : query.toLowerCase();
    }
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_album, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Album album = albums.get(position);

        holder.ivCover.setImageResource(album.getCoverResId());
        String title = album.getTitle();

        if (!highlightQuery.isEmpty()) {
            String lower = title.toLowerCase();
            int start = lower.indexOf(highlightQuery);

            if (start >= 0) {
                android.text.SpannableString spannable =
                        new android.text.SpannableString(title);

                spannable.setSpan(
                        new android.text.style.ForegroundColorSpan(
                                holder.itemView.getContext().getColor(sk.ukf.wavvy.R.color.accent)
                        ),
                        start,
                        start + highlightQuery.length(),
                        android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                );
                holder.tvTitle.setText(spannable);

            } else {
                holder.tvTitle.setText(title);
            }
        } else {
            holder.tvTitle.setText(title);
        }
        holder.tvArtist.setText(album.getArtist());
        holder.itemView.setOnClickListener(v -> listener.onAlbumClick(album));
    }

    @Override
    public int getItemCount() {
        return albums.size();
    }
    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivCover;
        TextView tvTitle;
        TextView tvArtist;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            ivCover = itemView.findViewById(R.id.ivAlbumCover);
            tvTitle = itemView.findViewById(R.id.tvAlbumTitle);
            tvArtist = itemView.findViewById(R.id.tvAlbumArtist);
        }
    }
}