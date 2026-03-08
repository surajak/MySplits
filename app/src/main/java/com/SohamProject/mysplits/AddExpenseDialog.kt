package com.SohamProject.mysplits

//import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
//import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun AddExpenseDialog(
    members: List<Member>,
    // Optional Initial Values for Editing
    initialDescription: String = "",
    initialAmount: Double? = null,
    initialPayers: Map<Int, Double> = emptyMap(),
    initialWeights: Map<Int, Double> = emptyMap(),

    onDismiss: () -> Unit,
    onConfirm: (String, Double, Map<Int, Double>, Map<Int, Double>) -> Unit
) {
    // FIX 1: Add keys to remember() so it resets when opening a different expense
    var description by remember(initialDescription) { mutableStateOf(initialDescription) }
    var totalAmountString by remember(initialAmount) { mutableStateOf(initialAmount?.toString() ?: "") }

    // --- PAYER STATE ---
    val selectedPayerIds = remember(initialPayers) {
        mutableStateListOf<Int>().apply { addAll(initialPayers.keys) }
    }
    val payerAmounts = remember(initialPayers) {
        mutableStateMapOf<Int, String>().apply {
            initialPayers.forEach { (id, amt) -> put(id, amt.toString()) }
        }
    }

    // --- SPLIT STATE ---
    val involvedIds = remember(initialWeights) {
        mutableStateListOf<Int>().apply {
            if (initialWeights.isNotEmpty()) addAll(initialWeights.keys)
            else addAll(members.map { it.id })
        }
    }

    val involvedWeights = remember(initialWeights) {
        mutableStateMapOf<Int, String>().apply {
            if (initialWeights.isNotEmpty()) {
                initialWeights.forEach { (id, w) -> put(id, w.toString()) }
            } else {
                members.forEach { put(it.id, it.defaultWeight.toString()) }
            }
        }
    }

    // Helper: Distribute payer amounts
    fun redistributePayers(lockedId: Int, newValue: Double, total: Double) {
        val targets = selectedPayerIds.filter { it != lockedId }
        if (targets.isEmpty()) return
        val remaining = total - newValue
        val split = remaining / targets.size
        targets.forEach { id -> payerAmounts[id] = String.format("%.2f", split) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialDescription.isEmpty()) "Add Expense" else "Edit Expense") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {

                // 1. Basic Info
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") })
                OutlinedTextField(
                    value = totalAmountString,
                    onValueChange = {
                        if (it.all { c -> c.isDigit() || c == '.' }) {
                            totalAmountString = it
                            // Auto-fill payer logic (only if adding fresh or single payer selected)
                            val total = it.toDoubleOrNull() ?: 0.0
                            if (selectedPayerIds.size == 1) {
                                payerAmounts[selectedPayerIds.first()] = it
                            }
                        }
                    },
                    label = { Text("Total Amount ($)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)
                Text("Who Paid?", fontWeight = FontWeight.Bold)

                // 2. Payers
                members.forEach { member ->
                    val isSelected = selectedPayerIds.contains(member.id)
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = { checked ->
                                if (checked) {
                                    selectedPayerIds.add(member.id)
                                    val total = totalAmountString.toDoubleOrNull() ?: 0.0
                                    val split = total / selectedPayerIds.size
                                    selectedPayerIds.forEach { id -> payerAmounts[id] = String.format("%.2f", split) }
                                } else {
                                    selectedPayerIds.remove(member.id)
                                    payerAmounts.remove(member.id)
                                    val total = totalAmountString.toDoubleOrNull() ?: 0.0
                                    if (selectedPayerIds.isNotEmpty()) {
                                        val split = total / selectedPayerIds.size
                                        selectedPayerIds.forEach { id -> payerAmounts[id] = String.format("%.2f", split) }
                                    }
                                }
                            }
                        )
                        Text(member.name, modifier = Modifier.weight(1f))
                        if (isSelected) {
                            OutlinedTextField(
                                value = payerAmounts[member.id] ?: "",
                                onValueChange = { newStr ->
                                    if (newStr.all { c -> c.isDigit() || c == '.' }) {
                                        payerAmounts[member.id] = newStr
                                        val manualVal = newStr.toDoubleOrNull() ?: 0.0
                                        val total = totalAmountString.toDoubleOrNull() ?: 0.0
                                        redistributePayers(member.id, manualVal, total)
                                    }
                                },
                                modifier = Modifier.width(90.dp),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                        }
                    }
                }

                HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)
                Text("Split amongst (Weights):", fontWeight = FontWeight.Bold)

                // 3. Shares
                members.forEach { member ->
                    val isInvolved = involvedIds.contains(member.id)
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = isInvolved, onCheckedChange = { checked ->
                            if (checked) involvedIds.add(member.id) else involvedIds.remove(member.id)
                        })
                        Text(member.name, modifier = Modifier.weight(1f))
                        if (isInvolved) {
                            OutlinedTextField(
                                value = involvedWeights[member.id] ?: "1.0",
                                onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) involvedWeights[member.id] = it },
                                modifier = Modifier.width(70.dp),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                        }
                    }
                }

                // VALIDATION FEEDBACK
                val total = totalAmountString.toDoubleOrNull()
                val sumPaid = payerAmounts.values.sumOf { it.toDoubleOrNull() ?: 0.0 }
                if (total != null && Math.abs(sumPaid - total) > 0.1) {
                    Text(
                        text = "Amounts don't match! Total: $total, Paid: $sumPaid",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            val total = totalAmountString.toDoubleOrNull()
            val finalPayers = payerAmounts.mapValues { it.value.toDoubleOrNull() ?: 0.0 }
            val sumPaid = finalPayers.values.sum()
            val finalInvolved = mutableMapOf<Int, Double>()
            involvedIds.forEach { id -> finalInvolved[id] = involvedWeights[id]?.toDoubleOrNull() ?: 0.0 }

            // Logic: Is the form valid?
            val isValid = description.isNotBlank() &&
                    total != null &&
                    Math.abs(sumPaid - total) < 0.1 && // Allow small rounding errors
                    finalInvolved.isNotEmpty()

            Button(
                onClick = {
                    if (isValid) {
                        onConfirm(description, total!!, finalPayers, finalInvolved)
                    }
                },
                enabled = isValid // Disable button if math is wrong
            ) {
                Text(if(initialDescription.isEmpty()) "Add" else "Save")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
