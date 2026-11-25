package com.guerr.barriored.ui.profile;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.guerr.barriored.R;
import com.guerr.barriored.databinding.FragmentProfileBinding;
import com.guerr.barriored.model.User;
import com.guerr.barriored.mqtt.MqttManager;
import com.guerr.barriored.ui.auth.LoginActivity;

import java.util.HashMap;
import java.util.Map;

public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;
    private DatabaseReference userRef;
    private ValueEventListener userListener;
    private User currentUser;
    private String currentUid = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        FirebaseUser fUser = FirebaseAuth.getInstance().getCurrentUser();
        if (fUser != null) {
            currentUid = fUser.getUid();
            binding.tvProfileEmail.setText(fUser.getEmail());
            loadUserProfile();
        }

        binding.btnSaveProfile.setOnClickListener(v -> saveProfileChanges());
        binding.btnLogout.setOnClickListener(v -> performLogout());
    }

    private void loadUserProfile() {
        userRef = FirebaseDatabase.getInstance().getReference("users").child(currentUid);
        userListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded() || binding == null) return;
                currentUser = snapshot.getValue(User.class);
                if (currentUser != null) {
                    String nombre = currentUser.getNombre() != null && !currentUser.getNombre().trim().isEmpty()
                            ? currentUser.getNombre().trim() : "Vecino BarrioRed";
                    binding.tvProfileName.setText(nombre);
                    binding.tvProfileInitials.setText(computeInitials(nombre));

                    if (currentUser.getEmail() != null && !currentUser.getEmail().isEmpty()) {
                        binding.tvProfileEmail.setText(currentUser.getEmail());
                    }
                    if (currentUser.getNombre() != null) {
                        binding.etProfileNameInput.setText(currentUser.getNombre());
                    }
                    if (currentUser.getVivienda() != null) {
                        binding.etProfileViviendaInput.setText(currentUser.getVivienda());
                    }

                    boolean isAdmin = currentUser.isAdmin();
                    binding.tvProfileRole.setText(isAdmin ? R.string.role_admin : R.string.role_vecino);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        };

        userRef.addValueEventListener(userListener);
    }

    private String computeInitials(String name) {
        if (name == null || name.trim().isEmpty()) return "BR";
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        }
        return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase();
    }

    private void saveProfileChanges() {
        String name = binding.etProfileNameInput.getText() != null ? binding.etProfileNameInput.getText().toString().trim() : "";
        String vivienda = binding.etProfileViviendaInput.getText() != null ? binding.etProfileViviendaInput.getText().toString().trim().toUpperCase() : "";

        if (TextUtils.isEmpty(name)) {
            binding.tilProfileName.setError("El nombre no puede estar vacío");
            return;
        } else {
            binding.tilProfileName.setError(null);
        }

        if (TextUtils.isEmpty(vivienda)) {
            binding.tilProfileVivienda.setError("Ingresa tu número de vivienda (ej: 311A)");
            return;
        } else if (!User.isValidVivienda(vivienda)) {
            binding.tilProfileVivienda.setError("Formato inválido. Usa números y letras (ej: 311A, 102B)");
            return;
        } else {
            binding.tilProfileVivienda.setError(null);
        }

        if (userRef != null) {
            Map<String, Object> updates = new HashMap<>();
            updates.put("nombre", name);
            updates.put("vivienda", vivienda);
            updates.put("direccion", vivienda);

            userRef.updateChildren(updates)
                    .addOnSuccessListener(aVoid -> {
                        if (isAdded()) {
                            Toast.makeText(getContext(), "Datos actualizados correctamente", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (isAdded()) {
                            Toast.makeText(getContext(), "Error al actualizar: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    private void performLogout() {
        Context context = getContext();
        if (context == null) return;

        // 1. Disconnect MQTT
        MqttManager.getInstance(context).disconnect();

        // 2. Sign out Firebase Auth
        FirebaseAuth.getInstance().signOut();

        // 3. Sign out Google
        try {
            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build();
            GoogleSignIn.getClient(context, gso).signOut();
        } catch (Exception ignored) {
        }

        // 4. Navigate back to LoginActivity
        Intent intent = new Intent(context, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        if (getActivity() != null) {
            getActivity().finish();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (userRef != null && userListener != null) {
            userRef.removeEventListener(userListener);
        }
        binding = null;
    }
}
