package com.chifuz.enfermerapp.ui.screens.units

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chifuz.enfermerapp.R
import com.chifuz.enfermerapp.ads.AdLocation
import com.chifuz.enfermerapp.ads.AdsManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnitsScreen(
    viewModel: UnitsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }

    LaunchedEffect(Unit) {
        AdsManager.loadInterstitial(context, AdLocation.UNITS)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            text = stringResource(R.string.units_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 24.dp, bottom =  24.dp)
        )
        // 1. Selector de Categoría
        ScrollableTabRow(
            selectedTabIndex = uiState.category.ordinal,
            edgePadding = 8.dp,
            containerColor = Color.Transparent,
            divider = {}
        ) {
            UnitCategory.entries.forEach { cat ->
                Tab(
                    selected = uiState.category == cat,
                    onClick = { viewModel.onCategoryChanged(cat) },
                    text = {
                        Text(
                            text = when (cat) {
                                UnitCategory.PESO -> stringResource(R.string.units_cat_peso)
                                UnitCategory.VOLUMEN -> stringResource(R.string.units_cat_volumen)
                                UnitCategory.INFUSION -> stringResource(R.string.units_cat_infusion)
                                UnitCategory.TEMPERATURA -> stringResource(R.string.units_cat_temp)
                            }
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 2. Input Principal
        OutlinedTextField(
            value = uiState.inputValue,
            onValueChange = { viewModel.onInputChanged(it) },
            label = { Text(stringResource(R.string.units_label_valor)) },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            isError = uiState.error
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 3. Selectores
        UnitSelectors(uiState, viewModel)

        Spacer(modifier = Modifier.height(32.dp))

        // 4. Botón Convertir
        Button(
            onClick = { viewModel.convert() },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(stringResource(R.string.units_btn_convertir), style = MaterialTheme.typography.titleMedium)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // NUEVO: Native Ad con margen
        NativeAdUnitsComponent()

        Spacer(modifier = Modifier.weight(1f))

        // 5. Espacio para el anuncio nativo al final
        Spacer(modifier = Modifier.weight(1f))

        // Aquí llamarás a SmallNativeAd() en el futuro
        Spacer(modifier = Modifier.height(16.dp))
    }

    // DIÁLOGO DE RESULTADO
    if (uiState.showResultDialog) {
        UnitsResultDialog(
            result = uiState.result,
            unit = uiState.toUnit,
            onDismiss = {
                viewModel.hideResultDialog()

                activity?.let { act ->
                    AdsManager.showInterstitial(act, AdLocation.UNITS) {
                        // Flujo después del anuncio
                    }
                }

            }
        )
    }
}

@Composable
fun UnitsResultDialog(result: String, unit: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.aceptar))
            }
        },
        title = {
            Text(
                text = stringResource(R.string.units_result_hint),
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    text = "$result $unit",
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    )
}

@Composable
fun UnitSelectors(uiState: UnitsUiState, viewModel: UnitsViewModel) {
    val options = when (uiState.category) {
        UnitCategory.PESO -> listOf("kg", "g", "mg", "mcg")
        UnitCategory.VOLUMEN -> listOf("L", "mL", "µL")
        UnitCategory.INFUSION -> listOf("mL/h", "gtt/min", "microgtt/min")
        UnitCategory.TEMPERATURA -> listOf("°C", "°F")
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        UnitDropDown(
            label = "Desde",
            selectedUnit = uiState.fromUnit,
            options = options,
            onUnitSelected = { viewModel.onUnitsChanged(it, uiState.toUnit) },
            modifier = Modifier.weight(1f)
        )

        Icon(
            imageVector = Icons.Default.ArrowForward,
            contentDescription = null,
            modifier = Modifier.padding(horizontal = 8.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        UnitDropDown(
            label = "Hasta",
            selectedUnit = uiState.toUnit,
            options = options,
            onUnitSelected = { viewModel.onUnitsChanged(uiState.fromUnit, it) },
            modifier = Modifier.weight(1f)
        )
    }
}

// ... Mantener el componente UnitDropDown igual que lo tenías

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnitDropDown(
    label: String,
    selectedUnit: String,
    options: List<String>,
    onUnitSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedUnit,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor(),
            textStyle = MaterialTheme.typography.bodyMedium
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onUnitSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

// Función de extensión para obtener la Activity desde el contexto de Compose
private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}