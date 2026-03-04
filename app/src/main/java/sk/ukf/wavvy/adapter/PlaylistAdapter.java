package sk.ukf.wavvy.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
    public interface OnPlaylistLongClickListener {
        void onPlaylistLongClick(Playlist playlist);
    }
    private final List<Playlist> playlists;
    private final OnPlaylistClickListener clickListener;
    private final OnPlaylistLongClickListener longClickListener;
    public PlaylistAdapter(List<Playlist> playlists,
                           OnPlaylistClickListener clickListener,
                           OnPlaylistLongClickListener longClickListener) {
        this.playlists = playlists;
        this.clickListener = clickListener;
        this.longClickListener = longClickListener;
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

        holder.tvName.setText(p.getName());
        holder.tvCount.setText(p.getSongAudioResIds().size() + " songs");
        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) clickListener.onPlaylistClick(p);
        });
        holder.itemView.setOnLongClickListener(v -> {
            if (longClickListener != null) {
                longClickListener.onPlaylistLongClick(p);
                return true;
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return playlists.size();
    }
    static class PlaylistVH extends RecyclerView.ViewHolder {
        TextView tvName, tvCount;
        public PlaylistVH(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvPlaylistName);
            tvCount = itemView.findViewById(R.id.tvPlaylistCount);
        }
    }
}