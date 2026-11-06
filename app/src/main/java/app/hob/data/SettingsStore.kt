package app.hob.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("settings")

data class Settings(
    val onboarded: Boolean = false,
    /** 0: evening (dark), 1: daylight, 2: as the system. */
    val theme: Int = 0,
    /** Bowls of milk left for the hob after clear-outs. */
    val treats: Int = 0,
    /** The epoch day of the last clear-out finished. */
    val clearOut: Long = 0,
    /** Morning reminders on. */
    val remind: Boolean = true,
    /** Things the reminders already told about (so a hint comes once): keys like «lent:12». */
    val told: Set<String> = emptySet(),
    /** Screens whose first-visit tip was already shown. */
    val tips: Set<String> = emptySet(),
    val tipsOff: Boolean = false,
    /** The owner's own sections, by name. */
    val cats: List<String> = emptyList(),
)

class SettingsStore(context: Context) {
    private val store = context.applicationContext.dataStore

    val settings: Flow<Settings> = store.data.map { it.read() }

    private fun Preferences.read() = Settings(
        onboarded = this[ONBOARDED] ?: false,
        theme = this[THEME] ?: 0,
        treats = this[TREATS] ?: 0,
        clearOut = this[CLEAR_OUT] ?: 0L,
        remind = this[REMIND] ?: true,
        told = this[TOLD]?.split(',')?.filter { it.isNotBlank() }?.toSet() ?: emptySet(),
        tips = this[TIPS]?.split(',')?.filter { it.isNotBlank() }?.toSet() ?: emptySet(),
        tipsOff = this[TIPS_OFF] ?: false,
        cats = this[CATS]?.split('\n')?.filter { it.isNotBlank() } ?: emptyList(),
    )

    suspend fun now(): Settings = settings.first()

    suspend fun update(change: (Settings) -> Settings) {
        store.edit {
            val s = change(it.read())
            it[ONBOARDED] = s.onboarded
            it[THEME] = s.theme
            it[TREATS] = s.treats
            it[CLEAR_OUT] = s.clearOut
            it[REMIND] = s.remind
            it[TOLD] = s.told.joinToString(",")
            it[TIPS] = s.tips.joinToString(",")
            it[TIPS_OFF] = s.tipsOff
            it[CATS] = s.cats.joinToString("\n")
        }
    }

    private companion object {
        val ONBOARDED = booleanPreferencesKey("onboarded")
        val THEME = intPreferencesKey("theme")
        val TREATS = intPreferencesKey("treats")
        val CLEAR_OUT = longPreferencesKey("clear_out")
        val REMIND = booleanPreferencesKey("remind")
        val TOLD = stringPreferencesKey("told")
        val TIPS = stringPreferencesKey("tips")
        val TIPS_OFF = booleanPreferencesKey("tips_off")
        val CATS = stringPreferencesKey("cats")
    }
}
