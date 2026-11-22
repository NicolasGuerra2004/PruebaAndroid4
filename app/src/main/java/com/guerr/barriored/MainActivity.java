package com.guerr.barriored;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.google.firebase.auth.FirebaseAuth;
import com.guerr.barriored.databinding.ActivityMainBinding;
import com.guerr.barriored.mqtt.MqttManager;
import com.guerr.barriored.ui.alerts.AlertsFragment;
import com.guerr.barriored.ui.announcements.AnnouncementsFragment;
import com.guerr.barriored.ui.auth.LoginActivity;
import com.guerr.barriored.ui.chat.ChatFragment;
import com.guerr.barriored.ui.profile.ProfileFragment;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private final Fragment alertsFragment = new AlertsFragment();
    private final Fragment announcementsFragment = new AnnouncementsFragment();
    private final Fragment chatFragment = new ChatFragment();
    private final Fragment profileFragment = new ProfileFragment();

    private Fragment activeFragment = alertsFragment;
    private final FragmentManager fragmentManager = getSupportFragmentManager();

    private ActivityResultLauncher<String> requestPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Security check: If not logged in, redirect to Login
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupNotificationPermission();
        setupFragments();
        setupBottomNavigation();

        // Connect to MQTT broker for live alerts
        MqttManager.getInstance(this).connect();

        // Suscribirse al tópico FCM de avisos oficiales de la comunidad
        com.google.firebase.messaging.FirebaseMessaging.getInstance().subscribeToTopic("anuncios_comunidad");

        handleNotificationIntent(getIntent());
    }

    @Override
    protected void onNewIntent(@NonNull Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleNotificationIntent(intent);
    }

    private void handleNotificationIntent(Intent intent) {
        if (intent != null && intent.hasExtra("select_tab")) {
            String tab = intent.getStringExtra("select_tab");
            if ("alerts".equalsIgnoreCase(tab)) {
                binding.bottomNavigation.setSelectedItemId(R.id.navigation_alerts);
            } else if ("announcements".equalsIgnoreCase(tab)) {
                binding.bottomNavigation.setSelectedItemId(R.id.navigation_announcements);
            }
        }
    }

    private void setupNotificationPermission() {
        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    // Permission granted or denied handled gracefully
                }
        );

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }

    private void setupFragments() {
        fragmentManager.beginTransaction()
                .add(R.id.fragment_container, profileFragment, "4").hide(profileFragment)
                .add(R.id.fragment_container, chatFragment, "3").hide(chatFragment)
                .add(R.id.fragment_container, announcementsFragment, "2").hide(announcementsFragment)
                .add(R.id.fragment_container, alertsFragment, "1")
                .commit();
    }

    private void setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.navigation_alerts) {
                switchFragment(alertsFragment);
                return true;
            } else if (itemId == R.id.navigation_announcements) {
                switchFragment(announcementsFragment);
                return true;
            } else if (itemId == R.id.navigation_chat) {
                switchFragment(chatFragment);
                return true;
            } else if (itemId == R.id.navigation_profile) {
                switchFragment(profileFragment);
                return true;
            }
            return false;
        });
    }

    private void switchFragment(Fragment targetFragment) {
        if (activeFragment != targetFragment) {
            fragmentManager.beginTransaction()
                    .hide(activeFragment)
                    .show(targetFragment)
                    .commit();
            activeFragment = targetFragment;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // MqttManager lives for the session, will disconnect explicitly on logout or OS termination
    }
}
