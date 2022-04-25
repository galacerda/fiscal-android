package com.example.fiscal_app

import com.google.gson.annotations.SerializedName

class GenericUserResponse {
        @SerializedName("email")
        var email: String? = null;

        @SerializedName("password")
        var password: String? = null;
}