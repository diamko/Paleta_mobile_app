package ru.diamko.paleta

import android.app.Application
import ru.diamko.paleta.core.di.AppContainer

class PaletaApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
