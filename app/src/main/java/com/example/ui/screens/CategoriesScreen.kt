package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.CategoryEntity
import com.example.ui.components.CategoryIconHelper
import com.example.ui.theme.FinanceRed
import com.example.ui.viewmodel.BudgetViewModel

@Composable
fun CategoriesScreen(
    viewModel: BudgetViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val categories by viewModel.allCategories.collectAsState()
    var selectedTab by remember { mutableStateOf("EXPENSE") }
    var showAddDialog by remember { mutableStateOf(false) }

    var snackbarMessage by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            snackbarMessage = null
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Surface(tonalElevation = 2.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Back")
                    }
                    Text(
                        text = "Manage Categories",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("btn_fab_add_category")
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Add Category")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("categories_screen")
        ) {
            // Tabs (Expense / Income)
            TabRow(
                selectedTabIndex = if (selectedTab == "EXPENSE") 0 else 1,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = selectedTab == "EXPENSE",
                    onClick = { selectedTab = "EXPENSE" },
                    text = { Text("Expense Categories") }
                )
                Tab(
                    selected = selectedTab == "INCOME",
                    onClick = { selectedTab = "INCOME" },
                    text = { Text("Income Categories") }
                )
            }

            val filtered = categories.filter { it.type == selectedTab }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filtered, key = { it.id }) { cat ->
                    val color = CategoryIconHelper.parseColor(cat.colorHex)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(color.copy(alpha = 0.16f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = CategoryIconHelper.getIcon(cat.iconName),
                                        contentDescription = cat.name,
                                        tint = color,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column {
                                    Text(
                                        text = cat.name,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (cat.isDefault) {
                                        Text(
                                            text = "Default category",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            if (!cat.isDefault) {
                                IconButton(
                                    onClick = {
                                        viewModel.deleteCategory(cat) { success ->
                                            if (!success) {
                                                snackbarMessage = "Cannot delete: Category has transactions linked to it"
                                            }
                                        }
                                    }
                                ) {
                                    Icon(Icons.Rounded.Delete, contentDescription = "Delete", tint = FinanceRed)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        var newName by remember { mutableStateOf("") }
        var selectedIcon by remember { mutableStateOf("LocalGasStation") }
        var selectedColor by remember { mutableStateOf("#42A5F5") }

        val availableIcons = listOf(
            "LocalGasStation", "Coffee", "MedicalServices", "Pets",
            "SportsEsports", "LocalHospital", "Celebration", "School",
            "Flight", "Restaurant", "ShoppingCart", "Store"
        )

        val availableColors = listOf(
            "#EF5350", "#EC407A", "#AB47BC", "#5C6BC0",
            "#42A5F5", "#26A69A", "#66BB6A", "#FFA726",
            "#8D6E63", "#78909C"
        )

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add New ${if (selectedTab == "EXPENSE") "Expense" else "Income"} Category") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Category Name") },
                        placeholder = { Text("e.g. Fuel, Pet Care, Gaming") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    Text("Select Icon", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        availableIcons.take(6).forEach { iconName ->
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (selectedIcon == iconName) MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .clickable { selectedIcon = iconName },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = CategoryIconHelper.getIcon(iconName),
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = if (selectedIcon == iconName) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Select Color", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        availableColors.take(6).forEach { hex ->
                            val color = CategoryIconHelper.parseColor(hex)
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .clickable { selectedColor = hex },
                                contentAlignment = Alignment.Center
                            ) {
                                if (selectedColor == hex) {
                                    Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newName.isNotBlank()) {
                            viewModel.addCategory(newName.trim(), selectedTab, selectedIcon, selectedColor)
                            showAddDialog = false
                        }
                    },
                    enabled = newName.isNotBlank()
                ) {
                    Text("Add Category")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
