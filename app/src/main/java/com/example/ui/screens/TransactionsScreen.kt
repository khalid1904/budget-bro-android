package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TransactionDetail
import com.example.ui.components.TransactionRowItem
import com.example.ui.viewmodel.BudgetViewModel
import com.example.ui.viewmodel.TransactionFilterType
import com.example.ui.viewmodel.TransactionSortOrder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    viewModel: BudgetViewModel,
    onSelectTransaction: (TransactionDetail) -> Unit,
    onAddTransaction: () -> Unit,
    onScanReceipt: () -> Unit,
    modifier: Modifier = Modifier
) {
    val userSettings by viewModel.userSettings.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val typeFilter by viewModel.typeFilter.collectAsState()
    val sortOrder by viewModel.sortOrder.collectAsState()
    val selectedCatId by viewModel.selectedCategoryIdFilter.collectAsState()
    val categories by viewModel.allCategories.collectAsState()
    val paymentMethods by viewModel.allPaymentMethods.collectAsState()
    val selectedPmId by viewModel.selectedPaymentMethodIdFilter.collectAsState()

    val groupedTransactions by viewModel.filteredTransactionsGrouped.collectAsState()

    var showSortMenu by remember { mutableStateOf(false) }
    var showCategoryFilterMenu by remember { mutableStateOf(false) }
    var showPaymentFilterMenu by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("transactions_screen")
    ) {
        // Search bar
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        placeholder = { Text("Search transactions...") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.Search,
                                contentDescription = "Search",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                    Icon(Icons.Rounded.Close, contentDescription = "Clear")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("transactions_search_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                        )
                    )

                    FilledTonalIconButton(
                        onClick = onScanReceipt,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .size(52.dp)
                            .testTag("btn_scan_receipt_transactions")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DocumentScanner,
                            contentDescription = "Scan UPI Receipt",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Filter & Sort Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Type Filter: ALL
                    FilterChip(
                        selected = typeFilter == TransactionFilterType.ALL,
                        onClick = { viewModel.setTypeFilter(TransactionFilterType.ALL) },
                        label = { Text("All") },
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Type Filter: EXPENSE
                    FilterChip(
                        selected = typeFilter == TransactionFilterType.EXPENSE,
                        onClick = { viewModel.setTypeFilter(TransactionFilterType.EXPENSE) },
                        label = { Text("Expenses") },
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Type Filter: INCOME
                    FilterChip(
                        selected = typeFilter == TransactionFilterType.INCOME,
                        onClick = { viewModel.setTypeFilter(TransactionFilterType.INCOME) },
                        label = { Text("Income") },
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Category Filter Chip
                    val selectedCategoryName = categories.find { it.id == selectedCatId }?.name
                    Box {
                        FilterChip(
                            selected = selectedCatId != null,
                            onClick = { showCategoryFilterMenu = true },
                            label = { Text(selectedCategoryName ?: "Category") },
                            trailingIcon = {
                                Icon(
                                    imageVector = if (selectedCatId != null) Icons.Rounded.Close else Icons.Rounded.ArrowDropDown,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clickable {
                                            if (selectedCatId != null) {
                                                viewModel.setCategoryFilter(null)
                                            } else {
                                                showCategoryFilterMenu = true
                                            }
                                        }
                                )
                            },
                            shape = RoundedCornerShape(12.dp)
                        )
                        DropdownMenu(
                            expanded = showCategoryFilterMenu,
                            onDismissRequest = { showCategoryFilterMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("All Categories") },
                                onClick = {
                                    viewModel.setCategoryFilter(null)
                                    showCategoryFilterMenu = false
                                }
                            )
                            categories.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat.name) },
                                    onClick = {
                                        viewModel.setCategoryFilter(cat.id)
                                        showCategoryFilterMenu = false
                                    }
                                )
                            }
                        }
                    }

                    // Sort Order Chip
                    Box {
                        AssistChip(
                            onClick = { showSortMenu = true },
                            label = {
                                Text(
                                    when (sortOrder) {
                                        TransactionSortOrder.NEWEST -> "Newest"
                                        TransactionSortOrder.OLDEST -> "Oldest"
                                        TransactionSortOrder.HIGHEST -> "Highest"
                                        TransactionSortOrder.LOWEST -> "Lowest"
                                    }
                                )
                            },
                            leadingIcon = {
                                Icon(Icons.Rounded.Sort, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            shape = RoundedCornerShape(12.dp)
                        )
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Newest first") },
                                onClick = {
                                    viewModel.setSortOrder(TransactionSortOrder.NEWEST)
                                    showSortMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Oldest first") },
                                onClick = {
                                    viewModel.setSortOrder(TransactionSortOrder.OLDEST)
                                    showSortMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Highest amount") },
                                onClick = {
                                    viewModel.setSortOrder(TransactionSortOrder.HIGHEST)
                                    showSortMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Lowest amount") },
                                onClick = {
                                    viewModel.setSortOrder(TransactionSortOrder.LOWEST)
                                    showSortMenu = false
                                }
                            )
                        }
                    }
                }
            }
        }

        // List Grouped by Date
        if (groupedTransactions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SearchOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No matching transactions",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Try adjusting your search or filters.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("transactions_list"),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                groupedTransactions.forEach { (dateHeader, txs) ->
                    item(key = "header_$dateHeader") {
                        Text(
                            text = dateHeader,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
                        )
                    }
                    items(txs, key = { it.id }) { tx ->
                        TransactionRowItem(
                            item = tx,
                            currencySymbol = userSettings.currencySymbol,
                            onClick = { onSelectTransaction(tx) }
                        )
                    }
                }
            }
        }
    }
}
