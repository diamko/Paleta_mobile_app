/**
 * Модуль: FaqScreen.
 * Назначение: Экран FAQ: часто задаваемые вопросы и ответы.
 */
package ru.diamko.paleta.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.diamko.paleta.R
import ru.diamko.paleta.ui.components.PaletaCard
import ru.diamko.paleta.ui.components.PaletaGhostButton
import ru.diamko.paleta.ui.components.PaletaGradientBackground
import ru.diamko.paleta.ui.components.PaletaSectionTitle

@Composable
fun FaqScreen(
    onBack: () -> Unit,
) {
    PaletaGradientBackground(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                PaletaCard(modifier = Modifier.fillMaxWidth()) {
                    PaletaSectionTitle(
                        title = stringResource(id = R.string.faq_title),
                        subtitle = stringResource(id = R.string.faq_subtitle),
                    )

                    Text(stringResource(id = R.string.faq_q1_title), fontWeight = FontWeight.SemiBold)
                    Text(stringResource(id = R.string.faq_q1_body))

                    Text(stringResource(id = R.string.faq_q2_title), fontWeight = FontWeight.SemiBold)
                    Text(stringResource(id = R.string.faq_q2_body))

                    Text(stringResource(id = R.string.faq_q3_title), fontWeight = FontWeight.SemiBold)
                    Text(stringResource(id = R.string.faq_q3_body))

                    Text(stringResource(id = R.string.faq_q4_title), fontWeight = FontWeight.SemiBold)
                    Text(stringResource(id = R.string.faq_q4_body))

                    Text(stringResource(id = R.string.faq_q5_title), fontWeight = FontWeight.SemiBold)
                    Text(stringResource(id = R.string.faq_q5_body))

                    Text(stringResource(id = R.string.faq_q6_title), fontWeight = FontWeight.SemiBold)
                    Text(stringResource(id = R.string.faq_q6_body))

                    Text(stringResource(id = R.string.faq_q7_title), fontWeight = FontWeight.SemiBold)
                    Text(stringResource(id = R.string.faq_q7_body))

                    Text(stringResource(id = R.string.faq_q8_title), fontWeight = FontWeight.SemiBold)
                    Text(stringResource(id = R.string.faq_q8_body))

                    Text(stringResource(id = R.string.faq_q9_title), fontWeight = FontWeight.SemiBold)
                    Text(stringResource(id = R.string.faq_q9_body))

                    Text(stringResource(id = R.string.faq_q10_title), fontWeight = FontWeight.SemiBold)
                    Text(stringResource(id = R.string.faq_q10_body))

                    Text(stringResource(id = R.string.faq_q11_title), fontWeight = FontWeight.SemiBold)
                    Text(stringResource(id = R.string.faq_q11_body))

                    PaletaGhostButton(
                        modifier = Modifier.fillMaxWidth(),
                        text = stringResource(id = R.string.back),
                        onClick = onBack,
                    )
                }
            }
        }
    }
}
