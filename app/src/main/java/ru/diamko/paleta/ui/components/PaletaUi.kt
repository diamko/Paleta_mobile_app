/**
 * Модуль: PaletaUi.
 * Назначение: Переиспользуемые UI-компоненты: кнопки, карточки, фон, баннеры.
 */
package ru.diamko.paleta.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import ru.diamko.paleta.ui.theme.BrandBlue
import ru.diamko.paleta.ui.theme.BrandSuccess
import ru.diamko.paleta.ui.theme.BrandViolet

@Composable
fun PaletaGradientBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.background,
                        BrandBlue.copy(alpha = 0.1f),
                        BrandViolet.copy(alpha = 0.12f),
                    ),
                ),
            ),
        content = content,
    )
}

@Composable
fun PaletaCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
    }
}

@Composable
fun PaletaSectionTitle(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        if (!subtitle.isNullOrBlank()) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun PaletaPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
) {
    val shape = RoundedCornerShape(18.dp)
    val isClickable = enabled && !isLoading
    val disabledColor = MaterialTheme.colorScheme.surfaceVariant
    Box(
        modifier = modifier
            .shadow(
                elevation = if (isClickable) 10.dp else 0.dp,
                shape = shape,
                clip = false,
            )
            .clip(shape)
            .background(
                if (isClickable) {
                    Brush.horizontalGradient(listOf(BrandViolet, BrandBlue))
                } else {
                    Brush.horizontalGradient(listOf(disabledColor, disabledColor))
                },
            )
            .clickable(
                enabled = isClickable,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = 18.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.padding(2.dp),
                strokeWidth = 2.dp,
                color = Color.White,
            )
        } else {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
fun PaletaGhostButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isDanger: Boolean = false,
) {
    val strokeColor = when {
        isDanger -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.primary
    }
    val background = strokeColor.copy(alpha = 0.08f)
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(background)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = if (enabled) strokeColor else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun PaletaMessageBanner(
    message: String,
    isError: Boolean,
    modifier: Modifier = Modifier,
) {
    val tone = when {
        isError -> MaterialTheme.colorScheme.error
        else -> BrandSuccess
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(tone.copy(alpha = 0.85f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White,
        )
    }
}

@Composable
fun BoxScope.PaletaTopBannerHost(
    error: String?,
    info: String?,
    modifier: Modifier = Modifier,
    topPadding: Dp = 0.dp,
    onErrorDismissed: (() -> Unit)? = null,
    onInfoDismissed: (() -> Unit)? = null,
) {
    var visibleError by remember { mutableStateOf<String?>(null) }
    var visibleInfo by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(error) {
        visibleError = error
        if (!error.isNullOrBlank()) {
            delay(2500)
            visibleError = null
            onErrorDismissed?.invoke()
        }
    }

    LaunchedEffect(info) {
        visibleInfo = info
        if (!info.isNullOrBlank()) {
            delay(2500)
            visibleInfo = null
            onInfoDismissed?.invoke()
        }
    }

    if (visibleError.isNullOrBlank() && visibleInfo.isNullOrBlank()) return
    Column(
        modifier = modifier
            .align(Alignment.TopCenter)
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = topPadding + 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (!visibleError.isNullOrBlank()) {
            PaletaMessageBanner(message = visibleError!!, isError = true)
        }
        if (!visibleInfo.isNullOrBlank()) {
            PaletaMessageBanner(message = visibleInfo!!, isError = false)
        }
    }
}

@Composable
fun paletaTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
    focusedLabelColor = MaterialTheme.colorScheme.primary,
    cursorColor = MaterialTheme.colorScheme.primary,
    focusedContainerColor = MaterialTheme.colorScheme.surface,
    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
)
