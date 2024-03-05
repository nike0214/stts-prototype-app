package com.example.stts
import SharedViewModel
import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import com.example.stts.databinding.ActivityHistoryBinding
import android.widget.ListView
import android.widget.Toast
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import android.media.MediaPlayer
import androidx.lifecycle.ViewModelProvider
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

object RetrofitClient {
    val instance: ServerApi by lazy {
        Retrofit.Builder()
            .baseUrl("http://211.63.148.66:5000") // Replace with your actual server IP
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ServerApi::class.java)
    }
}

object LanguageSelection {
    var selectedLanguage: String = "en" // default value
}

class HistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHistoryBinding
    private lateinit var viewModel: SharedViewModel


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Dummy data for the list
        val historyItems = arrayOf("Item 1", "Item 2", "Item 3") // Replace with real data

        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, historyItems)
        binding.listViewHistory.adapter = adapter

        val selectedLanguage = LanguageSelection.selectedLanguage
        fetchHistory(selectedLanguage)
    }

    private fun fetchHistory(language: String) {
        RetrofitClient.instance.getHistory().enqueue(object : Callback<Map<String, HistoryItem>> {
            override fun onResponse(call: Call<Map<String, HistoryItem>>, response: Response<Map<String, HistoryItem>>) {
                if (response.isSuccessful) {

                    val historyMap = response.body()
                    val historyItems = historyMap?.values?.map { it.id } ?: emptyList()
                    val adapter = ArrayAdapter(this@HistoryActivity, android.R.layout.simple_list_item_1, historyItems)
                    binding.listViewHistory.adapter = adapter

                    binding.listViewHistory.setOnItemClickListener { _, _, position, _ ->
                        historyMap?.values?.toList()?.get(position)?.id?.let { id ->
                            val fileUrl = "http://211.63.148.66:5000/history/$id/$language"
                            playWavFile(fileUrl)
                        }
                    }
                } else {
                    Toast.makeText(this@HistoryActivity, "Error fetching history", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<Map<String, HistoryItem>>, t: Throwable) {
                Toast.makeText(this@HistoryActivity, "Failed to fetch history", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun playWavFile(url: String) {
        RetrofitClient.instance.downloadFile(url).enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    val downloadedFile = saveFile(response.body()!!)
                    playAudio(downloadedFile)
                } else {
                    Toast.makeText(this@HistoryActivity, "Error downloading file", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Toast.makeText(this@HistoryActivity, "Failed to download file", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun saveFile(body: ResponseBody): File {
        val file = File(getExternalFilesDir(null), "downloaded_audio.wav")
        var fos: FileOutputStream? = null
        try {
            fos = FileOutputStream(file)
            fos.write(body.bytes())
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            fos?.close()
        }
        return file
    }

    private fun playAudio(file: File) {
        val mediaPlayer = MediaPlayer()
        try {
            mediaPlayer.setDataSource(file.path)
            mediaPlayer.prepare()
            mediaPlayer.start()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}