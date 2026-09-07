package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.data.model.PaymentRecord
import com.example.ui.payments.PaymentListItem
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun greeting_screenshot() {
    val samplePayment = PaymentRecord(
      id = 1L,
      amount = 50.00,
      currency = "BDT",
      transactionId = "5FL1NWXBPH",
      sender = "bKash",
      receivedAt = 1725697800000L,
      status = PaymentRecord.STATUS_MATCHED,
      syncStatus = PaymentRecord.SYNC_SYNCED
    )
    composeTestRule.setContent {
      MyApplicationTheme {
        PaymentListItem(payment = samplePayment, onClick = {})
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
  }
}

