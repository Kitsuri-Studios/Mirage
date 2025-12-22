package io.kitsuri.m1rage.ui.components

import android.graphics.drawable.GradientDrawable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import io.kitsuri.m1rage.ui.theme.M1rageTheme


@Composable
fun AppItem(
    modifier: Modifier = Modifier,
    icon: ImageBitmap,
    label: String,
    packageName: String,
    checked: Boolean? = null,
    rightIcon: (@Composable () -> Unit)? = null,
    additionalContent: (@Composable ColumnScope.() -> Unit)? = null,
) {
    if (checked != null && rightIcon != null)
        throw IllegalArgumentException("`checked` and `rightIcon` should not be both set")
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                bitmap = icon,
                contentDescription = label,
                tint = Color.Unspecified,
                modifier = Modifier.size(40.dp)
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = packageName,
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                additionalContent?.invoke(this)
            }
            if (checked != null) {
                Checkbox(
                    checked = checked,
                    onCheckedChange = null,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            if (rightIcon != null) {
                rightIcon()
            }
        }
    }
}
