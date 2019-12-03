package com.example.cosmix3

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class Adapter(var context: Context) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var songs : MutableList<Song> = mutableListOf<Song>()

    override fun getItemCount() = songs.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder , position: Int) {
        val curr = songs[position]
        with(holder as ViewHolder) {
            name.text = curr.name
            artist.text = curr.artist
            Glide.with(context).load(curr.image).placeholder(R.drawable.cosmix_logo)
                .into(imageView)

        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return ViewHolder(LayoutInflater.from(context).inflate(R.layout.song_row, parent, false))
    }

    fun updateData(newData: MutableList<Song>) {
        songs = newData
        notifyDataSetChanged()
    }

    fun clear() {
        updateData(mutableListOf())
    }

    fun filter(text: String) {
        val newData: MutableList<Song> = mutableListOf()
        for (song in songs) {
            if (song.name.toLowerCase().contains(text.toLowerCase()) || song.artist.toLowerCase().contains(text.toLowerCase())) {
                newData.add(song)
            }
        }
        updateData(newData)
    }

    fun addSong(song: Song) {
        songs.add(song)
        notifyDataSetChanged()
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var name = itemView.findViewById<TextView>(R.id.songName)
        var artist = itemView.findViewById<TextView>(R.id.songArtist)
        var imageView: ImageView = itemView.findViewById(R.id.songLogo)
    }
}
