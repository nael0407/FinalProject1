package com.example.finalproject

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var imagePreview: ImageView
    private lateinit var layoutHasil: LinearLayout
    private lateinit var textPenyakit: TextView
    private lateinit var textKeyakinan: TextView
    private lateinit var textSaran: TextView
    private lateinit var btnPilihGambar: Button
    private lateinit var btnDeteksi: Button

    private var selectedImageFile: File? = null

    companion object {
        const val REQUEST_IMAGE_PICK = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        imagePreview = findViewById(R.id.imagePreview)
        layoutHasil = findViewById(R.id.layoutHasil)
        textPenyakit = findViewById(R.id.textPenyakit)
        textKeyakinan = findViewById(R.id.textKeyakinan)
        textSaran = findViewById(R.id.textSaran)
        btnPilihGambar = findViewById(R.id.btnPilihGambar)
        btnDeteksi = findViewById(R.id.btnDeteksi)

        btnPilihGambar.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, REQUEST_IMAGE_PICK)
        }

        btnDeteksi.setOnClickListener {
            selectedImageFile?.let { file ->
                uploadImageToApi(file)
            } ?: Toast.makeText(this, "Pilih gambar terlebih dahulu", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_IMAGE_PICK && resultCode == Activity.RESULT_OK && data != null) {
            val imageUri = data.data
            imagePreview.setImageURI(imageUri)
            imagePreview.visibility = View.VISIBLE

            selectedImageFile = File(getRealPathFromURI(imageUri!!))

            layoutHasil.visibility = View.GONE
            textPenyakit.text = ""
            textKeyakinan.text = ""
            textSaran.text = ""
        }
    }

    private fun getRealPathFromURI(uri: Uri): String {
        var path = ""
        val projection = arrayOf(MediaStore.Images.Media.DATA)
        val cursor = contentResolver.query(uri, projection, null, null, null)
        if (cursor != null) {
            cursor.moveToFirst()
            val columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            path = cursor.getString(columnIndex)
            cursor.close()
        }
        return path
    }

    private fun uploadImageToApi(imageFile: File) {
        val requestFile = imageFile.asRequestBody("image/*".toMediaTypeOrNull())
        val body = MultipartBody.Part.createFormData("file", imageFile.name, requestFile)

        val call = ApiClient.instance.predictImage(body)
        call.enqueue(object : Callback<PredictionResponse> {
            override fun onResponse(call: Call<PredictionResponse>, response: Response<PredictionResponse>) {
                if (response.isSuccessful) {
                    val result = response.body()
                    result?.let {
                        val confidencePercent = it.confidence * 100
                        val formattedConfidence = String.format(Locale.US, "%.2f", confidencePercent)

                        textPenyakit.text = "Penyakit: ${it.label}"
                        textKeyakinan.text = "Keyakinan: $formattedConfidence%"
                        textSaran.text = "Saran Pengobatan: ${it.suggestion}"
                        layoutHasil.visibility = View.VISIBLE
                    }
                } else {
                    Toast.makeText(this@MainActivity, "Gagal: ${response.code()}", Toast.LENGTH_SHORT).show()
                    layoutHasil.visibility = View.GONE
                }
            }

            override fun onFailure(call: Call<PredictionResponse>, t: Throwable) {
                Toast.makeText(this@MainActivity, "Error: ${t.localizedMessage}", Toast.LENGTH_SHORT).show()
                layoutHasil.visibility = View.GONE
            }
        })
    }
}