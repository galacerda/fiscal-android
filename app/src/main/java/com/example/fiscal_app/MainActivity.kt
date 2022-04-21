package com.example.fiscal_app

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.fiscal_app.databinding.ActivityMainBinding
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import com.google.gson.GsonBuilder

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var functions: FirebaseFunctions
    private val gson = GsonBuilder().enableComplexMapKeySerialization().create()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        functions = Firebase.functions("southamerica-east1")

        binding.createAccountButton.setOnClickListener{
            consultPlate(binding.plateEditText.text.toString()).addOnCompleteListener((OnCompleteListener { task ->
                if(!task.isSuccessful){
                    val e = task.exception
                    if(e is FirebaseFunctionsException){
                        val code = e.code
                        val details = e.details
                    }
                }else{
                    println(task.result)
                    Snackbar.make(it, "Produto cadastrado: ",
                        Snackbar.LENGTH_LONG).show();
                }
            }))
        }
    }

    private fun consultPlate(plate: String): Task<String> {
        println("eitaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa")
        val data = hashMapOf(
            "plate" to plate,
        )
        return functions
            .getHttpsCallable("consultPlate")
            .call(data)
            .continueWith { task ->
                // convertendo o resultado em string Json válida
                val res = gson.toJson(task.result?.data)
                res
            }
    }
}