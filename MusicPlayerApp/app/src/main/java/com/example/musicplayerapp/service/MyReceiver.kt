package com.example.musicplayerapp.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class MyReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val actionMusic = intent?.getIntExtra("action_music", 0)
        Log.i("Test122", "$actionMusic")

        val intentService = Intent(context, SongService::class.java)
        intentService.putExtra("action_music_service", actionMusic)
        context!!.startService(intentService)
    }
}