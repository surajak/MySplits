package com.SohamProject.mysplits

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupListScreen(viewModel: SplitViewModel, onGroupClick: (Group) -> Unit) {
    // 1. COLLECT DATA
    val groups by viewModel.allGroups.collectAsState()

    // 2. DIALOG STATES
    var showAddDialog by remember { mutableStateOf(false) }
    var groupToDelete by remember { mutableStateOf<Group?>(null) }
    var showJoinDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Split It") }) },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                FloatingActionButton(
                    onClick = { showJoinDialog = true },
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Icon(Icons.Default.GroupAdd, "Join Group")
                }
                FloatingActionButton(onClick = { showAddDialog = true }) {
                    Icon(Icons.Default.Add, "Add Group")
                }
            }
        }
    ) { padding ->
        if (groups.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No groups yet. Tap + to start!")
            }
        } else {
            // --- THE LIST OF GROUPS ---
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(items = groups, key = { it.id }) { group ->
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = { newValue ->
                            if (newValue == SwipeToDismissBoxValue.EndToStart) {
                                groupToDelete = group // Trigger the delete confirmation
                                false // Snap the card back so it doesn't stay stuck
                            } else {
                                false
                            }
                        }
                    )

                    SwipeToDismissBox(
                        state = dismissState,
                        enableDismissFromStartToEnd = false,
                        backgroundContent = {
                            val color = if (dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart) {
                                Color.Red.copy(alpha = 0.8f)
                            } else Color.Transparent

                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp)
                                    .background(color, shape = CardDefaults.shape),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = Color.White,
                                    modifier = Modifier.padding(end = 16.dp)
                                )
                            }
                        }
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onGroupClick(group) }
                        ) {
                            Text(
                                group.name,
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }
            }
        }

        // --- THE "ADD GROUP" DIALOG ---
        if (showAddDialog) {
            var name by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { showAddDialog = false },
                title = { Text("New Group") },
                text = {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Group Name") },
                        singleLine = true
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        if (name.isNotBlank()) {
                            viewModel.addGroup(name) // This calls your SplitViewModel
                            showAddDialog = false
                        }
                    }) { Text("Create") }
                },
                dismissButton = {
                    TextButton(onClick = { showAddDialog = false }) { Text("Cancel") }
                }
            )
        }

        // --- THE "DELETE CONFIRMATION" DIALOG ---
        groupToDelete?.let { group ->
            AlertDialog(
                onDismissRequest = { groupToDelete = null },
                title = { Text("Delete Group?") },
                text = { Text("Are you sure you want to delete '${group.name}'? This will remove all members and expenses inside.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteGroup(group)
                            groupToDelete = null
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                    ) { Text("Delete") }
                },
                dismissButton = {
                    TextButton(onClick = { groupToDelete = null }) { Text("Cancel") }
                }
            )
        }

        // --- NEW "JOIN GROUP" DIALOG ---
        if (showJoinDialog) {
            var groupCode by remember { mutableStateOf("") }
            val snackbarHostState = remember { SnackbarHostState() } // For showing messages
            val scope = rememberCoroutineScope() // For launching snackbar coroutines

            AlertDialog(
                onDismissRequest = { showJoinDialog = false },
                title = { Text("Join Group") },
                text = {
                    Column {
                        OutlinedTextField(
                            value = groupCode,
                            onValueChange = { groupCode = it },
                            label = { Text("Group Code (e.g., -XYZ123)") },
                            singleLine = true
                        )
                        // SnackbarHost can be used for messages like "Group not found"
                        SnackbarHost(hostState = snackbarHostState)
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        if (groupCode.isNotBlank()) {
                            // Call ViewModel to join
                            scope.launch {
                                val result = viewModel.joinGroup(groupCode)
                                if (result) {
                                    snackbarHostState.showSnackbar("Successfully joined group!")
                                    showJoinDialog = false
                                } else {
                                    snackbarHostState.showSnackbar("Group not found or error joining.")
                                }
                            }
                        } else {
                            scope.launch {
                                snackbarHostState.showSnackbar("Please enter a group code.")
                            }
                        }
                    }) { Text("Join") }
                },
                dismissButton = {
                    TextButton(onClick = { showJoinDialog = false }) { Text("Cancel") }
                }
            )
        }
    }
}
