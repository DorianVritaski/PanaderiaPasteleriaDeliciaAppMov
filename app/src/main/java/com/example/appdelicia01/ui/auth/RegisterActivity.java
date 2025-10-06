package com.example.appdelicia01.ui.auth;

import android.content.Intent;
// No necesitamos SharedPreferences para esto ahora
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar; // Asegúrate que este import esté
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.appdelicia01.R;
// Imports de Firebase
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest; // Para actualizar el perfil de Auth
import com.google.firebase.firestore.FirebaseFirestore; // Para Cloud Firestore

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private TextInputLayout tilFullName, tilEmailRegister, tilPasswordRegister, tilConfirmPassword;
    private TextInputEditText etFullName, etEmailRegister, etPasswordRegister, etConfirmPassword;
    private Button btnRegister;
    private TextView tvLoginHere;
    private ProgressBar progressBarRegister; // Referencia al ProgressBar

    // Instancias de Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private static final String TAG = "RegisterActivity";
    public static final String USERS_COLLECTION = "users"; // Nombre de la colección en Firestore

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Inicializar Firebase Auth y Firestore
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Inicializar vistas
        tilFullName = findViewById(R.id.tilFullName);
        etFullName = findViewById(R.id.etFullName);
        tilEmailRegister = findViewById(R.id.tilEmailRegister);
        etEmailRegister = findViewById(R.id.etEmailRegister);
        tilPasswordRegister = findViewById(R.id.tilPasswordRegister);
        etPasswordRegister = findViewById(R.id.etPasswordRegister);
        tilConfirmPassword = findViewById(R.id.tilConfirmPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnRegister = findViewById(R.id.btnRegister);
        tvLoginHere = findViewById(R.id.tvLoginHere);
        // Asegúrate de que el ID coincida con el de tu layout XML
        progressBarRegister = findViewById(R.id.progressBarRegister);


        // Configurar Listeners
        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performRegistration();
            }
        });

        tvLoginHere.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); // Simplemente cierra esta actividad para volver a LoginActivity
            }
        });
    }

    private void performRegistration() {
        String fullName = etFullName.getText().toString().trim();
        String email = etEmailRegister.getText().toString().trim().toLowerCase();
        String password = etPasswordRegister.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        // Validaciones (se mantienen igual)
        boolean isValid = true;
        if (fullName.isEmpty()) {
            tilFullName.setError("El nombre completo es requerido");
            isValid = false;
        } else {
            tilFullName.setError(null);
        }

        if (email.isEmpty()) {
            tilEmailRegister.setError("El correo es requerido");
            isValid = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmailRegister.setError("Ingrese un correo válido");
            isValid = false;
        } else {
            tilEmailRegister.setError(null);
        }

        if (password.isEmpty()) {
            tilPasswordRegister.setError("La contraseña es requerida");
            isValid = false;
        } else if (password.length() < 6) {
            tilPasswordRegister.setError("La contraseña debe tener al menos 6 caracteres");
            isValid = false;
        } else {
            tilPasswordRegister.setError(null);
        }

        if (confirmPassword.isEmpty()) {
            tilConfirmPassword.setError("Confirme la contraseña");
            isValid = false;
        } else if (!password.equals(confirmPassword)) {
            tilConfirmPassword.setError("Las contraseñas no coinciden");
            isValid = false;
        } else {
            tilConfirmPassword.setError(null);
        }

        if (!isValid) {
            return; // No continuar si hay errores de validación
        }
        // --- LÓGICA DE REGISTRO CON FIREBASE AUTHENTICATION Y FIRESTORE ---
        setLoading(true); // Mostrar ProgressBar

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Registro en Auth exitoso
                            Log.d(TAG, "createUserWithEmail:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                // Guardar información adicional del usuario en Firestore
                                // y luego actualizar el perfil de Firebase Auth
                                saveAdditionalUserInfo(user, fullName, email);
                            } else {
                                setLoading(false);
                                Toast.makeText(RegisterActivity.this, "Error al obtener usuario después del registro.", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            // Si el registro falla, muestra un mensaje al usuario.
                            setLoading(false);
                            Log.w(TAG, "createUserWithEmail:failure", task.getException());
                            String errorMessage = "Fallo el registro.";
                            if (task.getException() != null) {
                                errorMessage += " " + task.getException().getMessage();
                                // Manejo específico para colisión de email
                                if (task.getException() instanceof com.google.firebase.auth.FirebaseAuthUserCollisionException) {
                                    tilEmailRegister.setError("Este correo electrónico ya está registrado.");
                                    errorMessage = "Este correo electrónico ya está registrado. Intenta iniciar sesión.";
                                }
                            }
                            Toast.makeText(RegisterActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }
    private void saveAdditionalUserInfo(FirebaseUser firebaseUser, String fullName, String email) {
        String userId = firebaseUser.getUid();

        Map<String, Object> userData = new HashMap<>();
        userData.put("fullName", fullName);
        userData.put("email", email); // Guardar email en Firestore es opcional pero puede ser útil
        userData.put("createdAt", com.google.firebase.firestore.FieldValue.serverTimestamp()); // Fecha de creación

        // --- LÍNEA A AÑADIR (SÓLO PARA CREAR TU ADMIN) ---
        //userData.put("role", "admin"); // <-- ¡Añade la línea aquí!

        db.collection(USERS_COLLECTION).document(userId)
                .set(userData)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "Información adicional del usuario (con rol de admin) guardada en Firestore."); // Mensaje actualizado
                        // Ahora actualizamos el perfil de Firebase Auth
                        updateFirebaseUserProfile(firebaseUser, fullName, email);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // ... el resto del método se mantiene igual
                        Log.w(TAG, "Error al guardar información adicional del usuario en Firestore", e);
                        Toast.makeText(RegisterActivity.this, "Advertencia: Error al guardar datos adicionales: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        updateFirebaseUserProfile(firebaseUser, fullName, email);
                    }
                });

    }
    private void updateFirebaseUserProfile(FirebaseUser firebaseUser, String fullName, String registeredEmail) {
        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setDisplayName(fullName)
                // Aquí podrías añadir una URL de foto si la tuvieras: .setPhotoUri(Uri.parse("url_de_la_imagen"))
                .build();

        firebaseUser.updateProfile(profileUpdates)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        // Ocultamos el ProgressBar aquí, ya que es el final del flujo de registro exitoso.
                        setLoading(false);
                        if (task.isSuccessful()) {
                            Log.d(TAG, "Perfil de usuario de Firebase Auth actualizado.");
                        } else {
                            Log.w(TAG, "Error al actualizar el perfil de Firebase Auth.", task.getException());
                            // Opcional: Mostrar un Toast si la actualización del perfil falla,
                            // pero el registro principal (Auth y Firestore) ya debería haber ocurrido.
                            Toast.makeText(RegisterActivity.this, "Advertencia: No se pudo actualizar el nombre en el perfil de Auth.", Toast.LENGTH_SHORT).show();
                        }
                        // Esta acción se llama independientemente del éxito de updateProfile,
                        // ya que el registro principal (Auth y Firestore) ya debería haber ocurrido.
                        registrationSuccessActions(registeredEmail);
                    }
                });
    }
    private void registrationSuccessActions(String registeredEmail) {
        // Asegurarse que el ProgressBar esté oculto si no se hizo antes.
        if (progressBarRegister.getVisibility() == View.VISIBLE) {
            setLoading(false);
        }
        Toast.makeText(this, "Registro exitoso para " + etFullName.getText().toString().trim() + ". Ahora puedes iniciar sesión.", Toast.LENGTH_LONG).show();
        Intent resultIntent = new Intent();
        resultIntent.putExtra("REGISTERED_EMAIL", registeredEmail); // Devolver el email para pre-rellenar en LoginActivity
        setResult(RESULT_OK, resultIntent);
        finish(); // Cierra RegisterActivity
    }
    private void setLoading(boolean isLoading) {
        if (isLoading) {
            btnRegister.setEnabled(false); // Deshabilitar botón para evitar múltiples clics
            btnRegister.setAlpha(0.5f); // Opcional: hacerlo visualmente inactivo
            progressBarRegister.setVisibility(View.VISIBLE);
        } else {
            btnRegister.setEnabled(true);
            btnRegister.setAlpha(1.0f);
            progressBarRegister.setVisibility(View.GONE);
        }
    }
}
