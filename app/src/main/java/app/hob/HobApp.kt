package app.hob

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.net.Uri
import androidx.appcompat.app.AppCompatDelegate
import app.hob.data.Borrowed
import app.hob.data.Box
import app.hob.data.DemoSeed
import app.hob.data.Ev
import app.hob.data.Event
import app.hob.data.HobDb
import app.hob.data.Kid
import app.hob.data.Measure
import app.hob.data.Photos
import app.hob.data.Place
import app.hob.data.Room
import app.hob.data.SettingsStore
import app.hob.data.Spot
import app.hob.data.Status
import app.hob.data.Thing
import app.hob.data.now
import app.hob.domain.today
import app.hob.notify.Reminders
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class AppContainer(context: Context) {
    val app: Context = context.applicationContext
    val db = HobDb.create(app)
    val dao = db.dao()
    val settings = SettingsStore(app)
    val photos = Photos(app)
    /** On the main thread: Room and DataStore are main-safe. */
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    /** True while the demo build fills its house, so the first screen doesn't jump. */
    val seeding = MutableStateFlow(BuildConfig.DEMO)

    fun write(block: suspend AppContainer.() -> Unit) {
        scope.launch { block() }
    }

    private suspend fun log(thing: Long, kind: Int, who: String = "") = dao.add(Event(thing = thing, day = today(), kind = kind, who = who))

    // ── places, rooms, spots, boxes ──

    suspend fun addPlace(name: String): Long = dao.add(Place(name = name.trim(), sort = dao.placesNow().size))

    suspend fun addRoom(place: Long, name: String, emoji: String, spots: List<String>): Long {
        val id = dao.add(Room(place = place, name = name.trim(), emoji = emoji, sort = dao.roomsNow().count { it.place == place }))
        spots.forEachIndexed { i, s -> dao.add(Spot(room = id, name = s.trim(), sort = i)) }
        return id
    }

    suspend fun addSpot(room: Long, name: String): Long = dao.add(Spot(room = room, name = name.trim(), sort = dao.spotsNow().count { it.room == room }))

    suspend fun addBox(spot: Long, label: String): Long = dao.add(Box(num = dao.lastBox() + 1, label = label.trim(), spot = spot))

    /** Removes a room with its spots; only offered when nothing is in it. */
    suspend fun dropRoom(r: Room) {
        dao.spotsNow().filter { it.room == r.id }.forEach { s -> dao.boxesNow().filter { it.spot == s.id }.forEach { dao.drop(it) }; dao.drop(s) }
        dao.drop(r)
    }

    // ── things ──

    suspend fun addThing(t: Thing, photo: Uri? = null, past: List<String> = emptyList()): Long {
        val d = today()
        val id = dao.add(t.copy(added = d, touched = d, photo = false))
        past.filter { it.isNotBlank() }.forEach { dao.add(Event(thing = id, day = d - 1, kind = Ev.PAST, who = it.trim())) }
        log(id, Ev.ADDED)
        if (photo != null) setPhoto(id, photo)
        return id
    }

    suspend fun save(t: Thing) = dao.save(t.copy(updated = now()))

    suspend fun setPhoto(id: Long, uri: Uri) {
        val bmp = photos.read(uri) ?: return
        photos.keep(id, bmp)
        dao.thing(id)?.let { dao.save(it.copy(photo = true, updated = now())) }
    }

    suspend fun touch(t: Thing) = save(t.copy(touched = today()))

    suspend fun move(t: Thing, spot: Long, box: Long, where: String) {
        save(t.copy(spot = spot, box = box, touched = today()))
        log(t.id, Ev.MOVED, where)
    }

    suspend fun lend(t: Thing, who: String, remindIn: Int) {
        val d = today()
        save(t.copy(lentTo = who.trim(), lentDay = d, lentRemind = if (remindIn > 0) d + remindIn else 0))
        log(t.id, Ev.LENT, who.trim())
    }

    suspend fun back(t: Thing) {
        log(t.id, Ev.BACK, t.lentTo)
        save(t.copy(lentTo = "", lentDay = 0, lentRemind = 0, touched = today()))
    }

    /** A child takes it off the shelf to wear. */
    suspend fun wear(t: Thing, kid: Kid) {
        save(t.copy(state = Status.OUT, outKid = kid.id, touched = today()))
        log(t.id, Ev.WORN, kid.name)
    }

    /** Outgrown: back onto a shelf to wait for the next child. */
    suspend fun putAway(t: Thing, spot: Long, box: Long) {
        save(t.copy(state = Status.HOME, outKid = 0, spot = spot, box = box, touched = today()))
        log(t.id, Ev.RETURNED)
    }

    suspend fun letGo(t: Thing, how: Int, sum: Int = 0) {
        save(t.copy(state = Status.GONE, goneHow = how, goneDay = today(), sold = sum, lentTo = ""))
        log(t.id, Ev.GONE, how.toString())
    }

    suspend fun keep(t: Thing) = save(t.copy(kept = today()))

    suspend fun delete(t: Thing) {
        dao.dropEvents(t.id)
        dao.drop(t)
        photos.delete(t.id)
    }

    // ── kids, borrowed ──

    suspend fun addKid(name: String, born: Long, height: Float, foot: Float): Long {
        val id = dao.add(Kid(name = name.trim(), born = born, color = dao.kidsNow().size))
        if (height > 0f || foot > 0f) dao.add(Measure(kid = id, day = today(), height = height, foot = foot))
        return id
    }

    suspend fun measure(kid: Long, height: Float, foot: Float) = dao.add(Measure(kid = kid, day = today(), height = height, foot = foot))

    suspend fun dropKid(k: Kid) {
        dao.dropMeasures(k.id)
        dao.drop(k)
    }

    suspend fun borrow(name: String, emoji: String, from: String) = dao.add(Borrowed(name = name.trim(), emoji = emoji, from = from.trim(), day = today()))
    suspend fun giveBack(b: Borrowed) = dao.save(b.copy(back = today(), updated = now()))

    /** A clear-out finished: a bowl of milk for the hob. */
    suspend fun treat() = settings.update { it.copy(treats = it.treats + 1, clearOut = today()) }
}

class HobApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.LANG.isNotEmpty()) AppCompatDelegate.setApplicationLocales(androidx.core.os.LocaleListCompat.forLanguageTags(BuildConfig.LANG))
        container = AppContainer(this)
        container.scope.launch {
            if (BuildConfig.DEMO) DemoSeed.seedIfEmpty(container)
            container.seeding.value = false
        }
        Reminders.schedule(this)
    }
}

val Context.container: AppContainer get() = (applicationContext as HobApp).container

val Context.ru: Boolean get() = resources.configuration.locales[0].language == "ru"

/** A context in the app's own language, for text made outside an activity. */
fun Context.localized(): Context {
    val tags = AppCompatDelegate.getApplicationLocales()
    if (tags.isEmpty) return this
    val config = Configuration(resources.configuration)
    config.setLocale(tags[0])
    return createConfigurationContext(config)
}
