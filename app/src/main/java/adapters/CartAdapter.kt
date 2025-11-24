package com.example.markettecnm.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.markettecnm.R
import com.example.markettecnm.models.ProductModel

class CartAdapter(
    private var products: List<ProductModel>,
    private var quantities: Map<String, Int>, // Mapa de ID -> Cantidad
    private val onDeleteClick: (ProductModel) -> Unit,
    private val onProductClick: (ProductModel) -> Unit,
    // Agregamos un listener para notificar a la Activity sobre selecciones/totales
    private val onSelectionChange: (ProductModel, Boolean) -> Unit
) : RecyclerView.Adapter<CartAdapter.CartViewHolder>() {

    // Cambiamos el tipo de dato de String a Int para las cantidades
    private var selectedItems = mutableSetOf<Int>()

    inner class CartViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        // IDs del layout item_cart_product.xml
        val cbSelectProduct: CheckBox = view.findViewById(R.id.cbSelectProduct)
        val ivImage: ImageView = view.findViewById(R.id.ivCartProduct)
        val tvName: TextView = view.findViewById(R.id.tvCartProductName) // Coincide con tu XML
        val tvQuantity: TextView = view.findViewById(R.id.tvProductQuantity) // Coincide con tu XML
        val tvPrice: TextView = view.findViewById(R.id.tvCartProductPrice) // Coincide con tu XML
        val btnDelete: ImageButton = view.findViewById(R.id.btnDeleteProduct) // Coincide con tu XML
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartViewHolder {
        // Usamos R.layout.item_cart_product para que coincida con tu XML
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_cart_product, parent, false)
        return CartViewHolder(view)
    }

    override fun onBindViewHolder(holder: CartViewHolder, position: Int) {
        val product = products[position]
        val idString = product.id.toString()
        val quantity = quantities[idString] ?: 1

        val price = product.price.toDoubleOrNull() ?: 0.0

        // 1. Textos
        holder.tvName.text = product.name
        holder.tvPrice.text = String.format("$%.2f", price)
        holder.tvQuantity.text = "Cantidad: $quantity"

        // 2. CheckBox y Selección
        holder.cbSelectProduct.isChecked = selectedItems.contains(product.id)

        holder.cbSelectProduct.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                selectedItems.add(product.id)
            } else {
                selectedItems.remove(product.id)
            }
            onSelectionChange(product, isChecked)
        }

        // 3. Imagen con Glide
        if (!product.image.isNullOrEmpty()) {
            Glide.with(holder.itemView.context)
                .load(product.image)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .into(holder.ivImage)
        } else {
            holder.ivImage.setImageResource(android.R.drawable.ic_menu_gallery)
        }

        // 4. Clicks
        holder.btnDelete.setOnClickListener { onDeleteClick(product) }
        holder.itemView.setOnClickListener { onProductClick(product) }
    }

    override fun getItemCount() = products.size

    fun updateData(newProducts: List<ProductModel>, newQuantities: Map<String, Int>) {
        this.products = newProducts
        this.quantities = newQuantities

        // Limpiamos las selecciones al actualizar para evitar conflictos con IDs viejos
        selectedItems.clear()
        notifyDataSetChanged()
    }

    // Función pública para que la Activity pueda obtener los seleccionados
    fun getSelectedProductIds(): Set<Int> {
        return selectedItems
    }

    // Función pública para obtener el subtotal de los productos seleccionados
    fun getSelectedProductsForOrder(): List<Pair<ProductModel, Int>> {
        return products.filter { selectedItems.contains(it.id) }
            .map { it to (quantities[it.id.toString()] ?: 1) }
    }
}