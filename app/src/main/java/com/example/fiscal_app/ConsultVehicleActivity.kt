package com.example.fiscal_app

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
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
    private var type: String = "consult"

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConsultVehicleBinding.inflate(layoutInflater)
        setContentView(binding.root)

        type = intent.extras?.getString("type").toString()

        if (type == "register") {
            binding.consultPlateButton.text = "Continuar"
        }

        functions = Firebase.functions("southamerica-east1")

        binding.consultPlateButton.setOnClickListener {
            val plateHelperText = verifyPlate(binding.plateEditText.text.toString())
            binding.plateContainer.helperText = plateHelperText

            if (isOnline(applicationContext)) {
                if (plateHelperText == null) {
                    if (type == "register") {
                        val intent = Intent(this, RegisterIrregularityActivity::class.java)
                        intent.putExtra("plate", binding.plateEditText.text.toString())
                        startActivity(intent)
                    } else {
                        consultPlate(binding.plateEditText.text.toString()).addOnCompleteListener(
                            (OnCompleteListener { task ->
                                if (!task.isSuccessful) {
                                    AlertDialog.Builder(this).setTitle("Erro ao consultar")
                                        .setMessage("Ops. Parece que aconteceu um problema aqui do nosso lado. Tente novamente. Se o problema persistir contate o suporte técnico pelo telefone 0800-000-0000")
                                        .setPositiveButton("Tentar novamente") { _, _ ->
                                            //vai faze nada n
                                        }.show()
                                } else {
                                    println(task.result)
                                    val result =
                                        gson.fromJson(
                                            task.result,
                                            FunctionsGenericResponse::class.java
                                        )
                                    println(result.payload.toString())
                                    val payload = gson.fromJson(
                                        result.payload.toString(),
                                        GenericConsultResponse::class.java
                                    )
                                    if (payload.regularizado) {
                                        updateUI("regularity", result.message)
                                    } else {
                                        updateUI("irregularity", result.message)
                                    }
                                }
                            })
                        )
                    }
                }
            } else {
                AlertDialog.Builder(this).setTitle("Problemas na conexão")
                    .setMessage("Sem conexão com a internet")
                    .setPositiveButton("Tentar novamente") { _, _ ->
                    }.show()
            }
        }

    }

    @RequiresApi(Build.VERSION_CODES.M)
    fun isOnline(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (connectivityManager != null) {
            val capabilities =
                connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
            if (capabilities != null) {
                if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                    Log.i("Internet", "NetworkCapabilities.TRANSPORT_CELLULAR")
                    return true
                } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                    Log.i("Internet", "NetworkCapabilities.TRANSPORT_WIFI")
                    return true
                } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
                    Log.i("Internet", "NetworkCapabilities.TRANSPORT_ETHERNET")
                    return true
                }
            }
        }
        return false
    }

    private fun verifyPlate(plate: String): String? {
        if (plate.length > 8 || plate.length < 7) {
            return "Placa inválida"
        }
        var plateTrated = plate.replace("-", "");

        val re = Regex("[A-Z]{3}[0-9]{4}")

        val verify = plateTrated.matches(re)

        if (!verify) {
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

    private fun updateUI(type: String, message: String?) {
        hideOrShowConsultPlate(true)
        binding.cardView.visibility = View.VISIBLE
        binding.firstButton.visibility = View.VISIBLE
        binding.secondButton.visibility = View.VISIBLE
        if (type == "regularity") {
            binding.messageTextView.text = message
            regularityActionButtons()
        } else {
            binding.messageTextView.text = "$message \n Gostaria de registrar uma irregularidade?"
            irregularityActionButtons()
        }
    }

    private fun hideOrShowConsultPlate(isHide: Boolean) {
        if (isHide) {
            binding.plateContainer.visibility = View.GONE
            binding.consultPlateButton.visibility = View.GONE
        } else {
            binding.firstButton.visibility = View.GONE
            binding.secondButton.visibility = View.GONE
            binding.cardView.visibility = View.GONE
            binding.plateContainer.visibility = View.VISIBLE
            binding.consultPlateButton.visibility = View.VISIBLE
        }
    }

    private fun irregularityActionButtons() {
        binding.firstButton.text = "Sim"
        binding.secondButton.text = "Não"
        binding.firstButton.setOnClickListener {
            val intent = Intent(this, RegisterIrregularityActivity::class.java)
            intent.putExtra("plate", binding.plateEditText.text.toString())
            startActivity(intent)
        }
        binding.secondButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
    }

    private fun regularityActionButtons() {
        binding.firstButton.text = "Consultar outro veiculo"
        binding.secondButton.text = "Retornar ao menu"
        binding.firstButton.setOnClickListener {
            hideOrShowConsultPlate(false)
        }
        binding.secondButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
    }
}