package com.example.appdelicia01.domain.model;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class UserManager {

    private static final UserManager instance = new UserManager();
    private FirebaseUser firebaseUser;
    private String userRole;
    private static final String TAG = "UserManager";

    private UserManager() {
        // Constructor privado para el patrón Singleton
    }

    public static UserManager getInstance() {
        return instance;
    }

    // Interfaz para notificar cuando los datos del usuario están cargados
    public interface UserDataLoadListener {
        void onUserDataLoaded();
        void onUserDataLoadFailed();
    }

    public void loadUserData(UserDataLoadListener listener) {
        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser != null) {
            String userId = firebaseUser.getUid();
            FirebaseFirestore.getInstance().collection("users").document(userId)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document != null && document.exists()) {
                                // Cargar el rol desde Firestore
                                userRole = document.getString("role");
                                Log.d(TAG, "Datos del usuario cargados. Rol: " + userRole);
                                if (listener != null) listener.onUserDataLoaded();
                            } else {
                                Log.w(TAG, "No se encontró documento de usuario en Firestore.");
                                userRole = null; // No hay documento, no hay rol
                                if (listener != null) listener.onUserDataLoadFailed();
                            }
                        } else {
                            Log.e(TAG, "Error al cargar datos del usuario desde Firestore.", task.getException());
                            userRole = null;
                            if (listener != null) listener.onUserDataLoadFailed();
                        }
                    });
        } else {
            Log.d(TAG, "No hay usuario de Firebase logueado.");
            clearUserData();
            if (listener != null) listener.onUserDataLoadFailed(); // No hay usuario para cargar
        }
    }

    public boolean isAdmin() {
        // Compara de forma segura para evitar NullPointerException
        return "admin".equals(userRole);
    }

    public FirebaseUser getCurrentUser() {
        return firebaseUser;
    }

    public void clearUserData() {
        firebaseUser = null;
        userRole = null;
        Log.d(TAG, "Datos del usuario limpiados.");
    }
}
