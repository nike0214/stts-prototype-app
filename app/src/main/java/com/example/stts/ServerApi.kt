package com.example.stts

import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Url

interface ServerApi {
    @Multipart
    @POST("/translate-voice")
    fun sendVoice(
        @Part audio: MultipartBody.Part,
        @Part("language") language: RequestBody
    ): Call<ResponseBody>

    @GET("/history/all")
    fun getHistory(): Call<Map<String, HistoryItem>>

    @GET
    fun downloadFile(@Url fileUrl: String): Call<ResponseBody>


}
data class HistoryItem(
    val id: String,
//    val retranslated_text: String,
//    val translated_text: String,
//    val wav_file: String
)