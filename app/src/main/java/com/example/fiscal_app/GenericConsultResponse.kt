package com.example.fiscal_app

import com.google.gson.annotations.SerializedName

class GenericConsultResponse {

    @SerializedName("regularizado")
    var regularizado: Boolean = false

    @SerializedName("placaConsultada")
    var placaConsultada: String? = null
}