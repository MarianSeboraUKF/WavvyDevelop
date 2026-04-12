package sk.ukf.wavvy.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import sk.ukf.wavvy.R;
import sk.ukf.wavvy.SongRepository;
import sk.ukf.wavvy.model.Playlist;
import sk.ukf.wavvy.model.Song;

public class PlaylistAdapter extends RecyclerView.Adapter<PlaylistAdapter.PlaylistVH> {
    public interface OnPlaylistClickListener {
        void onPlaylistClick(Playlist playlist);
    }
    public interface OnPlaylistMenuClickListener {
        void onPlaylistMenuClick(Playlist playlist, View anchor);
    }
    private final List<Playlist> playlists;
    private final OnPlaylistClickListener clickListener;
    private final OnPlaylistMenuClickListener menuClickListener;
    public PlaylistAdapter(List<Playlist> playlists, OnPlaylistClickListener clickListener, OnPlaylistMenuClickListener menuClickListener) {
        this.playlists = playlists;
        this.clickListener = clickListener;
        this.menuClickListener = menuClickListener;
    }

    @NonNull
    @Override
    public PlaylistVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_playlist, parent, false);
        return new PlaylistVH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull PlaylistVH holder, int position) {
        Playlist p = playlists.get(position);
        holder.ivOverlayIcon.setVisibility(View.GONE);
        holder.tvName.setText(p.getName());

        int count = p.getSongAudioResIds().size();
        if (count == 0) {
            holder.tvCount.setText("Empty");
        } else if (count == 1) {
            holder.tvCount.setText("1 song");
        } else {
            holder.tvCount.setText(count + " songs");
        }

        if (!p.getSongAudioResIds().isEmpty()) {
            Song firstSong = SongRepository.findByAudioResId(holder.itemView.getContext(), p.getSongAudioResIds().get(0));
            if (firstSong != null) {
                if (firstSong.getCoverUri() != null && !firstSong.getCoverUri().isEmpty()) {
                    holder.ivCover.setImageURI(android.net.Uri.parse(firstSong.getCoverUri()));
                } else {
                    holder.ivCover.setImageResource(firstSong.getCoverResId());
                }
            } else {
                holder.ivCover.setImageResource(R.drawable.default_cover);
            }
        } else {
            holder.ivCover.setImageResource(R.drawable.default_cover);
        }

        if (p.isSystem()) {
            holder.btnMore.setVisibility(View.GONE);
        } else {
            holder.btnMore.setVisibility(View.VISIBLE);
        }

        if (p.getId().equals("liked")) {
            holder.ivCover.setImageResource(0);
            holder.ivCover.setBackgroundResource(R.drawable.background_liked_gradient);
            holder.ivOverlayIcon.setVisibility(View.VISIBLE);
            holder.ivOverlayIcon.setImageResource(R.drawable.icon_liked);
        }

        else if (p.getId().equals("local")) {
            holder.ivCover.setImageResource(0);
            holder.ivCover.setBackgroundResource(R.drawable.background_local_gradient);
            holder.ivOverlayIcon.setVisibility(View.VISIBLE);
            holder.ivOverlayIcon.setImageResource(R.drawable.icon_local);
        }

        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) clickListener.onPlaylistClick(p);
        });

        holder.btnMore.setOnClickListener(v -> {
            if (menuClickListener != null) menuClickListener.onPlaylistMenuClick(p, v);
        });
        holder.setIsRecyclable(false);
    }
    public void updateData(List<Playlist> newData) {
        this.playlists.clear();
        this.playlists.addAll(newData);
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return playlists.size();
    }
    static class PlaylistVH extends RecyclerView.ViewHolder {
        TextView tvName, tvCount;
        ImageButton btnMore;
        ImageView ivCover;
        ImageView ivOverlayIcon;
        public PlaylistVH(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvPlaylistName);
            tvCount = itemView.findViewById(R.id.tvPlaylistCount);
            btnMore = itemView.findViewById(R.id.btnMore);
            ivCover = itemView.findViewById(R.id.ivPlaylistCover);
            ivOverlayIcon = itemView.findViewById(R.id.ivOverlayIcon);
        }
    }
}