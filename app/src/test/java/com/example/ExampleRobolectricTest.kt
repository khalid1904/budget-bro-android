package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.ocr.ReceiptParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Budget Bro", appName)
  }

  @Test
  fun `parse Google Pay receipt text correctly`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val parser = ReceiptParser(context)

    val sampleReceipt = """
      Google Pay
      Paid to
      Starbucks Coffee
      ₹350.00
      Completed
      UPI transaction ID: 408271891234
      Google transaction ID: CICAgODU12345
      To: starbucks@okhdfcbank
    """.trimIndent()

    val result = parser.extractUPIFields(sampleReceipt)

    assertEquals(350.0, result.amount)
    assertEquals("Google Pay", result.paymentMethod)
    assertNotNull(result.merchant)
    assertTrue(result.merchant!!.contains("Starbucks", ignoreCase = true))
    assertEquals("Food & Dining", result.suggestedCategoryName)
    assertEquals("408271891234", result.referenceId)
  }

  @Test
  fun `parse PhonePe Swiggy receipt text correctly`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val parser = ReceiptParser(context)

    val sampleReceipt = """
      PhonePe
      Payment Successful
      Paid to Swiggy
      ₹ 489.00
      Debited from State Bank of India
      UTR: 329048123999
    """.trimIndent()

    val result = parser.extractUPIFields(sampleReceipt)

    assertEquals(489.0, result.amount)
    assertEquals("PhonePe", result.paymentMethod)
    assertEquals("Swiggy", result.merchant)
    assertEquals("Food & Dining", result.suggestedCategoryName)
    assertEquals("329048123999", result.referenceId)
  }

  @Test
  fun `notification helper manages alert states and channels cleanly`() {
    val context = ApplicationProvider.getApplicationContext<Context>()

    // Channel creation
    com.example.util.NotificationHelper.createNotificationChannel(context)

    // State persistence
    val month = 9
    val year = 2026
    assertEquals(
      com.example.util.NotificationHelper.ALERT_STATE_NONE,
      com.example.util.NotificationHelper.getAlertState(context, month, year)
    )

    com.example.util.NotificationHelper.setAlertState(
      context,
      month,
      year,
      com.example.util.NotificationHelper.ALERT_STATE_WARNING_80
    )
    assertEquals(
      com.example.util.NotificationHelper.ALERT_STATE_WARNING_80,
      com.example.util.NotificationHelper.getAlertState(context, month, year)
    )

    // Trigger alert without throwing exceptions
    com.example.util.NotificationHelper.showMonthlyBudgetAlert(
      context = context,
      spentPaise = 5000000L,
      budgetPaise = 6000000L,
      currencySymbol = "₹",
      monthName = "September 2026",
      percentageUsed = 83
    )
  }
}
