package com.example.fiscal_app

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
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
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage
import com.google.gson.GsonBuilder
import java.io.ByteArrayOutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class RegisterIrregularityActivity : AppCompatActivity() {
    private var images: Array<ShapeableImageView?> = arrayOf(null,null,null,null)
    private var imageUrls: Array<String?> = arrayOf(null,null,null,null)
    private lateinit var binding: ActivityRegisterIrregularityBinding
    private lateinit var image: ShapeableImageView
    private lateinit var plate: String
    private lateinit var storage: FirebaseStorage
    private lateinit var functions: FirebaseFunctions
    private val gson = GsonBuilder().enableComplexMapKeySerialization().create()

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterIrregularityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        storage = Firebase.storage

        functions = Firebase.functions("southamerica-east1")

        permission.launch(android.Manifest.permission.CAMERA)

        plate = intent.extras?.getString("plate").toString()

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

        binding.uploadImage.setOnClickListener {
            uploadImage()
            println("aaaaaaaaaaaaaaaaaaaaaaaaaaaaporaaaaaaaaaaaaaaaaaaaaaaaaaaaa")
        }
    }

    private val permission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
        if (!isGranted) {
            Snackbar.make(
                binding.root,
                "Sem permissões para acessar a câmera",
                Snackbar.LENGTH_INDEFINITE
            ).show()
        }
    }

    private val startForResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){result: ActivityResult ->
        if(result.resultCode == Activity.RESULT_OK){
            val uriImage = result.data?.data
            image.setImageURI(uriImage)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun uploadImage() {
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

                val uploadTask = newImagesRef.putBytes(data)
                uploadTask.addOnFailureListener {
                    Snackbar.make(
                        binding.root,
                        "Erro ao inserir imagem",
                        Snackbar.LENGTH_INDEFINITE
                    ).show()
                }.addOnSuccessListener { taskSnapshot ->
                    taskSnapshot.metadata?.reference?.downloadUrl?.addOnSuccessListener {
                        imageUrls[count] = it.toString()
                        if(count == 3){
                            registerIrregularity().addOnCompleteListener((OnCompleteListener { task ->
                                if(!task.isSuccessful){
                                    println(task.result)
                                    AlertDialog.Builder(this).setTitle("Erro ao registrar irregularidade")
                                        .setMessage("Ops. Parece que aconteceu um problema aqui do nosso lado. Tente novamente. Se o problema persistir contate o suporte técnico pelo telefone 0800-000-0000")
                                        .setPositiveButton("Tentar novamente") { _, _ ->
                                            //vai faze nada n
                                        }.show()
                                }else{
                                    println(task.result)
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