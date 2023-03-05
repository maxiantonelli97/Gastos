package com.example.gastos.entity

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class CuentasModel(
    var Billetera: Float? = 0.0f,
    var Nación: Float? = 0.0f,
    var Galicia: Float? = 0.0f
) : Parcelable
