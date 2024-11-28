package com.example.myapplication

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.core.view.ViewCompat
import android.view.View
import androidx.core.view.WindowInsetsCompat
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.pm.PackageManager


class MainActivity : AppCompatActivity() {
    private lateinit var connectButton: Button
    private lateinit var inputField: EditText
    private lateinit var radioGroup: RadioGroup
    private lateinit var centeredBox: LinearLayout

    private val CAMERA_PERMISSION_REQUEST_CODE = 1
    private val MICROPHONE_PERMISSION_REQUEST_CODE = 2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Config.initialize(this)
        setContentView(R.layout.main_layout)

        val rootView: View = findViewById(android.R.id.content)
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { v, insets ->
            val systemBarsInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            // Apply system bar insets to add padding for safe areas
            v.setPadding(
                systemBarsInsets.left, // Left padding for safe area
                systemBarsInsets.top,  // Top padding for status bar
                systemBarsInsets.right, // Right padding
                systemBarsInsets.bottom // Bottom padding for navigation bar
            )

            // Return insets to allow the system to handle them
            insets
        }
        checkPermissions()

        connectButton = findViewById(R.id.connectButton)
        inputField = findViewById(R.id.inputField)
        radioGroup = findViewById(R.id.radioGroup)
        centeredBox = findViewById(R.id.centeredBox)
        connectButton.setOnClickListener {
            // Hide the LinearLayout (centeredBox)

            // Get the input field value
            val inputText = inputField.text.toString()

            if (inputText.isEmpty()) {
                inputField.error = "Input cannot be empty" // Show an error message
                return@setOnClickListener // Stop the method execution if input is empty
            }

            // Get selected radio button
            val selectedRadioButtonId = radioGroup.checkedRadioButtonId

            if (selectedRadioButtonId == -1) {
                // No radio button selected, show an error
                return@setOnClickListener
            }
            val radioButton: RadioButton = findViewById(selectedRadioButtonId)
            val radioText = radioButton.text.toString()

            // Replace the layout with the correct fragment based on input and radio selection
            replaceFragment(inputText, radioText)

            centeredBox.visibility = LinearLayout.GONE
        }

    }


    // Function to replace the layout with either AdminFragment or BellFragment
    private fun replaceFragment(inputText: String, radioText: String) {
        if (!(inputText.isEmpty() && radioText.isEmpty())) {
            val fragment: Fragment = when (radioText.lowercase()) {
                "admin" -> AdminFragment.new("demo/$inputText")
                "bell" -> BellFragment.new("demo/$inputText")
                else -> AdminFragment.new("demo/$inputText") // Default to AdminFragment if input is unrecognized
            }

            val fragmentTransaction: FragmentTransaction = supportFragmentManager.beginTransaction()
            fragmentTransaction.replace(R.id.fragmentContainer, fragment)  // Replace the container with the fragment
            fragmentTransaction.addToBackStack(null)  // Add to back stack so user can navigate back
            fragmentTransaction.commit()  // Commit the transaction
        }

    }

    private fun checkPermissions() {
        val cameraPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
        val microphonePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)

        // If both permissions are not granted, request them
        if (cameraPermission != PackageManager.PERMISSION_GRANTED || microphonePermission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO),
                CAMERA_PERMISSION_REQUEST_CODE
            )
        }
    }


}

