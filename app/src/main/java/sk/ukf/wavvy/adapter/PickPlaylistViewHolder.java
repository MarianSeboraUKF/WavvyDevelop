package sk.ukf.wavvy.adapter;

import android.widget.ImageView;
import android.view.View;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import sk.ukf.wavvy.R;
import sk.ukf.wavvy.SongRepository;
import sk.ukf.wavvy.model.Playlist;
import sk.ukf.wavvy.model.Song;

public class PickPlaylistViewHolder extends RecyclerView.ViewHolder {
    private final TextView tvName;
    private final TextView tvCount;
    private final ImageView ivCover;
    public PickPlaylistViewHolder(@NonNull View itemView) {
        super(itemView);
        tvName = itemView.findViewById(R.id.tvPlaylistName);
        tvCount = itemView.findViewById(R.id.tvPlaylistCount);
        ivCover = itemView.findViewById(R.id.ivPlaylistCover);
    }
    public void bind(Playlist p) {
        tvName.setText(p.getName());
        tvCount.setText(p.getSongAudioResIds().size() + " songs");

        if (!p.getSongAudioResIds().isEmpty()) {
            int firstSongId = p.getSongAudioResIds().get(0);

            Song song = SongRepository.findByAudioResId(itemView.getContext(), firstSongId);
            if (song != null) {
                if (song.getCoverUri() != null && !song.getCoverUri().isEmpty()) {
                    ivCover.setImageURI(android.net.Uri.parse(song.getCoverUri()));
                } else {
                    ivCover.setImageResource(song.getCoverResId());
                }
            } else {
                ivCover.setImageResource(R.drawable.default_cover);
            }
        } else {
            ivCover.setImageResource(R.drawable.default_cover);
        }
    }
}