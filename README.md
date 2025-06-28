# 📦 RouteTrack

**RouteTrack** is an Android application for scanning and processing delivery invoices with the ability to send data to a server in XML format. The app allows you to:

- Authenticate using a branch key
- Scan invoice QR codes
- Store and filter results by date and invoice number
- Add discounts and delivery amounts
- Upload results to an HTTP server
- Simulate scans when running on an emulator

## 🚀 Features

- 📷 QR code scanning via ML Kit / ZXing
- 💾 Local storage of invoices in JSON format
- 📅 Filtering invoices by date
- 🔍 Search by invoice number
- 🧾 XML file generation from scanned results
- ☁️ HTTP POST upload of XML file
- 🎨 User-friendly UI with highlighting and spinners
- 📱 Emulator support (QR code simulation)
- 🧠 Error handling and user feedback
- 🔐 Auth data persistence via SharedPreferences

## 📂 Project Structure

```
com.example.routetrack/
├── MainActivity.kt              # Branch key login screen
├── WorkActivity.kt              # Main scanning and upload screen
├── ScanResultsAdapter.kt        # RecyclerView adapter for invoices
├── RetrofitClient.kt            # Retrofit class for API auth
├── activity_login.xml           # Login layout
├── activity_work.xml            # Work screen layout
├── item_scan_result.xml         # Invoice item layout
├── DatePickerFragment.kt        # Optional: date picker dialog
```

## 🛠️ Requirements

- Android Studio Hedgehog or newer
- minSdkVersion: 24
- compileSdkVersion: 33+
- Dependencies:
  - Retrofit2
  - OkHttp
  - ML Kit Barcode Scanning
  - ZXing (JourneyApps)
  - Gson
  - Kotlin Coroutines
  - AndroidX libraries

## 🧪 Sample QR Code Format

```
N-123456&amount=1500
```

- `123456` — Invoice number
- `1500` — Invoice amount

## 🔐 Authentication

The app starts with a branch key login. After successful authentication, FTP connection data (host, login, password) is passed to `UnifiedLoginActivity` and can be used for syncing or uploads.

## 🌐 Uploading Data to Server

Scanned results are sent as XML via HTTP POST:

```
POST http://<your-server>/upload_scan_results
Content-Type: application/xml
Authorization: Basic <base64(login:password)>
```

## 🧪 Emulator Support

The emulator-friendly mode lets you simulate scans using a static QR code:

- Loads `qr.png` from resources on emulators
- Parses it as a real QR code

## 💾 Data Storage

Scanned data is saved to:

```
scan_results_<hash(profile)>.json
```

and reloaded automatically on app startup.

## 📸 Screenshots

_(Add images of login screen, invoice list, scanning and upload screens)_

## 📌 Notes

- Adapter supports editing, payment selection, and invoice deletion
- Filtering and search features included
- Delivered invoices are visually highlighted
- SharedPreferences store user settings and profiles

## 🧑‍💻 Author

- Author: Adilkhan Shukraliyev
- Email: adilkhan.shukraliyev@gmail.com

==============================================================================================================================================================
# 📦 RouteTrack

**RouteTrack** — это Android-приложение для сканирования и обработки товарных накладных с возможностью отправки данных на сервер в формате XML. Приложение позволяет:

- Авторизоваться по ключу филиала
- Сканировать QR-коды накладных
- Хранить и фильтровать результаты по дате и номеру
- Добавлять скидки и суммы доставки
- Загружать результаты на HTTP-сервер
- Работать в эмуляторе с заглушками

## 🚀 Возможности

- 📷 Поддержка сканирования QR-кодов с помощью ML Kit / ZXing
- 💾 Локальное хранение накладных в JSON-файле
- 📅 Фильтрация накладных по дате
- 🔍 Поиск по номеру накладной
- 🧾 Генерация XML-файла с результатами
- ☁️ Отправка XML на сервер по HTTP POST
- 🎨 Удобный UI с подсветкой и спиннерами
- 📱 Поддержка эмуляторов (эмуляция QR-сканирования)
- 🧠 Обработка ошибок и уведомления пользователю
- 🔐 Сохранение данных авторизации в SharedPreferences

## 📂 Структура проекта

```
com.example.routetrack/
├── MainActivity.kt              # Авторизация по ключу филиала
├── WorkActivity.kt              # Основной экран сканирования и загрузки
├── ScanResultsAdapter.kt        # Адаптер для списка накладных (RecyclerView)
├── RetrofitClient.kt            # Класс Retrofit для API авторизации
├── activity_login.xml           # Макет авторизации
├── activity_work.xml            # Макет основного экрана
├── item_scan_result.xml         # Макет одного элемента накладной
├── DatePickerFragment.kt        # Выбор даты (если есть)
```

## 🛠️ Требования

- Android Studio Hedgehog или выше
- minSdkVersion: 24
- compileSdkVersion: 33+
- Подключенные библиотеки:
  - Retrofit2
  - OkHttp
  - ML Kit Barcode Scanning
  - ZXing (JourneyApps)
  - Gson
  - Coroutines
  - AndroidX RecyclerView, AppCompat и т.д.

## 🧪 Пример QR-кода

Формат ожидаемого QR-кода:

```
N-123456&amount=1500
```

- `123456` — номер накладной
- `1500` — сумма

## 🔐 Авторизация

Приложение начинает с авторизации по `branch_key`. После успешной авторизации данные FTP сервера (host, login, password) передаются на второй экран (`UnifiedLoginActivity`), где могут быть использованы при синхронизации или загрузке данных.

## 🌐 Отправка на сервер

Результаты сканирования отправляются на API:

```
POST http://<ваш-сервер>/upload_scan_results
Content-Type: application/xml
Authorization: Basic <base64(login:password)>
```

## 🧪 Тестирование на эмуляторе

Для удобства отладки на эмуляторе добавлена заглушка:

- При запуске сканирования в эмуляторе, используется `qr.png` из ресурсов
- Он распознается как валидный QR-код

## 💾 Хранение данных

Сканированные данные сохраняются в файл:

```
scan_results_<hash(profile)>.json
```

И загружаются при повторном запуске.

## 📸 Скриншоты

_(Добавьте изображения экрана логина, списка накладных, формы сканирования и отправки данных)_

## 📌 Примечания

- Код адаптера обрабатывает редактирование, выбор способа оплаты и удаление накладных
- Поддерживаются фильтрация по дате и поиск
- Статусы доставки выделяются цветом
- SharedPreferences используются для сохранения состояния между сессиями

## 🧑‍💻 Автор

- Автор: Адильхан Шукралиев
- Email: adilkhan.shukraliyev@gmail.com


