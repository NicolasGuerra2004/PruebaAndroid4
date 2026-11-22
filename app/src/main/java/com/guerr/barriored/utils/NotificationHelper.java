package com.guerr.barriored.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.guerr.barriored.MainActivity;
import com.guerr.barriored.R;
import com.guerr.barriored.model.Alert;

public class NotificationHelper {
    public static final String CHANNEL_ID = "barriored_emergency_channel";
    public static final String CHANNEL_NAME = "Alertas de Emergencia BarrioRed";

    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Notificaciones inmediatas de alertas comunitarias");
            channel.enableLights(true);
            channel.setLightColor(Color.RED);
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{0, 500, 200, 500});

            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    public static void showEmergencyNotification(Context context, Alert alert) {
        if (context == null || alert == null) return;

        createNotificationChannel(context);

        Intent intent = new Intent(context, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra("select_tab", "alerts");

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                (int) System.currentTimeMillis(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        String detail = alert.getDescripcion();
        if (detail == null || detail.trim().isEmpty()) {
            detail = "Alerta emitida en " + (alert.getAutorDireccion() != null ? alert.getAutorDireccion() : "el vecindario");
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_alert)
                .setContentTitle("🚨 " + alert.getCategoriaDisplay())
                .setContentText(detail)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(detail + "\nEmisor: " + alert.getAutorNombre() +
                                (alert.getAutorDireccion() != null ? " (" + alert.getAutorDireccion() + ")" : "")))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setSound(defaultSoundUri)
                .setVibrate(new long[]{0, 500, 200, 500})
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        try {
            NotificationManagerCompat managerCompat = NotificationManagerCompat.from(context);
            managerCompat.notify((int) (System.currentTimeMillis() % 100000), builder.build());
        } catch (SecurityException ignored) {
            // Android 13 permission might not have been granted yet
        }
    }
}
