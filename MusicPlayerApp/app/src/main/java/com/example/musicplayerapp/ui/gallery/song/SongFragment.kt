package com.example.musicplayerapp.ui.gallery.song

import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.musicplayerapp.R
import com.example.musicplayerapp.adapter.RecyclerItemClickListener
import com.example.musicplayerapp.adapter.SongAdapter
import com.example.musicplayerapp.databinding.FragmentSongBinding
import com.example.musicplayerapp.model.CurrentSong
import com.example.musicplayerapp.model.SongModel
import com.example.musicplayerapp.network.API
import com.example.musicplayerapp.service.MyReceiver
import com.example.musicplayerapp.service.SongService
import com.example.musicplayerapp.ui.gallery.favorite.FavoriteFragment
import com.example.musicplayerapp.ui.home.HomeActivity
import com.example.musicplayerapp.ui.home.HomeActivity.Companion.listLoadDataFavorite
import com.example.musicplayerapp.ui.slideshow.SlideshowActivity
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import de.hdodenhof.circleimageview.CircleImageView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


class SongFragment : Fragment() {

    private lateinit var binding: FragmentSongBinding
    private lateinit var mSongAdapter: SongAdapter
    private val listLoadDataFavorite = HomeActivity.listLoadDataFavorite
    private lateinit var songInfoReference: DatabaseReference


    companion object {
        var listSongModel = mutableListOf<SongModel>()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_song,
            container,
            false
        )
        val linearLayoutManager = LinearLayoutManager(requireContext())
        linearLayoutManager.orientation = LinearLayoutManager.VERTICAL
        binding.rcvSong.layoutManager = linearLayoutManager
        mSongAdapter = SongAdapter(requireContext())
        binding.rcvSong.adapter = mSongAdapter
        getListSong()

        //firebase reference songInfo
        songInfoReference = FirebaseDatabase.getInstance().getReference("songInfo")
        currentSongInfo()

       // Log.i("Test3", "listSOng${listSongModel.size}")


        return binding.root
    }


    private fun getListSong() {
        API.apiService.getListSong().enqueue(object : Callback<MutableList<SongModel>> {
            override fun onResponse(
                call: Call<MutableList<SongModel>>,
                response: Response<MutableList<SongModel>>
            ) {
                val list = response.body() as MutableList<SongModel>
                listSongModel.addAll(list)
                setUpFavoriteSong(list)
                mSongAdapter.setData(list)
                Log.i("Test3", "list ${list.size}")
            }

            override fun onFailure(call: Call<MutableList<SongModel>>, t: Throwable) {
                Toast.makeText(context, "Can't not access database", Toast.LENGTH_SHORT).show()
            }
        })

    }


    private fun setUpFavoriteSong(list: MutableList<SongModel>) {
        for (item1 in list) {
            for (item2 in listLoadDataFavorite) {
                if (item1.songName == item2.songName
                    && item1.artistName == item2.artistName
                ) {
                    item1.isFavorite = true
                }
            }
        }
    }

    private fun currentSongInfo() {
        binding.rcvSong.addOnItemTouchListener(
            RecyclerItemClickListener(
                context,
                binding.rcvSong,
                object : RecyclerItemClickListener.OnItemClickListener {
                    override fun onItemClick(view: View?, position: Int) {


                    }

                    override fun onLongItemClick(view: View?, position: Int) {
                        val intent = Intent(activity, SlideshowActivity::class.java)
                        startActivity(intent)
                        val type = "song"
                        val currentSong = CurrentSong(
                            position.toString(),
                            type,
                            listSongModel[position].songName,
                            listSongModel[position].artistName,
                            listSongModel[position].imgUrl,
                            listSongModel[position].songUrl,
                            listSongModel[position].duration,
                            listSongModel[position].isFavorite
                        )
                        songInfoReference.setValue (null)
                        songInfoReference.child ("currentSong").setValue(currentSong)
                        FirebaseDatabase.getInstance().getReference("status").child("pause").setValue(false)
                        FirebaseDatabase.getInstance().getReference("status").child("repeat").setValue(false)

                    }
                })
        )
    }

}