package ru.diamko.paleta.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ru.diamko.paleta.R
import ru.diamko.paleta.ui.theme.BrandBlue
import ru.diamko.paleta.ui.theme.BrandViolet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorCountDropdown(
    label: String,
    selectedCount: Int?,
    onSelected: (Int?) -> Unit,
    modifier: Modifier = Modifier,
    allowAny: Boolean = false,
    enabled: Boolean = true,
) {
    val options = remember { (3..15).toList() }
    var expanded by remember { mutableStateOf(false) }
    val displayValue = when {
        selectedCount != null -> selectedCount.toString()
        allowAny -> stringResource(id = R.string.color_count_any)
        else -> options.first().toString()
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { if (enabled) expanded = !expanded },
        modifier = modifier,
    ) {
        OutlinedTextField(
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = enabled)
                .fillMaxWidth()
                .widthIn(min = 220.dp, max = 320.dp),
            value = displayValue,
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            label = { Text(text = label) },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = BrandViolet,
                unfocusedBorderColor = BrandBlue.copy(alpha = 0.7f),
                focusedLabelColor = BrandViolet,
                unfocusedLabelColor = BrandBlue.copy(alpha = 0.9f),
                cursorColor = BrandViolet,
                focusedContainerColor = BrandBlue.copy(alpha = 0.08f),
                unfocusedContainerColor = BrandBlue.copy(alpha = 0.06f),
            ),
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.widthIn(min = 220.dp, max = 320.dp),
        ) {
            if (allowAny) {
                DropdownMenuItem(
                    text = { Text(text = stringResource(id = R.string.color_count_any)) },
                    contentPadding = PaddingValues(vertical = 6.dp, horizontal = 12.dp),
                    onClick = {
                        expanded = false
                        onSelected(null)
                    },
                )
            }
            options.forEach { count ->
                DropdownMenuItem(
                    text = { Text(text = count.toString()) },
                    contentPadding = PaddingValues(vertical = 6.dp, horizontal = 12.dp),
                    onClick = {
                        expanded = false
                        onSelected(count)
                    },
                )
            }
        }
    }
}
