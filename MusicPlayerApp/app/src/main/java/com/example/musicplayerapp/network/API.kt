package com.example.musicplayerapp.network

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object API {
    private var retrofit: Retrofit? = null
    private val gson: Gson
        get() = GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss").create()
    val apiService: APIService
        get() {
            if (retrofit == null) {
                retrofit = Retrofit.Builder()
                        .baseUrl("http://10.0.2.2:3000/")
                        .addConverterFactory(GsonConverterFactory.create(gson))
                        .build()
            }
            return retrofit!!.create(APIService::class.java)
        }
}