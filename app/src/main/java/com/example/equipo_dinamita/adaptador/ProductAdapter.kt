package com.example.equipo_dinamita.adaptador

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.equipo_dinamita.R
import com.example.equipo_dinamita.modelo.Product
import java.util.Locale

// MVC: VISTA (adaptador)
// Conecta la lista de productos (MODELO) con el RecyclerView del catálogo.
// Cada fila se dibuja con el layout item_product.xml.
class ProductAdapter(
    private var products: List<Product> = emptyList(),
    // lambda que se ejecuta cuando tocan un producto (la define HomeActivity)
    private val onProductClick: (Product) -> Unit = {}
) : RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {

    // el ViewHolder guarda las vistas de una fila para reutilizarlas al hacer scroll
    inner class ProductViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivImage: ImageView = view.findViewById(R.id.ivProductImage)
        val tvTitle: TextView = view.findViewById(R.id.tvProductTitle)
        val tvCategory: TextView = view.findViewById(R.id.tvProductCategory)
        val tvPrice: TextView = view.findViewById(R.id.tvProductPrice)
    }

    // cambia la lista y le avisa al RecyclerView que se redibuje
    fun submitList(newProducts: List<Product>) {
        products = newProducts
        notifyDataSetChanged()
    }

    // crea el diseño de una fila
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_product, parent, false)
        return ProductViewHolder(view)
    }

    // llena la fila con los datos del producto de esa posición
    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val product = products[position]
        holder.tvTitle.text = product.title
        holder.tvCategory.text = product.category
        holder.tvPrice.text = String.format(Locale.getDefault(), "$%.2f", product.price)

        // Glide descarga la imagen de la URL (con imagen de carga y de error)
        Glide.with(holder.ivImage.context)
            .load(product.image)
            .placeholder(android.R.drawable.ic_menu_gallery)
            .error(android.R.drawable.ic_menu_report_image)
            .into(holder.ivImage)

        // al tocar la fila se avisa qué producto fue
        holder.itemView.setOnClickListener { onProductClick(product) }
    }

    // cuántas filas hay
    override fun getItemCount(): Int = products.size
}
