package com.example.gastos.ui.home

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor() : ViewModel() {
    val marcaTresProgresos: MutableStateFlow<Int> = MutableStateFlow(0)
    val months = arrayListOf("Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio", "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre")
    var month: MutableStateFlow<Int?> = MutableStateFlow(null)
    var year: MutableStateFlow<Int?> = MutableStateFlow(null)
    var day: MutableStateFlow<Int?> = MutableStateFlow(null)
}
