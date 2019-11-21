package com.example.cosmix3

import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.spotify.sdk.android.authentication.AuthenticationClient
import com.spotify.sdk.android.authentication.AuthenticationRequest
import com.spotify.sdk.android.authentication.AuthenticationResponse
import kotlinx.android.synthetic.main.activity_mix.*

class MixActivity : AppCompatActivity() {

    lateinit var recycler: RecyclerView
    lateinit var adapter: Adapter

    val db = FirebaseFirestore.getInstance()

    lateinit var partyId: String
    lateinit var authToken: String

    lateinit var currListener: ListenerRegistration

    fun getSpotifyToken() {
        val builder = AuthenticationRequest.Builder(CLIENT_ID, AuthenticationResponse.Type.TOKEN, REDIRECT_URI)

        builder.setScopes(arrayOf("playlist-read-private", "playlist-read-collaborative", "playlist-modify-private"))
        val request = builder.build()

        AuthenticationClient.openLoginActivity(this, GOT_TOKEN, request)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.filter -> {
            true
        }

        R.id.push -> {

            savePlaylistToSpotify(partyId)

            true

        }

        R.id.add -> {



            true
        }

        else -> {
            // If we got here, the user's action was not recognized.
            // Invoke the superclass to handle it.
            super.onOptionsItemSelected(item)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mix)
        initRecycler()

        partyId = intent.getStringExtra(PARTY_ID)


        setSupportActionBar(my_toolbar)

        fillAdapter()
    }

    override fun onStart() {
        super.onStart()

        getSpotifyToken()

        startRealTime()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu to use in the action bar
        val inflater = menuInflater
        inflater.inflate(R.menu.bar, menu)
        return super.onCreateOptionsMenu(menu)
    }

    fun fillAdapter() {
        class FillTask : AsyncTask<Void, Void, List<Map<String, String>>>() {
            override fun doInBackground(vararg params: Void?): List<Map<String, String>> {
                return AsyncUtils.getPartySongs(partyId)
            }

            override fun onPostExecute(result: List<Map<String, String>>?) {
                adapter.clear()
                result?.forEach { adapter.addSong(Song(it.getValue("name"), it.getValue("artist"))) }
            }
        }
        FillTask().execute()
    }

    fun startRealTime() {
        db.collection(PARTIES).document(partyId)
        currListener = db.collection(PARTIES).document(partyId)
            .addSnapshotListener { snapshot, _ ->
                fillAdapter()
            }
    }

    fun initRecycler() {
        recycler = findViewById(R.id.recyclerView)
        recycler.layoutManager = LinearLayoutManager(this)
        adapter = Adapter(this)
        recycler.adapter = adapter
    }

    fun savePlaylistToSpotify(text: String) {
        Toast.makeText(this@MixActivity, "Sending to Spotify...", Toast.LENGTH_SHORT).show()
        AsyncUtils.save(partyId, text, authToken)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == GOT_TOKEN) {
            val response = AuthenticationClient.getResponse(resultCode, data)

            when (response.type) {
                AuthenticationResponse.Type.ERROR -> Toast.makeText(this@MixActivity, "Cannot access Spotify!", Toast.LENGTH_LONG).show()

                AuthenticationResponse.Type.TOKEN -> {
                    authToken = response.accessToken

                }

                else -> Log.println(Log.ERROR, "Spotify login", "SHOULD NOT GET HERE")
            }
        }
    }

    companion object {
        const val PARTIES = "parties"
        const val PARTY_ID = "party-id"

        const val GOT_TOKEN = 1

        const val CLIENT_ID = "2fd46a7902e043e4bcb8ccda3d1381b2"
        const val REDIRECT_URI = "http://com.example.cosmix3/callback"
    }



}
