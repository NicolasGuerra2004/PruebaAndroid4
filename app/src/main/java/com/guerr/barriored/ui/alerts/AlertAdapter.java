package com.guerr.barriored.ui.alerts;

import android.content.Context;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.guerr.barriored.R;
import com.guerr.barriored.databinding.ItemAlertBinding;
import com.guerr.barriored.model.Alert;

import java.util.ArrayList;
import java.util.List;

public class AlertAdapter extends RecyclerView.Adapter<AlertAdapter.AlertViewHolder> {

    public interface OnAlertStateChangeListener {
        void onAlertStateChange(Alert alert, String newState);
    }

    private final List<Alert> alertList = new ArrayList<>();
    private boolean isAdmin = false;
    private final OnAlertStateChangeListener stateChangeListener;

    public AlertAdapter(OnAlertStateChangeListener listener) {
        this.stateChangeListener = listener;
    }

    public void setAlerts(List<Alert> newAlerts) {
        alertList.clear();
        if (newAlerts != null) {
            alertList.addAll(newAlerts);
        }
        notifyDataSetChanged();
    }

    public void setIsAdmin(boolean isAdmin) {
        this.isAdmin = isAdmin;
        notifyDataSetChanged();
    }

    public void addAlertAtTop(Alert alert) {
        if (alert == null) return;
        for (int i = 0; i < alertList.size(); i++) {
            if (alert.getId() != null && alert.getId().equals(alertList.get(i).getId())) {
                return;
            }
        }
        alertList.add(0, alert);
        notifyItemInserted(0);
    }

    @NonNull
    @Override
    public AlertViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemAlertBinding binding = ItemAlertBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new AlertViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull AlertViewHolder holder, int position) {
        holder.bind(alertList.get(position), isAdmin, stateChangeListener);
    }

    @Override
    public int getItemCount() {
        return alertList.size();
    }

    static class AlertViewHolder extends RecyclerView.ViewHolder {
        private final ItemAlertBinding binding;

        public AlertViewHolder(ItemAlertBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(Alert alert, boolean isAdmin, OnAlertStateChangeListener listener) {
            Context context = itemView.getContext();

            binding.tvAlertCategoryBadge.setText(alert.getCategoriaDisplay().toUpperCase());
            binding.tvAlertDescription.setText(alert.getDescripcion() != null && !alert.getDescripcion().trim().isEmpty()
                    ? alert.getDescripcion()
                    : "Alerta comunitaria.");

            // Identidad exacta requerida: Nombre · #311A
            binding.tvAlertAuthor.setText(alert.getAutorIdentidad());

            if (alert.getTimestamp() > 0) {
                CharSequence relative = DateUtils.getRelativeTimeSpanString(
                        alert.getTimestamp(),
                        System.currentTimeMillis(),
                        DateUtils.MINUTE_IN_MILLIS,
                        DateUtils.FORMAT_ABBREV_RELATIVE
                );
                binding.tvAlertTime.setText(relative);
            } else {
                binding.tvAlertTime.setText("Reciente");
            }

            // Category color
            int colorRes = R.color.cat_robo;
            String cat = alert.getCategoria() != null ? alert.getCategoria().toUpperCase() : "";
            switch (cat) {
                case Alert.CAT_ROBO:
                    colorRes = R.color.cat_robo;
                    break;
                case Alert.CAT_MEDICA:
                    colorRes = R.color.cat_medica;
                    break;
                case Alert.CAT_SOSPECHOSA:
                    colorRes = R.color.cat_sospechosa;
                    break;
                case Alert.CAT_INCENDIO:
                    colorRes = R.color.cat_incendio;
                    break;
                case Alert.CAT_MASCOTA:
                    colorRes = R.color.cat_mascota;
                    break;
            }
            binding.tvAlertCategoryBadge.setBackgroundColor(ContextCompat.getColor(context, colorRes));

            // Estado de la alerta: "activa", "atendida", "cerrada"
            String estado = alert.getEstado() != null ? alert.getEstado().toLowerCase() : Alert.STATE_ACTIVA;
            binding.tvAlertStateBadge.setText(estado.toUpperCase());

            if (Alert.STATE_CERRADA.equalsIgnoreCase(estado)) {
                binding.tvAlertStateBadge.setBackgroundColor(ContextCompat.getColor(context, R.color.text_muted));
                binding.cardAlert.setAlpha(0.7f);
            } else if (Alert.STATE_ATENDIDA.equalsIgnoreCase(estado)) {
                binding.tvAlertStateBadge.setBackgroundColor(ContextCompat.getColor(context, R.color.primary_variant));
                binding.cardAlert.setAlpha(1.0f);
            } else {
                binding.tvAlertStateBadge.setBackgroundColor(ContextCompat.getColor(context, R.color.cat_robo));
                binding.cardAlert.setAlpha(1.0f);
            }

            // Gestión de estado: Solo visible para usuarios con rol == "admin"
            if (isAdmin) {
                binding.btnAdminManageAlert.setVisibility(View.VISIBLE);
                binding.btnAdminManageAlert.setOnClickListener(v -> {
                    String[] options = new String[]{"Marcar como Atendida", "Cerrar Alerta", "Reabrir como Activa"};
                    new MaterialAlertDialogBuilder(context)
                            .setTitle("Cambiar Estado de Alerta")
                            .setItems(options, (dialog, which) -> {
                                if (listener == null) return;
                                if (which == 0) {
                                    listener.onAlertStateChange(alert, Alert.STATE_ATENDIDA);
                                } else if (which == 1) {
                                    listener.onAlertStateChange(alert, Alert.STATE_CERRADA);
                                } else if (which == 2) {
                                    listener.onAlertStateChange(alert, Alert.STATE_ACTIVA);
                                }
                            })
                            .show();
                });
            } else {
                binding.btnAdminManageAlert.setVisibility(View.GONE);
                binding.btnAdminManageAlert.setOnClickListener(null);
            }
        }
    }
}
