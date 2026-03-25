package sk.ukf.wavvy.adapter;

import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.PopupWindow;
import android.graphics.drawable.ColorDrawable;
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
import android.widget.LinearLayout;
import sk.ukf.wavvy.LikedSongsRepository;

public class SongAdapter extends RecyclerView.Adapter<SongAdapter.SongViewHolder> {
    public interface OnSongClickListener {
        void onSongClick(Song song);
    }
    private final List<Song> songs;
    private final OnSongClickListener clickListener;
    private String highlightQuery = "";
    private boolean isQueue;
    public SongAdapter(List<Song> songs, boolean isQueue, OnSongClickListener clickListener) {
        this.songs = songs;
        this.clickListener = clickListener;
        this.isQueue = isQueue;
    }

    @NonNull
    @Override
    public SongViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layout = isQueue ? R.layout.item_song_queue : R.layout.item_song;

        View view = LayoutInflater.from(parent.getContext())
                .inflate(layout, parent, false);

        return new SongViewHolder(view);
    }
    public void setHighlightQuery(String query) {
        this.highlightQuery = query != null ? query.toLowerCase() : "";
    }

    @Override
    public void onBindViewHolder(@NonNull SongViewHolder holder, int position) {
        Song song = songs.get(position);

        holder.tvTitle.setText(
                applyHighlight(
                        song.getTitle(),
                        highlightQuery,
                        holder
                )
        );

        holder.tvArtist.setText(
                applyHighlight(
                        song.getArtist(),
                        highlightQuery,
                        holder
                )
        );

        String album = song.getAlbum();

        if (album == null || album.trim().isEmpty()) {
            holder.tvAlbum.setVisibility(View.GONE);
        } else {
            holder.tvAlbum.setText(
                    applyHighlight(
                            album,
                            highlightQuery,
                            holder
                    )
            );
            holder.tvAlbum.setVisibility(View.VISIBLE);
        }
        holder.ivCover.setImageResource(song.getCoverResId());

        int currentId = PlaybackManager
                .get(holder.itemView.getContext())
                .getCurrentAudioResId();

        if (song.getAudioResId() == currentId) {
            holder.itemView.setForeground(
                    ContextCompat.getDrawable(
                            holder.itemView.getContext(),
                            R.drawable.bg_item_playing_overlay
                    )
            );
            holder.viewNowPlayingStripe.setVisibility(View.VISIBLE);

        } else {
            holder.itemView.setForeground(null);
            holder.viewNowPlayingStripe.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            v.animate()
                    .scaleX(0.96f)
                    .scaleY(0.96f)
                    .setDuration(80)
                    .withEndAction(() ->
                            v.animate()
                                    .scaleX(1f)
                                    .scaleY(1f)
                                    .setDuration(120)
                                    .start()
                    )
                    .start();

            if (clickListener != null) {
                clickListener.onSongClick(song);
            }
        });

        if (!isQueue && holder.btnSongMenu != null) {
            holder.btnSongMenu.setOnClickListener(v -> {
                android.content.Context ctx = holder.itemView.getContext();

                View popupView = LayoutInflater.from(ctx)
                        .inflate(R.layout.dialog_song_menu, null);

                PopupWindow popupWindow = new PopupWindow(
                        popupView,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        true
                );
                popupWindow.setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
                popupWindow.setElevation(16f);

                popupView.findViewById(R.id.actionPlayNext).setOnClickListener(v1 -> {
                    PlaybackManager.get(ctx).insertNext(song.getAudioResId());

                    android.widget.Toast.makeText(
                            ctx,
                            "Added to queue",
                            android.widget.Toast.LENGTH_SHORT
                    ).show();
                    popupWindow.dismiss();
                });

                popupView.findViewById(R.id.actionGoAlbum).setOnClickListener(v1 -> {
                    android.content.Intent intent =
                            new android.content.Intent(
                                    ctx,
                                    AlbumDetailActivity.class
                            );
                    intent.putExtra("album_title", song.getAlbum());
                    ctx.startActivity(intent);
                    popupWindow.dismiss();
                });

                LinearLayout favAction = popupView.findViewById(R.id.actionAddFavorite);
                ImageView favIcon = (ImageView) favAction.getChildAt(0);
                TextView favText = (TextView) favAction.getChildAt(1);

                String songId = String.valueOf(song.getAudioResId());
                boolean liked = LikedSongsRepository.isLiked(ctx, songId);

                if (liked) {
                    favIcon.setImageResource(R.drawable.ic_liked);
                    favIcon.setColorFilter(ContextCompat.getColor(ctx, R.color.accent));
                    favText.setText("Remove from favorites");
                } else {
                    favIcon.setImageResource(R.drawable.ic_like);
                    favIcon.setColorFilter(ContextCompat.getColor(ctx, R.color.textPrimary));
                    favText.setText("Add to favorites");
                }

                favAction.setOnClickListener(v1 -> {
                    LikedSongsRepository.toggleLike(ctx, songId);

                    boolean newLiked = LikedSongsRepository.isLiked(ctx, songId);

                    if (newLiked) {
                        android.widget.Toast.makeText(
                                ctx,
                                "Added to favorites",
                                android.widget.Toast.LENGTH_SHORT
                        ).show();
                    } else {
                        android.widget.Toast.makeText(
                                ctx,
                                "Removed from favorites",
                                android.widget.Toast.LENGTH_SHORT
                        ).show();
                    }
                    popupWindow.dismiss();
                });

                popupView.findViewById(R.id.actionAddPlaylist).setOnClickListener(v1 -> {
                    popupWindow.dismiss();

                    java.util.ArrayList<Playlist> playlists =
                            PlaylistRepository.getPlaylists(ctx);

                    if (playlists.isEmpty()) {
                        android.widget.Toast.makeText(
                                ctx,
                                "No playlists yet",
                                android.widget.Toast.LENGTH_SHORT
                        ).show();
                        return;
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
                });

                popupView.findViewById(R.id.actionInfo).setOnClickListener(v1 -> {
                    popupWindow.dismiss();

                    android.content.Context context = holder.itemView.getContext();

                    View dialogView = LayoutInflater.from(context)
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
                    tvArtist.setText(song.getArtist());
                    tvAlbum.setText(song.getAlbum());
                    tvProducer.setText(song.getProducedBy());
                    tvLength.setText(formatDuration(song.getDurationMs()));

                    AlertDialog dialog = new AlertDialog.Builder(context)
                            .setView(dialogView)
                            .create();

                    if (dialog.getWindow() != null) {
                        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
                    }
                    btnClose.setOnClickListener(v2 -> dialog.dismiss());
                    dialog.show();
                });
                popupWindow.showAsDropDown(holder.btnSongMenu, 0, 0, Gravity.END);
            });
        }

        if (holder.dragHandle != null) {
            holder.dragHandle.setVisibility(isQueue ? View.VISIBLE : View.GONE);
        }
    }
    private String formatDuration(long ms) {
        long totalSeconds = ms / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return minutes + ":" + String.format("%02d", seconds);
    }
    private CharSequence applyHighlight(String text, String query, SongViewHolder holder) {
        if (text == null) return "";

        if (query == null || query.isEmpty()) {
            return text;
        }

        String lowerText = text.toLowerCase();
        String lowerQuery = query.toLowerCase();

        int start = lowerText.indexOf(lowerQuery);

        if (start < 0) {
            return text;
        }
        SpannableString spannable = new SpannableString(text);

        spannable.setSpan(
                new ForegroundColorSpan(
                        ContextCompat.getColor(
                                holder.itemView.getContext(),
                                R.color.accent
                        )
                ),
                start,
                start + query.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        );
        return spannable;
    }

    @Override
    public int getItemCount() {
        return songs.size();
    }
    static class SongViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvArtist, tvAlbum;
        ImageView ivCover;
        View viewNowPlayingStripe;
        ImageButton btnSongMenu;
        ImageView dragHandle;
        public SongViewHolder(@NonNull View itemView) {
            super(itemView);

            tvTitle = itemView.findViewById(R.id.tvItemTitle);
            tvArtist = itemView.findViewById(R.id.tvItemArtist);
            tvAlbum = itemView.findViewById(R.id.tvItemAlbum);
            ivCover = itemView.findViewById(R.id.ivItemCover);
            btnSongMenu = itemView.findViewById(R.id.btnSongMenu);
            viewNowPlayingStripe = itemView.findViewById(R.id.viewNowPlayingStripe);
            dragHandle = itemView.findViewById(R.id.dragHandle);
        }
    }
}