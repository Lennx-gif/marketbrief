package app.marketbrief.feature.calculator

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.marketbrief.core.ui.components.AppleBanner
import app.marketbrief.feature.calculator.ui.CalculatorInputSection
import app.marketbrief.feature.calculator.ui.CalculatorResultCard
import app.marketbrief.feature.calculator.ui.CalculatorViewModel
import app.marketbrief.feature.calculator.ui.InstrumentPicker

@Composable
fun CalculatorScreen(
    initialSymbol: String = "",
    viewModel: CalculatorViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(initialSymbol) {
        if (initialSymbol.isNotBlank()) {
            viewModel.onInstrumentSelected(initialSymbol)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Position Sizer",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.8).sp,
                color = MaterialTheme.colorScheme.onBackground
            )

            InstrumentPicker(
                selectedInstrument = uiState.selectedInstrument,
                onInstrumentSelected = { viewModel.onInstrumentSelected(it) },
                instruments = uiState.availableInstruments
            )

            CalculatorInputSection(
                uiState = uiState,
                onBalanceChange = { viewModel.onBalanceChange(it) },
                onAccountCurrencyChange = { viewModel.onAccountCurrencyChange(it) },
                onRiskTypeChange = { viewModel.onRiskTypeChange(it) },
                onRiskValueChange = { viewModel.onRiskValueChange(it) },
                onDirectionChange = { viewModel.onDirectionChange(it) },
                onEntryChange = { viewModel.onEntryChange(it) },
                onStopChange = { viewModel.onStopChange(it) },
                onStopTypeChange = { viewModel.onStopTypeChange(it) },
                onTakeProfitChange = { viewModel.onTakeProfitChange(it) },
                onTakeProfitTypeChange = { viewModel.onTakeProfitTypeChange(it) },
                onLeverageChange = { viewModel.onLeverageChange(it) },
                onRateOverrideChange = { viewModel.onRateOverrideChange(it) }
            )

            Button(
                onClick = { viewModel.calculate() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                enabled = !uiState.isCalculating,
                shape = RoundedCornerShape(100.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                if (uiState.isCalculating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(
                        text = "Calculate Position Size",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            uiState.result?.let { result ->
                CalculatorResultCard(result = result)
            }

            uiState.error?.let { error ->
                AppleBanner(message = error, isWarning = true)
            }
        }
    }
}