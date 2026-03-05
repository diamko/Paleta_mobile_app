package ru.diamko.paleta

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import ru.diamko.paleta.core.di.AppContainer
import ru.diamko.paleta.ui.navigation.PaletaApp
import ru.diamko.paleta.ui.theme.PaletaTheme

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val container = AppContainer(applicationContext)

        setContent {
            PaletaTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    PaletaApp(container = container)
                }
            }
        }
    }
}
