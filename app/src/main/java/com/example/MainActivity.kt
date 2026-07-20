package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.HabitViewModel
import com.example.ui.MainLayout
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      val viewModel: HabitViewModel = viewModel()
      val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()

      MyApplicationTheme(darkTheme = isDarkMode) {
        Surface(modifier = Modifier.fillMaxSize()) {
          MainLayout(viewModel = viewModel)
        }
      }
    }
  }
}

@Composable
fun Surface(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    androidx.compose.material3.Surface(
        modifier = modifier,
        color = androidx.compose.material3.MaterialTheme.colorScheme.background,
        content = content
    )
}

