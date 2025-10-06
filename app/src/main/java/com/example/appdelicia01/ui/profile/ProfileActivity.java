package com.example.appdelicia01.ui.profile;

import android.content.Intent;
// SharedPreferences ya no es necesaria aquí para la info del perfil
// import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem; // Para el botón de atrás de la Toolbar
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar; // Para la Toolbar

import com.example.appdelicia01.R;
import com.example.appdelicia01.ui.auth.LoginActivity;
// Ya no necesitamos RegisterActivity para constantes aquí
// import com.example.appdelicia01.ui.auth.RegisterActivity;

// Imports de Firebase
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class ProfileActivity extends AppCompatActivity {

    private ImageView ivProfileUserAvatar;
    private TextView tvProfileFullName, tvProfileEmail;
    private Button btnProfileLogout;

    // Instancias de Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private static final String TAG = "ProfileActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // --- Configurar la Toolbar (opcional pero recomendado) ---
        Toolbar toolbar = findViewById(R.id.toolbarProfile); // Asume que tienes un Toolbar con este ID en activity_profile.xml
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true); // Muestra el botón de "atrás"
            getSupportActionBar().setTitle("Mi Perfil");
        }

        // Inicializar Firebase Auth y Firestore
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Inicializar vistas
        ivProfileUserAvatar = findViewById(R.id.ivProfileUserAvatar);
        tvProfileFullName = findViewById(R.id.tvProfileFullName);
        tvProfileEmail = findViewById(R.id.tvProfileEmail);
        btnProfileLogout = findViewById(R.id.btnProfileLogout);

        loadUserProfile();

        btnProfileLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performLogout();
            }
        });
    }

    private void loadUserProfile() {
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            // Establecer Email
            if (currentUser.getEmail() != null) {
                tvProfileEmail.setText(currentUser.getEmail());
            } else {
                tvProfileEmail.setText("Email no disponible");
            }

            // Establecer Nombre Completo
            // Primero intentar con el DisplayName del perfil de Auth
            String displayName = currentUser.getDisplayName();
            if (displayName != null && !displayName.isEmpty()) {
                tvProfileFullName.setText(displayName);
                Log.d(TAG, "Nombre cargado desde FirebaseUser.displayName: " + displayName);
            } else {
                // Si DisplayName está vacío, intentar cargar desde Firestore
                Log.d(TAG, "FirebaseUser.displayName está vacío. Intentando cargar desde Firestore...");
                loadNameFromFirestore(currentUser.getUid());
            }

            // Aquí podrías cargar la imagen del avatar si la gestionas con Firebase Storage
            // o si tienes una URL en el perfil de usuario o en Firestore.
            // Ejemplo:
            // if (currentUser.getPhotoUrl() != null) {
            //     Glide.with(this).load(currentUser.getPhotoUrl()).into(ivProfileUserAvatar);
            // }
            ivProfileUserAvatar.setImageResource(R.drawable.user_image); // Placeholder

        } else {
            // Esto no debería pasar si ProfileActivity solo es accesible por usuarios logueados.
            // Si ocurre, es un estado anómalo, redirigir a Login.
            Log.e(TAG, "Error: No se encontró usuario actual de Firebase. Redirigiendo a Login.");
            Toast.makeText(this, "Error al cargar datos del perfil. Sesión no encontrada.", Toast.LENGTH_LONG).show();
            navigateToLogin();
        }
    }

    private void loadNameFromFirestore(String userId) {
        DocumentReference userDocRef = db.collection("users").document(userId);
        userDocRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document != null && document.exists()) {
                        String fullName = document.getString("fullName"); // Asegúrate que el campo se llama "fullName"
                        if (fullName != null && !fullName.isEmpty()) {
                            tvProfileFullName.setText(fullName);
                            Log.d(TAG, "Nombre cargado desde Firestore: " + fullName);
                        } else {
                            tvProfileFullName.setText("Nombre no registrado en Firestore");
                            Log.w(TAG, "El campo 'fullName' no existe o está vacío en Firestore para UID: " + userId);
                        }
                    } else {
                        tvProfileFullName.setText("Datos de perfil no encontrados");
                        Log.w(TAG, "No se encontró documento en Firestore para UID: " + userId);
                    }
                } else {
                    tvProfileFullName.setText("Error al cargar nombre");
                    Log.e(TAG, "Error al obtener datos de Firestore: ", task.getException());
                    Toast.makeText(ProfileActivity.this, "Error al cargar nombre del perfil desde la base de datos.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }


    private void performLogout() {
        mAuth.signOut(); // Cerrar sesión en Firebase
        Toast.makeText(this, "Sesión cerrada", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "Usuario cerró sesión.");
        navigateToLogin();
    }

    private void navigateToLogin() {
        Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish(); // Cierra ProfileActivity
    }

    // Manejar el clic en el botón de "atrás" de la Toolbar
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            // Finaliza esta actividad y vuelve a la anterior en la pila (probablemente CatalogActivity)
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}

