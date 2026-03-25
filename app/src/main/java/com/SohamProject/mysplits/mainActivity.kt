package com.SohamProject.mysplits

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.activity.enableEdgeToEdge

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    enableEdgeToEdge()
            setContent {
            MySplitsTheme {
                val viewModel: SplitViewModel = viewModel()
                var selectedGroup by remember { mutableStateOf<Group?>(null) }

                Surface(color = MaterialTheme.colorScheme.background) {
                    if (selectedGroup == null) {
                        GroupListScreen(
                            viewModel = viewModel,
                            onGroupClick = { selectedGroup = it }
                        )
                    } else {
                        GroupDetailsScreen(
                            group = selectedGroup!!,
                            viewModel = viewModel,
                            onBack = { selectedGroup = null }
                        )
                    }
                }
            }
        }
    }
}
