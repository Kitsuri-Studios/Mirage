package io.kitsuri.m1rage.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ModItem(
    modName: String,
    loadOrder: Int,
    isEnabled: Boolean,
    isDeleteMode: Boolean,
    isSelected: Boolean,
    isDragging: Boolean,
    onToggleEnabled: () -> Unit,
    onToggleSelection: () -> Unit,
    onDragStart: () -> Unit,
    onDragEnd: () -> Unit,
    onMove: (Int, Int) -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isDragging) 1.05f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
    )

    val elevation by animateDpAsState(
        targetValue = if (isDragging) 8.dp else 0.dp,
        animationSpec = tween(200)
    )

    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .shadow(elevation, RoundedCornerShape(12.dp))
            .pointerInput(Unit) {
                if (!isDeleteMode) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = { onDragStart() },
                        onDragEnd = { onDragEnd() },
                        onDrag = { _, _ -> }
                    )
                }
            },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDragging)
                MaterialTheme.colorScheme.surfaceVariant
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AnimatedContent(
                targetState = isDeleteMode,
                transitionSpec = {
                    fadeIn() + scaleIn() togetherWith fadeOut() + scaleOut()
                }
            ) { deleteMode ->
                if (deleteMode) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onToggleSelection() }
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.DragIndicator,
                        contentDescription = "Drag to reorder",
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = modName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                    ) {
                        Text(
                            text = "#$loadOrder",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 10.sp
                        )
                    }
                    Text(
                        text = "Load Order",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }
            }

            if (!isDeleteMode) {
                AnimatedContent(
                    targetState = isEnabled,
                    transitionSpec = {
                        fadeIn() + scaleIn() togetherWith fadeOut() + scaleOut()
                    }
                ) { enabled ->
                    Switch(
                        checked = enabled,
                        onCheckedChange = { onToggleEnabled() }
                    )
                }
            }
        }
    }
}