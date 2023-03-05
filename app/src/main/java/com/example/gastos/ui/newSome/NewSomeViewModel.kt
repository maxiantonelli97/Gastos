package com.example.gastos.ui.newSome

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject

@HiltViewModel
class NewSomeViewModel @Inject constructor() : ViewModel() {
    val marcaDosProgresos: MutableStateFlow<Int> = MutableStateFlow(0)
    val marcaSendProgresos: MutableStateFlow<Int> = MutableStateFlow(0)
}
