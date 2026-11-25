package com.guerr.barriored.ui.alerts;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
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
                        binding.tvAlertsRoleBadge.setText(isAdmin ? "ADMINISTRADOR" : "VECINO");
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

        BottomSheetDialog sheetDialog = new BottomSheetDialog(context);
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_emit_alert, null);
        sheetDialog.setContentView(dialogView);

        MaterialCardView cardCategory = dialogView.findViewById(R.id.cardModalCategory);
        ImageView ivIcon = dialogView.findViewById(R.id.ivModalCategoryIcon);
        TextView tvCategoryName = dialogView.findViewById(R.id.tvCategoryName);
        TextView tvCategorySub = dialogView.findViewById(R.id.tvCategorySubtitle);
        EditText etAlertDesc = dialogView.findViewById(R.id.etAlertDesc);
        TextView tvSenderIdentity = dialogView.findViewById(R.id.tvModalSenderIdentity);
        MaterialButton btnConfirm = dialogView.findViewById(R.id.btnConfirmAlertModal);

        tvCategoryName.setText(categoryTitle);

        int bgRes = R.color.cat_robo_bg;
        int accentRes = R.color.cat_robo;
        int iconRes = R.drawable.ic_shield;
        switch (categoryKey) {
            case Alert.CAT_MEDICA:
                bgRes = R.color.cat_medica_bg;
                accentRes = R.color.cat_medica;
                iconRes = R.drawable.ic_medical;
                break;
            case Alert.CAT_SOSPECHOSA:
                bgRes = R.color.cat_sospechosa_bg;
                accentRes = R.color.cat_sospechosa;
                iconRes = R.drawable.ic_eye;
                break;
            case Alert.CAT_INCENDIO:
                bgRes = R.color.cat_incendio_bg;
                accentRes = R.color.cat_incendio;
                iconRes = R.drawable.ic_fire;
                break;
            case Alert.CAT_MASCOTA:
                bgRes = R.color.cat_mascota_bg;
                accentRes = R.color.cat_mascota;
                iconRes = R.drawable.ic_people;
                break;
        }

        cardCategory.setCardBackgroundColor(ContextCompat.getColor(context, bgRes));
        ivIcon.setImageResource(iconRes);
        tvCategoryName.setTextColor(ContextCompat.getColor(context, accentRes));
        tvCategorySub.setTextColor(ContextCompat.getColor(context, R.color.text_secondary));
        btnConfirm.setBackgroundTintList(ContextCompat.getColorStateList(context, accentRes));

        String identityText = currentUserProfile != null ? currentUserProfile.getIdentidad() : "Vecino · #S/N";
        tvSenderIdentity.setText("Se enviará como " + identityText);

        dialogView.findViewById(R.id.btnCloseAlertModal).setOnClickListener(v -> sheetDialog.dismiss());
        dialogView.findViewById(R.id.btnCancelAlertModal).setOnClickListener(v -> sheetDialog.dismiss());
        btnConfirm.setOnClickListener(v -> {
            String desc = etAlertDesc.getText() != null ? etAlertDesc.getText().toString().trim() : "";
            emitAlert(categoryKey, desc);
            sheetDialog.dismiss();
        });

        sheetDialog.show();
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

        alertsRef.child(alertId).setValue(alert);

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

                Collections.sort(list, (a1, a2) -> Long.compare(a2.getTimestamp(), a1.getTimestamp()));

                alertAdapter.setAlerts(list);
                updateAlertsCount(list.size());
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

    private void updateAlertsCount(int count) {
        if (binding == null) return;
        binding.tvAlertsCount.setText(count == 1 ? "1 reporte" : count + " reportes");
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

    @Override
    public void onConnectionStatusChanged(boolean connected, String statusMessage) {
        // Banner eliminado por preferencia de diseño
    }

    @Override
    public void onAlertMessageReceived(Alert alert) {
        if (!isAdded() || binding == null) return;
        alertAdapter.addAlertAtTop(alert);
        updateAlertsCount(alertAdapter.getItemCount());
        binding.tvEmptyAlerts.setVisibility(View.GONE);
        binding.rvAlerts.smoothScrollToPosition(0);
    }
}
