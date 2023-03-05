package com.example.gastos.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.gastos.MainActivity
import com.example.gastos.R
import com.example.gastos.databinding.FragmentHomeBinding
import com.example.gastos.entity.CuentasModel
import com.example.gastos.entity.SumaMesModel
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.Calendar

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private lateinit var binding: FragmentHomeBinding
    private lateinit var database: FirebaseFirestore
    private var botonesVisibles = true
    private val viewModel: HomeViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHomeBinding.inflate(inflater, container, false)
        database = FirebaseFirestore.getInstance()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val c = Calendar.getInstance()

        viewModel.year.value = c.get(Calendar.YEAR)
        viewModel.month.value = c.get(Calendar.MONTH) + 1
        viewModel.day.value = c.get(Calendar.DAY_OF_MONTH)
        startListeners()
        startCollects()
        traerDatosCuentas()
    }

    override fun onResume() {
        super.onResume()
        binding.tvMonth.text = viewModel.months[viewModel.month.value!! - 1]
        binding.tvYear.text = viewModel.year.value.toString()
        traerDatosCuentas()
        traerDatos(viewModel.year.value.toString(), viewModel.month.value.toString())
    }

    private fun traerDatosCuentas() {
        mostrarProgressBar(true)
        val user = (activity as MainActivity).getuser()
        if (user != null) {
            user.mail?.let { userMail ->
                database.collection(userMail)
                    .document("cuentas").get().addOnSuccessListener {
                        it.toObject(CuentasModel::class.java).let { cuentas ->
                            var total = 0.0f
                            if (cuentas?.Galicia != null) {
                                total += cuentas.Galicia!!
                                binding.tvBankMontoGalicia.text =
                                    getString(R.string.monto_pesos, cuentas.Galicia)
                            } else {
                                binding.tvBankMontoGalicia.text = "?"
                            }
                            if (cuentas?.Nación != null) {
                                total += cuentas.Nación!!
                                binding.tvBankMontoNacion.text =
                                    getString(R.string.monto_pesos, cuentas.Nación)
                            } else {
                                binding.tvBankMontoNacion.text = "?"
                            }
                            if (cuentas?.Billetera != null) {
                                total += cuentas.Billetera!!
                                binding.tvMonto.text =
                                    getString(R.string.monto_pesos, cuentas.Billetera)
                                binding.tvWalletMonto.text =
                                    getString(R.string.monto_pesos, cuentas.Billetera)
                            } else {
                                binding.tvWalletMonto.text = "?"
                            }
                            binding.tvTotalMonto.text = getString(R.string.monto_pesos, total)
                            viewModel.marcaTresProgresos.value += 1
                        }
                        traerDatos(viewModel.year.value.toString(), viewModel.month.value.toString())
                    }.addOnFailureListener {
                        mostrarProgressBar(false)
                        findNavController().navigate(R.id.nav_errorFragment)
                    }
            }
        }
    }

    private fun traerDatos(year: String, month: String) {
        viewModel.marcaTresProgresos.value = 0
        mostrarProgressBar(true)
        val user = (activity as MainActivity).getuser()
        if (user != null) {
            user.mail?.let { userMail ->
                database.collection(userMail).document(year).collection(month)
                    .document("gasto").get().addOnSuccessListener {
                        it.toObject(SumaMesModel::class.java).let { gastoMes ->
                            if (gastoMes != null) {
                                binding.tvGastos.text =
                                    getString(R.string.monto_pesos, gastoMes.sumaMes)
                            } else {
                                binding.tvGastos.text = "?"
                            }

                            viewModel.marcaTresProgresos.value += 1
                        }
                    }.addOnFailureListener {
                        mostrarProgressBar(false)
                        findNavController().navigate(R.id.nav_errorFragment)
                    }
                database.collection(userMail).document(year).collection(month)
                    .document("ingreso").get().addOnSuccessListener {
                        it.toObject(SumaMesModel::class.java).let { ingresoMes ->
                            if (ingresoMes != null) {
                                binding.tvIngresos.text =
                                    getString(R.string.monto_pesos, ingresoMes.sumaMes)
                            } else {
                                binding.tvIngresos.text = "?"
                            }
                            viewModel.marcaTresProgresos.value += 1
                        }
                    }.addOnFailureListener {
                        mostrarProgressBar(false)
                        findNavController().navigate(R.id.nav_errorFragment)
                    }
            }
        } else {
            (activity as MainActivity).usuarioNoLogueado()
        }
    }

    private fun startListeners() {
        binding.swipe.setOnRefreshListener {
            traerDatosCuentas()
            traerDatos(viewModel.year.value.toString(), viewModel.month.value.toString())
        }
        binding.bNewSome.setOnClickListener {
            binding.bNewGasto.isVisible = botonesVisibles
            binding.bNewIngreso.isVisible = botonesVisibles
            binding.bNewTransf.isVisible = botonesVisibles
            botonesVisibles = !botonesVisibles
        }
        binding.bNewGasto.setOnClickListener {
            val bundle = Bundle()
            bundle.putString("tipo", "gasto")
            findNavController().navigate(R.id.nav_NewGastoFragment, bundle)
        }

        binding.bNewTransf.setOnClickListener {
            val bundle = Bundle()
            bundle.putString("tipo", "transferencia")
            findNavController().navigate(R.id.nav_NewGastoFragment, bundle)
        }

        binding.bNewIngreso.setOnClickListener {
            val bundle = Bundle()
            bundle.putString("tipo", "ingreso")
            findNavController().navigate(R.id.nav_NewGastoFragment, bundle)
        }

        binding.llIngresosGastos.setOnClickListener {
            val bundle = Bundle()
            bundle.putInt("year", viewModel.year.value!!)
            bundle.putInt("month", viewModel.month.value!!)
            findNavController().navigate(R.id.nav_ListaFragment, bundle)
        }

        binding.tvBack.setOnClickListener {
            if (viewModel.month.value == 0) {
                viewModel.year.value = viewModel.year.value!! - 1
                viewModel.month.value = 11
                binding.tvYear.text = viewModel.year.value.toString()
            } else {
                viewModel.month.value = viewModel.month.value!! - 1
            }
            binding.tvMonth.text = viewModel.months[viewModel.month.value!! - 1]
        }
        binding.tvNext.setOnClickListener {
            if (viewModel.month.value == 11) {
                viewModel.year.value = viewModel.year.value!! + 1
                viewModel.month.value = 0
                binding.tvYear.text = viewModel.year.value.toString()
            } else {
                viewModel.month.value = viewModel.month.value!! + 1
            }
            binding.tvMonth.text = viewModel.months[viewModel.month.value!! - 1]
        }
    }

    private fun startCollects() {
        lifecycleScope.launch {
            viewModel.marcaTresProgresos.collect {
                if (it == 2) {
                    binding.swipe.isRefreshing = false
                    mostrarProgressBar(false)
                    viewModel.marcaTresProgresos.value = 0
                }
            }
        }
        lifecycleScope.launch {
            viewModel.month.collect {
                traerDatos(viewModel.year.value.toString(), viewModel.month.value.toString())
            }
        }
    }

    private fun mostrarProgressBar(mostrar: Boolean) {
        binding.bNewSome.isClickable = !mostrar
        binding.tvNext.isClickable = !mostrar
        binding.tvBack.isClickable = !mostrar
        binding.llIngresosGastos.isClickable = !mostrar
        binding.progressBar.isVisible = mostrar
    }
}
