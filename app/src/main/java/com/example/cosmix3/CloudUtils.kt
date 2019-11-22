package com.example.cosmix3

import java.net.HttpURLConnection
import java.net.URL
import com.google.gson.Gson
import java.io.FileNotFoundException

const val FUNCTION= "https://us-central1-streamline-5ab87.cloudfunctions.net/"

const val GET_FACTS = "get_facts"
const val CHECK_PARTY = "check_party"
const val NEW_PARTY = "new_party"
const val PLAYLISTS = "playlists"
const val ADD = "add"
const val SAVE = "save"
const val SAVE_GENRE = "gen_playlist"
const val GEN_FILTER = "gen_filter"
const val GET_FACTS_LIST = "get_facts_list"

private fun callFunction(name: String, params: Map<String, Any>) : String {

    var formatted = "$FUNCTION$name?"

    for ((param, value) in params) {
        formatted = formatted.plus("&$param=$value")
    }

    var response = ""

    with(URL(formatted).openConnection() as HttpURLConnection) {
        requestMethod = "POST"

        inputStream.bufferedReader().use {
            it.lines().forEach { line ->
                response = response.plus(line)
            }
        }
    }

    return response
}

fun getDict(json: String) : Map<String, String> {
    val gson = Gson()
    val map = gson.fromJson(json, Map::class.java)
    return map as Map<String, String>
}

fun getMapList(list: String) : List<Map<String, String>> {
    val gson = Gson()
    val map = gson.fromJson(list, List::class.java)
    return map as List<Map<String, String>>
}

fun getSongFacts(isrcs: List<String>) : List<Map<String, String>> = getMapList(callFunction(GET_FACTS, mapOf(Pair("isrc", isrcs))))

fun checkParty(partyId: String) : Boolean =
    getDict(callFunction(CHECK_PARTY, mapOf(Pair("id", partyId))))["result"] as Boolean

fun newParty(partyId: String) : Boolean =
    callFunction(NEW_PARTY, mapOf(Pair("id", partyId))) == "Success"

fun playlists(service: String, token: String) : List<Map<String, String>> {
    return getMapList(callFunction(PLAYLISTS, mapOf(Pair("service", service), Pair("token", token))))
}

fun add(id: String, playlist: String, token: String) : Boolean {
    try {
        callFunction(
            ADD,
            mapOf(Pair("id", id), Pair("playlist", playlist), Pair("token", token))
        )
        return true
    } catch (e: FileNotFoundException) {
        return false
    }
}

fun save(partyId: String, name: String, token: String) {
    callFunction(SAVE, mapOf(Pair("id", partyId), Pair("name", name), Pair("token", token)))
}

fun saveGenre(partyId: String, name: String, token: String) {
    callFunction(SAVE_GENRE, mapOf(Pair("id", partyId), Pair("name", name), Pair("token", token), Pair("numSongs", "5")))
}

fun genFilter(name: String, numSongs: Int, partyID: String) : List<String> = Gson().fromJson(
    callFunction(
        GEN_FILTER, mapOf(
    "name" to name,
    "numSongs" to numSongs,
    "id" to partyID
)), List::class.java) as List<String>

fun getFactsList(id: String) : List<Map<String, String>> = Gson().fromJson(
    callFunction(
        GET_FACTS_LIST, mapOf(Pair("id", id))), List::class.java) as List<Map<String, String>>