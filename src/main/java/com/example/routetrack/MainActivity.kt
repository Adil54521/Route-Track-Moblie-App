package com.example.routetrack

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainActivity : AppCompatActivity() {

    private lateinit var sharedPreferences: android.content.SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        
        sharedPreferences = getSharedPreferences("RouteTrackPrefs", Context.MODE_PRIVATE)

        val editTextBranchKey = findViewById<EditText>(R.id.insertText)
        val buttonSubmit = findViewById<Button>(R.id.buttonLogin)

        val savedBranchKey = sharedPreferences.getString("branch_key", null)
        if (!savedBranchKey.isNullOrEmpty()) {
            authenticate(savedBranchKey)
        }

        buttonSubmit.setOnClickListener {
            val branchKey = editTextBranchKey.text.toString()
            if (branchKey.isNotEmpty()) {
                authenticate(branchKey)
            } else {
                Toast.makeText(this, "Введите ключ", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun authenticate(branchKey: String) {
        // Отправляем POST-запрос с параметром branch_key
        RetrofitClient.retrofit.checkBranchKey(branchKey).enqueue(object :
            Callback<BranchDataResponse> {
            override fun onResponse(call: Call<BranchDataResponse>, response: Response<BranchDataResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    val ftpHost = response.body()?.ftp_host
                    val ftpLogin = response.body()?.ftp_login
                    val ftpPassword = response.body()?.ftp_password

                    // Сохраняем ключ в SharedPreferences
                    sharedPreferences.edit().apply {
                        putString("branch_key", branchKey)
                        apply()
                    }

                    // Переход на второй экран с передачей данных
                    val intent = Intent(this@MainActivity, UnifiedLoginActivity::class.java).apply {
                        putExtra("ftp_host", ftpHost)
                        putExtra("ftp_login", ftpLogin)
                        putExtra("ftp_password", ftpPassword)
                    }
                    startActivity(intent)
                } else {
                    Log.e("Error", "Response code: ${response.code()}, ErrorBody: ${response.errorBody()?.string()}")
                    Toast.makeText(this@MainActivity, "Неверный ключ или ошибка сервера", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<BranchDataResponse>, t: Throwable) {
                val errorMessage = "Ошибка подключения: ${t.message}"
                
                // Показываем тост
                Toast.makeText(this@MainActivity, errorMessage, Toast.LENGTH_SHORT).show()

                // Логируем в консоль
                Log.e("NETWORK_ERROR", errorMessage, t)
            }
        })
    }
}

