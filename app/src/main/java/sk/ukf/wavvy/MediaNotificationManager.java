package sk.ukf.wavvy;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.support.v4.media.session.MediaSessionCompat;
import androidx.palette.graphics.Palette;
import androidx.core.app.NotificationCompat;
import sk.ukf.wavvy.model.Song;
import androidx.media.app.NotificationCompat.MediaStyle;

public class MediaNotificationManager {
    private static final String CHANNEL_ID = "wavvy_playback";
    private static final int NOTIFICATION_ID = 1;
    private final Context context;
    private final NotificationManager notificationManager;
    public MediaNotificationManager(Context context) {
        this.context = context;
        this.notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        createChannel();
    }
    private void createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Playback",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Music playback controls");
            notificationManager.createNotificationChannel(channel);
        }
    }
    public void showNotification(Song song, boolean isPlaying, MediaSessionCompat session, long position, long duration) {
        Bitmap original = BitmapFactory.decodeResource(context.getResources(), song.getCoverResId());
        int size = Math.min(original.getWidth(), original.getHeight());
        Bitmap cover = Bitmap.createBitmap(
                original,
                (original.getWidth() - size) / 2,
                (original.getHeight() - size) / 2,
                size,
                size
        );

        int color = 0xFF1DB954;
        Palette palette = Palette.from(cover).generate();
        if (palette.getVibrantColor(0) != 0) {
            color = palette.getVibrantColor(0);
        }

        Intent openIntent = new Intent(context, PlayerActivity.class);
        openIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(
                context,
                0,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        PendingIntent playPauseIntent = getIntent("ACTION_PLAY_PAUSE");
        PendingIntent nextIntent = getIntent("ACTION_NEXT");
        PendingIntent prevIntent = getIntent("ACTION_PREV");

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setStyle(new MediaStyle()
                        .setMediaSession(session.getSessionToken())
                        .setShowActionsInCompactView(0,1,2))
                .setContentTitle(song.getTitle())
                .setContentText(song.getArtist())
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setLargeIcon(cover)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setColor(color)
                .setColorized(true)
                .setContentIntent(contentIntent)
                .addAction(R.drawable.ic_prev, "Prev", prevIntent)
                .addAction(isPlaying ? R.drawable.ic_pause : R.drawable.ic_play, "Play", playPauseIntent)
                .addAction(R.drawable.ic_next, "Next", nextIntent)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOnlyAlertOnce(true);
        Notification notification = builder.build();
        notificationManager.notify(NOTIFICATION_ID, notification);
    }
    private PendingIntent getIntent(String action) {
        Intent intent = new Intent(context, NotificationReceiver.class);
        intent.setAction(action);
        return PendingIntent.getBroadcast(
                context,
                action.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }
    public void cancel() {
        notificationManager.cancel(NOTIFICATION_ID);
    }
}