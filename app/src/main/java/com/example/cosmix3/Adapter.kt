package com.example.cosmix3

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.spotify.android.appremote.api.PlayerApi
import com.spotify.protocol.client.Subscription
import com.spotify.protocol.types.PlayerState

class Adapter(var context: Context) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var songs : MutableList<Song> = mutableListOf()

    var playing: ViewHolder? = null

    var playerApi : PlayerApi? = null
    var subscriptions: MutableList<Subscription<PlayerState>> = mutableListOf()

    var highlighted: ViewHolder? = null

    override fun getItemCount() = songs.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder , position: Int) {
        val curr = songs[position]
        with(holder as ViewHolder) {
            name.text = curr.name
            artist.text = curr.artist
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val vh = ViewHolder(LayoutInflater.from(context).inflate(R.layout.song_row, parent, false))
        if (playerApi != null) {
            val subscription = playerApi!!.subscribeToPlayerState()
            subscriptions.add(subscription)
            vh.giveSubscription(subscription!!)
        }
        return vh
    }

    fun updateData(newData: MutableList<Song>) {
        songs = newData
        notifyDataSetChanged()
    }

    fun clear() {
        updateData(mutableListOf())
    }

    fun revert() {
        subscriptions.forEach {
            it.cancel()
        }
        highlighted?.revert()
    }

    fun addSong(song: Song) {
        songs.add(song)
        notifyDataSetChanged()
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var name = itemView.findViewById<TextView>(R.id.songName)
        var artist = itemView.findViewById<TextView>(R.id.songArtist)

        fun highlight() {
            name.setTextColor(itemView.resources.getColor(R.color.normalBlue, null))
            artist.setTextColor(itemView.resources.getColor(R.color.normalBlue, null))
        }

        fun revert() {
            name.setTextColor(Color.parseColor("#ffffff"))
            artist.setTextColor(Color.parseColor("#ffffff"))
        }

        fun giveSubscription(subscription: Subscription<PlayerState>) {
            subscription.setEventCallback {
                if (it.track?.name == name.text) {
                    highlight()
                    highlighted = this
                } else {
                    revert()
                }
            }
        }
    }
}
