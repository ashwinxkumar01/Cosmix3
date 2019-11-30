package com.example.cosmix3

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class PlaylistsFragment : Fragment() {

    lateinit var recycler: RecyclerView
    lateinit var adapter: PlaylistsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val root = inflater.inflate(R.layout.fragment_playlists, container, false)

        myActivity = activity as MainActivity

        return root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recycler = view.findViewById(R.id.playlists_recycler)
        recycler.layoutManager = LinearLayoutManager(view.context)

        adapter = PlaylistsAdapter(AsyncUtils.getPlaylists("spotify", myActivity.authToken))

        recycler.adapter = adapter
    }

    companion object {
        lateinit var myActivity: MainActivity
    }

    inner class PlaylistsAdapter(var playlists: List<Playlist>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        override fun getItemCount() = playlists.size

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val curr = playlists[position]
            with (holder as PlaylistsViewHolder) {

                name.text = curr.name
                //TODO use glide or whatever to get the playlist image

                itemView.setOnClickListener {
                    AsyncUtils.add(myActivity.partyId, curr.id, myActivity.authToken)
                    activity?.supportFragmentManager?.popBackStack()
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return PlaylistsViewHolder(LayoutInflater.from(context).inflate(R.layout.playlists_row, parent, false))
        }

        inner class PlaylistsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            var name = itemView.findViewById<TextView>(R.id.playlist_name)
            var image = itemView.findViewById<ImageView>(R.id.playlist_image)
        }

    }
}