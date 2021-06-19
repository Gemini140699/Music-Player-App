package com.example.musicplayerapp.ui.gallery.favorite

import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.musicplayerapp.R
import com.example.musicplayerapp.adapter.FavoriteAdapter
import com.example.musicplayerapp.adapter.RecyclerItemClickListener
import com.example.musicplayerapp.databinding.FragmentSongBinding
import com.example.musicplayerapp.model.CurrentSong
import com.example.musicplayerapp.model.SongModel
import com.example.musicplayerapp.ui.gallery.album.AlbumFragment
import com.example.musicplayerapp.ui.gallery.song.SongFragment
import com.example.musicplayerapp.ui.home.HomeActivity
import com.example.musicplayerapp.ui.slideshow.SlideshowActivity
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import de.hdodenhof.circleimageview.CircleImageView


class FavoriteFragment : Fragment() {

    private lateinit var binding: FragmentSongBinding
    private lateinit var favoriteAdapter: FavoriteAdapter
    private val list: MutableList<SongModel> = SongFragment.listSongModel
    private val listAlbum: MutableList<SongModel> = AlbumFragment.album
    private  val listAlbumTemp: MutableList<SongModel> = mutableListOf()
    private val listLoadDataFavorite =
        HomeActivity.listLoadDataFavorite //Load Favourite Song in the last action stored in share sharedPreferences
    private lateinit var songInfoReference: DatabaseReference

    companion object {
        var listFavoriteSong = mutableListOf<SongModel>()
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

        Log.i("Test3", "xxx ${list.size}")

        listAlbumTemp.addAll(listAlbum)
        listFavoriteSong.addAll(listLoadDataFavorite) //add local data to listFavouriteSong to display in recycle view

        val linearLayoutManager = LinearLayoutManager(requireContext())
        linearLayoutManager.orientation = LinearLayoutManager.VERTICAL
        binding.rcvSong.layoutManager = linearLayoutManager
        favoriteAdapter = FavoriteAdapter(requireContext())
        binding.rcvSong.adapter = favoriteAdapter

        //firebase reference songInfo
        songInfoReference = FirebaseDatabase.getInstance().getReference("songInfo")
        currentSongInfo()

        return binding.root
    }

    private fun getFavoriteSong(): MutableList<SongModel> {
        list.addAll(listAlbumTemp)
        val favorite = activity?.findViewById(R.id.imgFavorite) as ImageView
        favorite.isEnabled = false
        favorite.isClickable = false
        favorite.isFocusable = false
        for (item in list) {
            if (item.isFavorite && !listFavoriteSong.contains(item)) {
                listFavoriteSong.add(item)
            } else if (!item.isFavorite) {
                listFavoriteSong.remove(item)
            }
        }
        return listFavoriteSong
    }

    override fun onResume() {
        listFavoriteSong.removeAll(listLoadDataFavorite)
        listLoadDataFavorite.clear()
        favoriteAdapter.setData(getFavoriteSong())  //reflate list favorite song to recycle view
        super.onResume()
    }

    override fun onPause() {
        list.removeAll(listAlbumTemp)
        listFavoriteSong.removeAll(listLoadDataFavorite)
        listLoadDataFavorite.clear()
        getFragmentManager()?.beginTransaction()?.detach(this)?.attach(this)?.commit()

        super.onPause()
    }

    override fun onStop() {
        listAlbumTemp.clear()
        listLoadDataFavorite.clear()
        super.onStop()
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
                        val type = "favorite"
                        val currentSong = CurrentSong(
                            position.toString(),
                            type,
                            listFavoriteSong[position].songName,
                            listFavoriteSong[position].artistName,
                            listFavoriteSong[position].imgUrl,
                            listFavoriteSong[position].songUrl,
                            listFavoriteSong[position].duration,
                            listFavoriteSong[position].isFavorite
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
