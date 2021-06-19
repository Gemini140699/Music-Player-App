package com.example.musicplayerapp.service

import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.MediaPlayer.OnCompletionListener
import android.media.MediaPlayer.OnPreparedListener
import android.net.Uri
import android.os.IBinder
import android.transition.Slide
import android.util.Log
import android.widget.RemoteViews
import android.widget.SeekBar
import android.widget.TextView
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.musicplayerapp.R
import com.example.musicplayerapp.model.CurrentSong
import com.example.musicplayerapp.model.SongModel
import com.example.musicplayerapp.service.MyApplication.Companion.CHANNEL_ID
import com.example.musicplayerapp.ui.gallery.album.AlbumFragment
import com.example.musicplayerapp.ui.gallery.favorite.FavoriteFragment
import com.example.musicplayerapp.ui.gallery.song.SongFragment
import com.example.musicplayerapp.ui.home.HomeActivity
import com.example.musicplayerapp.ui.slideshow.SlideshowActivity
import com.google.firebase.database.*
import java.io.IOException


class SongService : Service() {

    companion object {
        const val ACTION_PAUSE = 1
        const val ACTION_RESUME = 2
        const val ACTION_NEXT = 3
        const val ACTION_PREV = 4
        const val ACTION_REPEAT = 5
        const val ACTION_FORWARD = 6
        var TOTAL_TIME: Int = 0
    }

    private lateinit var songInfoReference: DatabaseReference
    private var currentPosition: Int? = 0
    private lateinit var typePosition: String
    private lateinit var currentSongName: String
    private lateinit var currentSingerName: String
    private lateinit var imageUrl: String
    private var isFavorite = true
    private var isFirstTime = true
    private var isPause = true
    private var isRepeat = false
    private var mPlayer = MediaPlayer()
    private var check = true


    override fun onCreate() {
        songInfoReference = FirebaseDatabase.getInstance().getReference("songInfo")
        super.onCreate()
    }


    override fun onBind(intent: Intent?): IBinder? {
        return null
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (isFirstTime) {
            getValueCurrentSong()
        }
        val actionMusic = intent?.getIntExtra("action_music_service", 0)
        Log.i("Test100", "actionService: $actionMusic")

        handleActionMusic(actionMusic!!)
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        mPlayer.release()
        super.onDestroy()
    }


    private fun getValueCurrentSong() {
        FirebaseDatabase.getInstance().getReference("status").child("pause")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    isPause = snapshot.value.toString().toBoolean()
                }
                override fun onCancelled(error: DatabaseError) {
                }
            })
        FirebaseDatabase.getInstance().getReference("status").child("repeat")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    isRepeat = snapshot.value.toString().toBoolean()
                }
                override fun onCancelled(error: DatabaseError) {
                }
            })
        songInfoReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val getCurrentPosition =
                        snapshot.child("currentSong").child("position").value.toString()
                    currentPosition = Integer.parseInt(getCurrentPosition)
                    typePosition = snapshot.child("currentSong").child("type").value.toString()
                    currentSongName =
                        snapshot.child("currentSong").child("songName").value.toString()
                    currentSingerName =
                        snapshot.child("currentSong").child("artistName").value.toString()
                    imageUrl = snapshot.child("currentSong").child("imgUrl").value.toString()
                    isRepeat = snapshot.child("currentSong").child("repeat").value.toString().toBoolean()
                    if (!isFirstTime) {
                        sendNotification(currentSongName, currentSingerName)
                        playMusic(typePosition, currentPosition!!)
                    }
                }
                isFirstTime = false
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }

        })
    }


    private fun sendNotification(songName: String, singerName: String) {
        val intent = Intent(this, HomeActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )
        val remoteViews = RemoteViews(packageName, R.layout.notification_layout)
        remoteViews.setTextViewText(R.id.playSongName, songName)
        remoteViews.setTextViewText(R.id.playSingerName, singerName)
        remoteViews.setImageViewResource(R.id.playImgSong, R.drawable.image_default)
        if (!isPause) {
            remoteViews.setOnClickPendingIntent(R.id.playPlay, getPendingIntent(this, ACTION_PAUSE))
            remoteViews.setImageViewResource(R.id.playImgPlay, R.drawable.baseline_pause_black_20)
        } else {
            remoteViews.setOnClickPendingIntent(
                R.id.playPlay,
                getPendingIntent(this, ACTION_RESUME)
            )
             remoteViews.setImageViewResource(R.id.playImgPlay, R.drawable.baseline_play_arrow_black_20)
        }
        remoteViews.setOnClickPendingIntent(R.id.playNext, getPendingIntent(this, ACTION_NEXT))
        remoteViews.setOnClickPendingIntent(R.id.playBefore, getPendingIntent(this, ACTION_PREV))


        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_baseline_music_note_24)
            .setCustomContentView(remoteViews)
            .setContentIntent(pendingIntent)
            .build()

        startForeground(1, notification)

    }


    private fun getPendingIntent(context: Context, action: Int): PendingIntent {
        val intent = Intent(this, MyReceiver::class.java)
        intent.putExtra("action_music", action)
        return PendingIntent.getBroadcast(
            context.applicationContext,
            action,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )
    }




    private fun handleActionMusic(action: Int) {
        when (action) {
            ACTION_PAUSE -> pauseMusic()
            ACTION_RESUME -> resumeMusic()
            ACTION_NEXT -> nextMusic(action)
            ACTION_PREV -> prevMusic(action)
            ACTION_REPEAT -> repeatMusic()
            ACTION_FORWARD -> forwardMusic()
        }

    }



    private fun playMusic(typePosition: String, currentPosition: Int) {
        try {
            mPlayer.stop()
            mPlayer.reset()
            mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC)
            when (typePosition) {
                "song" -> mPlayer.setDataSource(
                    Uri.parse(SongFragment.listSongModel[currentPosition].songUrl).toString()
                )
                "album" -> mPlayer.setDataSource(
                    Uri.parse(AlbumFragment.album[currentPosition].songUrl).toString()
                )
                "favorite" -> mPlayer.setDataSource(
                    Uri.parse(FavoriteFragment.listFavoriteSong[currentPosition].songUrl).toString()
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                mPlayer.prepareAsync()
            } catch (e: IllegalStateException) {
            } catch (e: IOException) {
            }
            mPlayer.setOnPreparedListener(OnPreparedListener { player -> player.start() })

            isPause = false
            mPlayer.setOnCompletionListener(OnCompletionListener {
                //nextMusic(ACTION_NEXT)
                if (!isRepeat) {
                    nextMusic(ACTION_NEXT)
                } else {
                    mPlayer.isLooping
                    playMusic(typePosition, currentPosition)
                }
            })
        }
    }


    private fun pauseMusic() {
        if (!isPause) {
            mPlayer.pause()
            isPause = true
            sendNotification(currentSongName, currentSingerName)
            sendActionToActivity(ACTION_PAUSE)
            isPauseToFirebase()
        }
    }


    private fun resumeMusic() {
        if (isPause) {
            if (check) {
                playMusic(typePosition, currentPosition!!)
                check = false
            } else {
                mPlayer.start()
            }
            isPause = false
            sendActionToActivity(ACTION_RESUME)
            sendNotification(currentSongName, currentSingerName)
            isPauseToFirebase()

        }
    }


    private fun nextMusic(action: Int) {
        updateSong(currentPosition!!, typePosition, action)
        sendNotification(currentSongName, currentSingerName)
        isPause = false
        isRepeat = false
        sendActionToActivity(ACTION_NEXT)
        isPauseToFirebase()
        isRepeatToFirebase()
    }


    private fun prevMusic(action: Int) {
        updateSong(currentPosition!!, typePosition, action)
        sendNotification(currentSongName, currentSingerName)
        isPause = false
        isRepeat = false
        sendActionToActivity(ACTION_PREV)
        isPauseToFirebase()
        isRepeatToFirebase()
    }


    private fun repeatMusic() {
        if(isRepeat) {
            isRepeat = false
            sendActionToActivity(ACTION_REPEAT)
            isRepeatToFirebase()
        } else {
            isRepeat = true
            sendActionToActivity(ACTION_REPEAT)
            isRepeatToFirebase()
        }
    }

    private fun forwardMusic() {

    }


    private fun isPauseToFirebase() {
        FirebaseDatabase.getInstance().getReference("status").child("pause").setValue(isPause)
    }

    private fun isRepeatToFirebase() {
        FirebaseDatabase.getInstance().getReference("status").child("repeat").setValue(isRepeat)
    }

    private fun updateSong(position: Int, type: String, action: Int) {
        //check if song is the last and press next
        when {
            type.equals("song") -> {
                val size = SongFragment.listSongModel.size - 1
                handleFirstAndLast(position, action, size)
                val song = SongFragment.listSongModel[currentPosition!!]
                songToFirebase(song)
            }
            type.equals("album") -> {
                val size = AlbumFragment.album.size - 1
                handleFirstAndLast(position, action, size)
                val song = AlbumFragment.album[currentPosition!!]
                songToFirebase(song)
            }
            else -> {
                val size = FavoriteFragment.listFavoriteSong.size - 1
                handleFirstAndLast(position, action, size)
                val song = FavoriteFragment.listFavoriteSong[currentPosition!!]
                songToFirebase(song)
            }
        }
    }

    private fun handleFirstAndLast(position: Int, action: Int, size: Int) {
        //check if song is the last and press next
        if (position == size && action == ACTION_NEXT) {
            this.currentPosition = 0
        } else if (action == ACTION_NEXT) {
            currentPosition = currentPosition?.plus(1)
        }

        //check if song is the first and press back
        if (position == 0 && action == ACTION_PREV) {
            this.currentPosition = size
        } else if (action == ACTION_PREV) {
            currentPosition = currentPosition?.minus(1)
        }
    }

    private fun songToFirebase(song: SongModel) {
        this.currentSongName = song.songName
        this.currentSingerName = song.artistName
        this.isFavorite = song.isFavorite
        this.imageUrl = song.imgUrl
        songInfoReference.setValue(null)
        val currentSong = CurrentSong(
            currentPosition.toString(),
            typePosition,
            song.songName,
            song.artistName,
            song.imgUrl,
            song.songUrl,
            song.duration,
            song.isFavorite
        )
        songInfoReference.child("currentSong").setValue(currentSong)
    }

    private fun sendActionToActivity(action: Int) {
        val intent = Intent("send_data_to_activity")
        intent.putExtra("action", action)
        intent.putExtra("isPause", isPause)
        intent.putExtra("isRepeat", isRepeat)
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)

    }


}