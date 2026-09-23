package com.example.ui.dialogs

import android.app.DatePickerDialog
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.CategoryEntity
import com.example.data.model.PaymentMethodEntity
import com.example.data.model.TransactionDetail
import com.example.data.ocr.ReceiptParser
import com.example.ui.components.CategoryIconHelper
import com.example.ui.theme.FinanceGreen
import com.example.ui.theme.FinanceRed
import com.example.util.CurrencyFormatter
import com.example.util.DateUtils
import kotlinx.coroutines.launch
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTransactionDialog(
    initialType: String = "EXPENSE",
    existingTransaction: TransactionDetail? = null,
    categories: List<CategoryEntity>,
    paymentMethods: List<PaymentMethodEntity>,
    currencySymbol: String = "₹",
    onDismiss: () -> Unit,
    onSave: (
        type: String,
        amountPaise: Long,
        categoryId: Long,
        title: String,
        merchant: String?,
        date: Long,
        paymentMethodId: Long,
        notes: String?,
        isRecurring: Boolean,
        recurringFrequency: String
    ) -> Unit
) {
    val context = LocalContext.current
    var type by remember { mutableStateOf(existingTransaction?.type ?: initialType) }
    var amountInput by remember {
        mutableStateOf(
            if (existingTransaction != null) CurrencyFormatter.paiseToInputString(existingTransaction.amountPaise) else ""
        )
    }
    var title by remember { mutableStateOf(existingTransaction?.title ?: "") }
    var merchant by remember { mutableStateOf(existingTransaction?.merchant ?: "") }
    var notes by remember { mutableStateOf(existingTransaction?.notes ?: "") }
    var selectedDate by remember { mutableStateOf(existingTransaction?.date ?: System.currentTimeMillis()) }

    val filteredCategories = remember(categories, type) {
        categories.filter { it.type == type }
    }

    var selectedCategoryId by remember(filteredCategories) {
        mutableStateOf(
            existingTransaction?.categoryId ?: filteredCategories.firstOrNull()?.id ?: 1L
        )
    }

    var selectedPaymentMethodId by remember(paymentMethods) {
        mutableStateOf(
            existingTransaction?.paymentMethodId ?: paymentMethods.firstOrNull()?.id ?: 1L
        )
    }

    var isRecurring by remember { mutableStateOf(false) }
    var recurringFrequency by remember { mutableStateOf("MONTHLY") }
    var showAdvanced by remember { mutableStateOf(existingTransaction != null) }

    val cal = Calendar.getInstance().apply { timeInMillis = selectedDate }
    val coroutineScope = rememberCoroutineScope()
    var isScanningReceipt by remember { mutableStateOf(false) }

    val receiptPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                isScanningReceipt = true
                try {
                    val parser = ReceiptParser(context)
                    val res = parser.parseReceiptImage(uri)
                    if (res.amount != null) {
                        amountInput = String.format(java.util.Locale.US, "%.2f", res.amount).removeSuffix(".00").removeSuffix(".0")
                    }
                    if (!res.merchant.isNullOrBlank()) {
                        merchant = res.merchant
                        if (title.isBlank()) title = res.merchant
                    }
                    if (!res.notes.isNullOrBlank()) {
                        notes = res.notes
                    }
                    if (res.transactionDate != null) {
                        selectedDate = res.transactionDate
                    }
                    if (!res.suggestedCategoryName.isNullOrBlank()) {
                        filteredCategories.find {
                            it.name.contains(res.suggestedCategoryName, ignoreCase = true) ||
                            res.suggestedCategoryName.contains(it.name, ignoreCase = true)
                        }?.let { selectedCategoryId = it.id }
                    }
                } catch (e: Exception) {
                    // Fallback to manual entry
                } finally {
                    isScanningReceipt = false
                }
            }
        }
    }

    val datePickerDialog = remember {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val newCal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                }
                selectedDate = newCal.timeInMillis
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.92f)
                .testTag("add_transaction_dialog"),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (existingTransaction != null) "Edit Transaction" else "Add Transaction",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    if (type == "EXPENSE") {
                        FilledTonalButton(
                            onClick = {
                                receiptPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier
                                .height(38.dp)
                                .testTag("btn_scan_receipt_in_dialog")
                        ) {
                            if (isScanningReceipt) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.DocumentScanner,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Scan",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Type Toggle (Expense / Income)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (type == "EXPENSE") FinanceRed else Color.Transparent)
                            .clickable {
                                type = "EXPENSE"
                                filteredCategories.firstOrNull { it.type == "EXPENSE" }?.let {
                                    selectedCategoryId = it.id
                                }
                            }
                            .padding(vertical = 10.dp)
                            .testTag("btn_type_expense"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Expense",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = if (type == "EXPENSE") Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (type == "INCOME") FinanceGreen else Color.Transparent)
                            .clickable {
                                type = "INCOME"
                                filteredCategories.firstOrNull { it.type == "INCOME" }?.let {
                                    selectedCategoryId = it.id
                                }
                            }
                            .padding(vertical = 10.dp)
                            .testTag("btn_type_income"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Income",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = if (type == "INCOME") Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Large Amount Input Field
                OutlinedTextField(
                    value = amountInput,
                    onValueChange = { input ->
                        if (input.matches(Regex("^\\d*(\\.\\d{0,2})?$"))) {
                            amountInput = input
                        }
                    },
                    leadingIcon = {
                        Text(
                            text = currencySymbol,
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (type == "EXPENSE") FinanceRed else FinanceGreen
                        )
                    },
                    placeholder = {
                        Text(
                            text = "0",
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    },
                    textStyle = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Start
                    ),
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_transaction_amount")
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Select Category Grid
                Text(
                    text = "Category",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Category chips scroll row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    filteredCategories.forEach { cat ->
                        val isSelected = cat.id == selectedCategoryId
                        val iconColor = CategoryIconHelper.parseColor(cat.colorHex)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedCategoryId = cat.id
                                if (title.isBlank()) {
                                    title = cat.name
                                }
                            },
                            label = { Text(cat.name) },
                            leadingIcon = {
                                Icon(
                                    imageVector = CategoryIconHelper.getIcon(cat.iconName),
                                    contentDescription = null,
                                    tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else iconColor,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("chip_category_${cat.name.lowercase()}")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Payment Method Selector
                Text(
                    text = "Payment Method",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    paymentMethods.forEach { pm ->
                        val isSelected = pm.id == selectedPaymentMethodId
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedPaymentMethodId = pm.id },
                            label = { Text(pm.name) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("chip_payment_${pm.name.lowercase()}")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Date Picker Chip
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Date",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedButton(
                        onClick = { datePickerDialog.show() },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Rounded.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(DateUtils.formatRelativeDate(selectedDate))
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Toggle for Advanced Details (Merchant, Notes, Recurring)
                TextButton(
                    onClick = { showAdvanced = !showAdvanced },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text(if (showAdvanced) "Hide additional fields" else "+ Add merchant, notes or repeat")
                }

                if (showAdvanced) {
                    Spacer(modifier = Modifier.height(8.dp))

                    // Merchant / Store Name
                    OutlinedTextField(
                        value = merchant,
                        onValueChange = { merchant = it },
                        label = { Text("Merchant / Store Name (optional)") },
                        placeholder = { Text("e.g. Swiggy, Amazon, DMart, Metro") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_merchant_name")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Title / Description
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Title / Description") },
                        placeholder = { Text("e.g. Dinner with friends, Grocery restock") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_transaction_title")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Notes
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notes (optional)") },
                        placeholder = { Text("e.g. Paid half by Ramesh, bill split") },
                        maxLines = 3,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Recurring Toggle
                    if (existingTransaction == null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Recurring Transaction",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                )
                                Text(
                                    text = "Repeat this transaction automatically",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = isRecurring,
                                onCheckedChange = { isRecurring = it }
                            )
                        }

                        if (isRecurring) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("MONTHLY", "WEEKLY", "DAILY").forEach { freq ->
                                    FilterChip(
                                        selected = recurringFrequency == freq,
                                        onClick = { recurringFrequency = freq },
                                        label = { Text(freq.lowercase().replaceFirstChar { it.uppercase() }) }
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.height(16.dp))

                // Save Button
                val parsedPaise = CurrencyFormatter.parseToPaise(amountInput)
                Button(
                    onClick = {
                        if (parsedPaise > 0) {
                            val finalTitle = title.ifBlank {
                                merchant.ifBlank {
                                    filteredCategories.find { it.id == selectedCategoryId }?.name ?: "Transaction"
                                }
                            }
                            onSave(
                                type,
                                parsedPaise,
                                selectedCategoryId,
                                finalTitle,
                                merchant.takeIf { it.isNotBlank() },
                                selectedDate,
                                selectedPaymentMethodId,
                                notes.takeIf { it.isNotBlank() },
                                isRecurring,
                                recurringFrequency
                            )
                        }
                    },
                    enabled = parsedPaise > 0,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("btn_save_transaction"),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = if (existingTransaction != null) "Update Transaction" else "Save Transaction",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}
