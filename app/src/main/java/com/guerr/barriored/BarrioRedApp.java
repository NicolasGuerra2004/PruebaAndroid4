package com.guerr.barriored;

import android.app.Application;

import com.google.firebase.FirebaseApp;
import com.guerr.barriored.utils.NotificationHelper;

public class BarrioRedApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // Initialize Firebase if needed
        FirebaseApp.initializeApp(this);
        // Initialize emergency notifications channel
        NotificationHelper.createNotificationChannel(this);
        // Initialize announcements notification channel
        com.guerr.barriored.service.MyFirebaseMessagingService.createAnnouncementChannel(this);
    }
}
