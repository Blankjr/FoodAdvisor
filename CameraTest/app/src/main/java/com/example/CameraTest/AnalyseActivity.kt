package com.example.CameraTest

import android.content.Intent
import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.Layout
import android.util.Log
import android.view.Gravity
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject
import org.tensorflow.lite.support.image.*
import org.tensorflow.lite.task.vision.classifier.ImageClassifier
import java.io.*
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel


class AnalyseActivity : AppCompatActivity() {
    // View Elements
    private lateinit var resetButton: Button
    private lateinit var resultTextView: TextView
    private var imgPath:String = ""
    private var loadType:String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.analyse_view)

        // Set view elements
        resetButton = findViewById(R.id.reset)
        resultTextView = findViewById(R.id.result)

        // Text configuration
        resultTextView.textSize = 20f
        resultTextView.gravity = Gravity.LEFT

        // Set listener
        resetButton.setOnClickListener { returnToMain() }

        // Get intent extras
        imgPath = intent.getStringExtra("FoodHelper.IMAGE").toString()
        loadType = intent.getStringExtra("FoodHelper.LOADTYPE").toString()

        // Setup preview
        val imgUri = Uri.parse(imgPath)
        val imgView: ImageView? = findViewById(R.id.loaded_img)
        imgView!!.setImageURI(imgUri)

        // Initialize bitmap
        var bitmap:Bitmap

        try {
            bitmap = if (loadType == "load") {
                val imgStream = baseContext.contentResolver.openInputStream(imgUri)
                BitmapFactory.decodeStream(imgStream)
            } else {
                val uri = Uri.fromFile(File(imgPath))
                MediaStore.Images.Media.getBitmap(contentResolver, uri)
            }

            /**
             * Worker thread for object detection
             */
            Thread {
                runObjectDetection(bitmap)
            }.start()
        } catch (e: Exception) {
            Log.e("EXCEPTION", e.stackTraceToString())
        }
    }

    /**
     * Used with Extractor-library for debugging
     */
    private fun loadModelFile(assets: AssetManager, modelFilename: String): MappedByteBuffer? {
        val fileDescriptor = assets.openFd(modelFilename)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    override fun onBackPressed() {
        super.onBackPressed()
        var intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
    }

    private fun returnToMain() {
        var intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
    }

    private fun runObjectDetection(bitmap: Bitmap) {
        // Step 1 : Create TensorImage from Bitmap
        val image = TensorImage.fromBitmap(bitmap)
        // Step 2: Initialize the classifier object
        val options = ImageClassifier.ImageClassifierOptions.builder()
            .setMaxResults(1)
            .build()
        val classifier = ImageClassifier.createFromFileAndOptions(
            this, "food.tflite", options
        )
        // Step 3: Feed given image to the classifier
        val results = classifier.classify(image)
        // Step 4: Parse the detection result and show it
        resultTextView.text = ""
        results.map {
            // Get the top-1 category and craft the display text
            val category = it.categories.first()
            resultTextView.text = "Genauigkeit der Bilderkennung : ${category.score.times(100).toInt()}%\n${searchFood(category.label)} \n"
        }
    }

        private fun loadJSON():String {
        var content = ""

        try {
            val `input`:InputStream = baseContext.assets.open("database.json")
            val size: Int = `input`.available()
            val buffer = ByteArray(size)
            `input`.read(buffer)
            `input`.close()
            content = String(buffer, Charsets.UTF_8)
        } catch (e:IOException) {
            Log.e("IOError", e.stackTraceToString())
        }

        return content
    }

    private fun searchFood(food:String): String {
        val foodList = JSONObject(loadJSON())
        var foodData = ""
        var categories = arrayOf("Name", "Kalorien", "Eiweiß/Protein", "Fett", "tierisch", "Kategorie")

        var food = foodList.getJSONObject(food)

        for ((index, key) in food.keys().withIndex()) {
            if(categories[index] == "tierisch") {
                foodData += "\n${showAnimalInformation(food[key].toString())} \n"
            } else if(categories[index] == "Kategorie") {
                foodData += "\n${showRecommendation(food[key].toString())}"
            } else if(categories[index] == "Name") {
                foodData += "\n${food[key]}\n ---------------------------------------\nAngaben pro 100g : \n"
            } else if(categories[index] == "Kalorien") {
                foodData += "●  ${categories[index]} ${food[key]} kcal \n"
            } else {
                foodData += "●  ${categories[index]} ${food[key]}g \n"
            }
        }
        return foodData
    }

    private fun showAnimalInformation(animalType:String):String {
        var animalDataString = ""

        when(animalType) {
            "VEGETARIAN" -> animalDataString = "Geeignet für vegetarische Ernährung"
            "VEGAN" -> animalDataString = "Geeignet für vegane Ernährung"
            "MEAT" -> animalDataString = "Enthält Fleisch"
            "FISH" -> animalDataString = "Enthält Fisch"
            "SEAFOOD" -> animalDataString = "Enthält Meeresfrüchte"
        }

        return "‣ $animalDataString"
    }

    private fun showRecommendation(foodCategory:String):String {
        var categoryDataString = ""

        when(foodCategory) {
            "SWEETS" -> categoryDataString = "Nicht zu viel davon naschen"
            "SOULFOOD" -> categoryDataString = "Gut für die Seele, suboptimal für den Körper"
            "RECOMMENDED" -> categoryDataString = "Gut für den Körper und das Gewissen"
        }

        return "⦿ $categoryDataString"
    }

}