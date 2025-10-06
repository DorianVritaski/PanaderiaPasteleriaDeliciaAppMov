package com.example.appdelicia01.ui.cart;

import android.content.Intent;
import android.os.Bundle;
// Quitar SharedPreferences si ya no se usa para nada más en esta Activity
// import android.content.SharedPreferences;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.appdelicia01.R;
import com.example.appdelicia01.domain.model.Product;
import com.example.appdelicia01.ui.auth.LoginActivity;
// Importar FirebaseAuth
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Locale;
import java.util.Map;

public class CartActivity extends AppCompatActivity {
    private LinearLayout container;
    private TextView txtTotal;

    // Estas constantes probablemente ya no sean necesarias aquí si solo se usaban para el login
    // public static final String PREFS_NAME = "AppPrefs";
    // public static final String KEY_IS_LOGGED_IN = "IS_LOGGED_IN";

    private static final int LOGIN_REQUEST_CODE = 1001;
    private static final String TAG = "CartActivity";

    // Instancia de FirebaseAuth
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_cart);

        // Inicializar Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        container = findViewById(R.id.container);
        txtTotal = findViewById(R.id.txtTotal);

        render();

        findViewById(R.id.btnClear).setOnClickListener(v -> {
            CartManager.getInstance().clearCart();
            render();
        });

        Button btnCheckout = findViewById(R.id.btnCheckout);
        if (btnCheckout != null) {
            btnCheckout.setOnClickListener(v -> {
                if (checkUserLoginStatusFirebase()) { // Usar el nuevo método
                    // Usuario ya logueado, ir a Checkout (simulado por ahora)
                    FirebaseUser currentUser = mAuth.getCurrentUser();
                    Toast.makeText(CartActivity.this, "Usuario logueado: " + (currentUser != null ? currentUser.getEmail() : "N/A") + ". Ir a Checkout.", Toast.LENGTH_SHORT).show();
                    // Intent intent = new Intent(CartActivity.this, CheckoutActivity.class);
                    // startActivity(intent);
                } else {
                    // Navegar a LoginActivity esperando un resultado
                    Log.d(TAG, "Usuario no logueado. Redirigiendo a LoginActivity.");
                    Intent intent = new Intent(CartActivity.this, LoginActivity.class);
                    startActivityForResult(intent, LOGIN_REQUEST_CODE);
                }
            });
        }

        Button btnBackToCatalog = findViewById(R.id.btnBackToCatalog);
        if (btnBackToCatalog != null) {
            btnBackToCatalog.setOnClickListener(v -> {
                finish();
            });
        }
    }

    // Nuevo método para verificar el estado de login con Firebase
    private boolean checkUserLoginStatusFirebase() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            Log.d(TAG, "checkUserLoginStatusFirebase: Usuario está logueado (" + currentUser.getEmail() + ")");
            return true;
        } else {
            Log.d(TAG, "checkUserLoginStatusFirebase: Usuario NO está logueado.");
            return false;
        }
    }


    /*
    private boolean checkUserLoginStatus() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean isLoggedIn = prefs.getBoolean(KEY_IS_LOGGED_IN, false);
        return isLoggedIn;
    }
    */


    // Actualizar performLogout para usar Firebase (aunque no se llame actualmente)
    private void performLogoutFirebase() {
        mAuth.signOut();
        Toast.makeText(this, "Sesión cerrada (Firebase)", Toast.LENGTH_SHORT).show();
        // Aquí podrías redirigir a LoginActivity o actualizar la UI si es necesario
        // Ejemplo:
        // Intent intent = new Intent(CartActivity.this, LoginActivity.class);
        // intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        // startActivity(intent);
        // finish();
        Log.d(TAG, "performLogoutFirebase: Sesión cerrada.");
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == LOGIN_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                // Login fue exitoso desde LoginActivity
                // Firebase Auth ya habrá actualizado el estado del usuario.
                // No necesitamos hacer nada con SharedPreferences aquí.
                // El onStart o la lógica en onCreate de esta actividad (si vuelve a primer plano)
                // debería reflejar el nuevo estado de login si es necesario.
                Toast.makeText(this, "Login exitoso desde CartActivity! Ahora puede proceder.", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "onActivityResult: Login exitoso (RESULT_OK). El usuario debería estar logueado ahora.");
                // Simplemente re-evaluar si se puede proceder a checkout
                // (Opcionalmente, puedes llamar directamente a la lógica de checkout aquí
                // o dejar que el usuario vuelva a presionar el botón "Checkout")
                // Por ejemplo, si el botón de checkout todavía está visible:
                Button btnCheckout = findViewById(R.id.btnCheckout);
                if (btnCheckout != null) {
                    btnCheckout.performClick(); // Simula un clic para re-evaluar
                }

            } else {
                // Login fue cancelado o falló
                Toast.makeText(this, "Inicio de sesión cancelado o fallido.", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "onActivityResult: Login cancelado o fallido (resultCode != RESULT_OK).");
            }
        }
    }

    private void render() {
        // ... (el resto del método render se mantiene igual)
        container.removeAllViews();
        LayoutInflater inf = LayoutInflater.from(this);
        Map<Product, Integer> cartItems = CartManager.getInstance().getCartItems();

        for (Map.Entry<Product, Integer> entry : cartItems.entrySet()) {
            Product p = entry.getKey();
            int quantity = entry.getValue();
            View row = inf.inflate(android.R.layout.simple_list_item_2, container, false);
            ((TextView) row.findViewById(android.R.id.text1)).setText(p.getName());
            ((TextView) row.findViewById(android.R.id.text2))
                    .setText("S/ " + p.getPrice() + " x" + quantity);
            row.setOnClickListener(v -> {
                CartManager.getInstance().addToCart(p, 1);
                render();
            });
            row.setOnLongClickListener(v -> {
                Toast.makeText(this, "Funcionalidad de remover uno no implementada con el nuevo CartManager", Toast.LENGTH_SHORT).show();
                render();
                return true;
            });
            container.addView(row);
        }
        txtTotal.setText(String.format(Locale.getDefault(), "Total: S/ %.2f",
                CartManager.getInstance().getTotalPrice()));
    }
}
