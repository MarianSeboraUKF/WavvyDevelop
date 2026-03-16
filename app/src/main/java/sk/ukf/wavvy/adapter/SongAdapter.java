package sk.ukf.wavvy.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import sk.ukf.wavvy.AlbumDetailActivity;
import sk.ukf.wavvy.PlaybackManager;
import sk.ukf.wavvy.PlaylistRepository;
import sk.ukf.wavvy.R;
import sk.ukf.wavvy.model.Playlist;
import sk.ukf.wavvy.model.Song;

public class SongAdapter extends RecyclerView.Adapter<SongAdapter.SongViewHolder> {
    public interface OnSongClickListener {
        void onSongClick(Song song);
    }
    private final List<Song> songs;
    private final OnSongClickListener clickListener;
    private String highlightQuery = "";
    public SongAdapter(List<Song> songs,
                       OnSongClickListener clickListener,
                       Object ignored) {
        this.songs = songs;
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public SongViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_song, parent, false);

        return new SongViewHolder(view);
    }
    public void setHighlightQuery(String query) {
        this.highlightQuery = query != null ? query.toLowerCase() : "";
    }

    @Override
    public void onBindViewHolder(@NonNull SongViewHolder holder, int position) {
        Song song = songs.get(position);

        holder.tvTitle.setText(song.getTitle());
        holder.tvArtist.setText(song.getArtist());
        holder.tvAlbum.setText(song.getAlbum());
        holder.ivCover.setImageResource(song.getCoverResId());

        int currentId = PlaybackManager
                .get(holder.itemView.getContext())
                .getCurrentAudioResId();

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

        holder.btnSongMenu.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(
                    holder.itemView.getContext(),
                    holder.btnSongMenu,
                    0,
                    0,
                    R.style.PopupMenuStyle
            );
            popup.inflate(R.menu.song_item_menu);

            try {
                java.lang.reflect.Field[] fields = popup.getClass().getDeclaredFields();
                for (java.lang.reflect.Field field : fields) {
                    if ("mPopup".equals(field.getName())) {
                        field.setAccessible(true);
                        Object menuPopupHelper = field.get(popup);
                        Class<?> classPopupHelper = Class.forName(menuPopupHelper.getClass().getName());
                        java.lang.reflect.Method setForceIcons =
                                classPopupHelper.getMethod("setForceShowIcon", boolean.class);
                        setForceIcons.invoke(menuPopupHelper, true);
                        break;
                    }
                }
            } catch (Exception ignored) {}

            popup.setOnMenuItemClickListener(item -> {
                int id = item.getItemId();

                if (id == R.id.action_play_next) {
                    PlaybackManager.get(holder.itemView.getContext())
                            .insertNext(song.getAudioResId());

                    android.widget.Toast.makeText(
                            holder.itemView.getContext(),
                            "Added next",
                            android.widget.Toast.LENGTH_SHORT
                    ).show();
                    return true;
                }

                if (id == R.id.action_go_to_album) {
                    android.content.Intent intent =
                            new android.content.Intent(
                                    holder.itemView.getContext(),
                                    AlbumDetailActivity.class
                            );
                    intent.putExtra("album_title", song.getAlbum());
                    holder.itemView.getContext().startActivity(intent);

                    return true;
                }

                if (id == R.id.action_add_to_playlist) {
                    android.content.Context ctx = holder.itemView.getContext();

                    java.util.ArrayList<Playlist> playlists =
                            PlaylistRepository.getPlaylists(ctx);

                    if (playlists.isEmpty()) {
                        android.widget.Toast.makeText(
                                ctx,
                                "No playlists yet",
                                android.widget.Toast.LENGTH_SHORT
                        ).show();
                        return true;
                    }

                    android.view.View dialogView = LayoutInflater.from(ctx)
                            .inflate(R.layout.dialog_pick_playlist, null);

                    androidx.recyclerview.widget.RecyclerView rv =
                            dialogView.findViewById(R.id.rvPickPlaylists);

                    Button btnCancel =
                            dialogView.findViewById(R.id.btnCancel);

                    AlertDialog dialog = new AlertDialog.Builder(ctx)
                            .setView(dialogView)
                            .create();

                    dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

                    rv.setLayoutManager(
                            new androidx.recyclerview.widget.LinearLayoutManager(ctx)
                    );

                    PickPlaylistAdapter pickAdapter = new PickPlaylistAdapter(
                            playlists,
                            playlist -> {
                                PlaylistRepository.addSongToPlaylist(
                                        ctx,
                                        playlist.getId(),
                                        song.getAudioResId()
                                );
                                android.widget.Toast.makeText(
                                        ctx,
                                        "Added to " + playlist.getName(),
                                        android.widget.Toast.LENGTH_SHORT
                                ).show();
                                dialog.dismiss();
                            }
                    );

                    rv.setAdapter(pickAdapter);
                    btnCancel.setOnClickListener(v2 -> dialog.dismiss());
                    dialog.show();

                    return true;
                }
                if (id == R.id.action_info) {
                    android.content.Context ctx = holder.itemView.getContext();

                    View dialogView = LayoutInflater.from(ctx)
                            .inflate(R.layout.dialog_song_info, null);

                    ImageView ivCover = dialogView.findViewById(R.id.ivSongInfoCover);
                    TextView tvTitle = dialogView.findViewById(R.id.tvSongInfoTitle);
                    TextView tvArtist = dialogView.findViewById(R.id.tvSongInfoArtist);
                    TextView tvAlbum = dialogView.findViewById(R.id.tvSongInfoAlbum);
                    TextView tvProducer = dialogView.findViewById(R.id.tvSongInfoProducer);
                    TextView tvLength = dialogView.findViewById(R.id.tvSongInfoLength);
                    Button btnClose = dialogView.findViewById(R.id.btnCloseSongInfo);

                    ivCover.setImageResource(song.getCoverResId());
                    tvTitle.setText(song.getTitle());
                    tvArtist.setText("Artist: " + song.getArtist());
                    tvAlbum.setText("Album: " + song.getAlbum());
                    tvProducer.setText("Produced by: " + song.getProducedBy());
                    tvLength.setText("Length: " + formatDuration(song.getDurationMs()));

                    AlertDialog dialog = new AlertDialog.Builder(ctx)
                            .setView(dialogView)
                            .create();

                    if (dialog.getWindow() != null) {
                        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
                    }
                    btnClose.setOnClickListener(v2 -> dialog.dismiss());
                    dialog.show();

                    return true;
                }
                return false;
            });
            popup.show();
        });
    }
    private String formatDuration(long ms) {
        long totalSeconds = ms / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return minutes + ":" + String.format("%02d", seconds);
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
        ImageButton btnSongMenu;
        public SongViewHolder(@NonNull View itemView) {
            super(itemView);

            ivCover = itemView.findViewById(R.id.ivItemCover);
            tvTitle = itemView.findViewById(R.id.tvItemTitle);
            tvArtist = itemView.findViewById(R.id.tvItemArtist);
            tvAlbum = itemView.findViewById(R.id.tvItemAlbum);
            viewNowPlayingStripe = itemView.findViewById(R.id.viewNowPlayingStripe);
            btnSongMenu = itemView.findViewById(R.id.btnSongMenu);
        }
    }
}