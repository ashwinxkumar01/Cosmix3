package com.example.cosmix3

import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.SearchView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.ListenerRegistration
import com.spotify.sdk.android.authentication.AuthenticationClient
import com.spotify.sdk.android.authentication.AuthenticationRequest
import com.spotify.sdk.android.authentication.AuthenticationResponse
import kotlinx.android.synthetic.main.fragment_mix.*
import kotlinx.android.synthetic.main.login_dialog.view.*


class MixFragment : Fragment() {

    lateinit var recycler: RecyclerView
    lateinit var adapter: Adapter

    lateinit var filterItem: MenuItem

    lateinit var currListener: ListenerRegistration

    var filtered = false
    var currFilter = ""
    var currList = mutableListOf<Song>()

    companion object {
        lateinit var myActivity: MixActivity
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_mix, container, false)
        myActivity = activity as MixActivity

        setHasOptionsMenu(true)

        return root
    }

    private fun setupSearchbar() {
        search_bar.setOnQueryTextListener(object : SearchView.OnQueryTextListener {

            override fun onQueryTextChange(newText: String): Boolean {

                if (newText.isEmpty()) {
                    if (filtered) {
                        startRealTime()
                        filtered = false
                    }
                } else {
                    stopRealTime()
                    filtered = true
                    val newData = mutableListOf<Song>()
                    for (song in currList) {
                        if (song.name.toLowerCase().contains(newText.toLowerCase()) || song.artist.toLowerCase().contains(newText.toLowerCase())) {
                            newData.add(song)
                        }
                    }
                    adapter.updateData(newData)
                }

                return true
            }

            override fun onQueryTextSubmit(query: String): Boolean {
                return false
            }

        })
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
                            .setTitle("Filter party")

                        //show dialog
                        val  mAlertDialog = mBuilder.show()
                        //(mDialogView.findViewById<EditText>(R.id.input)).hint = "Enter a mood or genre"

                        //login button click of custom layout
                        mDialogView.dialogLoginBtn.setOnClickListener {
                            //dismiss dialog
                            //get text from EditTexts of custom layout
                            currFilter = mDialogView.dialogPasswEt.text.toString()
                            filter(currFilter)
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

                    if (filtered) {
                        AsyncUtils.saveIsrcs(currFilter, adapter.songs, myActivity.authToken)
                    } else {
                        AsyncUtils.save(myActivity.partyId, myActivity.partyId, myActivity.authToken)
                    }

                    Toast.makeText(context, "Sent to Spotify!", Toast.LENGTH_SHORT).show()

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

        initRecycler()

        fillAdapter()

        setupToolbar()

        setupSearchbar()
    }

    fun getSpotifyToken() {
        val builder = AuthenticationRequest.Builder(
            MixActivity.CLIENT_ID, AuthenticationResponse.Type.TOKEN,
            MixActivity.REDIRECT_URI
        )

        builder.setScopes(arrayOf("playlist-read-private", "playlist-read-collaborative", "playlist-modify-private"))
        val request = builder.build()

        AuthenticationClient.openLoginActivity(activity, MixActivity.GOT_TOKEN, request)
    }

    override fun onStart() {
        super.onStart()

        getSpotifyToken()

        startRealTime()
    }

    override fun onPause() {
        super.onPause()
        stopRealTime()
    }

    override fun onResume() {
        super.onResume()

        fillAdapter()

        startRealTime()
    }

    fun fillAdapter() {
        class FillTask : AsyncTask<Void, Void, List<Map<String, String>>>() {
            override fun doInBackground(vararg params: Void?): List<Map<String, String>> {
                return AsyncUtils.getPartySongs(myActivity.partyId)
            }

            override fun onPostExecute(result: List<Map<String, String>>?) {
                if (result != null) {
                    currList = mutableListOf()
                    for (factsMap in result) {
                        if (factsMap != null) {
                            currList.add(
                                Song(
                                    factsMap["name"]!!,
                                    factsMap["artist"]!!,
                                    factsMap["image"]!!,
                                    factsMap["uri"]!!,
                                    factsMap["isrc"]!!
                                )
                            )
                        }
                    }
                }
                adapter.updateData(currList)
            }
        }
        FillTask().execute()
    }

    fun startRealTime() {
        myActivity.db.collection(MixActivity.PARTIES).document(myActivity.partyId)
        currListener = myActivity.db.collection(MixActivity.PARTIES).document(myActivity.partyId)
            .addSnapshotListener { snapshot, _ ->
                val mapList = snapshot?.get("filtTracks") as List<Map<String, String>>
                val songs: MutableList<Song> = mutableListOf()
                for (map in mapList) {
                    songs.add(Song(map["name"]!!, map["artist"]!!, map["image"]!!, map["uri"]!!, map["isrc"]!!))
                }
                adapter.updateData(songs.toList())
            }
    }

    fun filter(query: String) {
        class GenTask(val filter: String, val partyId: String, val adapter: Adapter) : AsyncTask<Void, Void, List<Song>>() {
            override fun doInBackground(vararg params: Void?): List<Song> {
                val isrcs: List<Song> = AsyncUtils.filterSongs(filter, 10, partyId)

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
}