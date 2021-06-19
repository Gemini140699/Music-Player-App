package com.example.musicplayerapp.network

import com.example.musicplayerapp.model.SongModel
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET

interface APIService {

    @GET("songs")
    fun getListSong(): Call<MutableList<SongModel>>
}