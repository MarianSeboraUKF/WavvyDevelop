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
import sk.ukf.wavvy.model.Playlist;

public class SystemPlaylistAdapter extends RecyclerView.Adapter<SystemPlaylistAdapter.VH> {
    public interface OnClick { void onClick(Playlist p); }
    private final List<Playlist> list;
    private final OnClick listener;
    public SystemPlaylistAdapter(List<Playlist> list, OnClick listener) {
        this.list = list;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_playlist_large, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int i) {
        Playlist p = list.get(i);
        h.tvName.setText(p.getName());
        int count = p.getSongAudioResIds().size();

        if (count == 0) {
            h.tvCount.setText("Empty");
        } else if (count == 1) {
            h.tvCount.setText("1 song");
        } else {
            if (p.getId().equals("liked")) {
                h.tvCount.setText(count + " liked");
            } else {
                h.tvCount.setText(count + " songs");
            }
        }

        if (p.getId().equals("liked")) {
            h.ivCover.setBackgroundResource(R.drawable.bg_liked_gradient);
            h.ivIcon.setImageResource(R.drawable.ic_liked);
        } else {
            h.ivCover.setBackgroundResource(R.drawable.bg_local_gradient);
            h.ivIcon.setImageResource(R.drawable.icon_local);
        }
        h.itemView.setOnClickListener(v -> listener.onClick(p));
    }

    @Override
    public int getItemCount() {
        return list.size();
    }
    static class VH extends RecyclerView.ViewHolder {
        ImageView ivCover, ivIcon;
        TextView tvName, tvCount;
        public VH(@NonNull View v) {
            super(v);
            ivCover = v.findViewById(R.id.ivCover);
            ivIcon = v.findViewById(R.id.ivIcon);
            tvName = v.findViewById(R.id.tvName);
            tvCount = v.findViewById(R.id.tvCount);
        }
    }
}