package com.example.cosmix3

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import com.spotify.sdk.android.authentication.AuthenticationClient
import com.spotify.sdk.android.authentication.AuthenticationResponse

class MixActivity : AppCompatActivity() {

    val db = FirebaseFirestore.getInstance()

    lateinit var partyId: String
    lateinit var authToken: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        partyId = intent.getStringExtra(PARTY_ID)

        supportFragmentManager.beginTransaction().replace(R.id.fragment_holder, MixFragment()).commit()
    }

    companion object {

        const val PARTIES = "parties"
        const val PARTY_ID = "party-id"

        const val GOT_TOKEN = 1

        const val CLIENT_ID = "2fd46a7902e043e4bcb8ccda3d1381b2"
        const val REDIRECT_URI = "http://com.example.cosmix3/callback"
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == GOT_TOKEN) {
            val response = AuthenticationClient.getResponse(resultCode, data)

            when (response.type) {
                AuthenticationResponse.Type.ERROR -> Toast.makeText(this, "Cannot access Spotify!", Toast.LENGTH_LONG).show()

                AuthenticationResponse.Type.TOKEN -> {
                    MixFragment.myActivity.authToken = response.accessToken
                }

                else -> Log.println(Log.ERROR, "Spotify login", "SHOULD NOT GET HERE")
            }
        }
    }
}
