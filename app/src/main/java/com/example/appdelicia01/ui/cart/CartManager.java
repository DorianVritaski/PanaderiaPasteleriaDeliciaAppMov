package com.example.appdelicia01.ui.cart;

import com.example.appdelicia01.domain.model.Product;

import java.util.HashMap;
import java.util.LinkedHashMap; // No se usa en el nuevo código, se puede quitar
import java.util.List;          // No se usa directamente en los métodos públicos del nuevo código
import java.util.Map;
import java.util.stream.Collectors; // No se usa en el nuevo código, se puede quitar

// Contenido anterior comentado para referencia, puedes eliminarlo después de verificar.
/*
public class CartManager {
    private static CartManager INSTANCE;
    private final Map<String,Integer> items = new LinkedHashMap<>(); // id -> qty
    private final Map<String, Product> index = new HashMap<>();
    private CartManager(){}
    public static synchronized CartManager get(){ return INSTANCE==null?
            (INSTANCE=new CartManager()):INSTANCE; }

    public void indexProducts(List<Product> products){ for (Product p:products)
        index.put(p.getId(), p); }
    public void add(String productId){ items.put(productId,
            items.getOrDefault(productId,0)+1); }
    public void removeOne(String productId){
        if(!items.containsKey(productId)) return;
        int q = items.get(productId)-1; if(q<=0) items.remove(productId); else
            items.put(productId,q);
    }
    public List<Product> products(){ return
            items.keySet().stream().map(index::get).collect(Collectors.toList()); }
    public int qtyOf(String productId){ return items.getOrDefault(productId,0); }
    public double total(){
        double sum=0;
        for(String id: items.keySet()){ sum += index.get(id).getPrice()*items.get(id); } //estructurada
        return sum;
    }
    public void clear(){ items.clear(); }
}
*/

// Nuevo código para CartManager
public class CartManager {
    private static CartManager instance; // Nombre de instancia cambiado de INSTANCE a instance
    private final Map<Product, Integer> cart = new HashMap<>(); // Product como clave, Integer como cantidad

    private CartManager() {
        // Constructor privado para el patrón Singleton
    }

    // Método get() renombrado a getInstance() y no es synchronized por defecto
    // Si la concurrencia es una preocupación importante, podrías necesitar
    // volver a hacerlo synchronized o usar double-checked locking.
    public static CartManager getInstance() {
        if (instance == null) {
            instance = new CartManager();
        }
        return instance;
    }

    // Método add() renombrado a addToCart() y ahora toma Product y quantity
    public void addToCart(Product p, int quantity) {
        // Asegúrate que la clase Product tenga implementados hashCode() y equals()
        // para que funcione correctamente como clave en un HashMap.
        int currentQuantity = cart.getOrDefault(p, 0);
        cart.put(p, currentQuantity + quantity);
    }

    // Nuevo método para obtener todos los items del carrito
    public Map<Product, Integer> getCartItems() {
        return cart; // Devuelve una referencia directa al mapa, considera devolver una copia si la inmutabilidad es importante
    }

    // Método clear() renombrado a clearCart()
    public void clearCart() {
        cart.clear();
    }

    // Métodos del antiguo CartManager que necesitarías re-implementar o adaptar
    // si aún los necesitas con la nueva estructura:

    // public void removeOne(Product product) { ... }
    // public List<Product> getProductsInCart() { ... } // similar a products()
    // public int getQuantityOfProduct(Product product) { ... } // similar a qtyOf()
    // public double getTotalPrice() { ... } // similar a total()

    // Ejemplo de cómo podría ser getTotalPrice con la nueva estructura:
    public double getTotalPrice() {
        double sum = 0;
        for (Map.Entry<Product, Integer> entry : cart.entrySet()) {
            sum += entry.getKey().getPrice() * entry.getValue();
        }
        return sum;
    }

    // Ejemplo de cómo podría ser getQuantityOfProduct:
    public int getTotalItemCount() {
        int totalItems = 0;
        if (cart == null || cart.isEmpty()) {
            return 0;
        }
        for (Integer quantity : cart.values()) {
            if (quantity != null) { // Buena práctica verificar nulls
                totalItems += quantity;
            }
        }
        return totalItems;
    }
}
