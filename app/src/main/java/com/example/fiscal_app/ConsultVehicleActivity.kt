package com.example.fiscal_app

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.fiscal_app.databinding.ActivityConsultVehicleBinding
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import com.google.gson.GsonBuilder

class ConsultVehicleActivity : AppCompatActivity() {
    private lateinit var binding: ActivityConsultVehicleBinding
    private lateinit var functions: FirebaseFunctions
    private val gson = GsonBuilder().enableComplexMapKeySerialization().create()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConsultVehicleBinding.inflate(layoutInflater)
        setContentView(binding.root)

        functions = Firebase.functions("southamerica-east1")

        binding.consultPlateButton.setOnClickListener {

            val plateHelperText = verifyPlate(binding.plateEditText.text.toString())
            binding.plateContainer.helperText = plateHelperText

            if (plateHelperText == null) {
            consultPlate(binding.plateEditText.text.toString()).addOnCompleteListener((OnCompleteListener { task ->
                if (!task.isSuccessful) {
                    val e = task.exception
                    if (e is FirebaseFunctionsException) {
                        val code = e.code
                        val details = e.details
                    }
                } else {
                    println(task.result)
                    Snackbar.make(
                        it, "Placa consultada",
                        Snackbar.LENGTH_LONG
                    ).show();
                }
            }))
        }
        }
    }

    private fun verifyPlate(plate: String):String? {
        if(plate.length > 8 || plate.length < 7) {
            return "Placa inválida"
        }
        var plateTrated = plate.replace("-","");

        val re = Regex("[A-Z]{3}[0-9]{4}")

        val verify = plateTrated.matches(re)

        if(!verify) {
            return "Placa inválida"
        }
        return null
    }

    private fun consultPlate(plate: String): Task<String> {


        val data = hashMapOf(
            "plate" to plate,
        )
        return functions
            .getHttpsCallable("consultPlate")
            .call(data)
            .continueWith { task ->
                val res = gson.toJson(task.result?.data)
                res
            }
    }
}