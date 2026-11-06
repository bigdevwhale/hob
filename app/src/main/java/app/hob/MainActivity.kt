package app.hob

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.hob.data.Settings
import app.hob.ui.HobRoot
import app.hob.ui.theme.HobTheme

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settings: Settings? by container.settings.settings.collectAsStateWithLifecycle(initialValue = null)
            val seeding by container.seeding.collectAsStateWithLifecycle()
            // Wait for settings so the welcome doesn't flash for someone already here.
            settings?.takeIf { !seeding }?.let { s ->
                HobTheme(s.theme) { HobRoot(s) }
            }
        }
    }
}
