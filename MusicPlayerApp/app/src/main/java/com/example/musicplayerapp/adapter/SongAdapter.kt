package com.example.musicplayerapp.adapter

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.musicplayerapp.R
import com.example.musicplayerapp.model.SongModel
import com.example.musicplayerapp.ui.gallery.song.SongFragment
import java.util.*


class SongAdapter(val context: Context) :
    RecyclerView.Adapter<SongAdapter.ViewHolder>(), Filterable {

    private var listSong = mutableListOf<SongModel>()
    private var listSongTemp = SongFragment.listSongModel



    fun setData(mListSong: MutableList<SongModel>) {
//        this.listSong = mListSong
        listSong = SongFragment.listSongModel
        notifyDataSetChanged()
    }



    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater: LayoutInflater = LayoutInflater.from(context)
        val view: View = inflater.inflate(R.layout.item_song, parent, false)

        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        Glide.with(context)
            .load(listSong[position].imgUrl)
            .placeholder(R.drawable.thumbnail_default)
            .error(R.drawable.thumbnail_default)
            .into(holder.imgCover)

        holder.txtSongName.text = listSong[position].songName
        holder.txtSingerName.text = listSong[position].artistName
        changeFavoriteIcon(holder, position)
        holder.favorite.setOnClickListener { view ->
            run {
                listSong[position].isFavorite = !listSong[position].isFavorite
                changeFavoriteIcon(holder, position)
            }
        }
        holder.itemView.setOnClickListener {

        }
    }

    override fun getItemCount(): Int {
        return listSong.size
    }


    private fun changeFavoriteIcon(holder: ViewHolder, position: Int) {
        holder.imgFavorite.setImageResource(R.drawable.ic_favorite_border)


        if (listSong[position].isFavorite) {
            holder.imgFavorite.setImageResource(R.drawable.ic_favorite)
        }
       // Log.i("Test3", "listSongTemp ${listSongTemp.size}")
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val songItem: RelativeLayout = itemView.findViewById(R.id.flSongItem)
        val imgCover: ImageView = itemView.findViewById(R.id.imgSong)
        val txtSongName: TextView = itemView.findViewById(R.id.txtSongName)
        val txtSingerName: TextView = itemView.findViewById(R.id.txtSingerName)
        val favorite: FrameLayout = itemView.findViewById(R.id.favorite)
        val imgFavorite: ImageView = itemView.findViewById(R.id.imgFavorite)
        val imgMore: ImageView = itemView.findViewById(R.id.imgMore)
    }

    override fun getFilter(): Filter {
        return object: Filter(){
            override fun performFiltering(constraint: CharSequence?): FilterResults {

                Log.i("Test3", "listSongTemp ${listSongTemp.size}")

                val strSearch = constraint.toString()
                Log.i("Test3", " ${strSearch}")

                if(strSearch.isEmpty()) {
                    listSong = listSongTemp
                } else {
                    val list = mutableListOf<SongModel>()
                    for(item in listSongTemp ) {
                        if (item.songName.toLowerCase(Locale.ROOT).contains(strSearch.toLowerCase(Locale.ROOT))) {
                            list.add(item)
                        }
                    }
                    listSong = list
                    Log.i("Test", "list $list")

                }
                val filterResults = FilterResults()
                filterResults.values = listSong
                return filterResults
            }

            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                //listSong.clear()
                listSong = results!!.values as MutableList<SongModel>
                Log.i("Test11", "list $listSong")
                notifyDataSetChanged()
            }

        }
    }


}