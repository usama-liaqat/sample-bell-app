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

class MainActivity : AppCompatActivity() {
    private val baseURI = "https://your-server.com" // Replace with your actual base URI

    private lateinit var connectButton: Button
    private lateinit var inputField: EditText
    private lateinit var radioGroup: RadioGroup
    private lateinit var centeredBox: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.main_layout)
        connectButton = findViewById(R.id.connectButton)
        inputField = findViewById(R.id.inputField)
        radioGroup = findViewById(R.id.radioGroup)
        centeredBox = findViewById(R.id.centeredBox)
        connectButton.setOnClickListener {
            // Hide the LinearLayout (centeredBox)
            centeredBox.visibility = LinearLayout.GONE

            // Get the input field value
            val inputText = inputField.text.toString()

            // Get selected radio button
            val selectedRadioButtonId = radioGroup.checkedRadioButtonId
            val radioButton: RadioButton = findViewById(selectedRadioButtonId)
            val radioText = radioButton.text.toString()

            // Replace the layout with the correct fragment based on input and radio selection
            replaceFragment(inputText, radioText)
        }
    }


    // Function to replace the layout with either AdminFragment or BellFragment
    private fun replaceFragment(inputText: String, radioText: String) {
        val fragment: Fragment = when (radioText.lowercase()) {
            "admin" -> AdminFragment.new(inputText)
            "bell" -> BellFragment.new(inputText)
            else -> AdminFragment.new(inputText) // Default to AdminFragment if input is unrecognized
        }

        val fragmentTransaction: FragmentTransaction = supportFragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.fragmentContainer, fragment)  // Replace the container with the fragment
        fragmentTransaction.addToBackStack(null)  // Add to back stack so user can navigate back
        fragmentTransaction.commit()  // Commit the transaction
    }


}

