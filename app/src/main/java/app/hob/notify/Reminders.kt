package app.hob.notify

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import app.hob.MainActivity
import app.hob.R
import app.hob.container
import app.hob.domain.Hint
import app.hob.domain.House
import app.hob.localized
import app.hob.ui.Words
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * Once a day, late in the morning, the hob looks round the house and says one thing he hasn't said before:
 * something fits a child now, a lent thing is overdue, the tree decorations will be needed soon.
 */
object Reminders {
    private const val CHANNEL = "hob"

    fun schedule(ctx: Context) {
        val am = ctx.getSystemService(AlarmManager::class.java) ?: return
        var at = LocalDateTime.of(LocalDate.now(), LocalTime.of(10, 30))
        if (at.isBefore(LocalDateTime.now())) at = at.plusDays(1)
        val ms = at.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        am.setInexactRepeating(AlarmManager.RTC, ms, AlarmManager.INTERVAL_DAY, pending(ctx))
    }

    private fun pending(ctx: Context) = PendingIntent.getBroadcast(ctx, 1, Intent(ctx, Look::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

    /** A key per piece of news, so each is told once. */
    fun key(h: Hint, year: Int): String = when (h) {
        is Hint.Season -> "season:${h.thing.season}:$year"
        is Hint.Fits -> "fits:${h.kid.id}:${h.things.map { it.id }.sorted().joinToString("-")}"
        is Hint.Lent -> "lent:${h.thing.id}:${h.thing.lentRemind}"
        is Hint.Soon -> "soon:${h.thing.id}:${h.kid.id}"
        is Hint.Return -> "return:${h.b.id}"
        is Hint.Measure -> "measure:${h.kid.id}:${LocalDate.now().year}-${LocalDate.now().monthValue / 4}"
    }

    suspend fun look(app: Context) {
        val c = app.container
        val s = c.settings.now()
        if (!s.remind || !s.onboarded) return
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(app, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        val d = c.dao
        val h = House(s, d.placesNow(), d.roomsNow(), d.spotsNow(), d.boxesNow(), d.thingsNow(), d.kidsNow(), d.measuresNow(), d.eventsNow(), d.borrowedNow())
        val year = LocalDate.now().year
        // Soon-to-fit things wait until they actually fit, unless it's the only news.
        val fresh = h.hints().filter { key(it, year) !in s.told }.sortedBy { if (it is Hint.Soon || it is Hint.Measure) 1 else 0 }
        val hint = fresh.firstOrNull() ?: return
        val ctx = app.localized()
        val (emoji, title, sub) = Words.note(ctx, h, hint)
        val nm = app.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CHANNEL, ctx.getString(R.string.channel), NotificationManager.IMPORTANCE_DEFAULT))
        val open = PendingIntent.getActivity(app, 0, Intent(app, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), PendingIntent.FLAG_IMMUTABLE)
        val n = NotificationCompat.Builder(app, CHANNEL).setSmallIcon(R.drawable.ic_notify).setContentTitle("$emoji $title").setContentText(sub)
            .setStyle(NotificationCompat.BigTextStyle().bigText(sub)).setContentIntent(open).setAutoCancel(true).build()
        runCatching { NotificationManagerCompat.from(app).notify(7, n) }
        c.settings.update { st -> st.copy(told = (st.told + fresh.filter { it is Hint.Season || it === hint }.map { key(it, year) }).toList().takeLast(200).toSet()) }
    }

    class Look : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            val done = goAsync()
            CoroutineScope(Dispatchers.Main).launch {
                try { look(context.applicationContext) } finally { done.finish() }
            }
        }
    }

    /** Alarms don't survive a restart. */
    class Boot : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            if (intent?.action == Intent.ACTION_BOOT_COMPLETED || intent?.action == Intent.ACTION_MY_PACKAGE_REPLACED) schedule(context)
        }
    }
}
