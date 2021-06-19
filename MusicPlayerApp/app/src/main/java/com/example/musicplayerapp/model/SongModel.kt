package com.example.musicplayerapp.model

import java.io.Serializable


data class SongModel (
        var songName: String,
        var artistName: String,
        var imgUrl: String,
        var songUrl:String,
        var duration: String,
        var isFavorite:Boolean)

data class CurrentSong (
        var position: String,
        var type: String,
        var songName: String,
        var artistName: String,
        var imgUrl: String,
        var songUrl: String,
        var duration: String,
        var isFavorite: Boolean): Serializable
