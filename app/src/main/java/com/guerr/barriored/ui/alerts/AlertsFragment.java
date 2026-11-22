package com.guerr.barriored.ui.alerts;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.guerr.barriored.R;
import com.guerr.barriored.databinding.FragmentAlertsBinding;
import com.guerr.barriored.model.Alert;
import com.guerr.barriored.model.User;
import com.guerr.barriored.mqtt.MqttManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AlertsFragment extends Fragment implements MqttManager.MqttStatusListener, AlertAdapter.OnAlertStateChangeListener {

    private FragmentAlertsBinding binding;
    private AlertAdapter alertAdapter;
    private DatabaseReference alertsRef;
    private DatabaseReference userRef;
    private User currentUserProfile;
    private ValueEventListener alertsListener;
    private ValueEventListener userListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentAlertsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        alertsRef = FirebaseDatabase.getInstance().getReference("alerts");
        setupRecyclerView();
        setupCategoryButtons();
        loadCurrentUserProfile();
        loadAlertsHistory();
    }

    private void setupRecyclerView() {
        alertAdapter = new AlertAdapter(this);
        binding.rvAlerts.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvAlerts.setAdapter(alertAdapter);
    }

    private void setupCategoryButtons() {
        binding.btnAlertRobo.setOnClickListener(v -> showEmitAlertDialog(Alert.CAT_ROBO, "Robo / Seguridad"));
        binding.btnAlertMedica.setOnClickListener(v -> showEmitAlertDialog(Alert.CAT_MEDICA, "Emergencia Médica"));
        binding.btnAlertSospechosa.setOnClickListener(v -> showEmitAlertDialog(Alert.CAT_SOSPECHOSA, "Actividad Sospechosa"));
        binding.btnAlertIncendio.setOnClickListener(v -> showEmitAlertDialog(Alert.CAT_INCENDIO, "Incendio / Siniestro"));
        binding.btnAlertMascota.setOnClickListener(v -> showEmitAlertDialog(Alert.CAT_MASCOTA, "Mascota / Comunidad"));
    }

    private void loadCurrentUserProfile() {
        FirebaseUser fUser = FirebaseAuth.getInstance().getCurrentUser();
        if (fUser != null) {
            userRef = FirebaseDatabase.getInstance().getReference("users").child(fUser.getUid());
            userListener = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (!isAdded() || binding == null) return;
                    if (snapshot.exists()) {
                        currentUserProfile = snapshot.getValue(User.class);
                        boolean isAdmin = currentUserProfile != null && currentUserProfile.isAdmin();
                        alertAdapter.setIsAdmin(isAdmin);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                }
            };
            userRef.addValueEventListener(userListener);
        }
    }

    private void showEmitAlertDialog(String categoryKey, String categoryTitle) {
        Context context = getContext();
        if (context == null) return;

        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_emit_alert, null);
        TextView tvCategoryName = dialogView.findViewById(R.id.tvCategoryName);
        EditText etAlertDesc = dialogView.findViewById(R.id.etAlertDesc);

        tvCategoryName.setText("Emitir Alerta: " + categoryTitle);

        new MaterialAlertDialogBuilder(context)
                .setView(dialogView)
                .setPositiveButton(R.string.btn_send_alert, (dialog, which) -> {
                    String desc = etAlertDesc.getText() != null ? etAlertDesc.getText().toString().trim() : "";
                    emitAlert(categoryKey, desc);
                })
                .setNegativeButton(R.string.btn_cancel, null)
                .show();
    }

    private void emitAlert(String category, String description) {
        FirebaseUser fUser = FirebaseAuth.getInstance().getCurrentUser();
        String uid = fUser != null ? fUser.getUid() : "anonimo";
        String authorName = (currentUserProfile != null && currentUserProfile.getNombre() != null)
                ? currentUserProfile.getNombre()
                : (fUser != null && fUser.getEmail() != null ? fUser.getEmail() : "Vecino");
        String authorVivienda = (currentUserProfile != null && currentUserProfile.getVivienda() != null)
                ? currentUserProfile.getVivienda()
                : "S/N";

        String alertId = alertsRef.push().getKey();
        if (alertId == null) {
            alertId = "alert_" + System.currentTimeMillis();
        }

        // Construir alerta con vivienda, identidad exacta (Nombre · #311A) y estado inicial "activa"
        Alert alert = new Alert(
                alertId,
                category,
                description,
                authorName,
                authorVivienda,
                uid,
                System.currentTimeMillis()
        );
        alert.setEstado(Alert.STATE_ACTIVA);

        // 1. Guardar en Firebase Realtime Database para persistencia
        alertsRef.child(alertId).setValue(alert);

        // 2. Publicar por MQTT (EMQX Cloud) para transmisión y alerta inmediata
        Context context = getContext();
        if (context != null) {
            MqttManager.getInstance(context).publishAlert(alert, new MqttManager.PublishCallback() {
                @Override
                public void onSuccess() {
                    if (isAdded()) {
                        Toast.makeText(getContext(), "🚨 Alerta emitida a toda la comunidad.", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onError(Exception e) {
                    if (isAdded()) {
                        Toast.makeText(getContext(), "Alerta guardada (MQTT reconectando...)", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }

    @Override
    public void onAlertStateChange(Alert alert, String newState) {
        if (alert == null || alert.getId() == null) return;

        alertsRef.child(alert.getId()).child("estado").setValue(newState)
                .addOnSuccessListener(aVoid -> {
                    if (isAdded()) {
                        Toast.makeText(getContext(), "Estado de alerta actualizado a: " + newState.toUpperCase(), Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    if (isAdded()) {
                        Toast.makeText(getContext(), "Error al actualizar estado: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadAlertsHistory() {
        binding.progressAlerts.setVisibility(View.VISIBLE);
        alertsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded() || binding == null) return;
                binding.progressAlerts.setVisibility(View.GONE);

                List<Alert> list = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    Alert alert = child.getValue(Alert.class);
                    if (alert != null) {
                        list.add(alert);
                    }
                }

                // Sort descending by timestamp (newest first)
                Collections.sort(list, (a1, a2) -> Long.compare(a2.getTimestamp(), a1.getTimestamp()));

                alertAdapter.setAlerts(list);
                binding.tvEmptyAlerts.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (!isAdded() || binding == null) return;
                binding.progressAlerts.setVisibility(View.GONE);
            }
        };

        alertsRef.limitToLast(50).addValueEventListener(alertsListener);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getContext() != null) {
            MqttManager.getInstance(getContext()).addListener(this);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (getContext() != null) {
            MqttManager.getInstance(getContext()).removeListener(this);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (alertsRef != null && alertsListener != null) {
            alertsRef.removeEventListener(alertsListener);
        }
        if (userRef != null && userListener != null) {
            userRef.removeEventListener(userListener);
        }
        binding = null;
    }

    // MQTT Callbacks
    @Override
    public void onConnectionStatusChanged(boolean connected, String statusMessage) {
        if (!isAdded() || binding == null || getContext() == null) return;

        binding.tvMqttStatus.setText(statusMessage);

        int textColor = connected ? R.color.status_connected : R.color.status_connecting;
        int bgColor = connected ? R.color.status_connected_bg : R.color.status_connecting_bg;

        binding.tvMqttStatus.setTextColor(ContextCompat.getColor(getContext(), textColor));
        binding.layoutMqttStatus.setBackgroundColor(ContextCompat.getColor(getContext(), bgColor));
    }

    @Override
    public void onAlertMessageReceived(Alert alert) {
        if (!isAdded() || binding == null) return;
        alertAdapter.addAlertAtTop(alert);
        binding.tvEmptyAlerts.setVisibility(View.GONE);
        binding.rvAlerts.smoothScrollToPosition(0);
    }
}
