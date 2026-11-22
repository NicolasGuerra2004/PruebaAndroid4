package com.guerr.barriored.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.guerr.barriored.MainActivity;
import com.guerr.barriored.R;
import com.guerr.barriored.databinding.ActivityLoginBinding;
import com.guerr.barriored.model.User;

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "LoginActivity";

    private ActivityLoginBinding binding;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private GoogleSignInClient mGoogleSignInClient;
    private ActivityResultLauncher<Intent> googleSignInLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // If user is already signed in, navigate straight to MainActivity
        if (mAuth.getCurrentUser() != null) {
            goToMainActivity();
            return;
        }

        setupGoogleSignIn();
        setupListeners();
    }

    private void setupGoogleSignIn() {
        String defaultClientId = null;
        try {
            int stringId = getResources().getIdentifier("default_web_client_id", "string", getPackageName());
            if (stringId != 0) {
                defaultClientId = getString(stringId);
            }
        } catch (Exception ignored) {
        }

        GoogleSignInOptions.Builder gsoBuilder = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail();

        if (defaultClientId != null && !defaultClientId.isEmpty()) {
            gsoBuilder.requestIdToken(defaultClientId);
        }

        mGoogleSignInClient = GoogleSignIn.getClient(this, gsoBuilder.build());

        googleSignInLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                    try {
                        GoogleSignInAccount account = task.getResult(ApiException.class);
                        if (account != null && account.getIdToken() != null) {
                            firebaseAuthWithGoogle(account.getIdToken(), account);
                        } else {
                            // If running in development without valid Google Web Client ID, handle gracefully
                            handleGoogleWithoutIdToken(account);
                        }
                    } catch (ApiException e) {
                        Log.w(TAG, "Google sign in failed, code=" + e.getStatusCode(), e);
                        Toast.makeText(LoginActivity.this, "Error de acceso Google (" + e.getStatusCode() + ")", Toast.LENGTH_SHORT).show();
                        setLoading(false);
                    }
                }
        );
    }

    private void setupListeners() {
        binding.btnLogin.setOnClickListener(v -> loginWithEmailPassword());

        binding.btnGoogleSignIn.setOnClickListener(v -> {
            setLoading(true);
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            googleSignInLauncher.launch(signInIntent);
        });

        binding.tvForgotPassword.setOnClickListener(v -> handleForgotPassword());

        binding.tvGoToRegister.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });
    }

    private void handleForgotPassword() {
        String email = binding.etEmail.getText() != null ? binding.etEmail.getText().toString().trim() : "";

        android.widget.EditText inputEmail = new android.widget.EditText(this);
        inputEmail.setHint("ejemplo@correo.com");
        inputEmail.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        if (!email.isEmpty()) {
            inputEmail.setText(email);
        }

        android.widget.FrameLayout container = new android.widget.FrameLayout(this);
        android.widget.FrameLayout.LayoutParams params = new android.widget.FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.leftMargin = (int) (24 * getResources().getDisplayMetrics().density);
        params.rightMargin = (int) (24 * getResources().getDisplayMetrics().density);
        inputEmail.setLayoutParams(params);
        container.addView(inputEmail);

        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle("Recuperar Contraseña")
                .setMessage("Ingresa tu correo registrado para recibir el enlace de restablecimiento:")
                .setView(container)
                .setPositiveButton("Enviar Correo", (dialog, which) -> {
                    String resetEmail = inputEmail.getText() != null ? inputEmail.getText().toString().trim() : "";
                    if (TextUtils.isEmpty(resetEmail)) {
                        Toast.makeText(this, "Por favor ingresa un correo válido", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    setLoading(true);
                    mAuth.sendPasswordResetEmail(resetEmail)
                            .addOnCompleteListener(task -> {
                                setLoading(false);
                                if (task.isSuccessful()) {
                                    Toast.makeText(this, "Correo de recuperación enviado a " + resetEmail, Toast.LENGTH_LONG).show();
                                } else {
                                    String err = task.getException() != null ? task.getException().getMessage() : "Error al enviar correo";
                                    Toast.makeText(this, "Error: " + err, Toast.LENGTH_LONG).show();
                                }
                            });
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void loginWithEmailPassword() {
        String email = binding.etEmail.getText() != null ? binding.etEmail.getText().toString().trim() : "";
        String password = binding.etPassword.getText() != null ? binding.etPassword.getText().toString().trim() : "";

        if (TextUtils.isEmpty(email)) {
            binding.tilEmail.setError("Ingresa tu correo electrónico");
            return;
        } else {
            binding.tilEmail.setError(null);
        }

        if (TextUtils.isEmpty(password)) {
            binding.tilPassword.setError("Ingresa tu contraseña");
            return;
        } else {
            binding.tilPassword.setError(null);
        }

        setLoading(true);

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    setLoading(false);
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        Toast.makeText(LoginActivity.this, "Bienvenido " + (user != null && user.getEmail() != null ? user.getEmail() : ""), Toast.LENGTH_SHORT).show();
                        goToMainActivity();
                    } else {
                        Log.w(TAG, "signInWithEmail:failure", task.getException());
                        String errorMsg = task.getException() != null ? task.getException().getMessage() : "Error de autenticación";
                        Toast.makeText(LoginActivity.this, "Error: " + errorMsg, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void firebaseAuthWithGoogle(String idToken, GoogleSignInAccount account) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = mAuth.getCurrentUser();
                        if (firebaseUser != null) {
                            ensureUserRecordExists(firebaseUser, account.getDisplayName());
                        } else {
                            setLoading(false);
                            goToMainActivity();
                        }
                    } else {
                        setLoading(false);
                        Log.w(TAG, "signInWithCredential:failure", task.getException());
                        Toast.makeText(LoginActivity.this, "Fallo al validar credencial de Google", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void handleGoogleWithoutIdToken(GoogleSignInAccount account) {
        // Fallback: If idToken is unavailable in developer local environment
        setLoading(false);
        Toast.makeText(this, "Sesión de Google iniciada (" + (account != null ? account.getEmail() : "") + ")", Toast.LENGTH_SHORT).show();
    }

    private void ensureUserRecordExists(FirebaseUser firebaseUser, String displayName) {
        String uid = firebaseUser.getUid();
        DatabaseReference userRef = mDatabase.child("users").child(uid);

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                setLoading(false);
                if (!snapshot.exists()) {
                    String name = displayName != null && !displayName.isEmpty() ? displayName : (firebaseUser.getEmail() != null ? firebaseUser.getEmail() : "Vecino");
                    String email = firebaseUser.getEmail() != null ? firebaseUser.getEmail() : "";
                    String photoUrl = firebaseUser.getPhotoUrl() != null ? firebaseUser.getPhotoUrl().toString() : "";
                    User newUser = new User(uid, name, email, "Sector Principal", "vecino", photoUrl);
                    userRef.setValue(newUser);
                }
                goToMainActivity();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                setLoading(false);
                goToMainActivity();
            }
        });
    }

    private void setLoading(boolean loading) {
        binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.btnLogin.setEnabled(!loading);
        binding.btnGoogleSignIn.setEnabled(!loading);
        binding.tvGoToRegister.setEnabled(!loading);
    }

    private void goToMainActivity() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
