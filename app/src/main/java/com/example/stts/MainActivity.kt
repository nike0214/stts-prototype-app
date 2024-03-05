package com.example.stts

import SharedViewModel
import android.content.Intent
import android.media.MediaRecorder
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.ViewModelProvider
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.io.IOException
import com.arthenica.mobileffmpeg.FFmpeg

class MainActivity : AppCompatActivity() {

    private var isRecording = false
    private lateinit var mediaRecorder: MediaRecorder
    private var audioFile: File? = null
    private var selectedLanguage = "ko" // Default language
    private lateinit var viewModel: SharedViewModel


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val recordButton = findViewById<Button>(R.id.button_record)
        val recordIndicator = findViewById<ImageView>(R.id.imageView)
        val historyButton = findViewById<Button>(R.id.button_history)

        viewModel = ViewModelProvider(this).get(SharedViewModel::class.java)

        recordButton.setOnClickListener {
            if (isRecording) {
                stopRecording()
                recordButton.text = "Record"
                recordIndicator.setImageResource(R.drawable.record_red_button)
            } else {
                startRecording()
                recordButton.text = "Stop"
                recordIndicator.setImageResource(R.drawable.pause_button)
            }
            isRecording = !isRecording
        }

        historyButton.setOnClickListener {
            val intent = Intent(this@MainActivity, HistoryActivity::class.java)
            startActivity(intent)
        }
        setupLanguageSpinner()
    }

    private fun startRecording() {
        audioFile = File(externalCacheDir?.absolutePath, "audio_record_${System.currentTimeMillis()}.3gp")
        mediaRecorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            setOutputFile(audioFile?.absolutePath)
            try {
                prepare()
                start()
            } catch (e: IOException) {
                e.printStackTrace() // Make sure this exception is not happening
            }
        }
    }


    private fun stopRecording() {
        if (::mediaRecorder.isInitialized) {
            mediaRecorder.apply {
                stop()
                release()
            }
        }
        val wavFilePath = audioFile?.absolutePath?.replace(".3gp", ".wav")
        audioFile?.absolutePath?.let {
            if (wavFilePath != null) {
                convertToWav(it, wavFilePath)
            }
        }
        sendAudioToServer(wavFilePath)
    }


    private fun sendAudioToServer(wavFilePath: String?) {
        val requestBody = audioFile?.let { RequestBody.create("audio/3gp".toMediaTypeOrNull(), it) }
        val audioBody =
            requestBody?.let { MultipartBody.Part.createFormData("audio", audioFile?.name, it) }
        val language = RequestBody.create("text/plain".toMediaTypeOrNull(), selectedLanguage) // Replace "en" with the actual language code

        // Build Retrofit instance
        val retrofit = Retrofit.Builder()
            .baseUrl("http://211.63.148.66:5000") // Replace with your actual server IP
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        // Create Retrofit service
        val serverApi = retrofit.create(ServerApi::class.java)

        // Make the network request
        if (audioBody != null) {
            serverApi.sendVoice(audioBody, language).enqueue(object : Callback<ResponseBody> {
                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                    if (response.isSuccessful) {
                        // Navigate to HistoryActivity
                    } else {
                        // Handle the error
                    }
                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    // Handle the failure
                }
            })

        }
    }

    private fun convertToWav(inputFilePath: String, outputFilePath: String) {
        FFmpeg.execute("-i $inputFilePath -acodec pcm_s16le -ar 44100 -ac 2 $outputFilePath")
    }

    // Inside MainActivity
    private fun setupLanguageSpinner() {
        val languages = arrayOf("en", "es", "fr", "de", "it", "pt", "pl", "tr", "ru", "nl", "ar", "zh-cn", "hu", "ko", "ja", "hi")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, languages)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        val spinnerLanguage = findViewById<Spinner>(R.id.spinner_language)
        spinnerLanguage.adapter = adapter

        spinnerLanguage.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                LanguageSelection.selectedLanguage = languages[position]
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

}
