# HisaabKitaab (Android)

100% local-first, offline personal finance and ledger app built natively with **Kotlin**, **Jetpack Compose**, **Material Design 3**, and optional **Liquid Glass UI** — with full `.db` SQLite compatibility with HisaabKitaab web backups.

## Features

- **Local Offline SQLite Storage**: Direct SQLite database (`categories`, `accounts`, `transactions`, `payment_mediums`, `profile`) with zero servers, zero accounts, and zero storage permissions using Android Storage Access Framework (SAF).
- **Full 7-Screen Ledger**:
  - **Dashboard**: Net balance hero, 6 KPIs, Spend Analytics with period presets & account filter, animated Donut Chart (by Category / Payment Medium), interactive Income vs Expense Line Chart, and 15-day recent activity lists.
  - **Transactions**: 3 summary KPIs, search & type filters, Quick-Add bottom sheet with form defaults persistence, negative-balance guard, virtual-balance edit checks, and CAPTCHA-protected deletion with closed-account settlement transfer support.
  - **Accounts**: Combined balance overview, List/Card toggle, last-6-digits account numbers, negative balance toggle (credit cards/OD), and CAPTCHA-protected zero-balance closure (soft-close vs hard-delete).
  - **Categories**: Custom hex color swatches, multi-select confirm-guarded delete, sample CSV export, and bulk CSV import with duplicate/validation preview and skipped-rows summary.
  - **Payment Mediums**: Online & Offline groups with pre-seeded defaults (UPI, Net Banking, Cards, Wallet, Cash, Cheque, Demand Draft) and multi-select bulk delete.
  - **Statement**: Per-account pre-period opening balances, 2-leg self-transfer expansion, chronological running balances with 25-row pagination, Excel (`.xlsx`) & CSV export/import with duplicate preview, and landscape A4 PDF / Watermarked PDF export & native Share Sheet.
  - **Profile**: Personal/business info, currency code & symbol presets, PDF diagonal watermark configuration, and live statement preview.
- **Appearance & Customization**:
  - Switchable **Liquid Glass UI** and **Classic Material 3**
  - **Dark / Light Mode**, **8 Accent Color Presets + Custom Hex Picker**
  - **4 Bundled Offline Fonts** (*Plus Jakarta Sans, Inter, Poppins, Outfit*) + *JetBrains Mono* tabular numerals
  - **6-Step Font Scale** (`XS` through `XXL`)
