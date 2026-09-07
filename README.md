# Payment Bridge 📱💳

[![Platform](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0+-purple.svg)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack-Compose%20M3-4285F4.svg)](https://developer.android.com/jetpack/compose)
[![Database](https://img.shields.io/badge/Database-Room%20(SQLite)-orange.svg)](https://developer.android.com/training/data-storage/room)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

**Payment Bridge** is a native Android application designed to run on physical Android smartphones. It continuously monitors incoming carrier SIM SMS messages, automatically detects mobile financial payment notifications (specifically **bKash** and customizable carriers), parses transaction amounts and unique Transaction IDs (`TrxID`), stores persistent history in an offline-first **Room database**, and optionally forwards verified payments to a **Telegram Bot** and a custom **REST Backend Server**.

The app is **100% functional in offline/local mode** without needing any backend server.

---

## 🌟 Key Features

* **⚡ Real-Time SIM SMS Monitoring**:
  * Intercepts incoming carrier broadcast SMS (`android.provider.Telephony.SMS_RECEIVED`).
  * Works in real time without polling carrier APIs.
  * Supports custom sender filters (e.g., `bKash`) and configurable keyword triggers.

* **🎯 Intelligent Payment Parser**:
  * Regex extraction tailored for bKash formats (e.g., `Tk 50.00`, `Tk50.00`, `BDT 50.00`).
  * Alphanumeric Transaction ID (`TrxID`) extractor (e.g., `5FL1NWXBPH`).
  * Rejects irrelevant carrier promotional messages.

* **🛡️ Duplicate Protection**:
  * Uses `TrxID` as a primary deduplication key.
  * Prevents duplicate entries if the carrier or device re-delivers the same SMS.

* **💾 Offline-First Room Local Database**:
  * Automatically stores all detected transactions locally with timestamp, status, and metadata.
  * Search by `TrxID`, sender, or amount.
  * Filter by statuses: `DETECTED`, `PENDING_SYNC`, `SYNCED`, `MATCHED`, `APPROVED`, `REJECTED`, `FAILED`.

* **✈️ Optional Telegram Integration**:
  * Dispatches instant transaction alerts directly to your private Telegram channel or chat.
  * Built-in connection tester and token masking.

* **🌐 Optional Backend Bridge & Auto-Retry**:
  * Sends verified payments to `POST /api/payments/detected`.
  * If the device is offline or without internet, transactions are stored as `PENDING` and can be synced in 1 tap once reconnected.

* **🧪 Built-In Test SMS Playground & Simulator**:
  * Test any custom SMS string without needing a second phone.
  * One-tap **"Simulate & Save to History"** to test notifications and database persistence.

---

## 🏗️ Architecture

```
Incoming SIM SMS
       │
       ▼
 [SmsReceiver] (BroadcastReceiver)
       │
       ▼
  [SmsParser] ──► Validates Sender & Keywords
       │      ──► Extracts Amount & TrxID
       ▼
[PaymentRepository]
       │
       ├──► Deduplication Check (by TrxID)
       │
       ├──► [Room Database] (Offline Local Persistence)
       │
       ├──► [NotificationHelper] (Android System Alert)
       │
       ├──► [TelegramService] (Optional Telegram Alert)
       │
       └──► [BackendService] (Optional REST POST /api/payments/detected)
```

---

## 📥 How to Install the APK on Your Phone

If you just want to run the app on your Android phone without compiling code:

1. **Download the APK**:
   * Download `PaymentBridge.apk` or `app-debug.apk` directly from this repository or the Releases page.
2. **Transfer to Device**:
   * Send the `.apk` to your phone via USB, Google Drive, or Telegram.
3. **Install**:
   * Open the APK on your device.
   * If prompted, enable **"Install unknown apps"** in your phone's Settings.
4. **Grant Permissions**:
   * Open **Payment Bridge**.
   * When prompted, grant **SMS permissions** (`RECEIVE_SMS` and `READ_SMS`) and **Notification permission**.
   * *(These permissions are required solely to detect incoming payment texts; no personal data or passwords are ever accessed or transmitted).*

---

## 🛠️ Building From Source

### Prerequisites
* **Android Studio**: Ladybug / Hedgehog or newer (JDK 17+)
* **Android SDK**: API 34 / 35 (Android 14 / 15)
* **Gradle**: 8.7+ (Kotlin DSL)

### Steps

1. **Clone the Repository**:
   ```bash
   git clone https://github.com/your-username/payment-bridge.git
   cd payment-bridge
   ```

2. **Open in Android Studio**:
   * Launch Android Studio -> **Open** -> select the cloned folder.
   * Allow Gradle to sync dependencies.

3. **Build Debug APK via CLI**:
   ```bash
   # On Linux / macOS:
   ./gradlew assembleDebug

   # On Windows:
   gradlew.bat assembleDebug
   ```
   The generated APK will be at:
   `app/build/outputs/apk/debug/app-debug.apk`

4. **Run Unit & Robolectric Tests**:
   ```bash
   ./gradlew testDebugUnitTest
   ```

---

## 📱 App Configuration & Setup

### 1. SMS Configuration
* **Sender ID**: `bKash` (or your MFS provider name)
* **Payment Keyword**: `received` (triggers on incoming deposit/cash-in)
* **Transaction Keyword**: `TrxID` (identifies transaction code)

### 2. Telegram Bot Configuration (Optional)
1. Open Telegram and search for `@BotFather`.
2. Send `/newbot` and follow the prompts to get your **Bot Token** (e.g., `123456789:ABCdef...`).
3. Message `@userinfobot` or add your bot to your target group/channel to get the **Chat ID**.
4. In **Payment Bridge Settings**:
   * Enter the **Bot Token** and **Chat ID**.
   * Tap **[ Test Connection ]** to verify.
   * Enable Telegram Alerts and tap **[ Save ]**.

### 3. Backend Server Integration (Optional)
If you operate a backend server (Node.js, Python, Laravel, Go, etc.), Payment Bridge can POST detected payments automatically.

* **Health Check**:
  ```http
  GET /api/health
  ```
  Response: `200 OK`

* **Payment Detected Webhook**:
  ```http
  POST /api/payments/detected
  Content-Type: application/json
  ```
  **Payload**:
  ```json
  {
    "amount": 50.00,
    "currency": "BDT",
    "transactionId": "5FL1NWXBPH",
    "sender": "bKash",
    "receivedAt": "2026-09-07T08:30:00Z",
    "deviceId": "PB-XXXX-XXXX"
  }
  ```
  **Expected Response (200 OK)**:
  ```json
  {
    "status": "MATCHED",
    "matchedOrderId": "ORDER-10029"
  }
  ```

---

## 🔒 Privacy & Security Guidelines

* **Zero Password/PIN Storage**: Payment Bridge **never** requests, reads, or stores any bKash account PINs, OTPs, or passwords.
* **Local-First**: All transaction records remain encrypted and stored locally in your phone's SQLite Room database.
* **Token Safety**: Telegram Bot Tokens and Backend URLs are stored in private Android `EncryptedSharedPreferences` / application storage, and tokens are masked in the UI.

---

## 📄 License

This project is licensed under the [MIT License](LICENSE).
