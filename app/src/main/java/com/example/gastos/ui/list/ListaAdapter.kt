package com.example.gastos.ui.list

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.gastos.R
import com.example.gastos.databinding.ItemListaLayoutBinding
import com.example.gastos.entity.GastoIngresoModel
import com.example.gastos.ui.list.utils.IClickListener

class ListaAdapter(
    val lista: ArrayList<GastoIngresoModel>,
    val iClickInterface: IClickListener
) :
    RecyclerView.Adapter<ListaAdapter.ViewHolder>() {

    var viewHolder: ViewHolder? = null
    private var contexto: Context? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        contexto = parent.context
        val binding = ItemListaLayoutBinding.inflate(LayoutInflater.from(contexto), parent, false)
        viewHolder = ViewHolder(binding)

        return viewHolder!!
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = lista[position]

        with(holder.binding) {
            tvMonto.text = contexto?.getString(R.string.monto_pesos, item.valor)
            tvFecha.text = item.fecha
            when (item.tipo) {
                "transferencia" -> {
                    tvCuenta.visibility = View.GONE
                    tvCategoria.text = "De ${item.cuenta} a ${item.acuenta}"
                    tvDesciption.text = "Transferencia"
                    contexto?.let {
                        llItem.setBackgroundColor(
                            ContextCompat.getColor(
                                it,
                                R.color.blue
                            )
                        )
                    }
                }
                "gasto" -> {
                    tvCuenta.text = item.cuenta
                    tvCategoria.text = item.categoria
                    if (item.descripcion == "") {
                        tvDesciption.text = item.categoria
                    } else {
                        tvDesciption.text = item.descripcion
                    }
                    contexto?.let {
                        llItem.setBackgroundColor(
                            ContextCompat.getColor(
                                it,
                                R.color.red
                            )
                        )
                    }
                }
                else -> {
                    tvCuenta.text = item.cuenta
                    tvCategoria.text = item.categoria
                    if (item.descripcion == "") {
                        tvDesciption.text = item.categoria
                    } else {
                        tvDesciption.text = item.descripcion
                    }
                    contexto?.let {
                        llItem.setBackgroundColor(
                            ContextCompat.getColor(
                                it,
                                R.color.green
                            )
                        )
                    }
                }
            }

            llItem.setOnLongClickListener {
                iClickInterface.delete(item)
                true
            }
        }
    }

    override fun getItemCount(): Int {
        return lista.size
    }

    inner class ViewHolder(val binding: ItemListaLayoutBinding) :
        RecyclerView.ViewHolder(binding.root)
}
