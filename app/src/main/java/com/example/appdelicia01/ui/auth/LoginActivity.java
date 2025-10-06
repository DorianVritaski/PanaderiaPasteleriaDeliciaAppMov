package com.example.appdelicia01.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.appdelicia01.R;
import com.example.appdelicia01.domain.model.UserManager; // <-- 1. IMPORTAR UserManager
import com.example.appdelicia01.ui.catalog.CatalogActivity;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {

    // ... (tus variables de instancia se mantienen igual)
    private TextInputLayout tilEmail, tilPassword;
    private TextInputEditText etEmail, etPassword;
    private Button btnLogin;
    private TextView tvRegister, tvForgotPassword;
    private ProgressBar progressBarLogin;
    private FirebaseAuth mAuth;
    private static final int REGISTER_REQUEST_CODE = 1002;
    private static final String TAG = "LoginActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // ... (tu método onCreate se mantiene igual)
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();

        tilEmail = findViewById(R.id.tilEmail);
        etEmail = findViewById(R.id.etEmail);
        tilPassword = findViewById(R.id.tilPassword);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvRegister = findViewById(R.id.tvRegister);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);
        progressBarLogin = findViewById(R.id.progressBarLogin);

        btnLogin.setOnClickListener(v -> performLogin());

        if (tvRegister != null) {
            tvRegister.setOnClickListener(v -> {
                Log.d(TAG, "tvRegister clicked, attempting to start RegisterActivity.");
                Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
                startActivityForResult(intent, REGISTER_REQUEST_CODE);
            });
        }

        if (tvForgotPassword != null) {
            tvForgotPassword.setOnClickListener(v -> {
                Toast.makeText(LoginActivity.this, "Ir a Olvidé Contraseña (Pendiente con Firebase)", Toast.LENGTH_SHORT).show();
            });
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Comprobar si el usuario ya está logueado (sesión activa)
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            // El usuario ya está logueado, pero necesitamos cargar sus datos del UserManager
            Log.d(TAG, "Usuario ya logueado: " + currentUser.getEmail() + ". Cargando datos de rol...");
            setLoading(true); // Mostrar un indicador mientras cargan los datos
            UserManager.getInstance().loadUserData(new UserManager.UserDataLoadListener() {
                @Override
                public void onUserDataLoaded() {
                    Log.d(TAG, "Datos de usuario (rol) cargados exitosamente en onStart.");
                    setLoading(false);
                    navigateToMainActivity();
                }

                @Override
                public void onUserDataLoadFailed() {
                    Log.w(TAG, "Fallo al cargar datos de usuario en onStart, pero el usuario está logueado. Navegando...");
                    setLoading(false);
                    navigateToMainActivity();
                }
            });
        } else {
            Log.d(TAG, "Ningún usuario logueado.");
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // ... (tu método onActivityResult se mantiene igual)
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REGISTER_REQUEST_CODE && resultCode == RESULT_OK) {
            Toast.makeText(this, "Registro exitoso. Por favor, inicia sesión.", Toast.LENGTH_LONG).show();
            if (data != null && data.hasExtra("REGISTERED_EMAIL")) {
                etEmail.setText(data.getStringExtra("REGISTERED_EMAIL"));
                etPassword.requestFocus();
            }
        }
    }

    private void performLogin() {
        // ... (tus validaciones se mantienen igual)
        String email = etEmail.getText().toString().trim().toLowerCase();
        String password = etPassword.getText().toString().trim();

        boolean isValid = true;
        if (email.isEmpty()) {
            tilEmail.setError("El correo es requerido");
            isValid = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError("Ingrese un correo válido");
            isValid = false;
        } else {
            tilEmail.setError(null);
        }

        if (password.isEmpty()) {
            tilPassword.setError("La contraseña es requerida");
            isValid = false;
        } else {
            tilPassword.setError(null);
        }

        if (!isValid) {
            return;
        }

        setLoading(true);

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    // El setLoading(false) se moverá dentro de los callbacks de UserManager
                    if (task.isSuccessful()) {
                        // Inicio de sesión en Auth exitoso, ahora cargar datos de usuario (rol)
                        Log.d(TAG, "signInWithEmail:success. Cargando datos del usuario...");
                        Toast.makeText(LoginActivity.this, "Verificando datos de usuario...", Toast.LENGTH_SHORT).show();

                        // --- 2. IMPLEMENTACIÓN DEL BLOQUE ---
                        UserManager.getInstance().loadUserData(new UserManager.UserDataLoadListener() {
                            @Override
                            public void onUserDataLoaded() {
                                setLoading(false);
                                Log.d(TAG, "Datos de usuario cargados, navegando a la actividad principal.");
                                navigateToMainActivity();
                            }

                            @Override
                            public void onUserDataLoadFailed() {
                                setLoading(false);
                                // Fallo al cargar los datos, podría ser un usuario sin documento en Firestore
                                Log.w(TAG, "Fallo al cargar datos de usuario desde Firestore. Navegando de todas formas.");
                                // Aún así navegamos, pero el usuario no tendrá rol de admin.
                                navigateToMainActivity();
                            }
                        });

                    } else {
                        // Si el inicio de sesión falla, muestra un mensaje al usuario.
                        setLoading(false); // Ocultar ProgressBar inmediatamente si falla Auth
                        Log.w(TAG, "signInWithEmail:failure", task.getException());
                        String errorMessage = "Error de autenticación.";
                        if (task.getException() != null) {
                            Log.e(TAG, "Error: " + task.getException().getMessage());
                            if (task.getException() instanceof com.google.firebase.auth.FirebaseAuthInvalidUserException) {
                                errorMessage = "No existe una cuenta con este correo electrónico.";
                                tilEmail.setError(errorMessage);
                                tilPassword.setError(null);
                            } else if (task.getException() instanceof com.google.firebase.auth.FirebaseAuthInvalidCredentialsException) {
                                errorMessage = "La contraseña es incorrecta.";
                                tilPassword.setError(errorMessage);
                                tilEmail.setError(null);
                            } else {
                                errorMessage = "Error de autenticación: " + task.getException().getLocalizedMessage();
                            }
                        }
                        Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void navigateToMainActivity() {
        // ... (tu método navigateToMainActivity se mantiene igual)
        Intent intent = new Intent(LoginActivity.this, CatalogActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void setLoading(boolean isLoading) {
        // ... (tu método setLoading se mantiene igual)
        if (isLoading) {
            btnLogin.setEnabled(false);
            btnLogin.setAlpha(0.5f);
            progressBarLogin.setVisibility(View.VISIBLE);
        } else {
            btnLogin.setEnabled(true);
            btnLogin.setAlpha(1.0f);
            progressBarLogin.setVisibility(View.GONE);
        }
    }
}
