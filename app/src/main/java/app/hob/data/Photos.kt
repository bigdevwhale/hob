package app.hob.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.LruCache
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** One photo per thing, inside the app's own files: `<id>.jpg` and a square thumbnail `<id>_t.jpg`. */
class Photos(context: Context) {
    private val app = context.applicationContext
    val dir = File(app.filesDir, "photos").apply { mkdirs() }
    /** Photos from the system camera land here first. */
    val inbox = File(app.cacheDir, "camera").apply { mkdirs() }

    fun photo(id: Long) = File(dir, "$id.jpg")
    fun thumb(id: Long) = File(dir, "${id}_t.jpg")

    private val cache = object : LruCache<String, Bitmap>(24 shl 20) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount
    }

    fun cached(id: Long, big: Boolean, stamp: Long): Bitmap? = cache.get("$id:$big:$stamp")

    suspend fun load(id: Long, big: Boolean, stamp: Long): Bitmap? {
        val key = "$id:$big:$stamp"
        cache.get(key)?.let { return it }
        return withContext(Dispatchers.IO) {
            val f = if (big) photo(id) else thumb(id)
            if (!f.exists()) null else BitmapFactory.decodeFile(f.path)?.also { cache.put(key, it) }
        }
    }

    /** Reads a shot or picked picture upright, no larger than [px] on its long side. */
    suspend fun read(uri: Uri, px: Int = 1600): Bitmap? = withContext(Dispatchers.IO) {
        runCatching {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            app.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            if (bounds.outWidth <= 0) return@runCatching null
            var sample = 1
            while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= px) sample *= 2
            val bmp = app.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample }) } ?: return@runCatching null
            val rot = app.contentResolver.openInputStream(uri)?.use { ExifInterface(it).rotationDegrees } ?: 0
            val up = if (rot == 0) bmp else Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, Matrix().apply { postRotate(rot.toFloat()) }, true)
            up.fit(px)
        }.getOrNull()
    }

    suspend fun keep(id: Long, bmp: Bitmap) = withContext(Dispatchers.IO) {
        photo(id).outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 88, it) }
        thumb(id).outputStream().use { square(bmp, 240).compress(Bitmap.CompressFormat.JPEG, 85, it) }
        cache.snapshot().keys.filter { it.startsWith("$id:") }.forEach { cache.remove(it) }
    }

    fun delete(id: Long) {
        photo(id).delete(); thumb(id).delete()
        cache.snapshot().keys.filter { it.startsWith("$id:") }.forEach { cache.remove(it) }
    }

    companion object {
        fun Bitmap.fit(px: Int): Bitmap {
            val m = max(width, height)
            if (m <= px) return this
            val k = px.toFloat() / m
            return Bitmap.createScaledBitmap(this, (width * k).roundToInt().coerceAtLeast(1), (height * k).roundToInt().coerceAtLeast(1), true)
        }

        fun square(b: Bitmap, px: Int): Bitmap {
            val s = min(b.width, b.height)
            val c = Bitmap.createBitmap(b, (b.width - s) / 2, (b.height - s) / 2, s, s)
            return Bitmap.createScaledBitmap(c, px, px, true)
        }
    }
}
