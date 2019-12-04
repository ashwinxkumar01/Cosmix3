package com.example.cosmix3;

import android.app.ProgressDialog;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class AsyncUtils {
//    public static List<Song> getSongs(List<String> isrcs) {
//        try {
//            List<Map<String, String>> map = Executors.newSingleThreadExecutor().submit(() -> CloudUtilsKt.getSongFacts(isrcs)).get();
//            List<Song> songs = new ArrayList<>();
//            for (Map<String, String> element : map) {
//                songs.add(new Song(element.get("name"), element.get("artist")));
//            }
//
//            return songs;
//        } catch (ExecutionException e) {
//            e.printStackTrace();
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//
//        return null;
//    }

    public static boolean checkParty(String partyID) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<Boolean> future = executor.submit(() -> CloudUtilsKt.checkParty(partyID));
        try {
            return future.get();
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static void newParty(String partyID) {
        Executors.newSingleThreadExecutor().execute(() -> CloudUtilsKt.newParty(partyID));
    }

    public static List<Playlist> getPlaylists(String service, String token){
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<List<Map<String, String>>> future = executor.submit(() -> CloudUtilsKt.playlists(service, token));

        List<Playlist> playlists = new ArrayList<>();

        try {
            List<Map<String, String>> rawPlaylists = future.get();
            for (Map<String, String> playlist : rawPlaylists) {
                String id = playlist.get("id");

                String name = playlist.get("name");
                String image = playlist.get("image");

                playlists.add(new Playlist(id, name == null ? "" : name, image == null ? "" : image));
            }
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }

        return playlists;
    }

    public static void add(String id, String playlist, String token) {
        Executors.newSingleThreadExecutor().execute(() -> {
            CloudUtilsKt.add(id, playlist, token);
        });
    }

    public static void save(String id, String name, String token) {
        Executors.newSingleThreadExecutor().execute(() -> CloudUtilsKt.save(id, name.replace(" ","%20"), token));
    }

    public static void saveGenre(String id, String name, String token, Toast toast) {
        Executors.newSingleThreadExecutor().execute(() -> {
            CloudUtilsKt.saveGenre(id, name.replace(" ","%20"), token);
            toast.cancel();
        });
    }

    public static List<Song> filterSongs(String name, int numSongs, String partyId) {
        Future<List<Map<String, String>>> isrcs = Executors.newSingleThreadExecutor().submit(() -> CloudUtilsKt.genFilter(name, numSongs, partyId));
        try {
            List<Song> songs = new ArrayList<>();

            List<Map<String, String>> map = isrcs.get();

            for (Map<String, String> factsMap : map) {
                songs.add(new Song(factsMap.get("name"), factsMap.get("artist"), factsMap.get("image"), factsMap.get("uri"), factsMap.get("isrc")));
            }

            return songs;
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static List<Map<String, String>> getPartySongs(String partyId) {
        try {
            return Executors.newSingleThreadExecutor().submit(() -> CloudUtilsKt.getFactsList(partyId)).get();
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void saveIsrcs(String name, List<Song> songs, String token) {
        if (songs.size() > 0) {
            StringBuilder isrcBuilder = new StringBuilder();
            isrcBuilder.append(songs.get(0).getIsrc());
            for (int i = 1; i < songs.size(); i++) {
                isrcBuilder.append('-').append(songs.get(i).getIsrc());
            }
            Executors.newSingleThreadExecutor().execute(() -> CloudUtilsKt.saveIsrcs(name, isrcBuilder.toString(), token));
        }
    }
}
