package sk.ukf.wavvy.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import sk.ukf.wavvy.R;
import sk.ukf.wavvy.model.Playlist;

public class PickPlaylistAdapter extends RecyclerView.Adapter<PickPlaylistViewHolder> {
    public interface OnPickListener {
        void onPick(Playlist playlist);
    }
    private final List<Playlist> playlists;
    private final OnPickListener listener;
    public PickPlaylistAdapter(List<Playlist> playlists, OnPickListener listener) {
        this.playlists = playlists;
        this.listener = listener;
    }

    @NonNull
    @Override
    public PickPlaylistViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_playlist, parent, false);
        return new PickPlaylistViewHolder(v);
    }
    @Override
    public void onBindViewHolder(@NonNull PickPlaylistViewHolder holder, int position) {
        Playlist p = playlists.get(position);

        holder.bind(p);
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onPick(p);
        });
    }

    @Override
    public int getItemCount() { return playlists.size(); }
}