package com.example.gastos.ui.newSome

import android.app.DatePickerDialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.gastos.MainActivity
import com.example.gastos.R
import com.example.gastos.databinding.FragmentNewSomeBinding
import com.example.gastos.entity.CategoriasModel
import com.example.gastos.entity.CuentasModel
import com.example.gastos.entity.GastoIngresoModel
import com.example.gastos.entity.SumaMesModel
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class NewSomeFragment : Fragment() {

    private var _binding: FragmentNewSomeBinding? = null
    private lateinit var tipo: String
    private val binding get() = _binding!!
    private lateinit var database: FirebaseFirestore
    private lateinit var contexto: Context
    private val viewModel: NewSomeViewModel by viewModels()

    override fun onAttach(context: Context) {
        super.onAttach(context)
        contexto = context
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNewSomeBinding.inflate(inflater, container, false)
        database = FirebaseFirestore.getInstance()
        tipo = arguments?.getString("tipo")!!
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (tipo == "transferencia") {
            binding.spACuenta.visibility = View.VISIBLE
            binding.spCuentas.hint = getString(R.string.de_cuenta)
            binding.spCategorias.visibility = View.GONE
            (activity as MainActivity).setLabel(getString(R.string.transf_label))
            binding.bSend.setBackgroundColor(
                ContextCompat.getColor(
                    contexto,
                    R.color.blue
                )
            )
        } else if (tipo == "gasto") {
            (activity as MainActivity).setLabel(getString(R.string.gasto_label))
            binding.bSend.setBackgroundColor(
                ContextCompat.getColor(
                    contexto,
                    R.color.red
                )
            )
        } else {
            (activity as MainActivity).setLabel(getString(R.string.ingreso_label))
            binding.bSend.setBackgroundColor(
                ContextCompat.getColor(
                    contexto,
                    R.color.green
                )
            )
        }

        startListeners()
        startCollects()
        getDatos()
    }

    private fun getDatos() {
        viewModel.marcaDosProgresos.value = 0
        mostrarProgressBar(true)
        if ((activity as MainActivity).isOnline()) {
            val user = (activity as MainActivity).getuser()
            if (user != null) {
                user.mail?.let { userMail ->
                    if (tipo != "transferencia") {
                        database.collection(userMail).document("categorias").collection(tipo).get()
                            .addOnSuccessListener { query ->
                                if (query.isEmpty) {
                                    error()
                                } else {
                                    val lista: ArrayList<String> = arrayListOf()
                                    query.forEach { queryDocumentSnapshot ->
                                        lista.add(queryDocumentSnapshot.toObject(CategoriasModel::class.java).name!!)
                                    }
                                    val adapter = ArrayAdapter(
                                        requireContext(),
                                        R.layout.item_spinner_layout,
                                        lista
                                    )
                                    (binding.spCategoriasSel as? AutoCompleteTextView)?.setAdapter(
                                        adapter
                                    )
                                    viewModel.marcaDosProgresos.value += 1
                                }
                            }.addOnFailureListener {
                                error()
                            }
                    }
                    database.collection(userMail).document("cuentas").get()
                        .addOnSuccessListener {
                            val lista: ArrayList<String> = arrayListOf()
                            val cuentas = it.toObject(CuentasModel::class.java)
                            if (cuentas?.Billetera != null) {
                                lista.add(getString(R.string.billetera))
                            }
                            if (cuentas?.Galicia != null) {
                                lista.add(getString(R.string.galicia))
                            }
                            if (cuentas?.Nación != null) {
                                lista.add(getString(R.string.nacion))
                            }
                            val adapter =
                                ArrayAdapter(requireContext(), R.layout.item_spinner_layout, lista)
                            (binding.spCuentaSel as? AutoCompleteTextView)?.setAdapter(adapter)
                            if (tipo == "transferencia") {
                                (binding.spACuentaSel as? AutoCompleteTextView)?.setAdapter(adapter)
                                viewModel.marcaDosProgresos.value += 2
                            } else {
                                viewModel.marcaDosProgresos.value += 1
                            }
                        }.addOnFailureListener {
                            error()
                        }
                }
            }
        }
    }

    private fun startCollects() {
        lifecycleScope.launch {
            viewModel.marcaDosProgresos.collect {
                if (it == 2) {
                    mostrarProgressBar(false)
                }
            }
        }
        lifecycleScope.launch {
            viewModel.marcaSendProgresos.collect {
                if (it == 3) {
                    Toast.makeText(contexto, "Movimiento agreagado con éxito", Toast.LENGTH_LONG)
                        .show()
                    findNavController().popBackStack()
                }
            }
        }
    }

    private fun startListeners() {
        val datePicker = DatePickerDialog(contexto)
        binding.tiFecha.setStartIconOnClickListener {
            val inputMethodManager =
                contexto.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            inputMethodManager.hideSoftInputFromWindow(view?.windowToken, 0)
            datePicker.show()
            datePicker.setOnDateSetListener { _, year, month, dayOfMonth ->
                binding.etFecha.setText(getString(R.string.dateFormat, dayOfMonth, month + 1, year))
            }
        }
        binding.bSend.setOnClickListener {
            if (validaciones()) {
                val dia = binding.etFecha.text.toString().split("/")[0]
                val mes = binding.etFecha.text.toString().split("/")[1]
                val anio = binding.etFecha.text.toString().split("/")[2]
                viewModel.marcaSendProgresos.value = 0
                if ((activity as MainActivity).isOnline()) {
                    val user = (activity as MainActivity).getuser()
                    if (user != null) {
                        user.mail?.let { it1 ->
                            if (tipo != "transferencia") {
                                database.collection(it1)
                                    .document(anio)
                                    .collection(mes)
                                    .document(tipo)
                                    .collection(dia)
                                    .add(
                                        GastoIngresoModel(
                                            null,
                                            binding.etMonto.text.toString().toFloat(),
                                            binding.spCategoriasSel.text.toString(),
                                            binding.etFecha.text.toString(),
                                            binding.etDesc.text.toString(),
                                            binding.spCuentaSel.text.toString(),
                                            null,
                                            true,
                                            tipo
                                        )
                                    )
                                    .addOnSuccessListener {
                                        it.update(mapOf("id" to it.id))
                                        viewModel.marcaSendProgresos.value += 1
                                    }.addOnFailureListener {
                                        error()
                                    }

                                database.collection(it1)
                                    .document(anio)
                                    .collection(mes)
                                    .document(tipo).get().addOnSuccessListener {
                                        val suma = it.toObject(SumaMesModel::class.java)
                                        if (suma?.sumaMes != null) {
                                            suma.sumaMes =
                                                suma.sumaMes!! + binding.etMonto.text.toString()
                                                .toFloat()
                                            val map = mapOf("sumaMes" to suma.sumaMes!!)
                                            database.collection(it1)
                                                .document(anio)
                                                .collection(mes)
                                                .document(tipo).update(map).addOnSuccessListener {
                                                    viewModel.marcaSendProgresos.value += 1
                                                }.addOnFailureListener {
                                                    error()
                                                }
                                        } else {
                                            val sumaMes = SumaMesModel(binding.etMonto.text.toString().toFloat())
                                            database.collection(it1)
                                                .document(anio)
                                                .collection(mes)
                                                .document(tipo).set(sumaMes).addOnSuccessListener {
                                                    viewModel.marcaSendProgresos.value += 1
                                                }.addOnFailureListener {
                                                    error()
                                                }
                                        }
                                    }.addOnFailureListener {
                                        error()
                                    }
                            } else {
                                database.collection(it1)
                                    .document(anio)
                                    .collection(mes)
                                    .document(tipo)
                                    .collection(dia)
                                    .add(
                                        GastoIngresoModel(
                                            null,
                                            binding.etMonto.text.toString().toFloat(),
                                            null,
                                            binding.etFecha.text.toString(),
                                            binding.etDesc.text.toString(),
                                            binding.spCuentaSel.text.toString(),
                                            binding.spACuentaSel.text.toString(),
                                            true,
                                            tipo
                                        )
                                    )
                                    .addOnSuccessListener {
                                        it.update(mapOf("id" to it.id))
                                        viewModel.marcaSendProgresos.value += 2
                                    }.addOnFailureListener {
                                        error()
                                    }
                            }
                            database.collection(it1)
                                .document("cuentas")
                                .get().addOnSuccessListener {
                                    val cuentas = it.toObject(CuentasModel::class.java)
                                    if (cuentas != null) {
                                        val map = mutableMapOf<String, Float>()
                                        if (tipo == "gasto") {
                                            if (binding.spCuentaSel.text.toString() == "Galicia") {
                                                map["Galicia"] =
                                                    cuentas.Galicia!! - binding.etMonto.text.toString()
                                                    .toFloat()
                                            } else if (binding.spCuentaSel.text.toString() == "Nación") {
                                                map["Nación"] =
                                                    cuentas.Nación!! - binding.etMonto.text.toString()
                                                    .toFloat()
                                            } else {
                                                map["Billetera"] =
                                                    cuentas.Billetera!! - binding.etMonto.text.toString()
                                                    .toFloat()
                                            }
                                        } else if (tipo == "ingreso") {
                                            if (binding.spCuentaSel.text.toString() == "Galicia") {
                                                map["Galicia"] =
                                                    cuentas.Galicia!! + binding.etMonto.text.toString()
                                                    .toFloat()
                                            } else if (binding.spCuentaSel.text.toString() == "Nación") {
                                                map["Nación"] =
                                                    cuentas.Nación!! + binding.etMonto.text.toString()
                                                    .toFloat()
                                            } else {
                                                map["Billetera"] =
                                                    cuentas.Billetera!! + binding.etMonto.text.toString()
                                                    .toFloat()
                                            }
                                        } else {
                                            if (binding.spCuentaSel.text.toString() == "Galicia") {
                                                map["Galicia"] =
                                                    cuentas.Galicia!! - binding.etMonto.text.toString()
                                                    .toFloat()
                                            } else if (binding.spCuentaSel.text.toString() == "Nación") {
                                                map["Nación"] =
                                                    cuentas.Nación!! - binding.etMonto.text.toString()
                                                    .toFloat()
                                            } else {
                                                map["Billetera"] =
                                                    cuentas.Billetera!! - binding.etMonto.text.toString()
                                                    .toFloat()
                                            }
                                            if (binding.spACuentaSel.text.toString() == "Galicia") {
                                                map["Galicia"] =
                                                    cuentas.Galicia!! + binding.etMonto.text.toString()
                                                    .toFloat()
                                            } else if (binding.spACuentaSel.text.toString() == "Nación") {
                                                map["Nación"] =
                                                    cuentas.Nación!! + binding.etMonto.text.toString()
                                                    .toFloat()
                                            } else {
                                                map["Billetera"] =
                                                    cuentas.Billetera!! + binding.etMonto.text.toString()
                                                    .toFloat()
                                            }
                                        }
                                        database.collection(it1)
                                            .document("cuentas")
                                            .update(map as Map<String, Float>)
                                            .addOnSuccessListener {
                                                viewModel.marcaSendProgresos.value += 1
                                            }.addOnFailureListener {
                                                error()
                                            }
                                    }
                                }.addOnFailureListener {
                                    error()
                                }
                        }
                    } else {
                        error()
                    }
                }
            }
        }
    }

    private fun error() {
        mostrarProgressBar(false)
        findNavController().navigate(R.id.nav_errorFragment)
    }

    private fun validaciones(): Boolean {
        var validaciones = true
        if (binding.etMonto.text.toString() == "") {
            validaciones = false
            binding.tiMonto.error = getString(R.string.no_vacio)
        } else if (binding.etMonto.text.toString() == "0") {
            validaciones = false
            binding.tiMonto.error = getString(R.string.no_cero)
        } else {
            binding.tiMonto.error = null
        }

        if (binding.etFecha.text.toString() == "") {
            validaciones = false
            binding.tiFecha.error = getString(R.string.no_vacio)
        } else {
            binding.tiFecha.error = null
        }

        if (tipo != "transferencia") {
            if (binding.spCategoriasSel.text.toString() == "") {
                validaciones = false
                binding.spCategorias.error = getString(R.string.no_vacio)
            } else {
                binding.spCategorias.error = null
            }
        }

        if (binding.spCuentaSel.text.toString() == "") {
            validaciones = false
            binding.spCuentas.error = getString(R.string.no_vacio)
        } else {
            binding.spCuentas.error = null
        }
        if (tipo == "transferencia") {
            if (binding.spACuentaSel.text.toString() == binding.spCuentaSel.text.toString()) {
                validaciones = false
                binding.spACuenta.error = getString(R.string.cuenta_no_igual)
            } else {
                binding.spACuenta.error = null
            }
        }
        return validaciones
    }

    private fun mostrarProgressBar(mostrar: Boolean) {
        binding.progressBar.isVisible = mostrar
        binding.newSomeLayout.isVisible = !mostrar
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
