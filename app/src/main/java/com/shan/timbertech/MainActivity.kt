package com.shan.timbertech

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainActivity : ComponentActivity() {

    private lateinit var previewView: PreviewView
    private lateinit var captureButton: ImageButton
    private lateinit var reTakeImage: ImageButton
    private lateinit var logOutBtn: ImageButton
    private lateinit var analyseButton: Button
    private var imageCapture: ImageCapture? = null

    private lateinit var cameraLay: ConstraintLayout
    private lateinit var imgPreviewLay: ConstraintLayout
    private lateinit var capturedImageView: ImageView
    private var imgPath = ""

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startCamera()
        } else {
            Toast.makeText(this, "Camera permission is required", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)

        previewView = findViewById(R.id.previewView)
        captureButton = findViewById(R.id.captureButton)
        analyseButton = findViewById(R.id.analyseButton)

        logOutBtn = findViewById(R.id.logoutButton)
        reTakeImage = findViewById(R.id.reCapture)

        cameraLay = findViewById(R.id.layCamera)
        imgPreviewLay = findViewById(R.id.layImagePreview)
        capturedImageView = findViewById(R.id.imgCapture)

        // Request camera permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        captureButton.setOnClickListener {
            takePhoto()
        }

        logOutBtn.setOnClickListener{
            showLogoutConfirmation()
        }

        reTakeImage.setOnClickListener {
            cameraLay.visibility = View.VISIBLE
            imgPreviewLay.visibility = View.GONE

            reTakeImage.visibility = View.GONE
            logOutBtn.visibility = View.VISIBLE
        }

        analyseButton.setOnClickListener {
            showLoadingAndNavigateResult()
        }

        reTakeImage.visibility = View.GONE
        logOutBtn.visibility = View.VISIBLE

        cameraLay.visibility = View.VISIBLE
        imgPreviewLay.visibility = View.GONE
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.surfaceProvider = previewView.surfaceProvider
            }

            imageCapture = ImageCapture.Builder().build()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview,
                    imageCapture
                )
            } catch (e: Exception) {
                Log.e("CameraX", "Use case binding failed", e)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return

        val photoFile = File(
            externalMediaDirs.firstOrNull(),
            SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
                .format(System.currentTimeMillis()) + ".jpg"
        )

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    /*Toast.makeText(
                        applicationContext,
                        "Photo saved: ${photoFile.name}",
                        Toast.LENGTH_SHORT
                    ).show()*/
                    // TODO: Send image to backend using Retrofit

                    cameraLay.visibility = View.GONE
                    imgPreviewLay.visibility = View.VISIBLE

                    reTakeImage.visibility = View.VISIBLE
                    logOutBtn.visibility = View.GONE

                    val bitmap = BitmapFactory.decodeFile(photoFile.absolutePath)
                    val rotatedBitmap = rotateImageIfRequired(bitmap, photoFile.absolutePath)
                    capturedImageView.setImageBitmap(rotatedBitmap)

                    imgPath = photoFile.absolutePath
                    Log.d("APP","path: $imgPath")
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e("CameraX", "Photo capture failed: ${exception.message}", exception)
                }
            }
        )
    }

    private fun rotateImageIfRequired(img: Bitmap, imagePath: String): Bitmap {
        val ei = ExifInterface(imagePath)
        return when (ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> rotateImage(img, 90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> rotateImage(img, 180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> rotateImage(img, 270f)
            else -> img
        }
    }

    private fun rotateImage(img: Bitmap, degree: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(degree)
        return Bitmap.createBitmap(img, 0, 0, img.width, img.height, matrix, true)
    }

    private fun showLogoutConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Exit")
            .setMessage("Are you sure you want to exit?")
            .setPositiveButton("Yes") { dialog, _ ->
                dialog.dismiss()
                finish() // or navigate to login screen
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .show()
    }

    private fun showLoadingAndNavigateResult() {
        /*val dialogView = layoutInflater.inflate(R.layout.dialog_loading, null)

        val loadingDialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        // 👇 Set transparent background
        loadingDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        loadingDialog.show()*/

        makeApiCall()

        /*Handler(Looper.getMainLooper()).postDelayed({
            loadingDialog.dismiss()
            startActivity(Intent(this, ResultActivity::class.java))
        }, 4000) // 4 seconds delay*/
    }

   private fun prepareImageFilePart(filePath: String): MultipartBody.Part {
        val file = File(filePath)
        val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
        return MultipartBody.Part.createFormData("image", file.name, requestFile)
    }

    private fun makeApiCall(){
        val dialogView = layoutInflater.inflate(R.layout.dialog_loading, null)

        val loadingDialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        // 👇 Set transparent background
        loadingDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        loadingDialog.show()

        val retrofit = Retrofit.Builder()
            .baseUrl("http://www.codesofy.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val apiService = retrofit.create(ApiService::class.java)

        val imagePart = prepareImageFilePart(imgPath)

        val call = apiService.uploadImage(imagePart)

        call.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                loadingDialog.hide()

                if (response.isSuccessful) {
                    Log.d("Upload", "Success: ${response.body()?.string()}")
                } else {
                    Log.e("Upload", "Error: ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Log.e("Upload", "Failure: ${t.message}")
                loadingDialog.hide()
            }
        })
    }
}