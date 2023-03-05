package com.example.gastos.entity

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class CategoriasModel(
    val id: String? = "",
    val name: String? = ""
) : Parcelable
