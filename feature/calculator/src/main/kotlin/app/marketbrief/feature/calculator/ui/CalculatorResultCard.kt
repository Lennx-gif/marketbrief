package app.marketbrief.feature.calculator.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.marketbrief.core.risk.SizingResult
import app.marketbrief.core.risk.SizingWarning
import app.marketbrief.core.ui.components.AppleGroupedSection
import app.marketbrief.core.ui.components.AppleHairlineDivider
import app.marketbrief.core.ui.theme.MarketBriefTheme

@Composable
fun CalculatorResultCard(result: SizingResult) {
    AppleGroupedSection {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SIZING RESULT",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${result.lots.toPlainString()} Lots",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            AppleHairlineDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ResultItem("Contract Units", result.units.toPlainString())
                ResultItem("Target Risk", "${result.targetRisk.toPlainString()} (${result.actualRiskPercent.toPlainString()}%)")
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ResultItem("Stop Loss Price", result.stopPrice.toPlainString())
                ResultItem("Stop Distance", "${result.stopPips.toPlainString()} pips")
            }

            result.takeProfitPrice?.let { tpPrice ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    ResultItem("Take Profit Price", tpPrice.toPlainString())
                    ResultItem("TP Distance", "${result.takeProfitPips?.toPlainString() ?: "N/A"} pips")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ResultItem("Pip Value / Lot", result.pipValuePerLot.toPlainString())
                ResultItem("Reward : Risk", result.rewardRisk?.toPlainString()?.let { "1 : $it" } ?: "N/A")
            }

            result.potentialProfit?.let { profit ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    ResultItem("Potential Profit", profit.toPlainString())
                    ResultItem("Margin Required", result.marginRequired?.toPlainString() ?: "N/A")
                }
            }

            if (result.warnings.isNotEmpty()) {
                AppleHairlineDivider()
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    result.warnings.forEach { warning ->
                        WarningRow(warning)
                    }
                }
            }
        }
    }
}

@Composable
fun ResultItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.Start) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

@Composable
fun WarningRow(warning: SizingWarning) {
    val message = when (warning) {
        is SizingWarning.HighRisk -> "High risk: ${warning.riskPercent.toPlainString()}% (threshold: ${warning.threshold.toPlainString()}%)"
        is SizingWarning.CappedAtMaxLot -> "Capped at max lot: ${warning.maxLot.toPlainString()}"
    }
    val color = when (warning) {
        is SizingWarning.HighRisk -> MarketBriefTheme.colors.bearish
        is SizingWarning.CappedAtMaxLot -> MaterialTheme.colorScheme.tertiary
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = "Warning",
            tint = color,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = message,
            fontSize = 12.sp,
            color = color,
            fontWeight = FontWeight.Medium
        )
    }
}