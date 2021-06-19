package com.example.musicplayerapp.ui.home.title

import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.example.musicplayerapp.R
import com.example.musicplayerapp.adapter.SongAdapter
import com.example.musicplayerapp.adapter.ViewPagerAdapter
import com.example.musicplayerapp.databinding.FragmentHomeBinding
import com.example.musicplayerapp.ui.gallery.album.AlbumFragment
import com.example.musicplayerapp.ui.gallery.favorite.FavoriteFragment
import com.example.musicplayerapp.ui.gallery.song.SongFragment
import com.example.musicplayerapp.ui.home.HomeActivity


class HomeFragment : Fragment() {
    private lateinit var binding: FragmentHomeBinding
    private lateinit var mSongAdapter: SongAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate<FragmentHomeBinding>(
            inflater,
            R.layout.fragment_home,
            container,
            false
        )
        mSongAdapter = SongAdapter(requireContext())
        setupTopBar()
        setupViewPager()
        return binding.root
    }



    private fun setupTopBar() {
        (context as AppCompatActivity).window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        (context as AppCompatActivity).supportActionBar!!.apply {
            setBackgroundDrawable(ColorDrawable(Color.parseColor("#1B212E")))
            title = "HOME"
            elevation = 0F
        }
    }


    private fun setupViewPager() {
        val adapter = ViewPagerAdapter((context as AppCompatActivity).supportFragmentManager)
        adapter.addFragment(SongFragment(), "Song")
        adapter.addFragment(AlbumFragment(), "Library")
        adapter.addFragment(FavoriteFragment(), "Favorite")

        binding.apply {
            viewPager.offscreenPageLimit = 3
            viewPager.adapter = adapter
            tabMode.setupWithViewPager(viewPager)
            tabMode.getTabAt(0)!!.setIcon(R.drawable.ic_baseline_music_note_24)
            tabMode.getTabAt(1)!!.setIcon(R.drawable.ic_baseline_library_music_24)
            tabMode.getTabAt(2)!!.setIcon(R.drawable.ic_favorite)
            tabMode.getTabAt(0)!!.icon?.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN)
            tabMode.getTabAt(1)!!.icon?.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN)
            tabMode.getTabAt(2)!!.icon?.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN)
        }
    }



}