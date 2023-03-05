package com.example.gastos.entity

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class GastoIngresoModel(
    val id: String? = "",
    val valor: Float? = 0.0f,
    val categoria: String? = "",
    val fecha: String = "",
    val descripcion: String? = "",
    val cuenta: String = "",
    val acuenta: String? = "",
    val realizado: Boolean = true,
    val tipo: String = ""
) : Parcelable
