package app.hob.ui.settings

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import app.hob.BuildConfig
import app.hob.R
import app.hob.container
import app.hob.data.Archive
import app.hob.data.Settings
import app.hob.domain.House
import app.hob.ui.Nav
import app.hob.ui.components.Btn
import app.hob.ui.components.Card
import app.hob.ui.components.Chip
import app.hob.ui.components.Chips
import app.hob.ui.components.Kind
import app.hob.ui.components.Sheet
import app.hob.ui.components.TopBar
import app.hob.ui.openUrl
import app.hob.ui.theme.C
import app.hob.ui.theme.T
import kotlinx.coroutines.launch
import java.time.LocalDate

/** The StillCraft bot: deep links file a bug or an idea for this app, or follow its releases. */
object Community {
    private const val BOT = "https://t.me/ArcaneWhisperBot?start="
    const val SLUG = "hob"
    const val BUG = "${BOT}bug_$SLUG"
    const val IDEA = "${BOT}wish_$SLUG"
    const val FOLLOW = "${BOT}f_$SLUG"
    const val CHAT = "https://t.me/+Q4gFEDk8O842OGVi"
    const val SUPPORT = "https://bigdevwhale.github.io/stillcraft/support/"
}

@Composable
private fun Section(title: String, hint: String? = null) {
    Text(title, style = T.Heading, modifier = Modifier.padding(top = 18.dp, bottom = 6.dp))
    if (hint != null) Text(hint, style = T.Small, modifier = Modifier.padding(bottom = 8.dp))
}

@Composable
private fun Link(label: Int, sub: Int, go: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = go).padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) { Text(stringResource(label), style = T.Strong); Text(stringResource(sub), style = T.Small) }
        Icon(Icons.Rounded.ChevronRight, null, tint = C.muted)
    }
}

@Composable
fun SettingsPage(h: House, nav: Nav) {
    val ctx = LocalContext.current
    val c = ctx.container
    val s = h.s
    val scope = rememberCoroutineScope()
    fun put(f: (Settings) -> Settings) = c.write { settings.update(f) }
    var restoring by remember { mutableStateOf<android.net.Uri?>(null) }
    val stamp = LocalDate.now().toString()

    fun done(ok: Boolean) = Toast.makeText(ctx, if (ok) R.string.saved_file else R.string.failed_file, Toast.LENGTH_LONG).show()
    val backup = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { u ->
        if (u != null) scope.launch { done(runCatching { ctx.contentResolver.openOutputStream(u)!!.use { Archive.export(c, it) } }.isSuccess) }
    }
    val csvHeader = ctx.getString(R.string.csv_header).split('|')
    val csv = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { u ->
        if (u != null) scope.launch { done(runCatching { ctx.contentResolver.openOutputStream(u)!!.use { Archive.csv(c, it, csvHeader) } }.isSuccess) }
    }
    val restore = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { u -> restoring = u }
    val notify = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok -> put { it.copy(remind = ok) } }

    Column(Modifier.verticalScroll(rememberScrollState()).navigationBarsPadding()) {
        TopBar(stringResource(R.string.settings), null, { nav.close() })
        Column(Modifier.padding(horizontal = 18.dp).padding(bottom = 24.dp)) {
            Card(Modifier.fillMaxWidth()) {
                Section(stringResource(R.string.set_theme))
                Chips {
                    for ((m, l) in listOf(0 to R.string.theme_evening, 1 to R.string.theme_day, 2 to R.string.theme_system)) Chip(stringResource(l), s.theme == m) { put { it.copy(theme = m) } }
                }
                Section(stringResource(R.string.set_remind), stringResource(R.string.set_remind_hint))
                Chips {
                    Chip(stringResource(R.string.on), s.remind) {
                        if (Build.VERSION.SDK_INT >= 33) notify.launch(Manifest.permission.POST_NOTIFICATIONS) else put { it.copy(remind = true) }
                    }
                    Chip(stringResource(R.string.off), !s.remind) { put { it.copy(remind = false) } }
                }
                Section(stringResource(R.string.set_lang))
                val cur = AppCompatDelegate.getApplicationLocales().toLanguageTags().substringBefore('-').substringBefore(',')
                Chips {
                    for ((tag, l) in listOf("" to R.string.lang_system, "ru" to R.string.lang_ru, "en" to R.string.lang_en))
                        Chip(stringResource(l), cur == tag) { AppCompatDelegate.setApplicationLocales(if (tag.isEmpty()) LocaleListCompat.getEmptyLocaleList() else LocaleListCompat.forLanguageTags(tag)) }
                }
                Btn(stringResource(R.string.tips_again), {
                    put { it.copy(tips = emptySet(), tipsOff = false) }
                    Toast.makeText(ctx, R.string.tips_again_done, Toast.LENGTH_SHORT).show()
                }, Modifier.padding(top = 18.dp), Kind.GHOST, small = true)
            }

            if (s.cats.isNotEmpty()) Card(Modifier.fillMaxWidth().padding(top = 12.dp)) {
                Section(stringResource(R.string.own_cats), stringResource(R.string.own_cats_hint))
                Chips { for (n in s.cats) Chip("$n  ✕", false) { put { it.copy(cats = it.cats - n) } } }
            }

            Card(Modifier.fillMaxWidth().padding(top = 12.dp)) {
                Section(stringResource(R.string.set_files), stringResource(R.string.set_files_hint))
                Link(R.string.backup, R.string.backup_sub) { backup.launch("hob-$stamp.zip") }
                Link(R.string.restore, R.string.restore_sub) { restore.launch(arrayOf("application/zip", "application/octet-stream")) }
                Link(R.string.export_csv, R.string.export_csv_sub) { csv.launch("hob-$stamp.csv") }
                Section(stringResource(R.string.privacy), stringResource(R.string.privacy_text))
            }

            Card(Modifier.fillMaxWidth().padding(top = 12.dp)) {
                Section(stringResource(R.string.community), stringResource(R.string.community_text))
                Link(R.string.community_bug, R.string.community_bug_sub) { openUrl(ctx, Community.BUG) }
                Link(R.string.community_idea, R.string.community_idea_sub) { openUrl(ctx, Community.IDEA) }
                Link(R.string.community_follow, R.string.community_follow_sub) { openUrl(ctx, Community.FOLLOW) }
                Link(R.string.community_chat, R.string.community_chat_sub) { openUrl(ctx, Community.CHAT) }
                Link(R.string.community_support, R.string.community_support_sub) { openUrl(ctx, Community.SUPPORT) }
                Section(stringResource(R.string.about))
                Text(stringResource(R.string.about_text, BuildConfig.VERSION_NAME), style = T.Small)
                Text(stringResource(R.string.font_credits), style = T.Small, modifier = Modifier.padding(top = 6.dp))
                Btn(stringResource(R.string.source), { openUrl(ctx, "https://github.com/bigdevwhale/hob") }, Modifier.padding(top = 12.dp), Kind.GHOST, small = true)
            }
        }
    }

    restoring?.let { u ->
        Sheet(stringResource(R.string.restore_t), { restoring = null }) {
            Text(stringResource(R.string.restore_b), style = T.Body)
            Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Btn(stringResource(R.string.cancel), { restoring = null }, Modifier.weight(1f), Kind.GHOST)
                Btn(stringResource(R.string.restore), {
                    restoring = null
                    scope.launch {
                        val ok = runCatching { ctx.contentResolver.openInputStream(u)!!.use { Archive.import(c, it) } }.getOrDefault(false)
                        Toast.makeText(ctx, if (ok) R.string.restored else R.string.failed_file, Toast.LENGTH_LONG).show()
                        if (ok) nav.stack.clear()
                    }
                }, Modifier.weight(1f), Kind.TERRA)
            }
        }
    }
}
