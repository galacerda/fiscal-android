package com.example.fiscal_app

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.example.fiscal_app.databinding.ActivityTakePictureBinding
import com.google.common.util.concurrent.ListenableFuture
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class TakePictureActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTakePictureBinding
    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private lateinit var cameraSelector: CameraSelector
    private lateinit var imgCaptureExecutor: ExecutorService
    private var imageCapture:ImageCapture? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide();
        binding = ActivityTakePictureBinding.inflate(layoutInflater)
        setContentView(binding.root)

        cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
        imgCaptureExecutor = Executors.newSingleThreadExecutor()

        startCamera()

        binding.btnTakePhoto.setOnClickListener{
            takePhoto()
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                blinkPreview()
            }
        }
    }

    private fun startCamera(){
        cameraProviderFuture.addListener({
            imageCapture = ImageCapture.Builder().build()

            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also{
                it.setSurfaceProvider(binding.cameraPreview.surfaceProvider)
            }

            try{
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)

            }catch(e: Exception){
                Log.e("Camera Preview", "Falha ao abrir a camera")
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhoto(){
        imageCapture?.let {
            val fileName = "FOTO_JPEG_${System.currentTimeMillis()}"
            val file = File(externalMediaDirs[0], fileName)
            val outPutFilesOptions = ImageCapture.OutputFileOptions.Builder(file).build()

            it.takePicture(
                outPutFilesOptions,
                imgCaptureExecutor,
                object: ImageCapture.OnImageSavedCallback{
                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                        Log.i("CameraPreview","A imagem foi salva no diretório: ${file.toURI()}")
                        val intent = Intent()
                        intent.data = Uri.parse(file.toString())
                        setResult(Activity.RESULT_OK,intent)
                        finish()
                    }
                    override fun onError(exception: ImageCaptureException) {
                        Toast.makeText(binding.root.context, "Erro ao salvar foto", Toast.LENGTH_LONG).show()
                        Log.e("CameraPreview", "Exceção ao gravar arquivo da foto: $exception")
                    }
                }
            )
        }
    }
    @RequiresApi(Build.VERSION_CODES.M)
    private fun blinkPreview(){
        binding.root.postDelayed({
            binding.root.foreground = ColorDrawable(Color.WHITE)
            binding.root.postDelayed({
                binding.root.foreground = null
            }, 50)
        },100)
    }
}