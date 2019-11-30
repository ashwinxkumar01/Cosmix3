package com.example.cosmix3

import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cosmix3.MainActivity.Companion.CLIENT_ID
import com.example.cosmix3.MainActivity.Companion.REDIRECT_URI
import com.google.firebase.firestore.ListenerRegistration
import com.spotify.sdk.android.authentication.AuthenticationClient
import com.spotify.sdk.android.authentication.AuthenticationRequest
import com.spotify.sdk.android.authentication.AuthenticationResponse
import kotlinx.android.synthetic.main.fragment_mix.*
import kotlinx.android.synthetic.main.login_dialog.view.*


class MixFragment : Fragment() {

    lateinit var recycler: RecyclerView
    lateinit var adapter: Adapter

    lateinit var player: Player

    lateinit var filterItem: MenuItem

    lateinit var currListener: ListenerRegistration

    var filtered = false

    var playing = false
    var shuffled = false
    var paused = false

    companion object {
        lateinit var myActivity: MainActivity
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_mix, container, false)
        myActivity = activity as MainActivity

        setHasOptionsMenu(true)

        return root
    }

    private fun setupPlayer() {

        player.setRecycler(recycler)

        player.setOnStop {
            playing = false
            paused = false
            play.setImageResource(R.drawable.ic_play_arrow_black_24dp)
            shuffle.setImageResource(R.drawable.ic_shuffle_black_24dp)
            startRealTime()
        }

        play.setOnClickListener {

            if (playing) {
                paused = if (paused) {
                    //resume pressed
                    player.resume()
                    play.setImageResource(R.drawable.ic_pause_black_24dp)
                    false
                } else {
                    //pause pressed
                    player.pause()
                    play.setImageResource(R.drawable.ic_play_arrow_black_24dp)
                    true
                }
            } else {
                //play pressed
                if (player.playable == 1) {
                    player.start()

                    stopRealTime()
                    playing = true
                    paused = false

                    play.setImageResource(R.drawable.ic_pause_black_24dp)
                    shuffle.setImageResource(R.drawable.ic_stop_black_24dp)

                    Toast.makeText(context, "Mix will stop updating until stopped or finished playing", Toast.LENGTH_SHORT).show()
                } else if (player.playable == -1) {
                    Toast.makeText(context, "Must be signed in to Spotify App", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Requires Spotify premium", Toast.LENGTH_SHORT).show()
                }
            }
        }

        shuffle.setOnClickListener {
            if (playing) {
                //stop pressed
                shuffled = false
                player.stop()
            } else {
                //shuffle pressed
                shuffled = true
                stopRealTime()
                adapter.songs.shuffle()
                adapter.notifyDataSetChanged()
            }
        }
    }

    private fun setupToolbar() {
        val toolbar = view?.findViewById(R.id.my_toolbar) as Toolbar
        toolbar.inflateMenu(R.menu.bar)
        val menu = toolbar.menu

        filterItem = menu.findItem(R.id.filter)

        toolbar.subtitle = myActivity.partyId

        toolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.filter -> {

                    if (!filtered) {

                        val mDialogView = LayoutInflater.from(context).inflate(R.layout.login_dialog, null)
                        //AlertDialogBuilder
                        val mBuilder = AlertDialog.Builder(context!!)
                            .setView(mDialogView)
                            .setTitle("Enter a filter")
                        //show dialog
                        val  mAlertDialog = mBuilder.show()
                        //login button click of custom layout
                        mDialogView.dialogLoginBtn.setOnClickListener {
                            //dismiss dialog
                            //get text from EditTexts of custom layout
                            filter(mDialogView.dialogPasswEt.text.toString())
                            mAlertDialog.dismiss()
                        }
                        //cancel button click of custom layout
                        mDialogView.dialogCancelBtn.setOnClickListener {
                            //dismiss dialog
                            mAlertDialog.dismiss()
                        }
                    } else {
                        filterItem.title = "Filter"
                        filtered = false
                        fillAdapter()
                        startRealTime()
                    }

                    true
                }

                R.id.push -> {

                    savePlaylistToSpotify(myActivity.partyId)

                    true

                }

                R.id.add -> {

                    activity?.supportFragmentManager?.beginTransaction()
                        ?.replace(R.id.fragment_holder, PlaylistsFragment())?.addToBackStack(null)?.commit()

                    true
                }

                else -> {
                    // If we got here, the user's action was not recognized.
                    // Invoke the superclass to handle it.
                    super.onOptionsItemSelected(it)
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        // Inflate the menu to use in the action bar
        val inflater = activity?.menuInflater
        inflater?.inflate(R.menu.bar, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        player = myActivity.myPlayer

        initRecycler()
        setupToolbar()
        fillAdapter()
        setupPlayer()
    }

    private fun getSpotifyToken() {
        val builder = AuthenticationRequest.Builder(
            CLIENT_ID, AuthenticationResponse.Type.TOKEN,
            REDIRECT_URI
        )

        builder.setScopes(arrayOf("playlist-read-private", "playlist-read-collaborative", "playlist-modify-private"))
        val request = builder.build()

        AuthenticationClient.openLoginActivity(activity, MainActivity.GOT_TOKEN, request)
    }

    override fun onStart() {
        super.onStart()

        if (!playing && !shuffled) {

            if (myActivity.authToken == null) {
                getSpotifyToken()
            }

            startRealTime()
        }
    }

    override fun onPause() {
        super.onPause()
        if (!playing) {
            stopRealTime()
        }
    }

    override fun onResume() {
        super.onResume()

        if (!playing && !shuffled) {
            fillAdapter()
            startRealTime()
        }
    }

    fun fillAdapter() {
        class FillTask : AsyncTask<Void, Void, List<Map<String, String>>>() {
            override fun doInBackground(vararg params: Void?): List<Map<String, String>> {
                return AsyncUtils.getPartySongs(myActivity.partyId)
            }

            override fun onPostExecute(result: List<Map<String, String>>?) {
                adapter.clear()
                result?.forEach { adapter.addSong(Song(it.getValue("name"), it.getValue("artist"), it.getValue("image"), it.getValue("uri"))) }
            }
        }
        FillTask().execute()
    }

    fun startRealTime() {
        myActivity.db.collection(MainActivity.PARTIES).document(myActivity.partyId)
        currListener = myActivity.db.collection(MainActivity.PARTIES).document(myActivity.partyId)
            .addSnapshotListener { snapshot, _ ->
                fillAdapter()
            }
    }

    fun filter(query: String) {
        class GenTask(val filter: String, val partyId: String, val adapter: Adapter) : AsyncTask<Void, Void, List<Song>>() {
            override fun doInBackground(vararg params: Void?): List<Song> {
                val isrcs: List<Song> = AsyncUtils.filterSongs(filter, 5, partyId)

                return isrcs
            }

            override fun onPostExecute(result: List<Song>?) {
                if (result != null) {
                    stopRealTime()
                    adapter.updateData(result as MutableList<Song>)
                }
            }
        }

        filtered = true
        filterItem.title = "Reset"
        stopRealTime()

        GenTask(query, myActivity.partyId, adapter).execute()

        Toast.makeText(context, "filtering party...", Toast.LENGTH_LONG).show()
    }

    fun stopRealTime() {
        currListener.remove()
    }

    fun initRecycler() {
        recycler = myActivity.findViewById(R.id.recyclerView)
        recycler.layoutManager = LinearLayoutManager(context)
        adapter = Adapter(context!!)
        recycler.adapter = adapter
    }

    fun savePlaylistToSpotify(text: String) {
        Toast.makeText(context, "Sending to Spotify...", Toast.LENGTH_SHORT).show()
        AsyncUtils.save(myActivity.partyId, text, myActivity.authToken)
    }
}