package sk.ukf.wavvy.adapter;

import android.view.View;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import sk.ukf.wavvy.R;
import sk.ukf.wavvy.model.Playlist;

public class PickPlaylistViewHolder extends RecyclerView.ViewHolder {
    private final TextView tvName;
    private final TextView tvCount;
    public PickPlaylistViewHolder(@NonNull View itemView) {
        super(itemView);
        tvName = itemView.findViewById(R.id.tvPlaylistName);
        tvCount = itemView.findViewById(R.id.tvPlaylistCount);
    }
    public void bind(Playlist p) {
        tvName.setText(p.getName());
        tvCount.setText(p.getSongAudioResIds().size() + " songs");
    }
}