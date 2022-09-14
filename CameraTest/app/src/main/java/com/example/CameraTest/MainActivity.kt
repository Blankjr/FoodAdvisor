package com.example.CameraTest

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : AppCompatActivity() {

    // CameraX Elements
    private var preview: Preview? = null
    private var imgCapture: ImageCapture? = null
    private var camera: Camera? = null
    private var imgPath: String? = null
    private var loadType: String? = null

    // File Elements
    private lateinit var outputDir: File

    // View Elements
    private lateinit var photoButton: Button
    private lateinit var loadButton: Button


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Set Buttons
        photoButton = findViewById(R.id.snap)
        loadButton = findViewById(R.id.load)

        // Set listener for buttons
        photoButton.setOnClickListener { takePhoto() }
        loadButton.setOnClickListener { loadPhoto() }

        if(allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this,
                REQUIRED_PERMISSIONS,
                REQUEST_CODE_PERMISSIONS
            )
        }

        outputDir = getOutputDir()!!

    }

    private fun startCamera() {
        val providerFuture = ProcessCameraProvider.getInstance((this))
        providerFuture.addListener({
            val provider:ProcessCameraProvider = providerFuture.get()
            preview = Preview.Builder()
                .build()


            imgCapture = ImageCapture.Builder()
                .build()

            val camSelector = CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build()

            try {
                // before using unbind all
                provider.unbindAll()

                // set the lifecycle for the camera
                camera = provider.bindToLifecycle(this, camSelector, preview, imgCapture)

                preview?.setSurfaceProvider(cam_preview.surfaceProvider)


            } catch (e:Exception) {
                Log.e(TAG, "Use case binding failed", e)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhoto() {
        val imgCapture:ImageCapture = imgCapture?:return

        // Make file with time
        val file = File(
            outputDir,
            SimpleDateFormat(FILENAME_FORMAT, Locale.GERMANY).format(System.currentTimeMillis()) + ".jpg"
        )

        val outputOptions:ImageCapture.OutputFileOptions = ImageCapture.OutputFileOptions.Builder(file).build()

        imgCapture.takePicture(outputOptions, ContextCompat.getMainExecutor(this), object:ImageCapture.OnImageSavedCallback{
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                val savedURI = Uri.fromFile(file)
                val msg = "Image capture succeeded : \n\n $savedURI"
                Toast.makeText(baseContext, msg, Toast.LENGTH_LONG).show()
                Log.d(TAG,msg)
                imgPath = savedURI.path
                loadType = "photo"
                startNextActivity()
            }

            override fun onError(exception: ImageCaptureException) {
                Log.e(TAG, "Image capture failed : ${exception.message}", exception)
            }
        })
    }

    private val startForResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            result: ActivityResult ->
        if (result.resultCode == RESULT_OK) {
            val data: Intent? = result.data
            val uri: Uri? = data!!.data
            imgPath = uri.toString()
            loadType = "load"

            startNextActivity()
        }
    }

    private fun loadPhoto() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.type = "image/*"
        startForResult.launch(intent)
    }

    private fun startNextActivity() {
        var intent = Intent(this, SetImageActivity::class.java).apply {
            putExtra(LOADED_IMG_TEXT, imgPath)
            putExtra(IMG_LOAD_TYPE, loadType)
        }
        startActivity(intent)
    }

    private fun allPermissionsGranted() = Companion.REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(this, "No permission granted for app use. Please grant access to use the app.", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    private fun getOutputDir():File? {
        val mediaDir = externalMediaDirs.firstOrNull()?.let {
            File(it, resources.getString(R.string.app_name)).apply { mkdirs() }}
            return if(mediaDir != null && mediaDir.exists())
                mediaDir
            else
                filesDir
    }

    companion object {
        private const val TAG = "CAMERAX Lib"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private const val LOADED_IMG_TEXT = "FoodHelper.IMAGE"
        private const val IMG_LOAD_TYPE = "FoodHelper.LOADTYPE"
        private val REQUIRED_PERMISSIONS = arrayOf(
            android.Manifest.permission.CAMERA,
            android.Manifest.permission.READ_EXTERNAL_STORAGE
        )
    }
}