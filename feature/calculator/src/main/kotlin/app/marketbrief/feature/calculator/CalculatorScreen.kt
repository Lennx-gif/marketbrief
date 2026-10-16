package app.marketbrief.feature.calculator

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
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
                .padding(horizontal = 16.dp),
            enabled = !uiState.isCalculating
        ) {
            if (uiState.isCalculating) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Calculate Position Size", fontSize = 16.sp)
            }
        }

        uiState.result?.let { result ->
            CalculatorResultCard(result = result)
        }

        uiState.error?.let { error ->
            ErrorMessage(message = error)
        }
    }
}

@Composable
fun ErrorMessage(message: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = message, color = MaterialTheme.colorScheme.onErrorContainer)
        }
    }
}