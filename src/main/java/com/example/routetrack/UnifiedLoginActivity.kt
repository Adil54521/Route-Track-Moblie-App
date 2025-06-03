package com.example.routetrack

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.commons.net.ftp.FTPClient
import org.json.JSONArray
import java.security.MessageDigest
import java.util.Locale

class UnifiedLoginActivity : AppCompatActivity() {

    private lateinit var listViewProfiles: ListView
    private val profileList = mutableListOf<String>()
    private lateinit var sharedPreferences: android.content.SharedPreferences
    private var ftpHost: String? = null
    private var ftpLogin: String? = null
    private var ftpPassword: String? = null

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profilelist)

        ftpHost = intent.getStringExtra("ftp_host")
        ftpLogin = intent.getStringExtra("ftp_login")
        ftpPassword = intent.getStringExtra("ftp_password")

        listViewProfiles = findViewById(R.id.listViewProfile)
        sharedPreferences = getSharedPreferences("RouteTrackPrefs", Context.MODE_PRIVATE)

        if(sharedPreferences.contains("saved_profile")){
            val savedProfile = sharedPreferences.getString("saved_profile", null)
            if (savedProfile != null) {
                navigateToWorkActivity(savedProfile)
            }
        }
        // Загружаем профили с FTP
        fetchProfilesFromFTP()

        // Настраиваем обработчик кликов по профилям
        setupProfileClickListener()

        val currentUsername = getCurrentUsername()

    }

    private fun fetchProfilesFromFTP() {
        CoroutineScope(Dispatchers.IO).launch {
            val ftpClient = FTPClient()
            try {
                ftpClient.connect(ftpHost)
                if (ftpClient.login(ftpLogin, ftpPassword)) {
                    ftpClient.enterLocalPassiveMode()
                    ftpClient.setFileType(FTPClient.BINARY_FILE_TYPE)

                    val inputStream = ftpClient.retrieveFileStream("/forwarders.json")
                    if (inputStream != null) {
                        val jsonString = inputStream.bufferedReader().use { it.readText() }
                        val jsonArray = JSONArray(jsonString)

                        for (i in 0 until jsonArray.length()) {
                            val jsonObject = jsonArray.getJSONObject(i)
                            val name = jsonObject.optString("name")
                            if (name.isNotEmpty() && !profileList.contains(name)) {
                                profileList.add(name)
                            }
                        }

                        withContext(Dispatchers.Main) {
                            listViewProfiles.adapter = ArrayAdapter(this@UnifiedLoginActivity, android.R.layout.simple_list_item_1, profileList)
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@UnifiedLoginActivity, "Не удалось загрузить данные с FTP", Toast.LENGTH_SHORT).show()
                        }
                    }
                    ftpClient.logout()
                }
                ftpClient.disconnect()
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@UnifiedLoginActivity, "Ошибка подключения к FTP: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setupProfileClickListener() {
        listViewProfiles.setOnItemClickListener { _, _, position, _ ->
            val selectedProfile = profileList[position]

            val currentUsername = getCurrentUsername()
            if (currentUsername != null) {
                Toast.makeText(this, "Текущий пользователь: $currentUsername", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Пользователь не найден", Toast.LENGTH_SHORT).show()
            }
            showLoginDialog(selectedProfile)
        }
    }

    private fun showLoginDialog(selectedProfile: String) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_login, null)
        val editTextUsername = dialogView.findViewById<EditText>(R.id.editTextUsername)
        val editTextPassword = dialogView.findViewById<EditText>(R.id.editTextPassword)

        editTextUsername.setText(selectedProfile) // Автозаполнение логина

        AlertDialog.Builder(this)
            .setTitle("Вход")
            .setView(dialogView)
            .setPositiveButton("Войти") { _, _ ->
                val username = editTextUsername.text.toString().trim()
                val password = editTextPassword.text.toString().trim()
                if (username.isNotEmpty() && password.isNotEmpty()) {
                    val encryptedPassword = encryptMD5(password)
                    performLogin(username, encryptedPassword)
                } else {
                    Toast.makeText(this, "Введите логин и пароль", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun performLogin(username: String, encryptedPassword: String) {
        val savedPasswordHash = sharedPreferences.getString("password_hash", null)
        val savedProfile = sharedPreferences.getString("saved_profile", null)

        if (savedPasswordHash != null && savedProfile == username && encryptedPassword == savedPasswordHash) {
            saveCurrentUsername(username)
            navigateToWorkActivity(username)
        } else {
            // Выполняем проверку через FTP
            verifyCredentialsWithFTP(username, encryptedPassword)
        }
    }

    private fun saveCurrentUsername(username: String) {
        sharedPreferences.edit().putString("current_username", username).apply()
    }

    private fun getCurrentUsername(): String? {
        return sharedPreferences.getString("current_username", null)
    }

    private fun verifyCredentialsWithFTP(username: String, encryptedPassword: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val ftpClient = FTPClient()
            try {
                ftpClient.connect(ftpHost)
                if (ftpClient.login(ftpLogin, ftpPassword)) {
                    ftpClient.enterLocalPassiveMode()
                    ftpClient.setFileType(FTPClient.BINARY_FILE_TYPE)

                    val inputStream = ftpClient.retrieveFileStream("/forwarders.json")
                    if (inputStream != null) {
                        val jsonString = inputStream.bufferedReader().use { it.readText() }
                        val jsonArray = JSONArray(jsonString)

                        var isValid = false
                        for (i in 0 until jsonArray.length()) {
                            val jsonObject = jsonArray.getJSONObject(i)
                            if (jsonObject.optString("name") == username && jsonObject.optString("password").lowercase(Locale.ROOT) == encryptedPassword) {
                                isValid = true
                                break
                            }
                        }

                        withContext(Dispatchers.Main) {
                            if (isValid) {
                                saveCredentials(username, encryptedPassword)
                                navigateToWorkActivity(username)
                            } else {
                                Toast.makeText(this@UnifiedLoginActivity, "Неверный логин или пароль", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@UnifiedLoginActivity, "Ошибка подключения к FTP: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun saveCredentials(username: String, passwordHash: String) {
        sharedPreferences.edit().apply {
            putString("saved_profile", username)
            putString("password_hash", passwordHash)
            apply()
        }
    }

    private fun navigateToWorkActivity(username: String) {
        Toast.makeText(this, "Добро пожаловать, $username", Toast.LENGTH_SHORT).show()
        val intent = Intent(this, WorkActivity::class.java)
        startActivity(intent)
    }

    private fun encryptMD5(input: String): String {
        return try {
            val trimmedInput = input.trim()
            val digest = MessageDigest.getInstance("MD5")
            val bytes = digest.digest(trimmedInput.toByteArray())
            val sb = StringBuilder()
            for (byte in bytes) {
                sb.append(String.format("%02x", byte))
            }
            sb.toString()
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }
}
