package com.example.CameraTest

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

class SetImageActivity : AppCompatActivity() {
    private var imgPath:String = ""
    private var loadType:String = ""

    // View Elements
    private lateinit var photoButton: Button
    private lateinit var analyseButton:Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.set_image_view)

        // Set buttons
        photoButton = findViewById(R.id.snap)
        analyseButton = findViewById(R.id.analyse)

        // Set listener for buttons
        photoButton.setOnClickListener { returnToMain() }
        analyseButton.setOnClickListener { analysePhoto() }

        imgPath = intent.getStringExtra("FoodHelper.IMAGE").toString()
        loadType = intent.getStringExtra("FoodHelper.LOADTYPE").toString()
        val imgUri = Uri.parse(imgPath)
        val imgView: ImageView? = findViewById(R.id.loaded_img)

        imgView!!.setImageURI(imgUri)
    }

    private fun returnToMain() {
        var intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
    }

    private fun analysePhoto() {
        var intent = Intent(this, AnalyseActivity::class.java).apply {
            putExtra(LOADED_IMG_TEXT, imgPath)
            putExtra(IMG_LOAD_TYPE, loadType)
        }
        startActivity(intent)
    }

    companion object {
        private const val LOADED_IMG_TEXT = "FoodHelper.IMAGE"
        private const val IMG_LOAD_TYPE = "FoodHelper.LOADTYPE"
    }
}