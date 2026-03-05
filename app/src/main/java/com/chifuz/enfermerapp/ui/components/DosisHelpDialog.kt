package com.chifuz.enfermerapp.ui.components


import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.chifuz.enfermerapp.R
import com.chifuz.enfermerapp.ui.screens.dosis.DosisCalcType

@Composable
fun DosisHelpDialog(calcType: DosisCalcType, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = stringResource(R.string.help_dosis_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.ExtraBold
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (calcType == DosisCalcType.ESTANDAR) {
                    Text(text = stringResource(R.string.help_std_intro), style = MaterialTheme.typography.bodyMedium)

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = stringResource(R.string.help_std_terms_header), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)

                    // Aquí usamos los nuevos strings de términos
                    Text(text = stringResource(R.string.help_std_term_dosis), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 4.dp))
                    Text(text = stringResource(R.string.help_std_term_volumen), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 4.dp))
                    Text(text = stringResource(R.string.help_std_term_concentracion), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 4.dp))

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = stringResource(R.string.help_std_formula_header), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                    FormulaCard(stringResource(R.string.help_std_formula_body))

                } else {
                    Text(text = stringResource(R.string.help_weight_intro), style = MaterialTheme.typography.bodyMedium)

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = "Componentes", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)

                    Text(text = stringResource(R.string.help_weight_term_dosis), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 4.dp))
                    Text(text = stringResource(R.string.help_weight_term_peso), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 4.dp))
                    Text(text = stringResource(R.string.help_weight_term_max), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 4.dp))

                    Spacer(modifier = Modifier.height(16.dp))
                    FormulaCard(stringResource(R.string.help_weight_formula_body))
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(stringResource(R.string.help_btn_ok), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun HelpSection(title: String, body: String) {
    Text(text = title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
    Spacer(modifier = Modifier.height(4.dp))
    Text(text = body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
    Spacer(modifier = Modifier.height(16.dp))
}

@Composable
fun FormulaCard(formula: String) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
    ) {
        Text(
            text = formula,
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}