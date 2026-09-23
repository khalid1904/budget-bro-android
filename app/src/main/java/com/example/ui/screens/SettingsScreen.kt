package com.example.ui.screens

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.drive.DrivePreferences
import com.example.data.drive.GoogleDriveService
import com.example.ui.theme.FinanceGreen
import com.example.ui.theme.FinanceRed
import com.example.ui.viewmodel.BudgetViewModel
import com.example.util.CurrencyFormatter
import com.example.util.DateUtils
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: BudgetViewModel,
    onNavigateToCategories: () -> Unit,
    onNavigateToGoals: () -> Unit,
    onNavigateToRecurring: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val userSettings by viewModel.userSettings.collectAsState()

    val drivePrefs = remember { DrivePreferences(context) }
    var signedInAccount by remember {
        mutableStateOf<GoogleSignInAccount?>(GoogleSignIn.getLastSignedInAccount(context))
    }
    var periodicBackupEnabled by remember {
        mutableStateOf(drivePrefs.isPeriodicBackupEnabled)
    }
    var periodicFrequency by remember {
        mutableStateOf(drivePrefs.periodicFrequency)
    }
    var lastBackupTimestamp by remember {
        mutableLongStateOf(drivePrefs.lastBackupTime)
    }

    val isDriveBackingUp by viewModel.isDriveBackingUp.collectAsState()
    val isDriveRestoring by viewModel.isDriveRestoring.collectAsState()
    val driveMessage by viewModel.driveBackupMessage.collectAsState()

    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showDemoDataDialog by remember { mutableStateOf(false) }
    var showClearDataDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var showConfirmDriveRestoreDialog by remember { mutableStateOf(false) }
    var exportedJsonText by remember { mutableStateOf("") }
    var importJsonInput by remember { mutableStateOf("") }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Google Sign-In launcher for Google Drive backup authorization
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.result
                if (account != null) {
                    signedInAccount = account
                    drivePrefs.userEmail = account.email
                    snackbarMessage = "Connected to Google account: ${account.email}"
                }
            } catch (e: Exception) {
                snackbarMessage = "Google Sign-In failed: ${e.localizedMessage}"
            }
        }
    }

    LaunchedEffect(driveMessage) {
        driveMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearDriveMessage()
            lastBackupTimestamp = drivePrefs.lastBackupTime
        }
    }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            snackbarMessage = null
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("settings_screen"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            item {
                Column {
                    Text(
                        text = "Settings & Privacy",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Personalize your experience and manage data",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Google Drive Backup & Sync Card (Prominent & Requested)
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("card_google_drive_backup")
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.CloudSync,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Google Drive Backup",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (signedInAccount != null) {
                                        "Connected: ${signedInAccount?.email}"
                                    } else {
                                        "Secure on-demand & periodic cloud backup"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        Spacer(modifier = Modifier.height(14.dp))

                        // Account connection or disconnect
                        if (signedInAccount == null) {
                            Button(
                                onClick = {
                                    val gso = GoogleDriveService.getGoogleSignInOptions()
                                    val signInClient = GoogleSignIn.getClient(context, gso)
                                    googleSignInLauncher.launch(signInClient.signInIntent)
                                },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("btn_connect_google_drive")
                            ) {
                                Icon(Icons.Rounded.CloudUpload, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Connect Google Drive")
                            }
                        } else {
                            // On-Demand Backup & Restore buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = {
                                        signedInAccount?.let { account ->
                                            viewModel.backupToGoogleDrive(context, account)
                                        }
                                    },
                                    enabled = !isDriveBackingUp && !isDriveRestoring,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("btn_backup_now_drive")
                                ) {
                                    if (isDriveBackingUp) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Backing up...", style = MaterialTheme.typography.labelMedium)
                                    } else {
                                        Icon(Icons.Rounded.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Backup Now", style = MaterialTheme.typography.labelMedium)
                                    }
                                }

                                OutlinedButton(
                                    onClick = {
                                        showConfirmDriveRestoreDialog = true
                                    },
                                    enabled = !isDriveBackingUp && !isDriveRestoring,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("btn_restore_now_drive")
                                ) {
                                    if (isDriveRestoring) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            color = MaterialTheme.colorScheme.primary,
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Restoring...", style = MaterialTheme.typography.labelMedium)
                                    } else {
                                        Icon(Icons.Rounded.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Restore", style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Last Backup Time
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Last Google Drive Backup",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = if (lastBackupTimestamp > 0L) {
                                        DateUtils.formatDateTime(lastBackupTimestamp)
                                    } else {
                                        "Never"
                                    },
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                            Spacer(modifier = Modifier.height(14.dp))

                            // Periodic Backup Toggle
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Periodic Auto-Backup",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                    )
                                    Text(
                                        text = "Automatically backs up to Google Drive in the background",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = periodicBackupEnabled,
                                    onCheckedChange = { isEnabled ->
                                        periodicBackupEnabled = isEnabled
                                        viewModel.setPeriodicBackupEnabled(context, isEnabled, periodicFrequency)
                                        snackbarMessage = if (isEnabled) {
                                            "Periodic backup enabled ($periodicFrequency)"
                                        } else {
                                            "Periodic backup disabled"
                                        }
                                    },
                                    modifier = Modifier.testTag("switch_periodic_backup")
                                )
                            }

                            if (periodicBackupEnabled) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Backup Frequency",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf("DAILY", "WEEKLY", "MONTHLY").forEach { freq ->
                                        val isSelected = periodicFrequency.equals(freq, ignoreCase = true)
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = {
                                                periodicFrequency = freq
                                                viewModel.setPeriodicBackupEnabled(context, true, freq)
                                                snackbarMessage = "Backup frequency set to $freq"
                                            },
                                            label = {
                                                Text(
                                                    when (freq) {
                                                        "DAILY" -> "Daily"
                                                        "WEEKLY" -> "Weekly"
                                                        else -> "Monthly"
                                                    }
                                                )
                                            },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            // Disconnect option
                            TextButton(
                                onClick = {
                                    val gso = GoogleDriveService.getGoogleSignInOptions()
                                    val signInClient = GoogleSignIn.getClient(context, gso)
                                    signInClient.signOut().addOnCompleteListener {
                                        signedInAccount = null
                                        drivePrefs.clear()
                                        viewModel.setPeriodicBackupEnabled(context, false)
                                        periodicBackupEnabled = false
                                        snackbarMessage = "Disconnected from Google Drive"
                                    }
                                },
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text("Disconnect Account", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }

            // Privacy Assurance Card
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(FinanceGreen),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Security,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.surface,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "100% Private & Safe",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Your data resides locally on your device. Google Drive backups use restricted file scope ('drive.file'), meaning Budget Bro only accesses the files it creates.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Profile & Currency Preferences Card
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Profile & Currency",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            TextButton(onClick = { showEditProfileDialog = true }) {
                                Text("Edit")
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        SettingsItemRow(label = "Name", value = userSettings.userName)
                        Spacer(modifier = Modifier.height(8.dp))
                        SettingsItemRow(label = "Currency Symbol", value = "${userSettings.currencySymbol} (${userSettings.currencyCode})")
                        Spacer(modifier = Modifier.height(8.dp))
                        SettingsItemRow(
                            label = "Default Monthly Budget",
                            value = CurrencyFormatter.formatPaise(userSettings.defaultMonthlyBudgetPaise, userSettings.currencySymbol)
                        )
                    }
                }
            }

            // Management Navigation Links
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        SettingsNavRow(
                            icon = Icons.Rounded.Category,
                            title = "Categories",
                            subtitle = "Customize expense & income categories",
                            onClick = onNavigateToCategories
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                        SettingsNavRow(
                            icon = Icons.Rounded.EmojiEvents,
                            title = "Financial Goals",
                            subtitle = "Track savings for milestones",
                            onClick = onNavigateToGoals
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                        SettingsNavRow(
                            icon = Icons.Rounded.Autorenew,
                            title = "Recurring Transactions",
                            subtitle = "Subscriptions, bills & automated tracking",
                            onClick = onNavigateToRecurring
                        )
                    }
                }
            }

            // Notification / Alert Preferences
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "Alerts & Notifications",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Budget Limit Warnings",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                )
                                Text(
                                    text = "Alert when reaching 80% or 100% of budget",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = userSettings.budgetAlertsEnabled,
                                onCheckedChange = { isChecked ->
                                    viewModel.updateUserSettings(userSettings.copy(budgetAlertsEnabled = isChecked))
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Recurring Reminders",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                )
                                Text(
                                    text = "Remind upcoming subscription payments",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = userSettings.recurringRemindersEnabled,
                                onCheckedChange = { isChecked ->
                                    viewModel.updateUserSettings(userSettings.copy(recurringRemindersEnabled = isChecked))
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        Spacer(modifier = Modifier.height(14.dp))

                        OutlinedButton(
                            onClick = {
                                viewModel.triggerTestBudgetAlert(context)
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Test 80% budget alert notification sent!")
                                }
                            },
                            enabled = userSettings.budgetAlertsEnabled,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("btn_test_budget_notification")
                        ) {
                            Icon(Icons.Rounded.NotificationsActive, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Send Test 80% Budget Alert")
                        }
                    }
                }
            }

            // Local Data Management (Demo Data, Export, Import, Reset)
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "Local Backup & Device Data",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        // Load Demo Data Button
                        OutlinedButton(
                            onClick = { showDemoDataDialog = true },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("btn_load_demo_data")
                        ) {
                            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = FinanceGreen)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Load Realistic Demo Data (India)")
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Export & Import Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    coroutineScope.launch {
                                        exportedJsonText = viewModel.exportJson()
                                        showExportDialog = true
                                    }
                                },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("btn_export_json")
                            ) {
                                Icon(Icons.Rounded.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Export JSON")
                            }

                            OutlinedButton(
                                onClick = { showImportDialog = true },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("btn_import_json")
                            ) {
                                Icon(Icons.Rounded.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Restore JSON")
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Clear All Data Button
                        OutlinedButton(
                            onClick = { showClearDataDialog = true },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = FinanceRed),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("btn_clear_all_data")
                        ) {
                            Icon(Icons.Rounded.DeleteSweep, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Reset All Data")
                        }
                    }
                }
            }

            // About Budget Bro App Info
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Budget Bro v1.1.0",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Crafted with Google Drive On-Demand & Periodic Cloud Backup",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    // Drive Restore Confirmation Dialog
    if (showConfirmDriveRestoreDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDriveRestoreDialog = false },
            title = { Text("Restore from Google Drive?") },
            text = {
                Text("This will download and restore your Budget Bro backup from Google Drive. Any current records will be updated or appended.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmDriveRestoreDialog = false
                        signedInAccount?.let { account ->
                            viewModel.restoreFromGoogleDrive(context, account)
                        }
                    }
                ) {
                    Text("Proceed with Restore")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDriveRestoreDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Edit Profile Dialog
    if (showEditProfileDialog) {
        var tempName by remember { mutableStateOf(userSettings.userName) }
        var tempSymbol by remember { mutableStateOf(userSettings.currencySymbol) }
        var tempBudgetInput by remember {
            mutableStateOf(CurrencyFormatter.paiseToInputString(userSettings.defaultMonthlyBudgetPaise))
        }

        AlertDialog(
            onDismissRequest = { showEditProfileDialog = false },
            title = { Text("Edit Profile & Defaults") },
            text = {
                Column {
                    OutlinedTextField(
                        value = tempName,
                        onValueChange = { tempName = it },
                        label = { Text("Your Name") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = tempSymbol,
                        onValueChange = { tempSymbol = it },
                        label = { Text("Currency Symbol") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = tempBudgetInput,
                        onValueChange = { input ->
                            if (input.matches(Regex("^\\d*(\\.\\d{0,2})?$"))) {
                                tempBudgetInput = input
                            }
                        },
                        label = { Text("Default Monthly Budget") },
                        leadingIcon = { Text(tempSymbol, fontWeight = FontWeight.Bold) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsedBudget = CurrencyFormatter.parseToPaise(tempBudgetInput)
                        viewModel.updateUserSettings(
                            userSettings.copy(
                                userName = tempName.ifBlank { "User" },
                                currencySymbol = tempSymbol.ifBlank { "₹" },
                                defaultMonthlyBudgetPaise = if (parsedBudget > 0) parsedBudget else userSettings.defaultMonthlyBudgetPaise
                            )
                        )
                        showEditProfileDialog = false
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditProfileDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Demo Data Confirmation Dialog
    if (showDemoDataDialog) {
        AlertDialog(
            onDismissRequest = { showDemoDataDialog = false },
            title = { Text("Load Demo Data?") },
            text = {
                Text("This will insert realistic Indian monthly sample data (Salary ₹1,00,000, Rent ₹20,000, Groceries ₹8,500, Swiggy ₹5,200, Subscriptions, Goals and Category Budgets). Existing data is preserved.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.loadDemoData()
                        showDemoDataDialog = false
                        snackbarMessage = "Demo financial data loaded successfully!"
                    }
                ) {
                    Text("Load Data")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDemoDataDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Clear Data Confirmation Dialog
    if (showClearDataDialog) {
        AlertDialog(
            onDismissRequest = { showClearDataDialog = false },
            title = { Text("Reset All Data?") },
            text = {
                Text("Are you sure? This will delete all your transactions, custom budgets, and goals. Default categories and your profile will be kept. This cannot be undone.", color = FinanceRed)
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllData()
                        showClearDataDialog = false
                        snackbarMessage = "All data cleared successfully"
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FinanceRed)
                ) {
                    Text("Clear All Data")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDataDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Export Dialog (Displays JSON with Copy to Clipboard button)
    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text("Exported Data (JSON)") },
            text = {
                Column {
                    Text(
                        text = "Copy this JSON string to back up your transactions:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = exportedJsonText,
                        onValueChange = {},
                        readOnly = true,
                        maxLines = 8,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Budget Bro Backup", exportedJsonText)
                        clipboard.setPrimaryClip(clip)
                        showExportDialog = false
                        snackbarMessage = "Backup copied to clipboard!"
                    }
                ) {
                    Text("Copy to Clipboard")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Import Dialog
    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = {
                showImportDialog = false
                importJsonInput = ""
            },
            title = { Text("Restore Data from JSON") },
            text = {
                Column {
                    Text(
                        text = "Paste a valid Budget Bro JSON backup string below:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = importJsonInput,
                        onValueChange = { importJsonInput = it },
                        placeholder = { Text("{\"version\": 1, \"transactions\": [...]}") },
                        maxLines = 8,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            val success = viewModel.importJson(importJsonInput)
                            if (success) {
                                snackbarMessage = "Data successfully restored!"
                                showImportDialog = false
                                importJsonInput = ""
                            } else {
                                snackbarMessage = "Invalid backup format. Please check JSON."
                            }
                        }
                    },
                    enabled = importJsonInput.isNotBlank()
                ) {
                    Text("Restore")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showImportDialog = false
                        importJsonInput = ""
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SettingsItemRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun SettingsNavRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
    }
}
