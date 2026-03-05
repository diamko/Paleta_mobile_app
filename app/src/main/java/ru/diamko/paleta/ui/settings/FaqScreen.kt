package ru.diamko.paleta.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.diamko.paleta.ui.components.PaletaCard
import ru.diamko.paleta.ui.components.PaletaGhostButton
import ru.diamko.paleta.ui.components.PaletaGradientBackground
import ru.diamko.paleta.ui.components.PaletaSectionTitle

@Composable
fun FaqScreen(
    onBack: () -> Unit,
) {
    PaletaGradientBackground(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PaletaCard(modifier = Modifier.fillMaxWidth()) {
                PaletaSectionTitle(
                    title = "FAQ",
                    subtitle = "Краткие ответы по работе с Paleta",
                )

                Text("1. Как создать палитру?", fontWeight = FontWeight.SemiBold)
                Text("Откройте Генератор, выберите случайную палитру или изображение, затем нажмите «Сохранить».")

                Text("2. Как экспортировать палитру?", fontWeight = FontWeight.SemiBold)
                Text("На экране «Мои палитры» выберите «Экспорт» и нужный формат.")

                Text("3. Как восстановить пароль?", fontWeight = FontWeight.SemiBold)
                Text("На экране входа нажмите «Забыли пароль», получите код на email и задайте новый пароль.")

                Text("4. Как изменить профиль?", fontWeight = FontWeight.SemiBold)
                Text("Откройте Настройки -> Профиль, внесите изменения и подтвердите текущим паролем.")

                PaletaGhostButton(
                    modifier = Modifier.fillMaxWidth(),
                    text = "Назад",
                    onClick = onBack,
                )
            }
        }
    }
}
