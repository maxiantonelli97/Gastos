package com.example.gastos.ui.list

import androidx.lifecycle.ViewModel
import com.example.gastos.entity.GastoIngresoModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject

@HiltViewModel
class ListaViewModel @Inject constructor() : ViewModel() {
    val marcaProgresos: MutableStateFlow<Int> = MutableStateFlow(0)
    val lista: MutableStateFlow<ArrayList<GastoIngresoModel>> = MutableStateFlow(arrayListOf())
    var month: MutableStateFlow<Int?> = MutableStateFlow(null)
    var year: MutableStateFlow<Int?> = MutableStateFlow(null)
}
