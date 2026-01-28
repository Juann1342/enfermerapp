package com.chifuz.enfermerapp.ui.screens.units

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
        // Título unificado
        Text(
            text = stringResource(R.string.units_title),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 24.dp, bottom = 16.dp),
            textAlign = TextAlign.Center
        )

        // Categorías con TabRow refinado
        ScrollableTabRow(
            selectedTabIndex = uiState.category.ordinal,
            edgePadding = 0.dp,
            containerColor = Color.Transparent,
            divider = {},
            indicator = { tabPositions ->
                if (uiState.category.ordinal < tabPositions.size) {
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[uiState.category.ordinal]),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
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
                            },
                            style = if (uiState.category == cat)
                                MaterialTheme.typography.labelLarge
                            else
                                MaterialTheme.typography.labelMedium
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Input Principal con el estilo unificado
        OutlinedTextField(
            value = uiState.inputValue,
            onValueChange = { viewModel.onInputChanged(it) },
            label = { Text(stringResource(R.string.units_label_valor)) },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            isError = uiState.error,
            shape = MaterialTheme.shapes.medium
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Selectores de unidades
        UnitSelectors(uiState, viewModel)

        Spacer(modifier = Modifier.height(32.dp))

        // Botón Convertir con Icono y Uppercase
        Button(
            onClick = { viewModel.convert() },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = MaterialTheme.shapes.large
        ) {
            Icon(Icons.Default.CheckCircle, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.units_btn_convertir).uppercase())
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Native Ad con su espacio respetado
        NativeAdUnitsComponent()

        Spacer(modifier = Modifier.height(16.dp))
    }

    if (uiState.showResultDialog) {
        UnitsResultDialog(
            result = uiState.result,
            unit = uiState.toUnit,
            onDismiss = {
                viewModel.hideResultDialog()
                activity?.let { act ->
                    AdsManager.showInterstitial(act, AdLocation.UNITS) {}
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
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "$result $unit",
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
            }
        }
    )
}

// ... UnitSelectors y UnitDropDown se mantienen con su lógica pero usando MaterialTheme.shapes.medium
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