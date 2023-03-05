package com.example.gastos.ui.list

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.gastos.MainActivity
import com.example.gastos.R
import com.example.gastos.databinding.FragmentListaBinding
import com.example.gastos.entity.CuentasModel
import com.example.gastos.entity.GastoIngresoModel
import com.example.gastos.entity.SumaMesModel
import com.example.gastos.ui.list.utils.IClickListener
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class ListaFragment : Fragment() {

    private var _binding: FragmentListaBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private lateinit var database: FirebaseFirestore
    private lateinit var contexto: Context
    private val viewModel: ListaViewModel by viewModels()
    private lateinit var adapter: ListaAdapter

    override fun onAttach(context: Context) {
        super.onAttach(context)
        contexto = context
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentListaBinding.inflate(inflater, container, false)
        database = FirebaseFirestore.getInstance()
        viewModel.year.value = arguments?.getInt("year")
        viewModel.month.value = arguments?.getInt("month")
        startCollects()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        getDatos()
    }

    private fun startCollects() {
        lifecycleScope.launch {
            viewModel.marcaProgresos.collect { marca ->
                if (marca == 3) {
                    binding.progressBar.visibility = View.GONE
                    if (viewModel.lista.value.isNotEmpty()) {
                        viewModel.lista.value.sortByDescending {
                            it.fecha.split("/")[0].toInt()
                        }
                        binding.noItems.visibility = View.GONE
                        binding.rvLista.visibility = View.VISIBLE
                        adapter = ListaAdapter(
                            viewModel.lista.value,
                            object : IClickListener {
                                override fun delete(item: GastoIngresoModel) {
                                    val builder = AlertDialog.Builder(contexto)
                                    builder.setMessage(getString(R.string.eliminar_item))
                                    builder.setPositiveButton(getString(R.string.eliminar)) { _, _ ->
                                        eliminarItem(item)
                                    }
                                    builder.setCancelable(true)
                                    builder.show()
                                }
                            }
                        )
                        binding.rvLista.adapter = adapter
                    } else {
                        binding.noItems.visibility = View.VISIBLE
                    }
                }
            }
        }
    }

    private fun eliminarItem(item: GastoIngresoModel) {
        if ((activity as MainActivity).isOnline()) {
            val user = (activity as MainActivity).getuser()
            if (user != null) {
                user.mail?.let { userMail ->
                    database.collection(userMail)
                        .document(viewModel.year.value.toString())
                        .collection(viewModel.month.value.toString())
                        .document(item.tipo)
                        .collection(item.fecha.split("/")[0])
                        .document(item.id!!)
                        .delete()
                    if (item.tipo == "transferencia") {
                        database.collection(userMail)
                            .document("cuentas").get().addOnSuccessListener { document ->
                                val cuentas = document.toObject(CuentasModel::class.java)!!
                                when (item.cuenta) {
                                    "Galicia" -> {
                                        cuentas.Galicia = cuentas.Galicia!! + item.valor!!
                                    }
                                    "Nación" -> {
                                        cuentas.Nación = cuentas.Nación!! + item.valor!!
                                    }
                                    "Billetera" -> {
                                        cuentas.Billetera = cuentas.Billetera!! + item.valor!!
                                    }
                                }
                                when (item.acuenta) {
                                    "Galicia" -> {
                                        cuentas.Galicia = cuentas.Galicia!! - item.valor!!
                                    }
                                    "Nación" -> {
                                        cuentas.Nación = cuentas.Nación!! - item.valor!!
                                    }
                                    "Billetera" -> {
                                        cuentas.Billetera = cuentas.Billetera!! - item.valor!!
                                    }
                                }
                                val map = mapOf(
                                    "Galicia" to cuentas.Galicia,
                                    "Billetera" to cuentas.Billetera,
                                    "Nación" to cuentas.Nación
                                )
                                database.collection(userMail)
                                    .document("cuentas").update(
                                        map
                                    )
                            }
                    } else {
                        database.collection(userMail)
                            .document(viewModel.year.value.toString())
                            .collection(viewModel.month.value.toString())
                            .document(item.tipo).get().addOnSuccessListener { document ->
                                val sumaMes = document.toObject(SumaMesModel::class.java)?.sumaMes!! - item.valor!!
                                database.collection(userMail)
                                    .document(viewModel.year.value.toString())
                                    .collection(viewModel.month.value.toString())
                                    .document(item.tipo).update(mapOf("sumaMes" to sumaMes))
                            }
                        database.collection(userMail)
                            .document("cuentas").get().addOnSuccessListener { document ->
                                val cuentas = document.toObject(CuentasModel::class.java)!!
                                when (item.cuenta) {
                                    "Galicia" -> {
                                        if (item.tipo == "gasto") {
                                            cuentas.Galicia = cuentas.Galicia!! + item.valor!!
                                        } else {
                                            cuentas.Galicia = cuentas.Galicia!! - item.valor!!
                                        }
                                    }
                                    "Nación" -> {
                                        if (item.tipo == "gasto") {
                                            cuentas.Nación = cuentas.Nación!! + item.valor!!
                                        } else {
                                            cuentas.Nación = cuentas.Nación!! - item.valor!!
                                        }
                                    }
                                    "Billetera" -> {
                                        if (item.tipo == "gasto") {
                                            cuentas.Billetera = cuentas.Billetera!! + item.valor!!
                                        } else {
                                            cuentas.Billetera = cuentas.Billetera!! - item.valor!!
                                        }
                                    }
                                }
                                val map = mapOf(
                                    "Galicia" to cuentas.Galicia,
                                    "Billetera" to cuentas.Billetera,
                                    "Nación" to cuentas.Nación
                                )
                                database.collection(userMail)
                                    .document("cuentas").update(
                                        map
                                    )
                            }
                    }
                    viewModel.lista.value.remove(item)
                    if (viewModel.lista.value.size == 0) {
                        binding.noItems.visibility = View.VISIBLE
                        binding.rvLista.visibility = View.GONE
                    }
                    adapter.notifyDataSetChanged()
                }
            }
        }
    }

    private fun getDatos() {
        viewModel.marcaProgresos.value = 0
        if ((activity as MainActivity).isOnline()) {
            val user = (activity as MainActivity).getuser()
            if (user != null) {
                user.mail?.let { userMail ->
                    for (i in 1..31) {
                        database.collection(userMail)
                            .document(viewModel.year.value.toString())
                            .collection(viewModel.month.value.toString())
                            .document("gasto").collection(i.toString()).get().addOnSuccessListener {
                                it.documents.forEach { documents ->
                                    viewModel.lista.value.add(documents.toObject(GastoIngresoModel::class.java)!!)
                                }
                                if (i == 31) {
                                    viewModel.marcaProgresos.value = viewModel.marcaProgresos.value + 1
                                }
                            }.addOnFailureListener {
                                // Nothing
                            }
                    }
                    for (i in 1..31) {
                        database.collection(userMail)
                            .document(viewModel.year.value.toString())
                            .collection(viewModel.month.value.toString())
                            .document("ingreso").collection(i.toString()).get().addOnSuccessListener {
                                it.documents.forEach { documents ->
                                    viewModel.lista.value.add(documents.toObject(GastoIngresoModel::class.java)!!)
                                }
                                if (i == 31) {
                                    viewModel.marcaProgresos.value = viewModel.marcaProgresos.value + 1
                                }
                            }.addOnFailureListener {
                                // Nothing
                            }
                    }
                    for (i in 1..31) {
                        database.collection(userMail)
                            .document(viewModel.year.value.toString())
                            .collection(viewModel.month.value.toString())
                            .document("transferencia").collection(i.toString()).get().addOnSuccessListener {
                                it.documents.forEach { documents ->
                                    viewModel.lista.value.add(documents.toObject(GastoIngresoModel::class.java)!!)
                                }
                                if (i == 31) {
                                    viewModel.marcaProgresos.value = viewModel.marcaProgresos.value + 1
                                }
                            }.addOnFailureListener {
                                // Nothing
                            }
                    }
                }
            }
        }
    }
}
