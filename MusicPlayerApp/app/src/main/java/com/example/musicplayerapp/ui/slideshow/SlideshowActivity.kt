package com.example.musicplayerapp.ui.slideshow

import android.content.*
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.animation.LinearInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.example.musicplayerapp.R
import com.example.musicplayerapp.databinding.ActivitySlideshowBinding
import com.example.musicplayerapp.service.SongService
import com.example.musicplayerapp.ui.home.HomeActivity
import com.google.firebase.database.*
import jp.wasabeef.glide.transformations.BlurTransformation
import java.util.*


class SlideshowActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySlideshowBinding
    private lateinit var songInfoReference: DatabaseReference
    private lateinit var typePosition: String
    private lateinit var currentSongName: String
    private lateinit var currentSingerName: String
    private lateinit var imageUrl: String
    private lateinit var isFavorite: String
    private var duration: Int = 0
    private var isPause: String = ""
    private var isRepeat: Boolean = false

    //Handle action on fullPlayer
    private val broadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.getIntExtra("action", 0)
            isPause = intent.getBooleanExtra("isPause", false).toString()
            isRepeat = intent.getBooleanExtra("isRepeat", false)
            Log.i("Test100", "action: $action")
            handleLayoutMusic(action, isPause.toBoolean(), isRepeat)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_slideshow)

        //firebase reference songInfo
        songInfoReference = FirebaseDatabase.getInstance().getReference("songInfo")


        getValueCurrentSong()


        sendActionToService()

        LocalBroadcastManager.getInstance(this).registerReceiver(
            broadcastReceiver,
            IntentFilter("send_data_to_activity")
        )

        binding.reSize.setOnClickListener() {
            finish()
        }

    }



    private fun getValueCurrentSong() {
        FirebaseDatabase.getInstance().getReference("status").child("pause").addValueEventListener(
            object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    isPause =
                        snapshot.value.toString()
                    statusActionPauseOrResume(isPause.toBoolean())
                }


                override fun onCancelled(error: DatabaseError) {
                    TODO("Not yet implemented")
                }

            })
        FirebaseDatabase.getInstance().getReference("status").child("repeat")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    isRepeat =
                        snapshot.value.toString().toBoolean()
                    statusActionRepeat(isRepeat)
                }

                override fun onCancelled(error: DatabaseError) {
                    TODO("Not yet implemented")
                }

            })
        songInfoReference.child("currentSong").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {

                if (snapshot.exists()) {
                    typePosition = snapshot.child("type").value.toString()
                    currentSongName =
                        snapshot.child("songName").value.toString()
                    currentSingerName =
                        snapshot.child("artistName").value.toString()
                    imageUrl =
                        snapshot.child("imgUrl").value.toString()
                    isFavorite =
                        snapshot.child("favorite").value.toString()
                    duration =
                        snapshot.child("duration").value.toString().toInt()

                    setupSlideShow(
                        typePosition,
                        currentSongName,
                        currentSingerName,
                        imageUrl,
                        isFavorite,
                        duration,
                        isPause
                    )
                }
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })

    }


    private fun setupSlideShow(
        typePosition: String,
        currentSongName: String,
        currentSingerName: String,
        imageUrl: String,
        isFavorite: String,
        duration: Int,
        checkPause: String,
    ) {
        if (typePosition == "album") {
            val homeActivity = HomeActivity()
            val img: ByteArray = homeActivity.getImageUri(imageUrl)
            Glide.with(applicationContext)
                .load(img)
                .override(300, 300)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(R.drawable.image_default)
                .error(R.drawable.image_default)
                .into(binding.imgSong)

            Glide.with(applicationContext)
                .load(img)
                .apply(RequestOptions.bitmapTransform(BlurTransformation(21, 4)))
                .error(R.color.colorBackground)
                .placeholder(R.color.colorBackground)
                .into(binding.imgBackground)
        } else {
            Glide.with(applicationContext)
                .load(imageUrl)
                .placeholder(R.drawable.image_default)
                .error(R.drawable.image_default)
                .into(binding.imgSong)

            Glide.with(applicationContext)
                .load(imageUrl)
                .apply(RequestOptions.bitmapTransform(BlurTransformation(21, 4)))
                .error(R.color.colorBackground)
                .placeholder(R.color.colorBackground)
                .into(binding.imgBackground)
        }

        binding.apply {
            txtSongName.text = currentSongName
            txtSingerName.text = currentSingerName
            if (isFavorite.toBoolean()) {
                imgFavorite.setImageResource(R.drawable.ic_favorite)
            } else {
                imgFavorite.setImageResource(R.drawable.ic_favorite_border)
            }
        }

        setupSeekBar(duration)
        statusActionPauseOrResume(checkPause.toBoolean())
        statusActionRepeat(isRepeat)
    }



    private fun animationImageView(duration: Long) {
        val runnable: Runnable = object : Runnable {
            override fun run() {
                binding.imgSong.animate().rotationBy(360F).withEndAction(this).setDuration(duration)
                    .setInterpolator(LinearInterpolator()).start()
            }
        }
        binding.imgSong.animate().rotationBy(360F).withEndAction(runnable).setDuration(duration)
            .setInterpolator(LinearInterpolator()).start()
    }



    private fun handleLayoutMusic(action: Int, isPause: Boolean, isRepeat: Boolean) {
        when(action) {
            SongService.ACTION_PAUSE -> statusActionPauseOrResume(isPause)
            SongService.ACTION_RESUME -> {
                statusActionPauseOrResume(isPause)
                statusActionRepeat(isRepeat)
            }
            SongService.ACTION_NEXT -> {
                statusActionPauseOrResume(isPause)
                statusActionRepeat(isRepeat)
            }
            SongService.ACTION_REPEAT -> statusActionRepeat(isRepeat)
        }
    }

    private fun statusActionPauseOrResume(isPause: Boolean) {
        if(isPause) {
            binding.imgPlayPause.setImageResource(R.drawable.ic_play)
            binding.imgSong.animate().startDelay
            animationImageView(0)
        } else {
            animationImageView(10000)
            binding.imgPlayPause.setImageResource(R.drawable.ic_pause)
        }
    }



    private fun statusActionRepeat(isRepeat: Boolean) {
        if(isRepeat) {
            binding.imgRepeat.setImageResource(R.drawable.baseline_repeat_one_white_24)
        } else {
            binding.imgRepeat.setImageResource(R.drawable.ic_repeat)
        }
    }


    private fun setupSeekBar(duration: Int) {
//        var totalTime = StringBuilder("")
//        val minute = duration/100
//        if(minute < 10) {
//            totalTime.append("0$minute:")
//        } else {
//            totalTime.append("0$minute:")
//        }
//        val second = duration%100
//        totalTime = if(second<10) {
//            totalTime.append("0$second")
//        } else {
//            totalTime.append("$second")
//        }
//        binding.totalDuration.text = totalTime
        binding.sbSeekBar.progress = 0
        //binding.sbSeekBar.max = 56
        object : CountDownTimer(duration*1000.toLong(), 1000) {

            override fun onTick(millisUntilFinished: Long) {
                //Log.i("T")
                var totalTime = StringBuilder("")
                val minute =millisUntilFinished/100000
                if(minute < 10) {
                    totalTime.append("0$minute:")
                } else {
                    totalTime.append("0$minute:")
                }
                val secondTemp = millisUntilFinished%100000
                val second = secondTemp/1000
                totalTime = if(second<10) {
                    totalTime.append("0$second")
                } else {
                    totalTime.append("$second")
                }
                binding.totalDuration.text = totalTime
//                binding.totalDuration.text = (millisUntilFinished/1000).toString()
            }

            override fun onFinish() {
//                editText.setText("Done")
            }
        }.start()
    }


    private fun sendActionToService() {
        val intent = Intent(this, SongService::class.java)

        binding.apply {
            next.setOnClickListener() {
                intent.putExtra("action_music_service", SongService.ACTION_NEXT)
                startService(intent)
            }

            before.setOnClickListener() {
                intent.putExtra("action_music_service", SongService.ACTION_PREV)
                startService(intent)
            }


            imgPlayPause.setOnClickListener() {
                if(isPause.toBoolean()) {
                    intent.putExtra("action_music_service", SongService.ACTION_RESUME)
                    startService(intent)
                } else {
                    intent.putExtra("action_music_service", SongService.ACTION_PAUSE)
                    startService(intent)
                }
            }

            imgRepeat.setOnClickListener() {
                    intent.putExtra("action_music_service", SongService.ACTION_REPEAT)
                    startService(intent)
            }
        }
    }



}