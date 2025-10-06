package com.example.appdelicia01.ui.admin;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.appdelicia01.R;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class AddProductActivity extends AppCompatActivity {

    private TextInputLayout tilProductName, tilProductPrice, tilProductDescription, tilProductImageUrl;
    private TextInputEditText etProductName, etProductPrice, etProductDescription, etProductImageUrl;
    private Button btnSaveProduct;
    private ProgressBar progressBarAddProduct;

    private FirebaseFirestore db;
    private static final String TAG = "AddProductActivity";
    public static final String PRODUCTS_COLLECTION = "products"; // Nombre de la colección en Firestore

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_product);

        // Configurar la Toolbar
        Toolbar toolbar = findViewById(R.id.toolbarAddProduct);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Agregar Producto");
        }

        // Inicializar Firestore
        db = FirebaseFirestore.getInstance();

        // Inicializar vistas
        tilProductName = findViewById(R.id.tilProductName);
        etProductName = findViewById(R.id.etProductName);
        tilProductPrice = findViewById(R.id.tilProductPrice);
        etProductPrice = findViewById(R.id.etProductPrice);
        tilProductDescription = findViewById(R.id.tilProductDescription);
        etProductDescription = findViewById(R.id.etProductDescription);
        tilProductImageUrl = findViewById(R.id.tilProductImageUrl);
        etProductImageUrl = findViewById(R.id.etProductImageUrl);
        btnSaveProduct = findViewById(R.id.btnSaveProduct);
        progressBarAddProduct = findViewById(R.id.progressBarAddProduct);

        btnSaveProduct.setOnClickListener(v -> saveProduct());
    }

    private void saveProduct() {
        String name = etProductName.getText().toString().trim();
        String priceStr = etProductPrice.getText().toString().trim();
        String description = etProductDescription.getText().toString().trim();
        String imageUrl = etProductImageUrl.getText().toString().trim();

        // Validaciones
        if (name.isEmpty()) {
            tilProductName.setError("El nombre es requerido");
            return;
        } else {
            tilProductName.setError(null);
        }

        if (priceStr.isEmpty()) {
            tilProductPrice.setError("El precio es requerido");
            return;
        } else {
            tilProductPrice.setError(null);
        }

        double price;
        try {
            price = Double.parseDouble(priceStr);
        } catch (NumberFormatException e) {
            tilProductPrice.setError("Ingrese un precio válido");
            return;
        }

        if (imageUrl.isEmpty()) {
            tilProductImageUrl.setError("La URL de la imagen es requerida");
            return;
        } else {
            tilProductImageUrl.setError(null);
        }

        setLoading(true);

        // Crear mapa de datos del producto
        Map<String, Object> product = new HashMap<>();
        product.put("name", name);
        product.put("price", price);
        product.put("description", description);
        product.put("imageUrl", imageUrl);
        product.put("createdAt", com.google.firebase.firestore.FieldValue.serverTimestamp());

        // Añadir a la colección "products"
        db.collection(PRODUCTS_COLLECTION)
                .add(product) // .add() genera un ID automático
                .addOnSuccessListener(documentReference -> {
                    setLoading(false);
                    Log.d(TAG, "Producto guardado con ID: " + documentReference.getId());
                    Toast.makeText(AddProductActivity.this, "Producto guardado exitosamente", Toast.LENGTH_SHORT).show();
                    // Limpiar campos o cerrar actividad
                    finish(); // Cierra la actividad al guardar con éxito
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Log.e(TAG, "Error al guardar producto", e);
                    Toast.makeText(AddProductActivity.this, "Error al guardar producto: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void setLoading(boolean isLoading) {
        if (isLoading) {
            btnSaveProduct.setEnabled(false);
            btnSaveProduct.setAlpha(0.5f);
            progressBarAddProduct.setVisibility(View.VISIBLE);
        } else {
            btnSaveProduct.setEnabled(true);
            btnSaveProduct.setAlpha(1.0f);
            progressBarAddProduct.setVisibility(View.GONE);
        }
    }

    // Para manejar el clic en el botón de "atrás" de la Toolbar
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
