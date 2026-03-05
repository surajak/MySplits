package com.SohamProject.mysplits

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MySplitsTheme { // Assuming MySplitsTheme is defined elsewhere in your project
                val viewModel: SplitViewModel = viewModel()
                var selectedGroup by remember { mutableStateOf<Group?>(null) }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (selectedGroup == null) {
                        GroupListScreen(
                            viewModel = viewModel,
                            onGroupClick = { group -> selectedGroup = group }
                        )
                    } else {
                        GroupDetailsScreen(
                            group = selectedGroup!!,
                            viewModel = viewModel,
                            onBack = { selectedGroup = null } // Go back to GroupListScreen
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupListScreen(viewModel: SplitViewModel, onGroupClick: (Group) -> Unit) {
    val groups by viewModel.allGroups.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    var groupToDelete by remember { mutableStateOf<Group?>(null) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Split It") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, "Add Group")
            }
        }
    ) { padding ->
        if (groups.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No groups yet. Tap + to start!")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(items = groups, key = { it.id }) { group ->

                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = { newValue ->
                            if (newValue == SwipeToDismissBoxValue.EndToStart) {
                                groupToDelete = group
                                false
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

        if (showAddDialog) {
            var name by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { showAddDialog = false },
                title = { Text("New Group") },
                text = { OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }) },
                confirmButton = {
                    TextButton(onClick = {
                        if (name.isNotBlank()) {
                            viewModel.addGroup(name)
                            showAddDialog = false
                        }
                    }) { Text("Create") }
                }
            )
        }

        groupToDelete?.let { group ->
            AlertDialog(
                onDismissRequest = { groupToDelete = null },
                title = { Text("Delete Group") },
                text = { Text("Are you sure you want to delete '${group.name}'? All expenses inside will be lost.") },
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
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailsScreen(group: Group, viewModel: SplitViewModel, onBack: () -> Unit) {
    BackHandler { onBack() }

    // --- SCROLL STATE & FAB VISIBILITY MANAGEMENT ---

    // For Expenses LazyColumn
    val expensesListState = rememberLazyListState()
    var isExpensesFabVisible by remember { mutableStateOf(true) }
    var previousExpensesFirstVisibleItemIndex by remember(expensesListState) { mutableIntStateOf(expensesListState.firstVisibleItemIndex) }
    var previousExpensesScrollOffset by remember(expensesListState) { mutableIntStateOf(expensesListState.firstVisibleItemScrollOffset) }

    LaunchedEffect(expensesListState) {
        snapshotFlow {
            expensesListState.firstVisibleItemIndex to expensesListState.firstVisibleItemScrollOffset
        }
            .distinctUntilChanged()
            .collect { (currentIndex, currentOffset) ->
                if (currentIndex == 0 && currentOffset == 0) {
                    isExpensesFabVisible = true
                } else {
                    val isScrollingDown = currentIndex > previousExpensesFirstVisibleItemIndex ||
                            (currentIndex == previousExpensesFirstVisibleItemIndex && currentOffset > previousExpensesScrollOffset)
                    val isScrollingUp = currentIndex < previousExpensesFirstVisibleItemIndex ||
                            (currentIndex == previousExpensesFirstVisibleItemIndex && currentOffset < previousExpensesScrollOffset)

                    if (isScrollingDown) {
                        isExpensesFabVisible = false
                    } else if (isScrollingUp) {
                        isExpensesFabVisible = true
                    }
                }
                previousExpensesFirstVisibleItemIndex = currentIndex
                previousExpensesScrollOffset = currentOffset
            }
    }

    // For Group Info Column
    val groupInfoScrollState = rememberScrollState()
    var isGroupInfoFabVisible by remember { mutableStateOf(true) }
    var previousGroupInfoScrollOffset by remember(groupInfoScrollState) { mutableIntStateOf(groupInfoScrollState.value) }

    LaunchedEffect(groupInfoScrollState) {
        snapshotFlow { groupInfoScrollState.value }
            .distinctUntilChanged()
            .collect { currentOffset ->
                if (currentOffset == 0) {
                    isGroupInfoFabVisible = true
                } else {
                    val isScrollingDown = currentOffset > previousGroupInfoScrollOffset
                    val isScrollingUp = currentOffset < previousGroupInfoScrollOffset

                    if (isScrollingDown) {
                        isGroupInfoFabVisible = false
                    } else if (isScrollingUp) {
                        isGroupInfoFabVisible = true
                    }
                }
                previousGroupInfoScrollOffset = currentOffset
            }
    }
    // --- END SCROLL STATE & FAB VISIBILITY MANAGEMENT ---


    // --- OTHER UI STATES ---
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Expenses, 1 = Group Info

    val members by viewModel.getMembers(group.id).collectAsState(initial = emptyList())
    val expenses by viewModel.getExpenses(group.id).collectAsState(initial = emptyList())
    val allPayers by viewModel.getPayers(group.id).collectAsState(initial = emptyList())
    val allShares by viewModel.getGroupShares(group.id).collectAsState(initial = emptyList())

    var showAddExpenseDialog by remember { mutableStateOf(false) }
    var showAddMemberDialog by remember { mutableStateOf(false) }
    var showSettlementDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    var editingExpenseId by remember { mutableStateOf<Int?>(null) }
    var editData by remember { mutableStateOf<Triple<Expense, Map<Int, Double>, Map<Int, Double>>?>(null) }
    val scope = rememberCoroutineScope() // Needs to be here if used inside a Composable

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(group.name) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                actions = {
                    val context = androidx.compose.ui.platform.LocalContext.current
                    IconButton(onClick = {
                        PdfExporter.generateAndShare(context, group, members, expenses, allPayers, allShares, emptyList())
                    }) { Icon(Icons.Default.Send, "Export") }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    label = { Text("Expenses") },
                    icon = { Icon(Icons.Default.List, null) }
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
            // Determine which FAB visibility state to use based on the selected tab
            val currentFabVisible = if (selectedTab == 0) isExpensesFabVisible else isGroupInfoFabVisible

            AnimatedVisibility(
                visible = currentFabVisible, // Uses the appropriate visibility state
                enter = expandVertically(expandFrom = Alignment.Bottom) + fadeIn(),
                exit = shrinkVertically(shrinkTowards = Alignment.Bottom) + fadeOut()
            ) {
                ExtendedFloatingActionButton(
                    onClick = {
                        // FAB changes its behavior based on the active tab
                        if (selectedTab == 0) showAddExpenseDialog = true
                        else showAddMemberDialog = true
                    },
                    icon = { Icon(if (selectedTab == 0) Icons.Default.Add else Icons.Default.PersonAdd, null) },
                    text = { Text(if (selectedTab == 0) "Add Expense" else "Add Member") }
                )
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {

            if (selectedTab == 0) {
                Text("Group Expenses", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))

                LazyColumn(
                    state = expensesListState, // Attach the expenses list scroll state
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(expenses, key = { it.id }) { exp ->
                        val payersForExp = allPayers.filter { it.expenseId == exp.id }
                        val payerNames = payersForExp.mapNotNull { p -> members.find { it.id == p.memberId }?.name }

                        Card(
                            modifier = Modifier.fillMaxWidth().clickable {
                                scope.launch {
                                    val pMap = payersForExp.associate { it.memberId to it.amountPaid }
                                    val shares = viewModel.getExpenseShares(exp.id)
                                    val sMap = shares.associate { it.memberId to it.weight }
                                    editData = Triple(exp, pMap, sMap)
                                    editingExpenseId = exp.id
                                }
                            }
                        ) {
                            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(exp.description, fontWeight = FontWeight.Bold)
                                    Text("Paid by ${payerNames.joinToString(", ")}", style = MaterialTheme.typography.bodySmall)
                                }
                                Text("$${String.format("%.2f", exp.totalAmount)}", fontWeight = FontWeight.Bold)
                                Spacer(Modifier.width(8.dp))
                                IconButton(onClick = {
                                    editingExpenseId = exp.id
                                    showDeleteConfirm = true
                                }) {
                                    Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            } else {
                // --- TAB 1: GROUP INFO & SETTLEMENT ---
                // Attach the group info scroll state here
                Column(modifier = Modifier.fillMaxSize().verticalScroll(groupInfoScrollState)) {

                    // Dashboard Card (The Table)
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            val totalSpent = expenses.sumOf { it.totalAmount }
                            Text("Total Group Spend: $${String.format("%.2f", totalSpent)}", fontWeight = FontWeight.Bold)

                            Button(
                                onClick = { showSettlementDialog = true },
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                            ) { Text("View Settlement Plan") }

                            HorizontalDivider(Modifier.padding(vertical = 8.dp))

                            // Table Header
                            Row(Modifier.fillMaxWidth()) {
                                Text("Member", Modifier.weight(2f), fontWeight = FontWeight.Bold)
                                Text("Share", Modifier.weight(1f), fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
                                Text("Paid", Modifier.weight(1f), fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
                            }

                            members.forEach { member ->
                                val myShare = allShares.filter { it.memberId == member.id }.sumOf { it.amountOwed }
                                val myPaid = allPayers.filter { it.memberId == member.id }.sumOf { it.amountPaid }
                                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                    Text(member.name, Modifier.weight(2f))
                                    Text(String.format("%.0f", myShare), Modifier.weight(1f), textAlign = TextAlign.End)
                                    Text(String.format("%.0f", myPaid), Modifier.weight(1f), textAlign = TextAlign.End)
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(24.dp))
                    Text("Members List", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))

                    members.forEach { m ->
                        AssistChip(
                            onClick = { /* Could add edit member here if needed */ },
                            label = { Text("${m.name} (Weight: ${m.defaultWeight})") },
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            }
        }

        // --- DIALOGS (Kept outside Column for proper layering) ---

        if (showAddExpenseDialog) {
            AddExpenseDialog(
                members = members,
                onDismiss = { showAddExpenseDialog = false },
                onConfirm = { desc, amount, payerMap, involvedMap ->
                    viewModel.addExpense(group.id, desc, amount, payerMap, involvedMap)
                    showAddExpenseDialog = false
                }
            )
        }

        if (showAddMemberDialog) {
            AddMemberDialog(onDismiss = { showAddMemberDialog = false }, onConfirm = { name, weight ->
                viewModel.addMember(group.id, name, weight)
                showAddMemberDialog = false
            })
        }

        if (editingExpenseId != null && editData != null) {
            val (exp, pMap, sMap) = editData!!
            AddExpenseDialog(
                members = members,
                initialDescription = exp.description,
                initialAmount = exp.totalAmount,
                initialPayers = pMap,
                initialWeights = sMap,
                onDismiss = {
                    editingExpenseId = null
                    editData = null
                },
                onConfirm = { desc, amount, payerMap, involvedMap ->
                    viewModel.deleteExpense(exp.id)
                    viewModel.addExpense(group.id, desc, amount, payerMap, involvedMap)
                    editingExpenseId = null
                    editData = null
                }
            )
        }

        if (showDeleteConfirm) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = false },
                title = { Text("Delete Expense?") },
                text = { Text("Are you sure you want to delete this?") },
                confirmButton = {
                    TextButton(onClick = {
                        editingExpenseId?.let { viewModel.deleteExpense(it) }
                        showDeleteConfirm = false
                        editingExpenseId = null
                    }) { Text("Delete", color = Color.Red) }
                },
                dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") } }
            )
        }

        if (showSettlementDialog) {
            val settlementPlan by viewModel.getSettlementPlan(group.id).collectAsState(initial = emptyList())
            AlertDialog(
                onDismissRequest = { showSettlementDialog = false },
                confirmButton = { TextButton(onClick = { showSettlementDialog = false }) { Text("Done") } },
                title = { Text("Settlement") },
                text = {
                    Column(Modifier.verticalScroll(rememberScrollState())) {
                        settlementPlan.forEach { s ->
                            Text("${s.fromName} pays ${s.toName}: $${String.format("%.2f", s.amount)}")
                            HorizontalDivider(Modifier.padding(vertical = 4.dp))
                        }
                        if (settlementPlan.isEmpty()) Text("No debts!")
                    }
                }
            )
        }
    }
}
