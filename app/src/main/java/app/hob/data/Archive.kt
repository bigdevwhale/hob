package app.hob.data

import app.hob.AppContainer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * The whole house in one zip: `house.json` with every table, and `photos/<id>.jpg`. Restoring replaces what is on the
 * phone, ids and all, so the photos keep matching their things. This is also how a house moves to another phone.
 */
object Archive {
    private const val VERSION = 1

    suspend fun export(c: AppContainer, out: OutputStream) = withContext(Dispatchers.IO) {
        val d = c.dao
        val root = JSONObject().put("app", "hob").put("version", VERSION)
        root.put("places", JSONArray(d.placesNow().map { JSONObject().put("id", it.id).put("name", it.name).put("sort", it.sort).put("uid", it.uid) }))
        root.put("rooms", JSONArray(d.roomsNow().map { JSONObject().put("id", it.id).put("place", it.place).put("name", it.name).put("emoji", it.emoji).put("sort", it.sort).put("uid", it.uid) }))
        root.put("spots", JSONArray(d.spotsNow().map { JSONObject().put("id", it.id).put("room", it.room).put("name", it.name).put("sort", it.sort).put("uid", it.uid) }))
        root.put("boxes", JSONArray(d.boxesNow().map { JSONObject().put("id", it.id).put("num", it.num).put("label", it.label).put("spot", it.spot).put("uid", it.uid) }))
        root.put("things", JSONArray(d.thingsNow().map { t ->
            JSONObject().put("id", t.id).put("name", t.name).put("emoji", t.emoji).put("cat", t.cat).put("spot", t.spot).put("box", t.box).put("note", t.note)
                .put("photo", t.photo).put("added", t.added).put("touched", t.touched).put("sizeKind", t.sizeKind).put("sizeMin", t.sizeMin.toDouble())
                .put("sizeMax", t.sizeMax.toDouble()).put("sizeLabel", t.sizeLabel).put("lentTo", t.lentTo).put("lentDay", t.lentDay).put("lentRemind", t.lentRemind)
                .put("season", t.season).put("state", t.state).put("goneHow", t.goneHow).put("goneDay", t.goneDay).put("sold", t.sold).put("kept", t.kept)
                .put("outKid", t.outKid).put("uid", t.uid)
        }))
        root.put("kids", JSONArray(d.kidsNow().map { JSONObject().put("id", it.id).put("name", it.name).put("born", it.born).put("color", it.color).put("uid", it.uid) }))
        root.put("measures", JSONArray(d.measuresNow().map { JSONObject().put("id", it.id).put("kid", it.kid).put("day", it.day).put("height", it.height.toDouble()).put("foot", it.foot.toDouble()) }))
        root.put("events", JSONArray(d.eventsNow().map { JSONObject().put("id", it.id).put("thing", it.thing).put("day", it.day).put("kind", it.kind).put("who", it.who) }))
        root.put("borrowed", JSONArray(d.borrowedNow().map { JSONObject().put("id", it.id).put("name", it.name).put("emoji", it.emoji).put("from", it.from).put("day", it.day).put("back", it.back) }))
        ZipOutputStream(out).use { z ->
            z.putNextEntry(ZipEntry("house.json")); z.write(root.toString(1).toByteArray()); z.closeEntry()
            c.photos.dir.listFiles()?.filter { it.isFile }?.forEach { f ->
                z.putNextEntry(ZipEntry("photos/${f.name}")); f.inputStream().use { it.copyTo(z) }; z.closeEntry()
            }
        }
    }

    private fun JSONObject.f(k: String) = optDouble(k, 0.0).toFloat()
    private fun JSONArray.objects() = (0 until length()).map { getJSONObject(it) }

    /** Replaces the house with the archive's. False if it isn't a Hob archive. */
    suspend fun import(c: AppContainer, input: InputStream): Boolean = withContext(Dispatchers.IO) {
        var json: JSONObject? = null
        val photos = mutableMapOf<String, ByteArray>()
        ZipInputStream(input).use { z ->
            while (true) {
                val e = z.nextEntry ?: break
                when {
                    e.name == "house.json" -> json = JSONObject(z.readBytes().decodeToString())
                    e.name.startsWith("photos/") && !e.isDirectory -> photos[e.name.removePrefix("photos/").substringAfterLast('/')] = z.readBytes()
                }
            }
        }
        val root = json?.takeIf { it.optString("app") == "hob" } ?: return@withContext false
        val d = c.dao
        withContext(Dispatchers.Main) {
            d.clearEvents(); d.clearMeasures(); d.clearThings(); d.clearBoxes(); d.clearSpots(); d.clearRooms(); d.clearPlaces(); d.clearKids(); d.clearBorrowed()
            root.optJSONArray("places")?.objects()?.forEach { d.add(Place(it.getLong("id"), it.getString("name"), it.optInt("sort"), it.optString("uid", uid()))) }
            root.optJSONArray("rooms")?.objects()?.forEach { d.add(Room(it.getLong("id"), it.getLong("place"), it.getString("name"), it.optString("emoji", "📦"), it.optInt("sort"), it.optString("uid", uid()))) }
            root.optJSONArray("spots")?.objects()?.forEach { d.add(Spot(it.getLong("id"), it.getLong("room"), it.getString("name"), it.optInt("sort"), it.optString("uid", uid()))) }
            root.optJSONArray("boxes")?.objects()?.forEach { d.add(Box(it.getLong("id"), it.getInt("num"), it.optString("label"), it.getLong("spot"), it.optString("uid", uid()))) }
            root.optJSONArray("things")?.objects()?.forEach { o ->
                d.add(Thing(id = o.getLong("id"), name = o.getString("name"), emoji = o.optString("emoji", "📦"), cat = o.optString("cat", "other"), spot = o.optLong("spot"),
                    box = o.optLong("box"), note = o.optString("note"), photo = o.optBoolean("photo"), added = o.optLong("added"), touched = o.optLong("touched"),
                    sizeKind = o.optInt("sizeKind"), sizeMin = o.f("sizeMin"), sizeMax = o.f("sizeMax"), sizeLabel = o.optString("sizeLabel"), lentTo = o.optString("lentTo"),
                    lentDay = o.optLong("lentDay"), lentRemind = o.optLong("lentRemind"), season = o.optInt("season"), state = o.optInt("state"), goneHow = o.optInt("goneHow"),
                    goneDay = o.optLong("goneDay"), sold = o.optInt("sold"), kept = o.optLong("kept"), outKid = o.optLong("outKid"), uid = o.optString("uid", uid())))
            }
            root.optJSONArray("kids")?.objects()?.forEach { d.add(Kid(it.getLong("id"), it.getString("name"), it.getLong("born"), it.optInt("color"), it.optString("uid", uid()))) }
            root.optJSONArray("measures")?.objects()?.forEach { d.add(Measure(it.getLong("id"), it.getLong("kid"), it.getLong("day"), it.f("height"), it.f("foot"))) }
            root.optJSONArray("events")?.objects()?.forEach { d.add(Event(it.getLong("id"), it.getLong("thing"), it.getLong("day"), it.getInt("kind"), it.optString("who"))) }
            root.optJSONArray("borrowed")?.objects()?.forEach { d.add(Borrowed(it.getLong("id"), it.getString("name"), it.optString("emoji", "📦"), it.optString("from"), it.getLong("day"), it.optLong("back"))) }
        }
        c.photos.dir.listFiles()?.forEach { it.delete() }
        photos.forEach { (name, bytes) -> java.io.File(c.photos.dir, name).writeBytes(bytes) }
        true
    }

    /** A plain table for a spreadsheet: one thing per row, with where it lies. */
    suspend fun csv(c: AppContainer, out: OutputStream, header: List<String>) = withContext(Dispatchers.IO) {
        val d = c.dao
        val rooms = d.roomsNow().associateBy { it.id }
        val spots = d.spotsNow().associateBy { it.id }
        val boxes = d.boxesNow().associateBy { it.id }
        val places = d.placesNow().associateBy { it.id }
        fun q(s: String) = "\"" + s.replace("\"", "\"\"") + "\""
        val sb = StringBuilder("\uFEFF").append(header.joinToString(";") { q(it) }).append("\r\n")
        for (t in d.thingsNow().filter { it.home }.sortedBy { it.name.lowercase() }) {
            val s = spots[t.spot]; val r = s?.let { rooms[it.room] }; val p = r?.let { places[it.place] }
            sb.append(listOf(t.name, p?.name ?: "", r?.name ?: "", s?.name ?: "", boxes[t.box]?.let { "№${it.num} ${it.label}".trim() } ?: "", t.sizeLabel, t.lentTo, t.note)
                .joinToString(";") { q(it) }).append("\r\n")
        }
        out.write(sb.toString().toByteArray())
    }
}
