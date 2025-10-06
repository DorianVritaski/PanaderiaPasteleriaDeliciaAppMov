package com.example.appdelicia01.ui.catalog;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appdelicia01.R;
// --- 1. IMPORTAR CLASES NECESARIAS ---
import com.example.appdelicia01.domain.model.Product;
import com.example.appdelicia01.domain.model.UserManager;
import com.example.appdelicia01.ui.admin.AddProductActivity; // La nueva Activity
import com.example.appdelicia01.ui.auth.LoginActivity;
import com.example.appdelicia01.ui.cart.CartActivity;
import com.example.appdelicia01.ui.cart.CartManager;
import com.example.appdelicia01.ui.profile.ProfileActivity;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class CatalogActivity extends AppCompatActivity {
    // ... (tus variables de instancia se mantienen igual)
    private ProductAdapter adapter;
    private FloatingActionButton fabCart;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private ProgressBar progressBarCatalog;
    private RecyclerView rvProducts;

    private static final String TAG = "CatalogActivity";

    @Override
    protected void onResume() {
        super.onResume();
        updateCartBadge();
        // --- 2. ACTUALIZAR EL MENÚ CADA VEZ QUE LA PANTALLA SE MUESTRE ---
        //    Esto asegura que si un admin cierra sesión, el menú se actualiza.
        invalidateOptionsMenu();
    }

    @Override
    protected void onCreate(Bundle b) {
        // ... (tu onCreate se mantiene igual hasta el final)
        super.onCreate(b);
        setContentView(R.layout.activity_catalog);

        // Inicializar Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Inicializar Vistas
        Toolbar toolbar = findViewById(R.id.toolbarCatalog);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Catálogo");
        }

        progressBarCatalog = findViewById(R.id.progressBarCatalog);
        rvProducts = findViewById(R.id.rvProducts);
        rvProducts.setLayoutManager(new LinearLayoutManager(this));

        // Inicializar el adaptador con una lista vacía
        adapter = new ProductAdapter(new ArrayList<>(), new ProductAdapter.Listener() {
            @Override
            public void onAdd(Product p, int quantity) {
                CartManager.getInstance().addToCart(p, quantity);
                Toast.makeText(CatalogActivity.this,
                        p.getName() + " x" + quantity + " agregado al carrito",
                        Toast.LENGTH_SHORT).show();
                updateCartBadge();
            }

            @Override
            public void onShare(Product p) {
                Intent share = new Intent(Intent.ACTION_SEND);
                share.setType("text/plain");
                share.putExtra(Intent.EXTRA_TEXT, p.getName() + " – S/ " + p.getPrice());
                startActivity(Intent.createChooser(share, "Compartir producto"));
            }
        });
        rvProducts.setAdapter(adapter);

        fabCart = findViewById(R.id.fabCart);
        fabCart.setOnClickListener(v -> {
            Intent intent = new Intent(CatalogActivity.this, CartActivity.class);
            startActivity(intent);
        });

        // Cargar productos desde Firestore
        loadProductsFromFirestore();

        updateCartBadge();
    }

    // ... (loadProductsFromFirestore, setLoading, updateCartBadge se mantienen igual)

    // --- 3. MODIFICAR onCreateOptionsMenu ---
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);

        // Ocultar o mostrar el ítem de "Agregar Producto" basado en el rol del usuario
        MenuItem addProductItem = menu.findItem(R.id.action_add_product);
        if (addProductItem != null) {
            boolean isAdmin = UserManager.getInstance().isAdmin();
            Log.d(TAG, "onCreateOptionsMenu - Is admin? " + isAdmin);
            addProductItem.setVisible(isAdmin);
        }

        return true;
    }

    // --- 4. MODIFICAR onOptionsItemSelected ---
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();

        if (itemId == R.id.action_profile) {
            navigateToProfile();
            return true;
        } else if (itemId == R.id.action_view_cart) {
            navigateToCart();
            return true;
        } else if (itemId == R.id.action_add_product) { // Manejar el clic del nuevo ítem
            navigateToAddProduct();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // --- 5. CREAR EL MÉTODO PARA NAVEGAR ---
    private void navigateToAddProduct() {
        Log.d(TAG, "Navegando a AddProductActivity...");
        Intent intent = new Intent(this, AddProductActivity.class);
        startActivity(intent);
    }

    // --- (El resto de los métodos se mantienen igual) ---
    private void loadProductsFromFirestore() {
        setLoading(true);
        db.collection("products")
                .orderBy("createdAt", Query.Direction.DESCENDING) // Ordenar por fecha de creación descendente
                .addSnapshotListener((value, error) -> {
                    setLoading(false);
                    if (error != null) {
                        Log.e(TAG, "Error al escuchar cambios en productos", error);
                        Toast.makeText(this, "Error al cargar productos.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (value != null) {
                        List<Product> productList = new ArrayList<>();
                        for (QueryDocumentSnapshot doc : value) {
                            // Convertir el documento a un objeto Product
                            Product product = doc.toObject(Product.class);
                            product.setId(doc.getId()); // Asignar el ID del documento al objeto
                            productList.add(product);
                        }
                        Log.d(TAG, "Productos cargados: " + productList.size());
                        adapter.updateProducts(productList); // Actualizar el adaptador con la nueva lista
                    }
                });
    }

    private void setLoading(boolean isLoading) {
        if (isLoading) {
            progressBarCatalog.setVisibility(View.VISIBLE);
            rvProducts.setVisibility(View.GONE);
        } else {
            progressBarCatalog.setVisibility(View.GONE);
            rvProducts.setVisibility(View.VISIBLE);
        }
    }

    private void updateCartBadge() {
        if (fabCart == null) return;
        int itemCount = CartManager.getInstance().getTotalItemCount();
        if (itemCount > 0) {
            fabCart.show();
        } else {
            fabCart.hide();
        }
    }

    private void navigateToProfile() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            Intent intent = new Intent(this, ProfileActivity.class);
            startActivity(intent);
        } else {
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            Toast.makeText(this, "Por favor, inicia sesión para ver tu perfil.", Toast.LENGTH_LONG).show();
        }
    }

    private void navigateToCart() {
        Intent intent = new Intent(this, CartActivity.class);
        startActivity(intent);
    }
}
