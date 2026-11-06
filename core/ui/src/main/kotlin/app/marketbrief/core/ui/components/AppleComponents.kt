package app.marketbrief.core.ui.components

import android.content.res.Configuration
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.marketbrief.core.ui.theme.MarketBriefTheme

/**
 * Apple 0.5dp hairline divider.
 */
@Composable
fun AppleHairlineDivider(
    modifier: Modifier = Modifier,
    color: Color = MarketBriefTheme.colors.hairline,
    thickness: Dp = 0.5.dp
) {
    HorizontalDivider(
        modifier = modifier,
        thickness = thickness,
        color = color
    )
}

/**
 * Apple unified grouped surface for grouped list items.
 */
@Composable
fun AppleGroupedSection(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 14.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(cornerRadius))
            .border(0.5.dp, MarketBriefTheme.colors.hairline, RoundedCornerShape(cornerRadius)),
        color = MarketBriefTheme.colors.cardSurface,
        shape = RoundedCornerShape(cornerRadius)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            content = content
        )
    }
}

/**
 * Apple-style segmented pill selector.
 */
@Composable
fun <T> AppleSegmentedControl(
    items: List<T>,
    selectedItem: T,
    onItemSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    equalWeights: Boolean = false,
    labelProvider: (T) -> String = { it.toString() }
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(100.dp))
            .border(0.5.dp, MarketBriefTheme.colors.hairline, RoundedCornerShape(100.dp)),
        color = MarketBriefTheme.colors.subtlePill,
        shape = RoundedCornerShape(100.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(3.dp)
                .then(if (equalWeights) Modifier.fillMaxWidth() else Modifier),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                val isSelected = item == selectedItem
                val backgroundColor by animateColorAsState(
                    targetValue = if (isSelected) MarketBriefTheme.colors.cardSurface else Color.Transparent,
                    animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
                    label = "segmentBackground"
                )
                val textColor by animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    animationSpec = tween(durationMillis = 180),
                    label = "segmentTextColor"
                )

                Box(
                    modifier = Modifier
                        .then(if (equalWeights) Modifier.weight(1f) else Modifier)
                        .defaultMinSize(minHeight = 40.dp)
                        .clip(RoundedCornerShape(100.dp))
                        .background(backgroundColor)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onItemSelected(item) }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = labelProvider(item),
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                        color = textColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/**
 * Labeled row with segmented control that stacks responsively when constrained.
 */
@Composable
fun <T> LabeledSegmentedRow(
    label: String,
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    forceStacked: Boolean = false,
    labelProvider: (T) -> String = { it.toString() }
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val fontScale = LocalDensity.current.fontScale
        val estimatedControlWidth = (options.size * 68).dp
        val shouldStack = forceStacked || fontScale > 1.15f || (estimatedControlWidth > maxWidth * 0.58f) || maxWidth < 360.dp

        if (shouldStack) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                AppleSegmentedControl(
                    items = options,
                    selectedItem = selected,
                    onItemSelected = onSelect,
                    modifier = Modifier.fillMaxWidth(),
                    equalWeights = true,
                    labelProvider = labelProvider
                )
                if (!supportingText.isNullOrBlank()) {
                    Text(
                        text = supportingText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f, fill = false),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    AppleSegmentedControl(
                        items = options,
                        selectedItem = selected,
                        onItemSelected = onSelect,
                        equalWeights = false,
                        labelProvider = labelProvider
                    )
                }
                if (!supportingText.isNullOrBlank()) {
                    Text(
                        text = supportingText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Standard settings row with consistent 16dp horizontal and 12dp vertical padding.
 */
@Composable
fun SettingRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leadingIcon: ImageVector? = null,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    trailingContent: (@Composable () -> Unit)? = null
) {
    val clickableModifier = if (onClick != null) {
        Modifier.clickable(onClick = onClick)
    } else {
        Modifier
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(clickableModifier)
            .padding(contentPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f, fill = false),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Column(
                modifier = Modifier.weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        if (trailingContent != null) {
            Box(
                modifier = Modifier.padding(start = 12.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                trailingContent()
            }
        }
    }
}

/**
 * Single-line, horizontally scrollable filter chip row with 36dp chip height,
 * 8dp spacing, and 16dp edge padding.
 */
@Composable
fun <T> FilterChipRow(
    items: List<T>,
    selectedItem: T?,
    onItemSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    labelProvider: (T) -> String = { it.toString() },
    leadingIconProvider: ((T) -> ImageVector?)? = null,
    trailingAction: (@Composable () -> Unit)? = null
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(items) { item ->
            val isSelected = item == selectedItem
            ApplePillChip(
                text = labelProvider(item),
                isSelected = isSelected,
                onClick = { onItemSelected(item) },
                leadingIcon = leadingIconProvider?.invoke(item),
                modifier = Modifier.height(36.dp)
            )
        }
        if (trailingAction != null) {
            item {
                trailingAction()
            }
        }
    }
}

/**
 * Apple-style minimalist pill chip.
 */
@Composable
fun ApplePillChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null
) {
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MarketBriefTheme.colors.subtlePill
    }
    val contentColor = if (isSelected) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val borderColor = Color.Transparent

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(100.dp))
            .border(0.5.dp, borderColor, RoundedCornerShape(100.dp))
            .clickable(onClick = onClick),
        color = backgroundColor,
        shape = RoundedCornerShape(100.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    modifier = Modifier.height(13.dp),
                    tint = contentColor
                )
            }
            Text(
                text = text,
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = contentColor,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

/**
 * Apple Non-blocking status/warning banner.
 */
@Composable
fun AppleBanner(
    message: String,
    modifier: Modifier = Modifier,
    isWarning: Boolean = false,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    val containerColor = if (isWarning) {
        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.85f)
    } else {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f)
    }
    val contentColor = if (isWarning) {
        MaterialTheme.colorScheme.onErrorContainer
    } else {
        MaterialTheme.colorScheme.onPrimaryContainer
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(0.5.dp, contentColor.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
        color = containerColor,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = if (isWarning) Icons.Default.Warning else Icons.Default.Info,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.height(18.dp)
            )
            Text(
                text = message,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = contentColor,
                modifier = Modifier.weight(1f)
            )
            if (actionLabel != null && onAction != null) {
                Text(
                    text = actionLabel,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = contentColor,
                    modifier = Modifier
                        .clickable(onClick = onAction)
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Preview(name = "320dp - Scale 1.0 - Light", widthDp = 320, fontScale = 1.0f, showBackground = true)
@Preview(name = "320dp - Scale 1.3 - Light", widthDp = 320, fontScale = 1.3f, showBackground = true)
@Preview(name = "360dp - Scale 1.0 - Light", widthDp = 360, fontScale = 1.0f, showBackground = true)
@Preview(name = "360dp - Scale 1.3 - Light", widthDp = 360, fontScale = 1.3f, showBackground = true)
@Preview(name = "411dp - Scale 1.0 - Light", widthDp = 411, fontScale = 1.0f, showBackground = true)
@Preview(name = "411dp - Scale 1.3 - Light", widthDp = 411, fontScale = 1.3f, showBackground = true)
@Preview(name = "360dp - Scale 1.0 - Dark", widthDp = 360, fontScale = 1.0f, uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Preview(name = "411dp - Scale 1.3 - Dark", widthDp = 411, fontScale = 1.3f, uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
annotation class ScreenMatrixPreview

@ScreenMatrixPreview
@Composable
private fun LabeledSegmentedRowPreview() {
    MarketBriefTheme {
        Surface {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                LabeledSegmentedRow(
                    label = "Risk Sizing Mode",
                    options = listOf("Percent", "Cash"),
                    selected = "Percent",
                    onSelect = {}
                )
                LabeledSegmentedRow(
                    label = "Theme",
                    options = listOf("System", "Light", "Dark"),
                    selected = "System",
                    onSelect = {},
                    supportingText = "Adjusts to system dark mode preferences."
                )
            }
        }
    }
}

@ScreenMatrixPreview
@Composable
private fun SettingRowPreview() {
    MarketBriefTheme {
        Surface {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                SettingRow(
                    title = "Notifications",
                    subtitle = "High-impact economic events",
                    trailingContent = {
                        Text(text = "Enabled", color = MaterialTheme.colorScheme.primary)
                    }
                )
                AppleHairlineDivider()
                SettingRow(
                    title = "Cache Size",
                    subtitle = "540 articles saved on this device",
                    trailingContent = {
                        Text(text = "12 MB", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                )
            }
        }
    }
}

@ScreenMatrixPreview
@Composable
private fun FilterChipRowPreview() {
    MarketBriefTheme {
        Surface {
            FilterChipRow(
                items = listOf("Saved", "All", "Forex", "Crypto", "Commodities", "Indices"),
                selectedItem = "Saved",
                onItemSelected = {},
                modifier = Modifier.padding(vertical = 12.dp)
            )
        }
    }
}
