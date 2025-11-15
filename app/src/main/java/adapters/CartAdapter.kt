package com.example.markettecnm.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.markettecnm.R
import com.example.markettecnm.models.Product

class CartAdapter(
    private val products: List<Product>,
    private val onDeleteClickListener: (Product) -> Unit,
    private val onImageClick: (Product) -> Unit
) : RecyclerView.Adapter<CartAdapter.CartViewHolder>() {

    private val selectedProductsForOrder = mutableSetOf<Product>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_cart_product, parent, false)
        return CartViewHolder(view)
    }

    override fun onBindViewHolder(holder: CartViewHolder, position: Int) {
        val product = products[position]

        holder.tvName.text = product.name
        holder.tvQuantity.text = "Cantidad: ${product.quantityInCart}"

        val totalItemPrice = product.price * product.quantityInCart
        holder.tvPrice.text = "$${String.format("%.2f", totalItemPrice)}"

        holder.ivProduct.setImageResource(product.imageRes)

        holder.cbSelect.visibility = View.VISIBLE
        holder.cbSelect.isChecked = selectedProductsForOrder.any { it.id == product.id }

        holder.cbSelect.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                selectedProductsForOrder.add(product)
            } else {
                selectedProductsForOrder.removeAll { it.id == product.id }
            }
        }

        holder.cbSelect.setOnClickListener { }

        holder.btnDeleteProduct.visibility = View.VISIBLE
        holder.btnDeleteProduct.setOnClickListener {
            onDeleteClickListener(product)
            selectedProductsForOrder.removeAll { it.id == product.id }
        }

        holder.ivProduct.setOnClickListener {
            onImageClick(product)
        }

        holder.itemView.setOnClickListener(null)
    }

    override fun getItemCount() = products.size

    fun getSelectedProductsForOrder(): Set<Product> {
        return selectedProductsForOrder
    }

    class CartViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivProduct: ImageView = itemView.findViewById(R.id.ivCartProduct)
        val tvName: TextView = itemView.findViewById(R.id.tvCartProductName)
        val tvQuantity: TextView = itemView.findViewById(R.id.tvProductQuantity)
        val tvPrice: TextView = itemView.findViewById(R.id.tvCartProductPrice)
        val cbSelect: CheckBox = itemView.findViewById(R.id.cbSelectProduct)
        val btnDeleteProduct: ImageButton = itemView.findViewById(R.id.btnDeleteProduct)
    }
}