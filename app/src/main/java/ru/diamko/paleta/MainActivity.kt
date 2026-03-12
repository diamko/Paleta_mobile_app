package ru.diamko.paleta

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import ru.diamko.paleta.core.di.AppContainer
import ru.diamko.paleta.ui.navigation.PaletaApp
import ru.diamko.paleta.ui.theme.PaletaTheme

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val container = AppContainer(applicationContext)

        setContent {
            val scope = rememberCoroutineScope()
            val isDarkTheme by container.themeStore.isDarkThemeFlow.collectAsState(initial = null)
            val useDark = isDarkTheme ?: isSystemInDarkTheme()

            PaletaTheme(darkTheme = useDark) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    PaletaApp(
                        container = container,
                        currentTheme = isDarkTheme,
                        onChangeTheme = { newTheme ->
                            scope.launch { container.themeStore.saveIsDarkTheme(newTheme) }
                        },
                    )
                }
            }
        }
    }
}
