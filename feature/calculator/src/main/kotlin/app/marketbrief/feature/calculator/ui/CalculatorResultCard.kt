package app.marketbrief.feature.calculator.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.marketbrief.core.risk.SizingResult
import app.marketbrief.core.risk.SizingWarning

@Composable
fun CalculatorResultCard(result: SizingResult) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Position Size Result",
                fontSize = 18.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ResultItem("Lots", result.lots.toPlainString())
                ResultItem("Units", result.units.toPlainString())
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ResultItem("Target Risk", "${result.targetRisk.toPlainString()} ${result.targetRisk.scale()}")
                ResultItem("Actual Risk", "${result.actualRisk.toPlainString()} (${result.actualRiskPercent.toPlainString()}%)")
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ResultItem("Stop Price", result.stopPrice.toPlainString())
                ResultItem("Stop Pips", result.stopPips.toPlainString())
            }

            result.takeProfitPrice?.let { tpPrice ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    ResultItem("TP Price", tpPrice.toPlainString())
                    ResultItem("TP Pips", result.takeProfitPips?.toPlainString() ?: "N/A")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ResultItem("Pip Value/Lot", result.pipValuePerLot.toPlainString())
                ResultItem("R:R Ratio", result.rewardRisk?.toPlainString() ?: "N/A")
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
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
            fontSize = 16.sp,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
            color = MaterialTheme.colorScheme.onPrimaryContainer
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
        is SizingWarning.HighRisk -> MaterialTheme.colorScheme.error
        is SizingWarning.CappedAtMaxLot -> MaterialTheme.colorScheme.tertiary
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = "Warning",
            tint = color
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = message,
            fontSize = 12.sp,
            color = color
        )
    }
}