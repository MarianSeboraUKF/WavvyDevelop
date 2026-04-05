package sk.ukf.wavvy.adapter;

import android.content.Intent;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
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
import sk.ukf.wavvy.PlaylistDetailActivity;
import sk.ukf.wavvy.PlaylistRepository;
import sk.ukf.wavvy.R;
import sk.ukf.wavvy.SongEditingHolder;
import sk.ukf.wavvy.SongRepository;
import sk.ukf.wavvy.WavvyDialogs;
import sk.ukf.wavvy.model.Playlist;
import sk.ukf.wavvy.model.Song;
import android.widget.LinearLayout;
import sk.ukf.wavvy.LikedSongsRepository;

public class SongAdapter extends RecyclerView.Adapter<SongAdapter.SongViewHolder> {
    public interface OnSongClickListener { void onSongClick(Song song);}
    private final List<Song> songs;
    private final OnSongClickListener clickListener;
    private String highlightQuery = "";
    private boolean isQueue;
    private boolean isSystemPlaylist;
    public SongAdapter(List<Song> songs, boolean isQueue, boolean isSystemPlaylist, OnSongClickListener clickListener) {
        this.songs = songs;
        this.clickListener = clickListener;
        this.isQueue = isQueue;
        this.isSystemPlaylist = isSystemPlaylist;
    }

    @NonNull
    @Override
    public SongViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layout = isQueue ? R.layout.item_song_queue : R.layout.item_song;
        View view = LayoutInflater.from(parent.getContext()).inflate(layout, parent, false);
        return new SongViewHolder(view);
    }
    public void setHighlightQuery(String query) {
        this.highlightQuery = query != null ? query.toLowerCase() : "";
    }

    @Override
    public void onBindViewHolder(@NonNull SongViewHolder holder, int position) {
        Song song = songs.get(position);
        holder.tvTitle.setText(applyHighlight(song.getTitle(), highlightQuery, holder));
        holder.tvArtist.setText(applyHighlight(song.getArtist(), highlightQuery, holder));

        String album = song.getAlbum();
        if (album == null || album.trim().isEmpty()) {
            holder.tvAlbum.setVisibility(View.GONE);
        } else {
            holder.tvAlbum.setText(applyHighlight(album, highlightQuery, holder));
            holder.tvAlbum.setVisibility(View.VISIBLE);
        }
        if (song.getCoverUri() != null) {
            holder.ivCover.setImageURI(android.net.Uri.parse(song.getCoverUri()));
        } else {
            holder.ivCover.setImageResource(song.getCoverResId());
        }

        int currentId = PlaybackManager.get(holder.itemView.getContext()).getCurrentAudioResId();
        boolean isPlaying = song.getAudioResId() == currentId;

        if (isPlaying) {
            holder.viewNowPlayingStripe.setVisibility(View.VISIBLE);
            holder.itemView.setForeground(ContextCompat.getDrawable(holder.itemView.getContext(), R.drawable.bg_item_playing_overlay));
            holder.itemView.animate().cancel();
            holder.itemView.setAlpha(0.85f);
            holder.itemView.animate().alpha(1f).setDuration(260).setInterpolator(new android.view.animation.DecelerateInterpolator()).start();
        } else {
            holder.viewNowPlayingStripe.setVisibility(View.GONE);
            holder.itemView.setForeground(null);
            holder.itemView.animate().cancel();
            holder.itemView.setAlpha(1f);
        }

        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onSongClick(song);
            }
        });

        if (!isQueue && holder.btnSongMenu != null) {
            holder.btnSongMenu.setOnClickListener(v -> {
                android.content.Context ctx = holder.itemView.getContext();
                View popupView = LayoutInflater.from(ctx).inflate(R.layout.dialog_song_menu, null);

                PopupWindow popupWindow = new PopupWindow(popupView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);
                popupWindow.setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
                popupWindow.setElevation(16f);
                popupView.findViewById(R.id.actionPlayNext).setOnClickListener(v1 -> {
                    PlaybackManager.get(ctx).insertNext(song.getAudioResId());
                    android.widget.Toast.makeText(ctx, "Added to queue", android.widget.Toast.LENGTH_SHORT).show();
                    popupWindow.dismiss();
                });

                popupView.findViewById(R.id.actionGoAlbum).setOnClickListener(v1 -> {
                    android.content.Intent intent = new android.content.Intent(ctx, AlbumDetailActivity.class);
                    intent.putExtra("album_title", song.getAlbum());
                    ctx.startActivity(intent);
                    popupWindow.dismiss();
                });

                LinearLayout favAction = popupView.findViewById(R.id.actionAddFavorite);
                LinearLayout removeFromPlaylist = popupView.findViewById(R.id.actionRemoveFromPlaylist);
                LinearLayout actionDelete = popupView.findViewById(R.id.actionDeleteSong);
                LinearLayout actionEdit = popupView.findViewById(R.id.actionEditSong);
                if (song.getUriString() != null) {
                    actionEdit.setVisibility(View.VISIBLE);
                } else {
                    actionEdit.setVisibility(View.GONE);
                }
                ImageView favIcon = (ImageView) favAction.getChildAt(0);
                TextView favText = (TextView) favAction.getChildAt(1);
                String songId = String.valueOf(song.getAudioResId());
                boolean liked = LikedSongsRepository.isLiked(ctx, songId);

                if (song.getUriString() != null) {
                    actionDelete.setVisibility(View.VISIBLE);
                } else {
                    actionDelete.setVisibility(View.GONE);
                }

                if (liked) {
                    favIcon.setImageResource(R.drawable.ic_liked);
                    favIcon.setColorFilter(ContextCompat.getColor(ctx, R.color.accent));
                    favText.setText("Remove from favorites");
                } else {
                    favIcon.setImageResource(R.drawable.ic_like);
                    favIcon.setColorFilter(ContextCompat.getColor(ctx, R.color.textPrimary));
                    favText.setText("Add to favorites");
                }

                actionEdit.setOnClickListener(v1 -> {
                    popupWindow.dismiss();
                    android.content.Context context = holder.itemView.getContext();
                    if (!(context instanceof android.app.Activity)) return;
                    android.app.Activity activity = (android.app.Activity) context;

                    View dialogView = LayoutInflater.from(activity).inflate(R.layout.dialog_edit_song, null);

                    android.widget.EditText etTitle = dialogView.findViewById(R.id.etTitle);
                    android.widget.EditText etArtist = dialogView.findViewById(R.id.etArtist);
                    android.widget.EditText etFeatArtist = dialogView.findViewById(R.id.etFeatArtist);
                    android.widget.EditText etAlbumArtist = dialogView.findViewById(R.id.etAlbumArtist);
                    android.widget.EditText etAlbum = dialogView.findViewById(R.id.etAlbum);
                    android.widget.EditText etTrack = dialogView.findViewById(R.id.etTrack);
                    android.widget.EditText etProducer = dialogView.findViewById(R.id.etProducer);
                    android.widget.ImageView ivCover = dialogView.findViewById(R.id.ivCoverEdit);
                    android.widget.Button btnSave = dialogView.findViewById(R.id.btnSave);

                    etTitle.setText(song.getTitle());
                    etArtist.setText(song.getMainArtist());
                    etFeatArtist.setText(song.getFeatArtist());
                    etAlbumArtist.setText(song.getAlbumArtist());
                    etAlbum.setText(song.getAlbum());
                    etTrack.setText(String.valueOf(song.getTrackNumber()));
                    etProducer.setText(song.getProducedBy());

                    if (song.getCoverUri() != null) {
                        ivCover.setImageURI(android.net.Uri.parse(song.getCoverUri()));
                    } else {
                        ivCover.setImageResource(song.getCoverResId());
                    }

                    final String[] selectedCover = {song.getCoverUri()};

                    ivCover.setOnClickListener(v2 -> {
                        android.content.Intent intent = new android.content.Intent(Intent.ACTION_OPEN_DOCUMENT);
                        intent.setType("image/*");
                        activity.startActivityForResult(intent, 9999);
                        SongEditingHolder.callback = uri -> {
                            selectedCover[0] = uri;
                            ivCover.setImageURI(android.net.Uri.parse(uri));
                        };
                    });
                    AlertDialog dialog = new AlertDialog.Builder(activity).setView(dialogView).create();

                    if (dialog.getWindow() != null) {
                        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
                    }

                    final int[] trackHolder = {0};

                    try {
                        trackHolder[0] = Integer.parseInt(etTrack.getText().toString());
                    } catch (Exception ignored) {}

                    btnSave.setOnClickListener(v2 -> {
                        SongRepository.updateLocalSong(
                                activity,
                                song.getAudioResId(),
                                etTitle.getText().toString().trim(),
                                etArtist.getText().toString().trim(),
                                etFeatArtist.getText().toString().trim(),
                                etAlbumArtist.getText().toString().trim(),
                                etAlbum.getText().toString().trim(),
                                etProducer.getText().toString().trim(),
                                selectedCover[0],
                                trackHolder[0]
                        );
                        android.widget.Toast.makeText(activity, "Song updated", android.widget.Toast.LENGTH_SHORT).show();
                        if (song.getAudioResId() == PlaybackManager.get(activity).getCurrentAudioResId()) {
                            PlaybackManager.get(activity).refreshCurrentSong();
                        }
                        activity.sendBroadcast(new Intent("songs_updated"));
                        dialog.dismiss();
                    });
                    TextView btnCancel = dialogView.findViewById(R.id.btnCancel);
                    btnCancel.setOnClickListener(vCancel -> dialog.dismiss());
                    dialog.show();
                });

                actionDelete.setOnClickListener(v1 -> {
                    android.content.Context context = holder.itemView.getContext();
                    View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_delete_song, null);
                    TextView tvMsg = dialogView.findViewById(R.id.tvMsg);
                    TextView btnDelete = dialogView.findViewById(R.id.btnDelete);
                    TextView btnCancel = dialogView.findViewById(R.id.btnCancel);
                    tvMsg.setText("Do you really want to delete „" + song.getTitle() + "“?");
                    android.app.Dialog dialog = WavvyDialogs.showCenteredCardDialog(context, (android.app.Activity) context, dialogView);

                    btnDelete.setOnClickListener(v2 -> {
                        SongRepository.deleteLocalSong(context, song.getAudioResId());

                        int pos = holder.getBindingAdapterPosition();
                        if (pos != RecyclerView.NO_POSITION) {
                            songs.remove(pos);
                            notifyItemRemoved(pos);
                            if (context instanceof PlaylistDetailActivity) {
                                ((PlaylistDetailActivity) context).updateMeta();
                                ((PlaylistDetailActivity) context).updateCover();
                            }
                        }
                        android.widget.Toast.makeText(context, "Deleted", android.widget.Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    });
                    btnCancel.setOnClickListener(v2 -> dialog.dismiss());
                    popupWindow.dismiss();
                });

                favAction.setOnClickListener(v1 -> {
                    LikedSongsRepository.toggleLike(ctx, songId);
                    boolean newLiked = LikedSongsRepository.isLiked(ctx, songId);

                    if (newLiked) {
                        android.widget.Toast.makeText(ctx, "Added to favorites", android.widget.Toast.LENGTH_SHORT).show();
                    } else {
                        android.widget.Toast.makeText(ctx, "Removed from favorites", android.widget.Toast.LENGTH_SHORT).show();
                    }

                    int pos = holder.getBindingAdapterPosition();
                    if (pos != RecyclerView.NO_POSITION) {
                        if (isSystemPlaylist && !newLiked) {
                            songs.remove(pos);
                            notifyItemRemoved(pos);
                            if (holder.itemView.getContext() instanceof PlaylistDetailActivity) {
                                ((PlaylistDetailActivity) holder.itemView.getContext()).updateMeta();
                            }
                        } else {
                            notifyItemChanged(pos);
                        }
                    }
                    popupWindow.dismiss();
                });

                if (ctx instanceof PlaylistDetailActivity) {
                    PlaylistDetailActivity act = (PlaylistDetailActivity) ctx;
                    String playlistId = act.getIntent().getStringExtra(PlaylistDetailActivity.EXTRA_PLAYLIST_ID);

                    if ("liked".equals(playlistId)) {
                        removeFromPlaylist.setVisibility(View.GONE);
                    } else if ("local".equals(playlistId)) {
                        removeFromPlaylist.setVisibility(View.GONE);
                    } else {
                        removeFromPlaylist.setVisibility(View.VISIBLE);
                        removeFromPlaylist.setOnClickListener(v1 -> {
                            PlaylistRepository.removeSong(ctx, playlistId, song.getAudioResId());

                            int pos = holder.getBindingAdapterPosition();
                            if (pos != RecyclerView.NO_POSITION) {
                                songs.remove(pos);
                                notifyItemRemoved(pos);
                                if (ctx instanceof PlaylistDetailActivity) {
                                    ((PlaylistDetailActivity) ctx).updateMeta();
                                    ((PlaylistDetailActivity) ctx).updateCover();
                                }
                            }
                            android.widget.Toast.makeText(ctx, "Removed from playlist", android.widget.Toast.LENGTH_SHORT).show();
                            popupWindow.dismiss();
                        });
                    }
                } else {
                    removeFromPlaylist.setVisibility(View.GONE);
                }

                popupView.findViewById(R.id.actionAddPlaylist).setOnClickListener(v1 -> {
                    popupWindow.dismiss();
                    java.util.ArrayList<Playlist> playlists = PlaylistRepository.getPlaylists(ctx);

                    if (playlists.isEmpty()) {
                        android.widget.Toast.makeText(ctx, "No playlists yet", android.widget.Toast.LENGTH_SHORT).show();
                        return;
                    }
                    android.view.View dialogView = LayoutInflater.from(ctx).inflate(R.layout.dialog_pick_playlist, null);
                    androidx.recyclerview.widget.RecyclerView rv = dialogView.findViewById(R.id.rvPickPlaylists);
                    Button btnCancel = dialogView.findViewById(R.id.btnCancel);
                    AlertDialog dialog = new AlertDialog.Builder(ctx).setView(dialogView).create();
                    dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
                    rv.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(ctx));

                    PickPlaylistAdapter pickAdapter = new PickPlaylistAdapter(
                            playlists,
                            playlist -> {
                                PlaylistRepository.addSongToPlaylist(ctx, playlist.getId(), song.getAudioResId());
                                android.widget.Toast.makeText(ctx, "Added to " + playlist.getName(), android.widget.Toast.LENGTH_SHORT).show();
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
                    View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_song_info, null);

                    ImageView ivCover = dialogView.findViewById(R.id.ivSongInfoCover);
                    TextView tvTitle = dialogView.findViewById(R.id.tvSongInfoTitle);
                    TextView tvArtist = dialogView.findViewById(R.id.tvSongInfoArtist);
                    TextView tvAlbum = dialogView.findViewById(R.id.tvSongInfoAlbum);
                    TextView tvProducer = dialogView.findViewById(R.id.tvSongInfoProducer);
                    TextView tvLength = dialogView.findViewById(R.id.tvSongInfoLength);
                    Button btnClose = dialogView.findViewById(R.id.btnCloseSongInfo);

                    if (song.getCoverUri() != null) {
                        ivCover.setImageURI(android.net.Uri.parse(song.getCoverUri()));
                    } else {
                        ivCover.setImageResource(song.getCoverResId());
                    }
                    tvTitle.setText(song.getTitle());
                    tvArtist.setText(song.getArtist());
                    tvAlbum.setText(song.getAlbum());
                    tvProducer.setText(song.getProducedBy());
                    tvLength.setText(formatDuration(song.getDurationMs()));
                    AlertDialog dialog = new AlertDialog.Builder(context).setView(dialogView).create();

                    if (dialog.getWindow() != null) {
                        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
                    }
                    btnClose.setOnClickListener(v2 -> dialog.dismiss());
                    dialog.show();
                });

                View anchor = holder.btnSongMenu;
                popupView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);

                int popupHeight = popupView.getMeasuredHeight();
                int popupWidth = popupView.getMeasuredWidth();
                int[] location = new int[2];
                anchor.getLocationOnScreen(location);

                int anchorX = location[0];
                int anchorY = location[1];
                int screenHeight = anchor.getResources().getDisplayMetrics().heightPixels;
                int screenWidth = anchor.getResources().getDisplayMetrics().widthPixels;

                int spaceBelow = screenHeight - (anchorY + anchor.getHeight());
                int spaceAbove = anchorY;

                int margin = (int) (4 * anchor.getResources().getDisplayMetrics().density); // optional
                int xOffset = screenWidth - (anchorX + popupWidth) - margin;
                int yOffset = (int) (4 * anchor.getResources().getDisplayMetrics().density);

                popupWindow.setAnimationStyle(android.R.style.Animation_Dialog);
                popupWindow.setClippingEnabled(false);

                if (spaceBelow < popupHeight && spaceAbove > popupHeight) {
                    popupWindow.showAsDropDown(anchor, xOffset, -anchor.getHeight() - popupHeight - yOffset);
                } else {
                    popupWindow.showAsDropDown(anchor, xOffset, yOffset);
                }
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
        if (query == null || query.isEmpty()) { return text; }

        String lowerText = text.toLowerCase();
        String lowerQuery = query.toLowerCase();

        SpannableString spannable = new SpannableString(text);

        int index = 0;
        while ((index = lowerText.indexOf(lowerQuery, index)) >= 0) {
            spannable.setSpan(
                    new ForegroundColorSpan(ContextCompat.getColor(holder.itemView.getContext(), R.color.accent)), index, index + query.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            index += query.length();
        }
        return spannable;
    }
    public void updateData(List<Song> newData) {
        this.songs.clear();
        this.songs.addAll(newData);
        notifyDataSetChanged();
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