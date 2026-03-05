package com.SohamProject.mysplits

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun AddMemberDialog(onDismiss: () -> Unit, onConfirm: (String, Double) -> Unit) {
    var name by remember { mutableStateOf("") }
    var weightString by remember { mutableStateOf("1.0") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Member") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, singleLine = true)
                OutlinedTextField(
                    value = weightString,
                    onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) weightString = it },
                    label = { Text("Weight (1.0 = normal)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val w = weightString.toDoubleOrNull() ?: 1.0
                if (name.isNotBlank()) onConfirm(name, w)
            }) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
