package sk.ukf.wavvy.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import sk.ukf.wavvy.R;
import sk.ukf.wavvy.model.Playlist;

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
    public PlaylistAdapter(List<Playlist> playlists,
                           OnPlaylistClickListener clickListener,
                           OnPlaylistMenuClickListener menuClickListener) {
        this.playlists = playlists;
        this.clickListener = clickListener;
        this.menuClickListener = menuClickListener;
    }

    @NonNull
    @Override
    public PlaylistVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_playlist, parent, false);
        return new PlaylistVH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull PlaylistVH holder, int position) {
        Playlist p = playlists.get(position);

        holder.tvName.setText(p.getName());
        holder.tvCount.setText(p.getSongAudioResIds().size() + " songs");

        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) clickListener.onPlaylistClick(p);
        });

        holder.btnMore.setOnClickListener(v -> {
            if (menuClickListener != null) menuClickListener.onPlaylistMenuClick(p, v);
        });
    }

    @Override
    public int getItemCount() {
        return playlists.size();
    }
    static class PlaylistVH extends RecyclerView.ViewHolder {
        TextView tvName, tvCount;
        ImageButton btnMore;
        public PlaylistVH(@NonNull View itemView) {
            super(itemView);

            tvName = itemView.findViewById(R.id.tvPlaylistName);
            tvCount = itemView.findViewById(R.id.tvPlaylistCount);
            btnMore = itemView.findViewById(R.id.btnMore);
        }
    }
}