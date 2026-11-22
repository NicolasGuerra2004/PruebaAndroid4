package com.guerr.barriored.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.guerr.barriored.MainActivity;
import com.guerr.barriored.databinding.ActivityRegisterBinding;
import com.guerr.barriored.model.User;

public class RegisterActivity extends AppCompatActivity {
    private ActivityRegisterBinding binding;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        binding.btnRegister.setOnClickListener(v -> performRegistration());
        binding.tvGoToLogin.setOnClickListener(v -> finish());
    }

    private void performRegistration() {
        String name = binding.etName.getText() != null ? binding.etName.getText().toString().trim() : "";
        String vivienda = binding.etVivienda.getText() != null ? binding.etVivienda.getText().toString().trim().toUpperCase() : "";
        String email = binding.etEmail.getText() != null ? binding.etEmail.getText().toString().trim() : "";
        String password = binding.etPassword.getText() != null ? binding.etPassword.getText().toString().trim() : "";

        if (TextUtils.isEmpty(name)) {
            binding.tilName.setError("Ingresa tu nombre completo");
            return;
        } else {
            binding.tilName.setError(null);
        }

        if (TextUtils.isEmpty(vivienda)) {
            binding.tilVivienda.setError("Ingresa el número de tu vivienda (ej: 311A)");
            return;
        } else if (!User.isValidVivienda(vivienda)) {
            binding.tilVivienda.setError("Formato inválido. Usa números y letras (ej: 311A, 102B)");
            return;
        } else {
            binding.tilVivienda.setError(null);
        }

        if (TextUtils.isEmpty(email)) {
            binding.tilEmail.setError("Ingresa tu correo electrónico");
            return;
        } else {
            binding.tilEmail.setError(null);
        }

        if (TextUtils.isEmpty(password) || password.length() < 6) {
            binding.tilPassword.setError("La contraseña debe tener al menos 6 caracteres");
            return;
        } else {
            binding.tilPassword.setError(null);
        }

        setLoading(true);

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = mAuth.getCurrentUser();
                        if (firebaseUser != null) {
                            String uid = firebaseUser.getUid();
                            User user = new User(uid, name, email, vivienda, "vecino", "");

                            mDatabase.child("users").child(uid).setValue(user)
                                    .addOnCompleteListener(dbTask -> {
                                        setLoading(false);
                                        Toast.makeText(RegisterActivity.this, "¡Registro exitoso! Bienvenido vecino.", Toast.LENGTH_SHORT).show();
                                        Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                        startActivity(intent);
                                        finish();
                                    });
                        } else {
                            setLoading(false);
                            finish();
                        }
                    } else {
                        setLoading(false);
                        String errorMsg = task.getException() != null ? task.getException().getMessage() : "Error al registrarse";
                        Toast.makeText(RegisterActivity.this, "Error: " + errorMsg, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void setLoading(boolean loading) {
        binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.btnRegister.setEnabled(!loading);
        binding.tvGoToLogin.setEnabled(!loading);
    }
}
