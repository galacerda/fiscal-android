package com.example.fiscal_app

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.telephony.TelephonyManager
import android.view.View
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.fiscal_app.databinding.ActivityMainBinding
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import com.google.gson.GsonBuilder
import java.util.*


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    private lateinit var auth: FirebaseAuth

    private lateinit var functions: FirebaseFunctions
    private val gson = GsonBuilder().enableComplexMapKeySerialization().create()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = Firebase.auth

        functions = Firebase.functions("southamerica-east1")

        binding.consultVehicleButton.setOnClickListener {
            sendToConsultPlateActivity()
        }

        binding.consultMapButton.setOnClickListener {
            sendToConsultMapActivity()
        }

        binding.createIrregularityButton.setOnClickListener {
            sendToRegisterIrregularityActivity()
        }
    }

    override fun onStart() {
        super.onStart()
        val currentUser = auth.currentUser
        if(currentUser != null){
            binding.consultMapButton.visibility = View.VISIBLE
            binding.consultVehicleButton.visibility = View.VISIBLE
            binding.createIrregularityButton.visibility = View.VISIBLE
        }else{
            val deviceId = getDeviceId()
            if(deviceId != null){
            getAuthUser(deviceId).addOnCompleteListener((OnCompleteListener { task ->
                if(!task.isSuccessful){
                    val e = task.exception
                    if (e is FirebaseFunctionsException) {
                        val details = e.details
                        AlertDialog.Builder(this).setTitle("Erro").setMessage(details.toString()).setPositiveButton("Okay"){ _, _->
                            //vai faze nada n
                        }.show()
                    }

                }else{
                    println(task.result)
                    val result = gson.fromJson(task.result, FunctionsGenericResponse::class.java)
                    val payload = gson.fromJson(result.payload.toString(), GenericUserResponse::class.java)
                    if(payload?.email != null && payload?.password != null){
                        signIn(payload.email as String,payload.password as String)
                    }
                }
            }))
        }}
    }

    private fun signIn(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    binding.consultMapButton.visibility = View.VISIBLE
                    binding.consultVehicleButton.visibility = View.VISIBLE
                    binding.createIrregularityButton.visibility = View.VISIBLE
                }else{
                    AlertDialog.Builder(this).setTitle("Erro ao logar").setMessage("Ocorreu um erro, tente novamente.").setPositiveButton("Okay"){ _, _->
                        //vai faze nada n
                    }.show()
                }
            }

    }

    private fun getDeviceId(): String? {
        return Settings.Secure.getString(applicationContext.contentResolver,Settings.Secure.ANDROID_ID)
    }

    private fun getAuthUser(deviceId: String?): Task<String> {
        val data = hashMapOf(
            "deviceId" to deviceId,
        )
        return functions
            .getHttpsCallable("getUserAuth")
            .call(data)
            .continueWith { task ->
                val res = gson.toJson(task.result?.data)
                res
            }
    }

    private fun sendToConsultPlateActivity(){
        val intent = Intent(this,ConsultVehicleActivity::class.java)
        startActivity(intent)
    }

    private fun sendToRegisterIrregularityActivity(){
        val intent = Intent(this,RegisterIrregularityActivity::class.java)
        startActivity(intent)
    }

    private fun sendToConsultMapActivity(){
        val intent = Intent(this,ConsultVehicleActivity::class.java)
        startActivity(intent)
    }
}