package com.example.markettecnm.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.example.markettecnm.R
import androidx.annotation.DrawableRes

class TendenciaAdapter(private val imageResources: List<Int>) :
    RecyclerView.Adapter<TendenciaAdapter.TendenciaViewHolder>() {

    /**
     * ViewHolder: Contiene la referencia a la ImageView en el layout del ítem.
     */
    class TendenciaViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        // CORRECCIÓN CLAVE: El ID debe ser el de la ImageView del ítem (ivTendencia)
        val imageView: ImageView = view.findViewById(R.id.ivTendencia)
    }

    // ----------------------------------------------------------------------
    // MÉTODOS DEL ADAPTADOR
    // ----------------------------------------------------------------------

    /**
     * Crea y devuelve una nueva instancia del ViewHolder (infla el layout del ítem).
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TendenciaViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_tendencia, parent, false)
        return TendenciaViewHolder(view)
    }

    /**
     * Reemplaza el contenido de una vista (ImageView) con el recurso de imagen.
     */
    override fun onBindViewHolder(holder: TendenciaViewHolder, position: Int) {
        @DrawableRes val resourceId = imageResources[position]
        holder.imageView.setImageResource(resourceId)

        // Opcional: Agregar lógica de clic si es necesario
        holder.itemView.setOnClickListener {
            // Ejemplo de acción al hacer clic en la imagen de tendencia
            // Toast.makeText(holder.itemView.context, "Clic en tendencia $position", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Devuelve el número total de ítems que el carrusel debe mostrar.
     */
    override fun getItemCount() = imageResources.size
}