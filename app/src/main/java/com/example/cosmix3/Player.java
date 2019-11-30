package com.example.cosmix3;

import android.content.Context;
import android.util.Log;

import androidx.recyclerview.widget.RecyclerView;

import com.spotify.android.appremote.api.ConnectionParams;
import com.spotify.android.appremote.api.Connector;
import com.spotify.android.appremote.api.PlayerApi;
import com.spotify.android.appremote.api.SpotifyAppRemote;
import com.spotify.protocol.client.Subscription;
import com.spotify.protocol.types.PlayerState;
import com.spotify.protocol.types.Track;

import java.util.List;

import static com.example.cosmix3.MainActivity.CLIENT_ID;
import static com.example.cosmix3.MainActivity.REDIRECT_URI;

public class Player {

    private SpotifyAppRemote mSpotifyAppRemote;
    boolean connected = false;
    int playable = 1;

    int currIdx;

    Runnable onStop;

    RecyclerView recycler;
    List<Song> queue;
    Context context;

    public Player(Context context) {
        this.context = context;

        connect();
    }

    public void connect() {
        ConnectionParams connectionParams =
                new ConnectionParams.Builder(CLIENT_ID)
                        .setRedirectUri(REDIRECT_URI)
                        .showAuthView(true)
                        .build();

        SpotifyAppRemote.connect(context, connectionParams,
                new Connector.ConnectionListener() {

                    public void onConnected(SpotifyAppRemote spotifyAppRemote) {
                        mSpotifyAppRemote = spotifyAppRemote;
                        Log.d("Player", "Connected! Yay!");

                        // Now you can start interacting with App Remote

                        mSpotifyAppRemote.getUserApi().getCapabilities().setResultCallback(result -> {
                            connected = true;

                            if (!result.canPlayOnDemand) {
                                playable = -2;
                            }
                        });


                    }

                    public void onFailure(Throwable throwable) {
                        Log.e("Player", throwable.getMessage(), throwable);

                        playable = -1;

                        // Something went wrong when attempting to connect! Handle errors here
                    }
                });
    }

    public void setRecycler(RecyclerView recycler) {
        this.recycler = recycler;
    }

    public void start() {

        Adapter adapter = (Adapter) recycler.getAdapter();

        queue = adapter.getSongs();

        if (connected && queue.size() > 0) {
            currIdx = -1;
            adapter.setPlayerApi(mSpotifyAppRemote.getPlayerApi());
            next(0);
        }
    }

    private void next(int idx) {

        currIdx++;
        if (currIdx < queue.size()) {
            PlayerApi playerApi = mSpotifyAppRemote.getPlayerApi();
            String currUri = queue.get(idx).getUri();

            playerApi.play(currUri).setResultCallback(callback -> {

                Subscription<PlayerState> subscription =

                // Subscribe to PlayerState
                playerApi
                        .subscribeToPlayerState();

                subscription
                        .setEventCallback(playerState -> {
                            final Track track = playerState.track;

                            if (track.name != null && !track.uri.equals(currUri)) {

                                subscription.cancel();
                                next(idx + 1);
                            }
                        });
            });

        } else {
            stop();
        }
    }

    public void setOnStop(Runnable runnable) {
        onStop = runnable;
    }

    public void pause() {
        mSpotifyAppRemote.getPlayerApi().pause();
    }

    public void resume() {
        mSpotifyAppRemote.getPlayerApi().resume();
    }

    public void stop() {
        ((Adapter) recycler.getAdapter()).revert();
        pause();
        onStop.run();
    }
}
