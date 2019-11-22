package com.example.cosmix3

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.os.AsyncTask
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cosmix3.PlaylistsActivity.Companion.token
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.spotify.sdk.android.authentication.AuthenticationClient
import com.spotify.sdk.android.authentication.AuthenticationRequest
import com.spotify.sdk.android.authentication.AuthenticationResponse
import com.spotify.sdk.android.authentication.LoginActivity.REQUEST_CODE
import kotlinx.android.synthetic.main.activity_mix.*

class MixActivity : AppCompatActivity() {

    lateinit var recycler: RecyclerView
    lateinit var adapter: Adapter
    lateinit var partyId: String
    var authToken: String? = null

    var state = 0

    lateinit var lastPlaylistName: String
    lateinit var currListener: ListenerRegistration

    val GOT_TOKEN = 1

    val CLIENT_ID = "2fd46a7902e043e4bcb8ccda3d1381b2"
    val REDIRECT_URI = "http://com.example.cosmix3/callback"

    val db = FirebaseFirestore.getInstance()

    fun getSpotifyToken() {
        val builder = AuthenticationRequest.Builder(CLIENT_ID, AuthenticationResponse.Type.TOKEN, REDIRECT_URI)

        builder.setScopes(arrayOf("playlist-read-private", "playlist-read-collaborative", "playlist-modify-private"))
        val request = builder.build()

        AuthenticationClient.openLoginActivity(this, GOT_TOKEN, request)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.filter -> {
            // User chose the "Filter" item, show the app settings UI...

            val builder = AlertDialog.Builder(this)
            builder.setTitle("Genre name")

            var viewInflated = LayoutInflater.from(this).inflate(R.layout.text_dialog, findViewById(android.R.id.content), false)
            val input: EditText = viewInflated.findViewById(R.id.input)
            builder.setView(viewInflated)

            builder.setPositiveButton("OK", DialogInterface.OnClickListener { _, _ ->

                class GenTask(val filter: String, val partyId: String, val adapter: Adapter) : AsyncTask<Void, Void, List<String>>() {
                    override fun doInBackground(vararg params: Void?): List<String> {
                        val isrcs: List<String> = genFilter(filter, 5, partyId)

                        return isrcs
                    }

                    override fun onPostExecute(result: List<String>?) {
                        if (result != null) {
                            stopRealTime()
                            adapter.updateData(AsyncUtils.getSongs(result))
                        }
                    }
                }

                GenTask(input.text.toString(), partyId, adapter).execute()

                Toast.makeText(this@MixActivity, "Generating filtered playlist", Toast.LENGTH_LONG).show()

            })
            builder.setNegativeButton("Cancel", DialogInterface.OnClickListener() { dialog, which ->
                dialog.cancel()
            })

            builder.show()

            true
        }

        R.id.push -> {
            // User chose the "Push" action, mark the current item
            // as a favorite...

            val builder = AlertDialog.Builder(this)
            builder.setTitle("Playlist name")

            var viewInflater = LayoutInflater.from(this).inflate(R.layout.text_dialog, findViewById(android.R.id.content), false)

            val input: EditText = viewInflater.findViewById(R.id.input)

            builder.setView(viewInflater)

            builder.setPositiveButton("OK", DialogInterface.OnClickListener { _, _ ->
                if (authToken != null) {
                    savePlaylistToSpotify(input.text.toString())
                } else {
                    state = 2
                    lastPlaylistName = input.text.toString()
                    getSpotifyToken()
                }
            })
            builder.setNegativeButton("Cancel", DialogInterface.OnClickListener() { dialog, which ->
                dialog.cancel()
            })

            builder.show()

            startRealTime()

            print("Pushed!") //dummy print
            true
        }

        R.id.add -> {
            // User chose the "Add" action, mark the current item
            // as a favorite...
           // print("Add!") //dummy print
            if (authToken != null) {

            } else {
                state = 1
                getSpotifyToken()
            }

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

    fun stopRealTime() {
        currListener.remove()
    }

    fun initRecycler() {
        recycler = findViewById(R.id.recyclerView)
        recycler.layoutManager = LinearLayoutManager(this)
        adapter = Adapter(this)
        recycler.adapter = adapter
    }

    fun savePlaylistToSpotify(text: String) {
        val toast = Toast.makeText(this@MixActivity, "Uploading to Spotify", Toast.LENGTH_LONG)
        toast.show()
        AsyncUtils.save(partyId, text, authToken, toast)
    }

    override fun onResume() {
        super.onResume()
        authToken = intent.getStringExtra("token")
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == GOT_TOKEN) {
            if (state == 2) {
                savePlaylistToSpotify(lastPlaylistName)
            } else if (state == 1) {
                val response = AuthenticationClient.getResponse(resultCode, data)

                when (response.type) {
                    AuthenticationResponse.Type.ERROR -> Log.println(Log.ERROR, "auth", "ERROR LOGGING IN!")
                    AuthenticationResponse.Type.TOKEN ->  startActivityForResult(Intent(this@MixActivity, PlaylistsActivity::class.java)
                        .putExtra("token", response.accessToken)
                        .putExtra("partyID", partyId)
                        , -1)
                }
            }
        }
    }

    companion object {
        const val PARTIES = "parties"
        const val PARTY_ID = "party-id"
    }



}
