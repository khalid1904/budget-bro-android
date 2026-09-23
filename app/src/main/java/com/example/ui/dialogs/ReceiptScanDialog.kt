package com.example.ui.dialogs

import android.app.DatePickerDialog
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.CategoryEntity
import com.example.data.model.PaymentMethodEntity
import com.example.data.ocr.ReceiptParser
import com.example.data.ocr.ReceiptScanResult
import com.example.ui.components.CategoryIconHelper
import com.example.ui.theme.FinanceGreen
import com.example.ui.theme.FinanceRed
import com.example.util.CurrencyFormatter
import com.example.util.DateUtils
import kotlinx.coroutines.launch
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptScanDialog(
    imageUri: Uri,
    categories: List<CategoryEntity>,
    paymentMethods: List<PaymentMethodEntity>,
    currencySymbol: String = "₹",
    onDismiss: () -> Unit,
    onSaveExpense: (
        amountPaise: Long,
        categoryId: Long,
        title: String,
        merchant: String?,
        date: Long,
        paymentMethodId: Long,
        notes: String?
    ) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var isScanning by remember { mutableStateOf(true) }
    var scanResult by remember { mutableStateOf<ReceiptScanResult?>(null) }
    var scanError by remember { mutableStateOf<String?>(null) }

    var amountInput by remember { mutableStateOf("") }
    var merchantInput by remember { mutableStateOf("") }
    var notesInput by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf(System.currentTimeMillis()) }

    val expenseCategories = remember(categories) {
        categories.filter { it.type == "EXPENSE" }
    }

    var selectedCategoryId by remember(expenseCategories) {
        mutableStateOf(expenseCategories.firstOrNull()?.id ?: 1L)
    }

    var selectedPaymentMethodId by remember(paymentMethods) {
        // default to UPI payment method if found
        val upiMethod = paymentMethods.find { it.type.equals("UPI", ignoreCase = true) }
        mutableStateOf(upiMethod?.id ?: paymentMethods.firstOrNull()?.id ?: 1L)
    }

    // Trigger OCR / AI analysis on launch
    LaunchedEffect(imageUri) {
        isScanning = true
        scanError = null
        try {
            val parser = ReceiptParser(context)
            val result = parser.parseReceiptImage(imageUri)
            scanResult = result

            if (result.amount != null) {
                amountInput = String.format(java.util.Locale.US, "%.2f", result.amount)
                    .removeSuffix(".00")
                    .removeSuffix(".0")
            }
            if (!result.merchant.isNullOrBlank()) {
                merchantInput = result.merchant
            }
            if (!result.notes.isNullOrBlank()) {
                notesInput = result.notes
            }
            if (result.transactionDate != null) {
                selectedDate = result.transactionDate
            }

            // Match suggested category
            if (!result.suggestedCategoryName.isNullOrBlank()) {
                val matched = expenseCategories.find {
                    it.name.contains(result.suggestedCategoryName, ignoreCase = true) ||
                            result.suggestedCategoryName.contains(it.name, ignoreCase = true)
                }
                if (matched != null) {
                    selectedCategoryId = matched.id
                }
            }

            // Match payment method
            if (!result.paymentMethod.isNullOrBlank()) {
                val matchedMethod = paymentMethods.find {
                    it.name.contains(result.paymentMethod, ignoreCase = true) ||
                            it.type.contains(result.paymentMethod, ignoreCase = true)
                }
                if (matchedMethod != null) {
                    selectedPaymentMethodId = matchedMethod.id
                }
            }
        } catch (e: Exception) {
            scanError = "Could not parse receipt automatically: ${e.localizedMessage}"
        } finally {
            isScanning = false
        }
    }

    val cal = Calendar.getInstance().apply { timeInMillis = selectedDate }
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
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.92f)
                .testTag("dialog_receipt_scan")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.DocumentScanner,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Scan UPI Receipt",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "AI & OCR Expense Extractor",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("btn_close_receipt_scan")
                    ) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Content scrollable
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(scrollState)
                ) {
                    // Image thumbnail card with scan overlay
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(imageUri)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Receipt Screenshot",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )

                            // Scrim overlay at bottom
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.BottomCenter)
                                    .background(Color.Black.copy(alpha = 0.65f))
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    if (isScanning) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(16.dp),
                                                color = Color.White,
                                                strokeWidth = 2.dp
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Analyzing receipt image...",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.White
                                            )
                                        }
                                    } else {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            val isGemini = scanResult?.source == "GEMINI_AI"
                                            Icon(
                                                imageVector = if (isGemini) Icons.Rounded.AutoAwesome else Icons.Rounded.Shield,
                                                contentDescription = null,
                                                tint = if (isGemini) Color(0xFFFBBF24) else FinanceGreen,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = if (isGemini) "Gemini AI Vision Extracted" else "ML Kit On-Device OCR",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                                color = Color.White
                                            )
                                        }

                                        if (scanResult?.referenceId != null) {
                                            Text(
                                                text = "Ref: ${scanResult?.referenceId?.takeLast(6)}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.White.copy(alpha = 0.8f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Amount Input Field (Prominent)
                    Text(
                        text = "Amount *",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = amountInput,
                        onValueChange = { input ->
                            if (input.isEmpty() || input.matches(Regex("""^\d*\.?\d{0,2}$"""))) {
                                amountInput = input
                            }
                        },
                        leadingIcon = {
                            Text(
                                text = currencySymbol,
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = FinanceRed,
                                modifier = Modifier.padding(start = 12.dp, end = 4.dp)
                            )
                        },
                        placeholder = { Text("0.00", style = MaterialTheme.typography.headlineMedium.copy(color = MaterialTheme.colorScheme.outline)) },
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                        ),
                        textStyle = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, color = FinanceRed),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_receipt_amount")
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Merchant / Title
                    Text(
                        text = "Merchant / Payee Name *",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = merchantInput,
                        onValueChange = { merchantInput = it },
                        leadingIcon = {
                            Icon(Icons.Rounded.Storefront, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        },
                        placeholder = { Text("e.g. Starbucks, Swiggy, Zepto") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_receipt_merchant")
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Category Selection
                    Text(
                        text = "Category *",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 160.dp)
                    ) {
                        items(expenseCategories) { cat ->
                            val isSelected = cat.id == selectedCategoryId
                            val catColor = CategoryIconHelper.parseColor(cat.colorHex)

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) catColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, catColor) else null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedCategoryId = cat.id }
                                    .testTag("receipt_cat_${cat.id}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(catColor.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = CategoryIconHelper.getIcon(cat.iconName),
                                            contentDescription = null,
                                            tint = catColor,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = cat.name,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Payment Method Chips
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
                        paymentMethods.forEach { method ->
                            val isSelected = method.id == selectedPaymentMethodId
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedPaymentMethodId = method.id },
                                label = { Text(method.name) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = when {
                                            method.type.equals("UPI", ignoreCase = true) -> Icons.Rounded.QrCodeScanner
                                            method.type.equals("CARD", ignoreCase = true) -> Icons.Rounded.CreditCard
                                            method.type.equals("BANK", ignoreCase = true) -> Icons.Rounded.AccountBalance
                                            else -> Icons.Rounded.Payments
                                        },
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                modifier = Modifier.testTag("receipt_pay_method_${method.id}")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Date selector
                    Text(
                        text = "Date",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedCard(
                        onClick = { datePickerDialog.show() },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Rounded.CalendarMonth,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = DateUtils.formatFullDate(selectedDate),
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                )
                            }
                            Icon(
                                Icons.Rounded.EditCalendar,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Notes & Transaction Reference
                    Text(
                        text = "Notes & Reference",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = notesInput,
                        onValueChange = { notesInput = it },
                        placeholder = { Text("UPI Ref / UTR / Order ID / Remarks") },
                        leadingIcon = {
                            Icon(Icons.Rounded.Notes, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        },
                        singleLine = false,
                        maxLines = 2,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Bottom Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Discard")
                    }

                    Button(
                        onClick = {
                            val parsedAmount = amountInput.toDoubleOrNull() ?: 0.0
                            val amountPaise = (parsedAmount * 100).toLong()
                            if (amountPaise > 0) {
                                val finalTitle = merchantInput.trim().ifEmpty { "UPI Expense" }
                                onSaveExpense(
                                    amountPaise,
                                    selectedCategoryId,
                                    finalTitle,
                                    merchantInput.trim().takeIf { it.isNotEmpty() },
                                    selectedDate,
                                    selectedPaymentMethodId,
                                    notesInput.trim().takeIf { it.isNotEmpty() }
                                )
                            }
                        },
                        enabled = (amountInput.toDoubleOrNull() ?: 0.0) > 0.0 && !isScanning,
                        colors = ButtonDefaults.buttonColors(containerColor = FinanceRed),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1.4f)
                            .testTag("btn_save_receipt_expense")
                    ) {
                        Icon(Icons.Rounded.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Save Expense",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }
    }
}
