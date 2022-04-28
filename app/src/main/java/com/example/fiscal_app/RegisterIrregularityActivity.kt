package com.example.fiscal_app

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.ActivityResult
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import com.example.fiscal_app.databinding.ActivityRegisterIrregularityBinding
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import com.google.firebase.storage.ktx.storage
import com.google.gson.GsonBuilder
import java.io.ByteArrayOutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class RegisterIrregularityActivity : AppCompatActivity() {
    private var images: Array<ShapeableImageView?> = arrayOf(null, null, null, null)
    private var imageUrls: Array<String?> = arrayOf(null, null, null, null)
    private lateinit var binding: ActivityRegisterIrregularityBinding
    private lateinit var image: ShapeableImageView
    private var plate: String = ""
    private lateinit var storage: FirebaseStorage
    private lateinit var functions: FirebaseFunctions
    private var type: String = "exceededTime"
    private val gson = GsonBuilder().enableComplexMapKeySerialization().create()
    private var isStarted: Boolean = false
    private var isFinished: Boolean = false
    private var uploadTasks:Array<UploadTask?> = arrayOf(null,null,null,null)

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterIrregularityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        storage = Firebase.storage

        functions = Firebase.functions("southamerica-east1")

        permission.launch(android.Manifest.permission.CAMERA)

        plate = intent.extras?.getString("plate").toString()

        if (plate == "no-plate") {
            binding.wrapperImgsLL.visibility = View.GONE
            binding.sendButton.visibility = View.GONE
            binding.wrapperOptions.visibility = View.VISIBLE
        }
        binding.imageOne.setOnClickListener {
            image = binding.imageOne
            images[0] = image
            startForResult.launch(Intent(this, TakePictureActivity::class.java))
        }
        binding.imageTwo.setOnClickListener {
            image = binding.imageTwo
            images[1] = image
            startForResult.launch(Intent(this, TakePictureActivity::class.java))
        }
        binding.imageThree.setOnClickListener {
            image = binding.imageThree
            images[2] = image
            startForResult.launch(Intent(this, TakePictureActivity::class.java))
        }
        binding.imageFour.setOnClickListener {
            image = binding.imageFour
            images[3] = image
            startForResult.launch(Intent(this, TakePictureActivity::class.java))
        }

        binding.sendButton.setOnClickListener {
            uploadImage()
            binding.sendButton.text = "Registrando irregularidade"
            binding.sendButton.isEnabled = false
            binding.progressBar.visibility = View.VISIBLE
        }

        binding.sendButtonOptionOne.setOnClickListener {
            type = "notPark"
            binding.wrapperImgsLL.visibility = View.VISIBLE
            binding.sendButton.visibility = View.VISIBLE
            binding.wrapperOptions.visibility = View.GONE
        }

        binding.sendButtonOptionTwo.setOnClickListener {
            var intent = Intent(this, ConsultVehicleActivity::class.java)
            intent.putExtra("type", "register")
            startActivity(intent)
        }
    }

    override fun onPause(){
        super.onPause()
        if(isStarted){
            uploadTasks[0]?.cancel()
            uploadTasks[1]?.cancel()
            uploadTasks[2]?.cancel()
            uploadTasks[3]?.cancel()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onResume() {
        super.onResume()
        if(isStarted && !isFinished){
            AlertDialog.Builder(this).setTitle("Erro")
                .setMessage("É necessário realizar o registro novamente, pois o processo foi interrompido.")
                .setPositiveButton("Prosseguir") { _, _ ->

                }.show()
            for(item in imageUrls){
                if(item != null) {
                    val photoRef = Firebase.storage.getReferenceFromUrl(item as String)
                    photoRef.delete().addOnFailureListener {
                        Snackbar.make(
                            binding.root,
                            "Erro ao deletar imagens, refaça todo o processo!",
                            Snackbar.LENGTH_LONG
                        ).show()
                    }
                }
            }
            binding.progressBar.visibility = View.GONE
            binding.sendButton.text = "Confirmar"
            binding.sendButton.isEnabled = true
        }
    }

    private val permission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (!isGranted) {
                Snackbar.make(
                    binding.root,
                    "Sem permissões para acessar a câmera",
                    Snackbar.LENGTH_INDEFINITE
                ).show()
            }
        }

    private val startForResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) {
                val uriImage = result.data?.data
                image.setImageURI(uriImage)
            }
        }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun uploadImage() {
        isStarted = true
        for ((count, item) in images.withIndex()) {
            if (item != null) {
                val current = LocalDateTime.now()
                val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
                val formatted = current.format(formatter)
                val storageRef = storage.reference
                val bitmap = (item.drawable as BitmapDrawable).bitmap
                val newRef = storageRef.child("image$count.jpg")

                val newImagesRef = storageRef.child("images/$formatted/$plate/$newRef")

                val baos = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
                val data = baos.toByteArray()

                uploadTasks[count] = newImagesRef.putBytes(data)
                uploadTasks[count]?.addOnFailureListener {
                    Snackbar.make(
                        binding.root,
                        "Ops. Parece que aconteceu um problema aqui do nosso lado. Tente novamente. Se o problema persistir contate o suporte técnico pelo telefone 0800-000-0000",
                        Snackbar.LENGTH_INDEFINITE
                    ).show()
                    binding.sendButton.text = "Confirmar"
                    binding.sendButton.isClickable = true
                    binding.progressBar.visibility = View.GONE
                }?.addOnSuccessListener { taskSnapshot ->
                    taskSnapshot.metadata?.reference?.downloadUrl?.addOnSuccessListener {
                        imageUrls[count] = it.toString()
                        if (count == 3) {
                            registerIrregularity().addOnCompleteListener((OnCompleteListener { task ->
                                binding.sendButton.isEnabled = true
                                binding.progressBar.visibility = View.GONE
                                isFinished = true
                                if (!task.isSuccessful) {
                                    val e = task.exception
                                    if (e is FirebaseFunctionsException) {
                                        val message = e.message
                                        AlertDialog.Builder(this).setTitle("Erro")
                                            .setMessage(message.toString())
                                            .setPositiveButton("Ok") { _, _ ->

                                            }.show()
                                    }
                                    binding.sendButton.text = "Confirmar"
                                } else {
                                    binding.cardView.visibility = View.VISIBLE
                                    binding.wrapperImgsLL.visibility = View.GONE
                                    binding.sendButton.text = "Menu"
                                    binding.sendButton.setOnClickListener {
                                        startActivity(Intent(this, MainActivity::class.java))
                                    }
                                }
                            }))
                        }
                    }
                }
            }
        }
    }


    private fun registerIrregularity(): Task<String> {
        val data = hashMapOf(
            "plate" to plate,
            "imageOne" to imageUrls[0],
            "imageTwo" to imageUrls[1],
            "imageThree" to imageUrls[2],
            "imageFour" to imageUrls[3],
            "type" to type
        )
        return functions
            .getHttpsCallable("registerIrregularity")
            .call(data)
            .continueWith { task ->
                val res = gson.toJson(task.result?.data)
                res
            }
    }
}