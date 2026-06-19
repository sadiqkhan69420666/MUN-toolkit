package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.example.data.BriarDatabase
import com.example.data.BriarRepository
import com.example.ui.BriarAppContent
import com.example.ui.BriarViewModel
import com.example.ui.BriarViewModelFactory
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 1. Initialise local Room database
        val database = BriarDatabase.getDatabase(applicationContext)
        val briarDao = database.briarDao()

        // 2. Initialise repository
        val repository = BriarRepository(briarDao)

        // 3. Setup ViewModel via Factory
        val viewModelFactory = BriarViewModelFactory(application, repository)
        val viewModel: BriarViewModel by viewModels { viewModelFactory }

        setContent {
            MyApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    BriarAppContent(
                        viewModel = viewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}
