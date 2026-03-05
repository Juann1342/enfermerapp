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

@Composable
fun PerfusionHelpDialog(onDismiss: () -> Unit) {
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
                    text = stringResource(R.string.help_perfusion_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.ExtraBold
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(text = stringResource(R.string.help_perfusion_intro), style = MaterialTheme.typography.bodyMedium)

                Spacer(modifier = Modifier.height(16.dp))
                Text(text = stringResource(R.string.help_perfusion_factors_header), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                Text(text = stringResource(R.string.help_perfusion_factor_macro), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 4.dp))
                Text(text = stringResource(R.string.help_perfusion_factor_micro), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 4.dp))

                Spacer(modifier = Modifier.height(16.dp))
                Text(text = stringResource(R.string.help_perfusion_results_header), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                Text(text = stringResource(R.string.help_perfusion_res_gtts), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 2.dp))
                Text(text = stringResource(R.string.help_perfusion_res_mlhr), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 2.dp))
                Text(text = stringResource(R.string.help_perfusion_res_mlmin), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 2.dp))

                Spacer(modifier = Modifier.height(16.dp))
                Text(text = stringResource(R.string.help_perfusion_formula_header), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)

                // Reutilizamos la estética de la tarjeta de fórmula
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.help_perfusion_formula_body),
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(text = stringResource(R.string.help_perfusion_sync_header), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                Text(text = stringResource(R.string.help_perfusion_sync_body), style = MaterialTheme.typography.bodySmall)

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