package com.SohamProject.mysplits

import android.content.Intent
import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

// ---------------------------------------------------------------------------
// Helper: Generate a QR code Bitmap from any text string
// ---------------------------------------------------------------------------
fun generateQrBitmap(content: String, size: Int = 512): Bitmap? {
    if (content.isBlank()) return null
    return try {
        val hints = mapOf(EncodeHintType.MARGIN to 1)
        val bitMatrix = MultiFormatWriter().encode(content, BarcodeFormat.QR_CODE, size, size, hints)
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        for (x in 0 until size) {
            for (y in 0 until size) {
                bmp.setPixel(
                    x, y,
                    if (bitMatrix[x, y]) android.graphics.Color.BLACK
                    else android.graphics.Color.WHITE
                )
            }
        }
        bmp
    } catch (e: Exception) {
        null
    }
}

// ---------------------------------------------------------------------------
// Helper: Open Android share sheet with the join text + link
// ---------------------------------------------------------------------------
fun shareGroupInvite(context: android.content.Context, groupName: String, remoteId: String) {
    val joinLink = "https://mysplits.page.link/join?code=$remoteId"
    val shareText = """
        Join my MySplits group "$groupName"!
        
        Enter this code in the app: $remoteId
        Or tap the link: $joinLink
    """.trimIndent()

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "Join my MySplits group")
        putExtra(Intent.EXTRA_TEXT, shareText)
    }
    context.startActivity(Intent.createChooser(intent, "Share invite via…"))
}

// ---------------------------------------------------------------------------
// QR Code Dialog
// ---------------------------------------------------------------------------
@Composable
fun QrCodeDialog(groupName: String, remoteId: String, onDismiss: () -> Unit) {
    val qrBitmap = remember(remoteId) { generateQrBitmap(remoteId) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Join \"$groupName\"",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Scan this QR code or share the code below",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(16.dp))

                if (qrBitmap != null) {
                    Image(
                        bitmap = qrBitmap.asImageBitmap(),
                        contentDescription = "QR code for group $groupName",
                        modifier = Modifier.size(220.dp)
                    )
                } else {
                    Box(
                        Modifier
                            .size(220.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Could not generate QR", textAlign = TextAlign.Center)
                    }
                }

                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Code: $remoteId",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(16.dp))
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                    Text("Close")
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Main Screen
// ---------------------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailsScreen(group: Group, viewModel: SplitViewModel, onBack: () -> Unit) {

    val liveGroup by viewModel.getGroup(group.id).collectAsState(initial = group)

    // Safety check: if the group is deleted, go back
    if (liveGroup == null) {
        onBack()
        return
    }

    // ✅ START FIREBASE LISTENER — placed correctly inside the Composable
    LaunchedEffect(group.id) {
        val currentGroup = viewModel.getGroup(group.id).first()
        if (currentGroup?.isOnline == true) {
            viewModel.startListeningToGroup(currentGroup)
        }
    }

    BackHandler { onBack() }

    val expensesListState = rememberLazyListState()
    val groupInfoScrollState = rememberScrollState()
    var selectedTab by remember { mutableIntStateOf(0) }
    var isFabVisible by remember { mutableStateOf(true) }

    val members by viewModel.getMembers(group.id).collectAsState(initial = emptyList())
    val expenses by viewModel.getExpenses(group.id).collectAsState(initial = emptyList())
    val allPayers by viewModel.getPayers(group.id).collectAsState(initial = emptyList())
    val allShares by viewModel.getGroupShares(group.id).collectAsState(initial = emptyList())

    var showAddExpenseDialog by remember { mutableStateOf(false) }
    var showAddMemberDialog by remember { mutableStateOf(false) }
    var showSettlementDialog by remember { mutableStateOf(false) }
    var expenseToDelete by remember { mutableStateOf<Expense?>(null) }
    var showQrDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(group.name) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        PdfExporter.generateAndShare(
                            context, group, members, expenses, allPayers, allShares, emptyList()
                        )
                    }) {
                        Icon(Icons.AutoMirrored.Filled.Send, "Export")
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    label = { Text("Expenses") },
                    icon = { Icon(Icons.AutoMirrored.Filled.List, null) }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    label = { Text("Group Info") },
                    icon = { Icon(Icons.Default.Info, null) }
                )
            }
        },
        floatingActionButton = {
            AnimatedVisibility(visible = isFabVisible) {
                ExtendedFloatingActionButton(
                    onClick = {
                        if (selectedTab == 0) showAddExpenseDialog = true
                        else showAddMemberDialog = true
                    },
                    icon = {
                        Icon(
                            if (selectedTab == 0) Icons.Default.Add else Icons.Default.PersonAdd,
                            null
                        )
                    },
                    text = { Text(if (selectedTab == 0) "Add Expense" else "Add Member") }
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {

            if (selectedTab == 0) {
                // ----------------------------------------------------------------
                // TAB 0: EXPENSES
                // ----------------------------------------------------------------
                Text(
                    "Expenses",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))

                LazyColumn(
                    state = expensesListState,
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(expenses, key = { it.id }) { exp ->
                        val payersForExp = allPayers.filter { it.expenseId == exp.id }
                        val payerNames =
                            payersForExp.mapNotNull { p -> members.find { it.id == p.memberId }?.name }

                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = { newValue ->
                                if (newValue == SwipeToDismissBoxValue.EndToStart) {
                                    expenseToDelete = exp
                                    false
                                } else false
                            }
                        )

                        SwipeToDismissBox(
                            state = dismissState,
                            enableDismissFromStartToEnd = false,
                            backgroundContent = {
                                Box(
                                    Modifier
                                        .fillMaxSize()
                                        .background(Color.Red.copy(alpha = 0.8f), CardDefaults.shape),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    Icon(
                                        Icons.Default.Delete, null,
                                        tint = Color.White,
                                        modifier = Modifier.padding(end = 16.dp)
                                    )
                                }
                            }
                        ) {
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(exp.description, fontWeight = FontWeight.Bold)
                                        Text(
                                            "Paid by ${payerNames.joinToString(", ")}",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                    Text(
                                        "$${String.format("%.2f", exp.totalAmount)}",
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

            } else {
                // ----------------------------------------------------------------
                // TAB 1: GROUP INFO
                // ----------------------------------------------------------------
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(groupInfoScrollState)
                ) {

                    // ONLINE SYNC CARD
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (liveGroup?.isOnline == true)
                                MaterialTheme.colorScheme.tertiaryContainer
                            else
                                MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text("Sync Online", fontWeight = FontWeight.Bold)
                                    Text(
                                        text = if (liveGroup?.isOnline == true)
                                            "Code: ${liveGroup?.remoteId}"
                                        else
                                            "Offline: Local copy only",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                Switch(
                                    checked = liveGroup?.isOnline == true,
                                    onCheckedChange = {
                                        liveGroup?.let { viewModel.toggleOnline(it) }
                                    }
                                )
                            }

                            if (liveGroup?.isOnline == true && !liveGroup?.remoteId.isNullOrBlank()) {
                                Spacer(Modifier.height(12.dp))
                                HorizontalDivider()
                                Spacer(Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { showQrDialog = true },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.QrCode,
                                            contentDescription = "QR Code",
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text("QR Code")
                                    }

                                    Button(
                                        onClick = {
                                            shareGroupInvite(
                                                context = context,
                                                groupName = liveGroup?.name ?: group.name,
                                                remoteId = liveGroup?.remoteId ?: ""
                                            )
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Share,
                                            contentDescription = "Share",
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text("Share Link")
                                    }
                                }
                            }
                        }
                    }

                    // DASHBOARD TABLE
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            val totalSpent = expenses.sumOf { it.totalAmount }
                            Text(
                                "Total Spent: $${String.format("%.2f", totalSpent)}",
                                fontWeight = FontWeight.Bold
                            )
                            Button(
                                onClick = { showSettlementDialog = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                            ) {
                                Text("Settle Up")
                            }
                            HorizontalDivider(Modifier.padding(vertical = 8.dp))
                            members.forEach { member ->
                                val myShare =
                                    allShares.filter { it.memberId == member.id }.sumOf { it.amountOwed }
                                val myPaid =
                                    allPayers.filter { it.memberId == member.id }.sumOf { it.amountPaid }
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                ) {
                                    Text(member.name, Modifier.weight(2f))
                                    Text(
                                        String.format("%.0f", myShare),
                                        Modifier.weight(1f),
                                        textAlign = TextAlign.End
                                    )
                                    Text(
                                        String.format("%.0f", myPaid),
                                        Modifier.weight(1f),
                                        textAlign = TextAlign.End
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    Text("Members List", style = MaterialTheme.typography.titleMedium)
                    members.forEach { m ->
                        AssistChip(
                            onClick = {},
                            label = { Text("${m.name} (x${m.defaultWeight})") },
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            }
        }

        // ----------------------------------------------------------------
        // DIALOGS
        // ----------------------------------------------------------------

        if (showAddExpenseDialog) {
            AddExpenseDialog(
                members = members,
                onDismiss = { showAddExpenseDialog = false },
                onConfirm = { desc, amount, pMap, iMap ->
                    viewModel.addExpense(group.id, desc, amount, pMap, iMap)
                    showAddExpenseDialog = false
                }
            )
        }

        if (showAddMemberDialog) {
            AddMemberDialog(
                onDismiss = { showAddMemberDialog = false },
                onConfirm = { name, weight ->
                    viewModel.addMember(group.id, name, weight)
                    showAddMemberDialog = false
                }
            )
        }

        if (expenseToDelete != null) {
            AlertDialog(
                onDismissRequest = { expenseToDelete = null },
                title = { Text("Delete Expense?") },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.deleteExpense(expenseToDelete!!.id)
                        expenseToDelete = null
                    }) {
                        Text("Delete", color = Color.Red)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { expenseToDelete = null }) { Text("Cancel") }
                }
            )
        }

        if (showSettlementDialog) {
            val settlementPlan by viewModel.getSettlementPlan(group.id).collectAsState(initial = emptyList())
            AlertDialog(
                onDismissRequest = { showSettlementDialog = false },
                confirmButton = {
                    TextButton(onClick = { showSettlementDialog = false }) { Text("Done") }
                },
                title = { Text("Settlement Plan") },
                text = {
                    Column(Modifier.verticalScroll(rememberScrollState())) {
                        settlementPlan.forEach { s ->
                            Text("${s.fromName} pays ${s.toName}: $${String.format("%.2f", s.amount)}")
                            HorizontalDivider(Modifier.padding(vertical = 4.dp))
                        }
                        if (settlementPlan.isEmpty()) Text("No debts to settle!")
                    }
                }
            )
        }

        if (showQrDialog) {
            QrCodeDialog(
                groupName = liveGroup?.name ?: group.name,
                remoteId = liveGroup?.remoteId ?: "",
                onDismiss = { showQrDialog = false }
            )
        }
    }
}