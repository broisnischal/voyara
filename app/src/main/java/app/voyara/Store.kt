package app.voyara

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject

data class Place(val name: String, val detail: String, val at: Geo, val zoom: Double = 16.0)

/** Favorites, recents and settings, persisted in SharedPreferences. */
class Store(context: Context) {
    private val prefs = context.getSharedPreferences("voyara", Context.MODE_PRIVATE)

    val favorites = mutableStateListOf<Place>().apply { addAll(read(FAVORITES)) }
    val recents = mutableStateListOf<Place>().apply { addAll(read(RECENTS)) }

    fun isFavorite(at: Geo) = favorites.any { distance(it.at, at) < 15 }

    fun toggleFavorite(place: Place) {
        if (!favorites.removeAll { distance(it.at, place.at) < 15 }) favorites.add(0, place)
        write(FAVORITES, favorites)
    }

    fun removeFavorite(place: Place) {
        favorites.remove(place)
        write(FAVORITES, favorites)
    }

    fun renameFavorite(place: Place, name: String) {
        val i = favorites.indexOf(place)
        if (i < 0 || name.isBlank()) return
        favorites[i] = place.copy(name = name.trim())
        write(FAVORITES, favorites)
    }

    fun clearRecents() {
        recents.clear()
        write(RECENTS, recents)
    }

    fun addRecent(place: Place) {
        recents.removeAll { distance(it.at, place.at) < 30 }
        recents.add(0, place)
        while (recents.size > 20) recents.removeAt(recents.lastIndex)
        write(RECENTS, recents)
    }

    fun removeRecent(place: Place) {
        recents.remove(place)
        write(RECENTS, recents)
    }

    var camera: CameraMove?
        get() = runCatching {
            prefs.getString("camera", null)?.split(',')?.map { it.toDouble() }
                ?.let { (lat, lng, zoom) -> CameraMove(Geo(lat, lng), zoom) }
        }.getOrNull()
        set(v) = prefs.edit { putString("camera", v?.let { "${it.at.lat},${it.at.lng},${it.zoom ?: 15.0}" }) }

    /** Last fix from the real GPS, kept so it can still be shown while mocking hides it. */
    var realLocation: Geo?
        get() = prefs.getString("real", null)?.split(',')?.mapNotNull { it.toDoubleOrNull() }
            ?.takeIf { it.size == 2 }?.let { (lat, lng) -> Geo(lat, lng) }
        set(v) = prefs.edit { putString("real", v?.let { "${it.lat},${it.lng}" }) }

    var speedKmh: Double
        get() = prefs.getFloat("speed", 5f).toDouble()
        set(v) = prefs.edit { putFloat("speed", v.toFloat()) }

    var followRoads: Boolean
        get() = prefs.getBoolean("roads", true)
        set(v) = prefs.edit { putBoolean("roads", v) }

    var loop: Boolean
        get() = prefs.getBoolean("loop", false)
        set(v) = prefs.edit { putBoolean("loop", v) }

    var roam: Boolean
        get() = prefs.getBoolean("roam", false)
        set(v) = prefs.edit { putBoolean("roam", v) }

    var roamRadius: Double
        get() = prefs.getFloat("roamRadius", 100f).toDouble()
        set(v) = prefs.edit { putFloat("roamRadius", v.toFloat()) }

    var mapLayer: MapLayer
        get() = MapLayer.entries.getOrElse(prefs.getInt("layer", 0)) { MapLayer.Mono }
        set(v) = prefs.edit { putInt("layer", v.ordinal) }

    var theme: ThemeMode
        get() = ThemeMode.entries.getOrElse(prefs.getInt("theme", 0)) { ThemeMode.System }
        set(v) = prefs.edit { putInt("theme", v.ordinal) }

    var traffic: Boolean
        get() = prefs.getBoolean("traffic", false)
        set(v) = prefs.edit { putBoolean("traffic", v) }

    var drift: Boolean
        get() = prefs.getBoolean("drift", true)
        set(v) = prefs.edit { putBoolean("drift", v) }

    private fun read(key: String): List<Place> = runCatching {
        val a = JSONArray(prefs.getString(key, "[]"))
        List(a.length()) { i ->
            a.getJSONObject(i).run { Place(getString("name"), optString("detail"), Geo(getDouble("lat"), getDouble("lng"))) }
        }
    }.getOrDefault(emptyList())

    private fun write(key: String, places: List<Place>) {
        val a = JSONArray()
        for (p in places) {
            a.put(JSONObject().put("name", p.name).put("detail", p.detail).put("lat", p.at.lat).put("lng", p.at.lng))
        }
        prefs.edit { putString(key, a.toString()) }
    }

    private companion object {
        const val FAVORITES = "favorites"
        const val RECENTS = "recents"
    }
}
