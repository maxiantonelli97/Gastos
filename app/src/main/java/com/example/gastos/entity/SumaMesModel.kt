package com.example.gastos.entity

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class SumaMesModel(
    var sumaMes: Float? = 0.0f
) : Parcelable
