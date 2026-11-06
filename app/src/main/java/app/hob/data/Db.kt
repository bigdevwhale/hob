package app.hob.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room as RoomDb
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import java.util.UUID

/*
 * The house as the hob knows it: places (the flat, the dacha) hold rooms, rooms hold spots (the top shelf, under
 * the bed), spots hold numbered boxes and loose things. Every row carries a [uid] and an [updated] stamp so two
 * phones can later be merged row by row.
 */

fun uid(): String = UUID.randomUUID().toString()
fun now(): Long = System.currentTimeMillis()

@Entity
data class Place(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val sort: Int = 0,
    val uid: String = uid(),
    val updated: Long = now(),
)

@Entity
data class Room(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val place: Long,
    val name: String,
    val emoji: String,
    val sort: Int = 0,
    val uid: String = uid(),
    val updated: Long = now(),
)

@Entity
data class Spot(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val room: Long,
    val name: String,
    val sort: Int = 0,
    val uid: String = uid(),
    val updated: Long = now(),
)

@Entity
data class Box(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** The number written on it (and in its QR label): unique in the house. */
    val num: Int,
    val label: String,
    val spot: Long,
    val uid: String = uid(),
    val updated: Long = now(),
)

/** Where a thing is now. */
object Status {
    const val HOME = 0
    /** Taken out to be used: a child wears it now. */
    const val OUT = 1
    /** Given, sold or thrown away. */
    const val GONE = 2
}

/** How a thing left the house. */
object Gone {
    const val GIVEN = 1
    const val SOLD = 2
    const val CHARITY = 3
    const val BINNED = 4
}

/** What a size is measured against. */
object Size {
    const val NONE = 0
    /** Clothes by height in cm (a «116» fits 111–116). */
    const val HEIGHT = 1
    /** Shoes and skates by shoe size. */
    const val FOOT = 2
}

@Entity
data class Thing(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val emoji: String,
    /** A [Cat] key. */
    val cat: String,
    val spot: Long,
    /** 0 = lies loose on the spot. */
    val box: Long = 0,
    val note: String = "",
    val photo: Boolean = false,
    /** Epoch days. */
    val added: Long,
    val touched: Long,
    val sizeKind: Int = Size.NONE,
    val sizeMin: Float = 0f,
    val sizeMax: Float = 0f,
    val sizeLabel: String = "",
    /** Someone outside the house has it now: their name, since when, when to remind. */
    val lentTo: String = "",
    val lentDay: Long = 0,
    val lentRemind: Long = 0,
    /** The month (1–12) it is needed every year, e.g. 12 for the tree decorations; 0 = any time. */
    val season: Int = 0,
    val state: Int = Status.HOME,
    val goneHow: Int = 0,
    val goneDay: Long = 0,
    /** What it was sold for, in whole currency units. */
    val sold: Int = 0,
    /** The day the owner looked at it in a clear-out and kept it. */
    val kept: Long = 0,
    /** Who has it out ([Status.OUT]): a kid id. */
    val outKid: Long = 0,
    val uid: String = uid(),
    val updated: Long = now(),
) {
    val home get() = state == Status.HOME
    val lent get() = lentTo.isNotBlank()
    val sized get() = sizeKind != Size.NONE
}

@Entity
data class Kid(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** Epoch day of birth. */
    val born: Long,
    val color: Int = 0,
    val uid: String = uid(),
    val updated: Long = now(),
)

@Entity
data class Measure(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val kid: Long,
    val day: Long,
    /** cm; 0 = not measured this time. */
    val height: Float,
    /** Shoe size (EU); 0 = not measured this time. */
    val foot: Float,
    val uid: String = uid(),
)

/** A line in a thing's history. */
object Ev {
    const val ADDED = 0
    const val MOVED = 1
    const val LENT = 2
    const val BACK = 3
    const val WORN = 4
    const val RETURNED = 5
    const val GONE = 6
    /** Worn before the app knew it (typed in by hand). */
    const val PAST = 7
}

@Entity
data class Event(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val thing: Long,
    val day: Long,
    val kind: Int,
    /** A name (who took it, who wore it) or a place, depending on [kind]. */
    val who: String = "",
    val uid: String = uid(),
)

/** Something of someone else's that lives here for now. */
@Entity
data class Borrowed(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val emoji: String,
    val from: String,
    val day: Long,
    /** Day it went back; 0 = still here. */
    val back: Long = 0,
    val uid: String = uid(),
    val updated: Long = now(),
)

@Dao
interface HobDao {
    @Query("SELECT * FROM Place ORDER BY sort, id") fun places(): Flow<List<Place>>
    @Query("SELECT * FROM Room ORDER BY sort, id") fun rooms(): Flow<List<Room>>
    @Query("SELECT * FROM Spot ORDER BY sort, id") fun spots(): Flow<List<Spot>>
    @Query("SELECT * FROM Box ORDER BY num") fun boxes(): Flow<List<Box>>
    @Query("SELECT * FROM Thing ORDER BY name COLLATE NOCASE") fun things(): Flow<List<Thing>>
    @Query("SELECT * FROM Kid ORDER BY born") fun kids(): Flow<List<Kid>>
    @Query("SELECT * FROM Measure ORDER BY day") fun measures(): Flow<List<Measure>>
    @Query("SELECT * FROM Event ORDER BY day, id") fun events(): Flow<List<Event>>
    @Query("SELECT * FROM Borrowed ORDER BY day DESC") fun borrowed(): Flow<List<Borrowed>>

    @Query("SELECT * FROM Place ORDER BY sort, id") suspend fun placesNow(): List<Place>
    @Query("SELECT * FROM Room ORDER BY sort, id") suspend fun roomsNow(): List<Room>
    @Query("SELECT * FROM Spot ORDER BY sort, id") suspend fun spotsNow(): List<Spot>
    @Query("SELECT * FROM Box ORDER BY num") suspend fun boxesNow(): List<Box>
    @Query("SELECT * FROM Thing") suspend fun thingsNow(): List<Thing>
    @Query("SELECT * FROM Kid ORDER BY born") suspend fun kidsNow(): List<Kid>
    @Query("SELECT * FROM Measure ORDER BY day") suspend fun measuresNow(): List<Measure>
    @Query("SELECT * FROM Event ORDER BY day, id") suspend fun eventsNow(): List<Event>
    @Query("SELECT * FROM Borrowed") suspend fun borrowedNow(): List<Borrowed>
    @Query("SELECT * FROM Thing WHERE id = :id") suspend fun thing(id: Long): Thing?
    @Query("SELECT COALESCE(MAX(num), 0) FROM Box") suspend fun lastBox(): Int

    @Insert suspend fun add(p: Place): Long
    @Insert suspend fun add(r: Room): Long
    @Insert suspend fun add(s: Spot): Long
    @Insert suspend fun add(b: Box): Long
    @Insert suspend fun add(t: Thing): Long
    @Insert suspend fun add(k: Kid): Long
    @Insert suspend fun add(m: Measure): Long
    @Insert suspend fun add(e: Event): Long
    @Insert suspend fun add(b: Borrowed): Long

    @Update suspend fun save(p: Place)
    @Update suspend fun save(r: Room)
    @Update suspend fun save(s: Spot)
    @Update suspend fun save(b: Box)
    @Update suspend fun save(t: Thing)
    @Update suspend fun save(k: Kid)
    @Update suspend fun save(b: Borrowed)

    @Delete suspend fun drop(p: Place)
    @Delete suspend fun drop(r: Room)
    @Delete suspend fun drop(s: Spot)
    @Delete suspend fun drop(b: Box)
    @Delete suspend fun drop(t: Thing)
    @Delete suspend fun drop(k: Kid)
    @Delete suspend fun drop(m: Measure)
    @Delete suspend fun drop(b: Borrowed)
    @Query("DELETE FROM Event WHERE thing = :thing") suspend fun dropEvents(thing: Long)
    @Query("DELETE FROM Measure WHERE kid = :kid") suspend fun dropMeasures(kid: Long)

    @Query("DELETE FROM Place") suspend fun clearPlaces()
    @Query("DELETE FROM Room") suspend fun clearRooms()
    @Query("DELETE FROM Spot") suspend fun clearSpots()
    @Query("DELETE FROM Box") suspend fun clearBoxes()
    @Query("DELETE FROM Thing") suspend fun clearThings()
    @Query("DELETE FROM Kid") suspend fun clearKids()
    @Query("DELETE FROM Measure") suspend fun clearMeasures()
    @Query("DELETE FROM Event") suspend fun clearEvents()
    @Query("DELETE FROM Borrowed") suspend fun clearBorrowed()
}

@Database(entities = [Place::class, Room::class, Spot::class, Box::class, Thing::class, Kid::class, Measure::class, Event::class, Borrowed::class], version = 1, exportSchema = true)
abstract class HobDb : RoomDatabase() {
    abstract fun dao(): HobDao

    companion object {
        fun create(context: Context): HobDb = RoomDb.databaseBuilder(context, HobDb::class.java, "hob.db").build()
    }
}
