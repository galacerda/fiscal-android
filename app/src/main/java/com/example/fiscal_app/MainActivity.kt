package com.example.fiscal_app

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.fiscal_app.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

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