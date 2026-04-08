package sk.ukf.wavvy.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
import sk.ukf.wavvy.R;
import sk.ukf.wavvy.model.Song;

public class SongHorizontalAdapter extends RecyclerView.Adapter<SongHorizontalAdapter.PageVH> {
    private List<List<Song>> pages;
    private final OnSongClick listener;
    public SongHorizontalAdapter(List<List<Song>> pages, OnSongClick listener) {
        this.pages = pages;
        this.listener = listener;
    }

    @NonNull
    @Override
    public PageVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_song_page, parent, false);
        return new PageVH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull PageVH holder, int position) {
        List<Song> pageSongs = pages.get(position);
        holder.rv.setAdapter(null);
        holder.rv.setLayoutManager(new LinearLayoutManager(holder.itemView.getContext()));

        if (holder.rv.getItemDecorationCount() == 0) {
            holder.rv.addItemDecoration(new RecyclerView.ItemDecoration() {
                @Override
                public void getItemOffsets(@NonNull android.graphics.Rect outRect, @NonNull View view, RecyclerView parent, RecyclerView.State state) {
                    outRect.bottom = 20;
                    if (parent.getChildAdapterPosition(view) == 0) {
                        outRect.top = 8;
                    }
                }
            });
        }

        ArrayList<Song> filtered = new ArrayList<>();
        for (Song s : pageSongs) {
            if (s != null) filtered.add(s);
        }

        SongAdapter adapter = new SongAdapter(filtered, false, false, listener::onClick);
        holder.rv.setAdapter(adapter);
    }
    public void updateData(List<List<Song>> newPages) {
        this.pages = newPages;
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return pages.size();
    }
    public interface OnSongClick {
        void onClick(Song song);
    }
    public static class PageVH extends RecyclerView.ViewHolder {
        RecyclerView rv;
        public PageVH(@NonNull View itemView) {
            super(itemView);
            rv = itemView.findViewById(R.id.rvPageSongs);
        }
    }
}