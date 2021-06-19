package com.example.musicplayerapp.ui.gallery.album

import android.content.Intent
import android.database.Cursor
import android.media.MediaMetadataRetriever
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.musicplayerapp.R
import com.example.musicplayerapp.adapter.AlbumAdapter
import com.example.musicplayerapp.adapter.RecyclerItemClickListener
import com.example.musicplayerapp.databinding.FragmentSongBinding
import com.example.musicplayerapp.model.CurrentSong
import com.example.musicplayerapp.model.SongModel
import com.example.musicplayerapp.ui.home.HomeActivity
import com.example.musicplayerapp.ui.slideshow.SlideshowActivity
import com.google.firebase.database.*


class AlbumFragment : Fragment() {

    companion object {
        var album = mutableListOf<SongModel>()
        var albumTemp = mutableListOf<SongModel>()
    }

    private lateinit var binding: FragmentSongBinding
    private lateinit var mSongAdapter: AlbumAdapter
    private lateinit var songInfoReference: DatabaseReference
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
        songInfoReference = FirebaseDatabase.getInstance().getReference("songInfo")
        val linearLayoutManager = LinearLayoutManager(requireContext())
        linearLayoutManager.orientation = LinearLayoutManager.VERTICAL
        binding.rcvSong.layoutManager = linearLayoutManager
        mSongAdapter = AlbumAdapter(requireContext())
        binding.rcvSong.adapter = mSongAdapter
        mSongAdapter.setData(getListAlbum())
        currentSongInfo()
        return binding.root
    }

    private fun getListAlbum(): MutableList<SongModel> {
        val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.ARTIST
        )

        val cursor: Cursor? = context?.contentResolver?.query(uri, projection, null, null, null)
        if (cursor != null) {
            while (cursor.moveToNext()) {
                val songName: String = cursor.getString(1)
                val artistName: String = cursor.getString(4)
                val data: String = cursor.getString(3)
                val songUrl: String = cursor.getString(3)
                val duration: String = cursor.getString(2)
                val isFavorite: Boolean = setUpFavoriteSong(songName)
                val songModel =
                    SongModel(songName, artistName, data, songUrl, duration, isFavorite)
                if (!album.contains(songModel)) {
                    album.add(songModel)
                }
                Log.i("Test", "$data")
            }
            cursor.close()
        }
        return album
    }

    private fun setUpFavoriteSong(songName: String): Boolean {
        for (item in HomeActivity.listLoadDataFavorite) {
            if (item.songName.equals(songName) && item.isFavorite) {
                return true
            }
        }
        return false
    }

    fun getImageUri(uri: String): ByteArray {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(uri)
        val art: ByteArray? = retriever.embeddedPicture
        retriever.release()
        return art!!
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
                        startActivity(Intent(activity, SlideshowActivity::class.java))
                        val type = "album"
                        val currentSong = CurrentSong(
                            position.toString(),
                            type,
                            album[position].songName,
                            album[position].artistName,
                            album[position].imgUrl,
                            album[position].songUrl,
                            album[position].duration,
                            album[position].isFavorite
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