# HisaabKitaab (Android)

[![Platform](https://img.shields.io/badge/Platform-Android-3DDC84?logo=android&logoColor=white)](https://github.com/MrImaginatory/HisaabKitaab_Android)
[![Language](https://img.shields.io/badge/Language-Kotlin-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![UI](https://img.shields.io/badge/UI-Jetpack%20Compose%20%7C%20Material%203-4285F4?logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Storage](https://img.shields.io/badge/Storage-100%25%20Local%20SQLite-003B57?logo=sqlite&logoColor=white)](https://www.sqlite.org/)
[![Offline](https://img.shields.io/badge/Mode-Offline--First-success)](#privacy--local-first-architecture)

**HisaabKitaab (Android)** is a 100% local-first, offline personal finance and ledger application built natively with **Kotlin**, **Jetpack Compose**, and **Material Design 3**—featuring an optional **Liquid Glass UI**. Designed as the native Android companion to the HisaabKitaab web app, it maintains full `.db` SQLite backup compatibility so your financial records remain portable and completely under your control.

---

## Table of Contents

- [Privacy & Local-First Architecture](#privacy--local-first-architecture)
- [Key Features](#key-features)
- [Appearance & Customization](#appearance--customization)
- [APK Installation Guide](#apk-installation-guide)
- [Web App Backup Compatibility](#web-app-backup-compatibility)
- [Tech Stack](#tech-stack)
- [Building the APK from Source](#building-the-apk-from-source)
- [Project Structure](#project-structure)
- [Contributing](#contributing)

---

## Privacy & Local-First Architecture

- **Zero Servers & Zero Accounts:** No sign-ups, no cloud tracking, and no internet connection required.
- **Direct Local SQLite Database:** All data is stored locally across clean, structured tables (`categories`, `accounts`, `transactions`, `payment_mediums`, `profile`).
- **Zero Storage Permissions Needed:** Uses Android's native **Storage Access Framework (SAF)** for secure, scoped file imports, exports, and backups.

---

## Key Features

HisaabKitaab is organized into a complete **7-screen ledger**:

### 1. Dashboard
- **Net Balance Hero & 6 KPIs:** Instant visibility into your overall financial position.
- **Spend Analytics:** Filter by period presets and specific accounts.
- **Interactive Visualizations:** Animated Donut Chart (grouped by Category or Payment Medium) and an interactive Income vs. Expense Line Chart.
- **Recent Activity:** Quick 15-day recent activity lists.

### 2. Transactions
- **Overview & Filtering:** 3 summary KPIs with instant search and transaction-type filters.
- **Quick-Add Bottom Sheet:** Streamlined entry with persistent form defaults.
- **Built-in Safeguards:** Negative-balance guard, virtual-balance edit checks, and CAPTCHA-protected deletion with closed-account settlement transfer support.

### 3. Accounts
- **Flexible Views:** Combined balance overview with a List/Card layout toggle.
- **Account Details:** Track last-6-digits account numbers and toggle negative balances for Credit Cards or Overdraft (OD) accounts.
- **Safe Closure:** CAPTCHA-protected zero-balance account closure supporting both soft-close and hard-delete.

### 4. Categories
- **Visual Organization:** Custom hex color swatches for clear visual grouping.
- **Bulk Management:** Multi-select confirm-guarded deletion.
- **CSV Workflows:** Sample CSV export and bulk CSV import featuring duplicate/validation previews and a skipped-rows summary.

### 5. Payment Mediums
- **Pre-Seeded Groups:** Organized into **Online** and **Offline** categories with built-in defaults (*UPI, Net Banking, Cards, Wallet, Cash, Cheque, Demand Draft*).
- **Bulk Actions:** Multi-select bulk delete support.

### 6. Statement
- **Accurate Running Balances:** Per-account pre-period opening balances, 2-leg self-transfer expansion, and chronological running balances with 25-row pagination.
- **Import & Export:** Excel (`.xlsx`) and CSV export/import with duplicate detection previews.
- **Professional PDF Reports:** Landscape A4 PDF and Watermarked PDF generation with native Android Share Sheet integration.

### 7. Profile
- **Personalization:** Configure personal or business details, currency codes, and currency symbol presets.
- **Document Branding:** Customize diagonal PDF watermarks with a live statement preview.

---

## Appearance & Customization

| Feature | Options Available |
| :--- | :--- |
| **UI Style** | Switchable **Liquid Glass UI** and **Classic Material 3** |
| **Theme Mode** | **Dark Mode** and **Light Mode** |
| **Accent Colors** | **8 Accent Color Presets** + **Custom Hex Color Picker** |
| **Offline Fonts** | 4 bundled fonts (*Plus Jakarta Sans, Inter, Poppins, Outfit*) + *JetBrains Mono* tabular numerals |
| **Typography Scale** | 6-step font scaling (`XS` through `XXL`) |

---

## APK Installation Guide

> **Note:** Since HisaabKitaab is distributed directly as an APK rather than through the Play Store, you will need to allow installation from unknown sources on your Android device.

1. **Download the APK:**
   - Go to the [Releases](https://github.com/MrImaginatory/HisaabKitaab_Android/releases) page of this repository and download the latest `.apk` file.
   *(If no pre-built APK is published yet, follow the [Building the APK from Source](#building-the-apk-from-source) section below.)*
2. **Enable Installation Permission:**
   - Open the downloaded `.apk` file from your file manager or browser.
   - If prompted, tap **Settings** and enable **Allow from this source** for your browser or file manager.
3. **Install & Launch:**
   - Tap **Install** and wait for the installation to complete.
   - Open **HisaabKitaab**—no account login or storage permissions are required to get started.

---

## Web App Backup Compatibility

HisaabKitaab Android shares full `.db` SQLite schema compatibility with the **HisaabKitaab Web App**:

- **Migrating from Web to Android:** Export your `.db` SQLite backup from the HisaabKitaab web app, transfer it to your phone, and import it directly inside the Android app via SAF.
- **Backing up from Android to Web:** Export your local `.db` database from the Android app and load it into the web app anytime for seamless cross-platform access.

---

## Tech Stack

- **Language:** Kotlin
- **UI Toolkit:** Jetpack Compose & Material Design 3
- **Database:** Local SQLite (`categories`, `accounts`, `transactions`, `payment_mediums`, `profile`)
- **File Handling:** Android Storage Access Framework (SAF)
- **Build System:** Gradle (Kotlin DSL)

---

## Building the APK from Source

### Prerequisites
- [Android Studio](https://developer.android.com/studio) (latest stable version recommended)
- JDK 17 or compatible version configured for Android Gradle Plugin
- Android SDK installed

### Steps

1. **Clone the repository:**
   ```bash
   git clone https://github.com/MrImaginatory/HisaabKitaab_Android.git
   cd HisaabKitaab_Android
   ```

2. **Build using Android Studio:**
   - Open the cloned folder in Android Studio and let Gradle sync.
   - To build a debug APK, go to **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
   - To build a signed release APK, go to **Build > Generate Signed App Bundle / APK...** and choose **APK**.

3. **Build via Command Line (Gradle):**
   ```bash
   # Build Debug APK
   ./gradlew assembleDebug

   # Build Release APK (requires signing configuration)
   ./gradlew assembleRelease
   ```
   The generated APK can be found in:
   ```text
   app/build/outputs/apk/debug/app-debug.apk
   ```

---

## Project Structure

```text
HisaabKitaab_Android/
├── app/                     # Main Android application module (Kotlin, Compose UI, SQLite)
├── gradle/                  # Gradle wrapper and version catalog files
├── public/assets/           # Static assets
├── build.gradle.kts         # Top-level Gradle build configuration
├── settings.gradle.kts      # Gradle module settings
├── gradle.properties        # Project-wide Gradle settings
└── README.md                # Project documentation
```

---

## Contributing

Contributions, bug reports, and feature suggestions are welcome!

1. Fork the repository.
2. Create a new feature branch (`git checkout -b feature/your-feature`).
3. Commit your changes (`git commit -m "Add your feature"`).
4. Push to your branch (`git push origin feature/your-feature`).
5. Open a Pull Request.
