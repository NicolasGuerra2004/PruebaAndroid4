package com.guerr.barriored.service;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.guerr.barriored.MainActivity;
import com.guerr.barriored.R;

public class MyFirebaseMessagingService extends FirebaseMessagingService {
    private static final String TAG = "FCMService";
    public static final String CHANNEL_ANNOUNCEMENTS = "barriored_announcements_channel";

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d(TAG, "Nuevo token FCM recibido: " + token);
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        Log.d(TAG, "Mensaje FCM recibido de: " + remoteMessage.getFrom());

        String title = "Aviso Oficial de la Comunidad";
        String body = "Tienes un nuevo aviso de la administración.";

        if (remoteMessage.getNotification() != null) {
            if (remoteMessage.getNotification().getTitle() != null) {
                title = remoteMessage.getNotification().getTitle();
            }
            if (remoteMessage.getNotification().getBody() != null) {
                body = remoteMessage.getNotification().getBody();
            }
        } else if (!remoteMessage.getData().isEmpty()) {
            if (remoteMessage.getData().containsKey("titulo")) {
                title = remoteMessage.getData().get("titulo");
            }
            if (remoteMessage.getData().containsKey("descripcion")) {
                body = remoteMessage.getData().get("descripcion");
            }
        }

        showAnnouncementNotification(title, body);
    }

    private void showAnnouncementNotification(String title, String body) {
        createAnnouncementChannel(this);

        // Al tocar la notificación se abre MainActivity directamente en la pestaña de Avisos
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra("select_tab", "announcements");

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                (int) System.currentTimeMillis(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ANNOUNCEMENTS)
                .setSmallIcon(R.drawable.ic_announcements)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setSound(defaultSoundUri)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        try {
            NotificationManagerCompat managerCompat = NotificationManagerCompat.from(this);
            managerCompat.notify((int) (System.currentTimeMillis() % 100000), builder.build());
        } catch (SecurityException ignored) {
        }
    }

    public static void createAnnouncementChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ANNOUNCEMENTS,
                    "Avisos Oficiales de la Comunidad",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Notificaciones de avisos y comunicados oficiales de la administración");
            channel.enableVibration(true);

            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }
}
