package com.example.appdelicia01.ui.catalog;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

// Importar Glide
import com.bumptech.glide.Glide;
import com.example.appdelicia01.R;
import com.example.appdelicia01.domain.model.Product;

import java.util.List;
import java.util.Locale;


import android.content.Context; // Necesario para el diálogo
import android.content.DialogInterface; // Necesario para el diálogo
import android.view.LayoutInflater; // Necesario para inflar el layout del diálogo
import android.view.View;
import android.widget.NumberPicker; // Importar NumberPicker
import android.widget.TextView; // Importar TextView

import androidx.appcompat.app.AlertDialog;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ViewHolder> {
    private List<Product> items;
    private Listener listener;

    public interface Listener {
        void onAdd(Product p, int quantity);
        void onShare(Product p);
    }

    public ProductAdapter(List<Product> items, Listener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_product, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Product p = items.get(position);
        holder.txtName.setText(p.getName());
        holder.txtPrice.setText(String.format(Locale.getDefault(), "S/ %.2f", p.getPrice()));

        // --- CAMBIO CLAVE: Cargar imagen desde URL con Glide ---
        Glide.with(holder.itemView.getContext())
                .load(p.getImageUrl()) // Cargar la URL de la imagen
                .placeholder(R.drawable.placeholder_image) // Una imagen de espera (opcional pero recomendado)
                .error(R.drawable.error_image) // Una imagen de error si la URL falla (opcional)
                .into(holder.imgProduct);

        holder.btnAdd.setOnClickListener(v -> {
            showQuantityDialog(v.getContext(), p);
        });

        holder.btnShare.setOnClickListener(v -> {
            if (listener != null) {
                listener.onShare(p);
            }
        });
    }

    // --- MÉTODO NUEVO A AÑADIR DENTRO DE ProductAdapter ---
    private void showQuantityDialog(Context context, Product product) {
        // 1. Inflar el layout personalizado para el diálogo
        LayoutInflater inflater = LayoutInflater.from(context);
        View dialogView = inflater.inflate(R.layout.dialog_quantity_picker, null);

        // 2. Referenciar las vistas dentro del layout del diálogo
        final TextView tvDialogProductName = dialogView.findViewById(R.id.tvDialogProductName);
        final NumberPicker numberPicker = dialogView.findViewById(R.id.npQuantity);

        // 3. Configurar las vistas
        tvDialogProductName.setText(product.getName());
        numberPicker.setMinValue(1); // Cantidad mínima
        numberPicker.setMaxValue(20); // Cantidad máxima (puedes ajustarla)
        numberPicker.setValue(1); // Valor inicial

        // 4. Crear y configurar el AlertDialog
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setView(dialogView);
        builder.setTitle("Agregar al Carrito");

        builder.setPositiveButton("Agregar", (dialog, which) -> {
            int quantity = numberPicker.getValue();
            // Llamamos al listener original con la cantidad seleccionada
            if (listener != null) {
                listener.onAdd(product, quantity);
            }
        });

        builder.setNegativeButton("Cancelar", (dialog, which) -> {
            dialog.dismiss(); // Simplemente cerrar el diálogo
        });

        // 5. Mostrar el diálogo
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    @Override
    public int getItemCount() {
        return items.size();
    }
    // Método para actualizar la lista de productos
    public void updateProducts(List<Product> newProducts) {
        this.items.clear();
        this.items.addAll(newProducts);
        notifyDataSetChanged(); // Notificar al RecyclerView que los datos han cambiado
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgProduct;
        TextView txtName, txtPrice;
        Button btnAdd;
        // ImageButton btnShare;  <-- CAMBIAR ESTA LÍNEA
        Button btnShare;          // <-- POR ESTA

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgProduct = itemView.findViewById(R.id.imgProduct);
            txtName = itemView.findViewById(R.id.txtName);
            txtPrice = itemView.findViewById(R.id.txtPrice);
            btnAdd = itemView.findViewById(R.id.btnAdd);
            btnShare = itemView.findViewById(R.id.btnShare); // Ahora la asignación es correcta
        }
    }
}
