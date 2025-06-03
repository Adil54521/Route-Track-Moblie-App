package com.example.routetrack

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageButton
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.facebook.shimmer.BuildConfig
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

class WorkActivity() : AppCompatActivity(){

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ScanResultsAdapter
    private lateinit var greetingTextView: TextView
    private val scanResults = mutableListOf<ScanResultsAdapter.Invoice>()
    private lateinit var scanButton: AppCompatImageButton
    private lateinit var itemCountTextView: TextView
    private lateinit var rowsPerPageSpinner: Spinner
    private lateinit var pageSpinner: Spinner
    private lateinit var imageButton: AppCompatImageButton
    private val Host = "127.0.0.1"
    private val Login = "demo_user"
    private val Password = "demo-pass"
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var searchEditText: EditText
    private lateinit var selectDateButton: AppCompatImageButton
    private val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
    private val calendar = Calendar.getInstance()
    private lateinit var selectedDateTextView: TextView

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_work)

        // Инициализация UI компонентов
        greetingTextView = findViewById(R.id.greetingTextView)
        itemCountTextView = findViewById(R.id.itemCountTextView)
        recyclerView = findViewById(R.id.invoiceRecyclerView)
        rowsPerPageSpinner = findViewById(R.id.rowsPerPageSpinner)
        pageSpinner = findViewById(R.id.pageSpinner)
        imageButton = findViewById(R.id.sendButton)
        scanButton = findViewById(R.id.scanButton)
        recyclerView.layoutManager = LinearLayoutManager(this)
        searchEditText = findViewById(R.id.searchEditText)
        selectDateButton = findViewById(R.id.selectDateButton)
        selectedDateTextView = findViewById(R.id.selectedDateTextView)
        sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        // Установка текущей даты
        val todayDate = calendar.time
        selectedDateTextView.text = "Дата: ${dateFormat.format(todayDate)}"

        adapter = ScanResultsAdapter(scanResults) { invoice ->
            showDeleteConfirmationDialog(invoice)
        }
        recyclerView.adapter = adapter

        selectDateButton.setOnClickListener {
            val datePickerFragment = DatePickerFragment { selectedDate ->
                // Форматируем выбранную дату
                val formattedDate = dateFormat.format(selectedDate)

                // Обновляем TextView
                selectedDateTextView.text = "Дата: $formattedDate"

                // Фильтруем данные по выбранной дате
                filterDate()
            }

            // Показываем DatePicker
            datePickerFragment.show(supportFragmentManager, "datePicker")
        }


        setupSearchFunctionality()
        uploadScanResultsToHttp()
        // Инициализация SharedPreferences
        sharedPreferences = getSharedPreferences("RouteTrackPrefs", Context.MODE_PRIVATE)

        // Получение имени пользователя из SharedPreferences
        val savedProfile = sharedPreferences.getString("saved_profile", null)
        Log.d("WorkActivity", "Loaded profile from SharedPreferences: $savedProfile")

        loadScanResultsFromLocalFile()

        // Установка приветственного текста
        if (!savedProfile.isNullOrEmpty()) {
            greetingTextView.text = "Приветствую, $savedProfile"
        } else {
            greetingTextView.text = "Приветствую, гость"
        }

        // Запуск сканера
        scanButton.setOnClickListener {
            startQrScan()
        }
    }

    private val qrScanLauncher = registerForActivityResult(ScanContract()) { result ->
        if(result.contents != null) {
            parseAndDisplayResult(result.contents)
        }
    }

    private fun getUserFileName(): String {
        val savedProfile = sharedPreferences.getString("saved_profile", null) ?: "default_user"
        val uniqueId = savedProfile.hashCode() // Можно использовать другой способ генерации уникальности
        return "scan_results_$uniqueId.json"
    }

    private fun prepareScanResultsXml(results: List<ScanResultsAdapter.Invoice>): String {
        return try {
            val documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            val document = documentBuilder.newDocument()

            val rootElement = document.createElement("ScanResults")
            document.appendChild(rootElement)

            results.forEach { scanResult ->
                val resultElement = document.createElement("Result")
                resultElement.setAttribute("invoiceNumber", scanResult.number)
                resultElement.setAttribute("amount", scanResult.amount.toString())
                resultElement.setAttribute(
                    "afterDeliveryAmount",
                    scanResult.afterDeliveryAmount.toString()
                )
                resultElement.setAttribute("discountAmount", scanResult.amount.toString())
                resultElement.setAttribute("paymentMethod", scanResult.result)

                rootElement.appendChild(resultElement)
            }

            val transformer = TransformerFactory.newInstance().newTransformer()
            val source = DOMSource(document)
            val writer = java.io.StringWriter()
            transformer.transform(source, StreamResult(writer))
            writer.toString()
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    private fun uploadScanResultsToHttp() {
        CoroutineScope(Dispatchers.IO).launch {
            val savedProfile = sharedPreferences.getString("saved_profile", null)
            if (savedProfile == null) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@WorkActivity, "Saved profile is null", Toast.LENGTH_SHORT).show()
                }
                return@launch
            }

            // Создание XML из результатов
            val xmlData = prepareScanResultsXml(scanResults)
            if (xmlData.isEmpty()) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@WorkActivity, "No scan results to upload", Toast.LENGTH_SHORT).show()
                }
                return@launch
            }

            try {
                val client = OkHttpClient()

                // Формируем тело запроса (если сервер принимает файл)
                val mediaType = "application/xml".toMediaTypeOrNull()
                val requestBody: RequestBody = xmlData.toRequestBody(mediaType)

                // Создаем запрос — замените URL на адрес вашего API для загрузки
                val request = Request.Builder()
                    .url("http://192.168.45.212:8000/upload_scan_results") // пример URL API
                    .post(requestBody)
                    .addHeader("Authorization", "Basic " + android.util.Base64.encodeToString("$Login:$Password".toByteArray(), android.util.Base64.NO_WRAP))
                    .build()

                val response = client.newCall(request).execute()

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        Toast.makeText(this@WorkActivity, "Файл успешно загружен", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@WorkActivity, "Ошибка загрузки: ${response.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@WorkActivity, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun startQrScan() {
        if (isRunningOnEmulator()) {
            simulateQrScanInEmulator()
        } else {
            val options = ScanOptions().apply {
                setPrompt("Наведите камеру на QR-код")
                setBeepEnabled(true)
                setBarcodeImageEnabled(true)
            }
            qrScanLauncher.launch(options)
        }
    }

    private fun simulateQrScanInEmulator() {
        val bitmap = BitmapFactory.decodeResource(resources, R.drawable.qr)
        val image = InputImage.fromBitmap(bitmap, 0)
        val scanner = BarcodeScanning.getClient()

        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                if (barcodes.isNotEmpty()) {
                    val result = barcodes[0].rawValue ?: "Нет данных"
                    parseAndDisplayResult(result)
                } else {
                    Toast.makeText(this, "QR-код не найден", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Ошибка: ${it.message}", Toast.LENGTH_SHORT).show()
                Log.e("QR", "Ошибка распознавания", it)
            }
    }

    private fun parseAndDisplayResult(contents: String) {
        try {
            val regex = Regex("N-(\\d+)&amount=(\\d+)")
            val matchResult = regex.find(contents)

            if (matchResult != null) {
                val invoiceNumber = matchResult.groupValues[1]
                val amount = matchResult.groupValues[2].toDoubleOrNull() ?: 0.0

                // Проверка данных
                if (invoiceNumber.isEmpty() || amount <= 0) {
                    Log.w("Parsing", "Невалидные данные: $invoiceNumber, $amount")
                    Toast.makeText(this, "Получены некорректные данные", Toast.LENGTH_SHORT).show()
                    return
                }

                val existingInvoice = scanResults.find { it.number == invoiceNumber }
                if (existingInvoice != null) {
                    // Если накладная уже существует, скроллим к ней в списке
                    val position = scanResults.indexOf(existingInvoice)
                    recyclerView.scrollToPosition(position)
                    Toast.makeText(
                        this,
                        "Накладная №$invoiceNumber уже добавлена. Переход к ней.",
                        Toast.LENGTH_SHORT
                    ).show()
                    return
                }

                // Получение текущей даты
                val currentDate = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date())

                // Создание результата
                val scanResult = ScanResultsAdapter.Invoice(
                    number = invoiceNumber,
                    amount = amount,
                    afterDeliveryAmount = 0.0,
                    amountBefore = 0.0,
                    discount = 0.0,
                    result = "",
                    date = currentDate,
                )

                // Добавление результата и обновление адаптера
                scanResults.add(scanResult)
                adapter.notifyDataSetChanged()

                // Сохранение данных
                saveScanResultsToLocalFile()

                Log.d("Parsing", "Успешно добавлен результат: $scanResult")
            } else {
                Log.w("Parsing", "Не удалось найти данные по регулярному выражению")
                Toast.makeText(this, "Не удалось распознать данные", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Ошибка обработки данных: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            Log.e("Parsing Error", "Ошибка обработки данных", e)
        }
    }

    private fun isRunningOnEmulator(): Boolean {
        val result = Build.FINGERPRINT.contains("generic")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.lowercase().contains("emulator")
        Log.d("EmulatorCheck", "Fingerprint=${Build.FINGERPRINT}, Model=${Build.MODEL}, isEmulator=$result")
        return result
    }


    private fun saveScanResultsToLocalFile() {
        val gson = Gson()
        val jsonData = gson.toJson(scanResults)
        val file = File(filesDir, getUserFileName())
        file.writeText(jsonData)
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun loadScanResultsFromLocalFile() {
        try {
            val file = File(filesDir, getUserFileName())
            if (file.exists()) {
                val jsonData = file.readText()
                val gson = Gson()
                val type = object : TypeToken<MutableList<ScanResultsAdapter.Invoice>>() {}.type
                val loadedResults: MutableList<ScanResultsAdapter.Invoice> = gson.fromJson(jsonData, type)
                scanResults.clear()
                scanResults.addAll(loadedResults)
                adapter.notifyDataSetChanged()
                Log.d("LoadFile", "Data loaded from file: ${file.name}")
            }
        } catch (e: IOException) {
            Log.e("File Error", "Ошибка загрузки данных: ${e.message}")
        }
    }

    private fun setupSearchFunctionality() {
        searchEditText.addTextChangedListener { editable ->
            val query = editable?.toString()?.trim() ?: ""
            performSearch(query)
        }
    }


    // Метод поиска
    private fun performSearch(query: String) {
        if (query.isNotEmpty()) {
            // Фильтруем элементы списка по номеру накладной
            val filteredResults = scanResults.filter {
                it is ScanResultsAdapter.Invoice && it.number.contains(
                    query,
                    ignoreCase = true
                )
            }

            if (filteredResults.isNotEmpty()) {
                adapter.updateItems(filteredResults) // Обновляем адаптер с результатами поиска
            } else {
                adapter.updateItems(emptyList()) // Показываем пустой список
            }
        } else {
            // Если поле пустое, сбрасываем поиск
            resetSearch()
        }
    }

    private fun filterDate() {
        // Убираем лишний префикс "Дата: "
        val selectedDateString = selectedDateTextView.text.toString().removePrefix("Дата: ").trim()

        // Фильтруем список данных
        val filteredResults = scanResults.filter { item ->
            item.date == selectedDateString
        }

        // Обновляем адаптер
        adapter.updateItems(filteredResults)

        // Если список пуст, выводим сообщение
        if (filteredResults.isEmpty()) {
            Toast.makeText(this, "Нет данных для выбранной даты: $selectedDateString", Toast.LENGTH_SHORT).show()
        }
    }

    // Сброс поиска
    private fun resetSearch() {
        searchEditText.text.clear() // Очищаем поле ввода
        adapter.updateItems(scanResults) // Возвращаем весь список
    }

    private fun deleteInvoice(invoice: ScanResultsAdapter.Invoice) {
        scanResults.remove(invoice) // Удаляем из исходного списка
        adapter.updateItems(scanResults) // Обновляем адаптер
        saveScanResultsToLocalFile() // Сохраняем изменения в файл
        Toast.makeText(this, "Накладная №${invoice.number} удалена", Toast.LENGTH_SHORT).show()
    }

    private fun showDeleteConfirmationDialog(invoice: ScanResultsAdapter.Invoice) {
        val alertDialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Удаление накладной")
            .setMessage("Вы уверены, что хотите удалить накладную №${invoice.number}?")
            .setPositiveButton("Удалить") { _, _ ->
                deleteInvoice(invoice)
            }
            .setNegativeButton("Отмена", null)
            .create()

        alertDialog.show()

    }
}

